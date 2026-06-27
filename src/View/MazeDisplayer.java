package View;

import algorithms.mazeGenerators.Maze;
import algorithms.mazeGenerators.Position;
import algorithms.search.AState;
import algorithms.search.MazeState;
import algorithms.search.Solution;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.BlendMode;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

/** Custom Canvas control that renders maze state and visual overlays. */
public class MazeDisplayer extends Canvas {
    private static final double BACKGROUND_OPACITY = 1.0;
    private static final double PATH_OPACITY = 0.70;
    private static final double WALL_OVERLAP_PIXELS = 0.35;
    private static final double CHARACTER_SCALE = 1.6;  // keeps character readable in small cells
    private static final double GOAL_MARKER_SCALE = 1.45;
    private static final double SOLUTION_PATH_OPACITY = 0.62;
    private static final Color PATH_COLOR = Color.rgb(244, 232, 195, PATH_OPACITY);
    private static final Color PATH_SHADOW_COLOR = Color.rgb(192, 176, 125, 0.10);
    private static final Color SOLUTION_COLOR = Color.rgb(255, 156, 184, SOLUTION_PATH_OPACITY);
    private static final Color GOAL_MARKER_COLOR = Color.rgb(255, 206, 222, 0.42);
    private static final Color GOAL_MARKER_STROKE = Color.rgb(138, 78, 105, 0.55);

    private Maze maze;
    private Position playerPosition;
    private Solution solution;
    private double zoomFactor = 1.0;

    private final Image wallImage       = loadImage("/Images/wall.png");
    private final Image backgroundImage = loadImage("/Images/background.jpg");

    /* Default character images are replaced when a couple is selected. */
    private Image playerImage = loadImage("/Images/bride.png");  // default: bride moves
    private Image goalImage   = loadImage("/Images/groom.png");  // default: groom waits

    /* Flower image used to mark visited cells. */
    private final Image flowerImage = loadImage("/Images/flowers.png");
    /* Ordered cell history lets backtracking remove later trail markers. */
    private final List<String> pathHistory = new ArrayList<>();

    public MazeDisplayer() {
        /* Canvas needs explicit redraws after resize events. */
        widthProperty().addListener((observable, oldValue, newValue) -> redraw());
        heightProperty().addListener((observable, oldValue, newValue) -> redraw());
    }

    /** Sets the maze and clears visual state from any previous maze. */
    public void setMaze(Maze maze) {
        this.maze = maze;
        this.solution = null;
        pathHistory.clear();
        redraw();
    }

    /** Updates the player position and keeps the trail history consistent. */
    public void setPlayerPosition(Position playerPosition) {
        this.playerPosition = playerPosition;
        if (playerPosition != null) {
            String key = playerPosition.getRowIndex() + "," + playerPosition.getColumnIndex();
            int existingIndex = pathHistory.indexOf(key);
            if (existingIndex >= 0) {
                // Backtracking removes cells after the revisited position.
                pathHistory.subList(existingIndex + 1, pathHistory.size()).clear();
            } else {
                // New cells extend the visible trail.
                pathHistory.add(key);
            }
        }
        redraw();
    }

    /** Stores the solution path to paint over the maze. */
    public void setSolution(Solution solution) {
        this.solution = solution;
        redraw();
    }

    /** Removes the visible solution path without changing maze or player state. */
    public void clearSolution() {
        this.solution = null;
        redraw();
    }

    /** Applies the selected player and goal sprites. */
    public void setCouple(CoupleType couple) {
        if (couple == null) return;
        Image loadedPlayer = loadImage(couple.getPlayerImagePath());
        Image loadedGoal   = loadImage(couple.getGoalImagePath());
        if (loadedPlayer != null) playerImage = loadedPlayer;
        if (loadedGoal   != null) goalImage   = loadedGoal;
        redraw();
    }

    /** Redraws the full board from the current state. */
    public void redraw() {
        GraphicsContext graphicsContext = getGraphicsContext2D();
        double width = getWidth();
        double height = getHeight();

        graphicsContext.setImageSmoothing(true);
        graphicsContext.clearRect(0, 0, width, height);

        if (width <= 0 || height <= 0) {
            return;
        }

        graphicsContext.save();
        applyZoom(graphicsContext, width, height);
        drawBackground(graphicsContext, width, height);

        if (maze == null) {
            graphicsContext.restore();
            return;
        }

        double cellWidth = width / maze.getColumns();
        double cellHeight = height / maze.getRows();

        drawMazeCells(graphicsContext, cellWidth, cellHeight);
        drawFlowerTrail(graphicsContext, cellWidth, cellHeight);  // trail sits above path, below solution
        drawSolution(graphicsContext, cellWidth, cellHeight);
        drawGoal(graphicsContext, cellWidth, cellHeight);
        drawPlayer(graphicsContext, cellWidth, cellHeight);
        graphicsContext.restore();
    }

    /** Sets visual zoom while keeping the canvas responsive to window resizing. */
    public void setZoomFactor(double zoomFactor) {
        this.zoomFactor = zoomFactor;
        redraw();
    }

    private void applyZoom(GraphicsContext graphicsContext, double width, double height) {
        graphicsContext.translate(width / 2.0, height / 2.0);
        graphicsContext.scale(zoomFactor, zoomFactor);
        graphicsContext.translate(-width / 2.0, -height / 2.0);
    }

    private Image loadImage(String resourcePath) {
        if (getClass().getResourceAsStream(resourcePath) == null) {
            return null;
        }
        return new Image(getClass().getResourceAsStream(resourcePath));
    }

    private void drawBackground(GraphicsContext graphicsContext, double width, double height) {
        if (backgroundImage != null) {
            graphicsContext.save();
            graphicsContext.setGlobalAlpha(BACKGROUND_OPACITY);
            drawImageCover(graphicsContext, backgroundImage, 0, 0, width, height);
            graphicsContext.restore();
        } else {
            graphicsContext.setFill(Color.web("#eef6dc"));
            graphicsContext.fillRect(0, 0, width, height);
        }
    }

    /** Draws an image cropped to cover the target area without stretching. */
    private void drawImageCover(GraphicsContext graphicsContext, Image image,
                                double targetX, double targetY, double targetWidth, double targetHeight) {
        double imageWidth = image.getWidth();
        double imageHeight = image.getHeight();
        double imageRatio = imageWidth / imageHeight;
        double targetRatio = targetWidth / targetHeight;

        double sourceX = 0;
        double sourceY = 0;
        double sourceWidth = imageWidth;
        double sourceHeight = imageHeight;

        if (imageRatio > targetRatio) {
            sourceWidth = imageHeight * targetRatio;
            sourceX = (imageWidth - sourceWidth) / 2;
        } else {
            sourceHeight = imageWidth / targetRatio;
            sourceY = (imageHeight - sourceHeight) / 2;
        }

        graphicsContext.drawImage(
                image,
                sourceX, sourceY, sourceWidth, sourceHeight,
                targetX, targetY, targetWidth, targetHeight
        );
    }

    private void drawMazeCells(GraphicsContext graphicsContext, double cellWidth, double cellHeight) {
        for (int row = 0; row < maze.getRows(); row++) {
            for (int column = 0; column < maze.getColumns(); column++) {
                double x = column * cellWidth;
                double y = row * cellHeight;

                if (maze.getCellValue(row, column) == 1) {
                    drawWall(graphicsContext, x, y, cellWidth, cellHeight);
                } else {
                    drawPath(graphicsContext, x, y, cellWidth, cellHeight);
                }
            }
        }
    }

    private void drawWall(GraphicsContext graphicsContext, double x, double y, double width, double height) {
        double wallX = x - WALL_OVERLAP_PIXELS;
        double wallY = y - WALL_OVERLAP_PIXELS;
        double wallWidth = width + 2 * WALL_OVERLAP_PIXELS;
        double wallHeight = height + 2 * WALL_OVERLAP_PIXELS;

        // Large mazes use a solid wall color for clearer rendering.
        boolean cellTooSmall = maze.getRows() > 50 || maze.getColumns() > 50;

        if (wallImage != null && !cellTooSmall) {
            graphicsContext.drawImage(wallImage, wallX, wallY, wallWidth, wallHeight);
            return;
        }

        graphicsContext.setFill(Color.web("#456f2a"));
        graphicsContext.fillRect(wallX, wallY, wallWidth, wallHeight);
    }

    private void drawPath(GraphicsContext graphicsContext, double x, double y, double width, double height) {
        graphicsContext.setFill(PATH_COLOR);
        graphicsContext.fillRect(x, y, width, height);
        double padding = Math.min(width, height) * 0.18;
        graphicsContext.setFill(PATH_SHADOW_COLOR);
        graphicsContext.fillRoundRect(
                x + padding,
                y + padding,
                Math.max(0, width - 2 * padding),
                Math.max(0, height - 2 * padding),
                width * 0.28,
                height * 0.28
        );
    }

    /** Paints flowers on visited cells, removing later markers after backtracking. */
    private void drawFlowerTrail(GraphicsContext gc, double cellWidth, double cellHeight) {
        if (flowerImage == null || pathHistory.isEmpty()) {
            return;
        }

        double padFraction = 0.09;           // Keep flowers slightly smaller than cells.
        double padW = cellWidth  * padFraction;
        double padH = cellHeight * padFraction;
        double drawW = cellWidth  - 2 * padW;
        double drawH = cellHeight - 2 * padH;

        gc.save();
        gc.setGlobalAlpha(0.85);
        /* MULTIPLY hides the white background in flowers.png. */
        gc.setGlobalBlendMode(BlendMode.MULTIPLY);
        gc.setImageSmoothing(true);

        for (String key : pathHistory) {
            int comma = key.indexOf(',');
            int row = Integer.parseInt(key.substring(0, comma));
            int col = Integer.parseInt(key.substring(comma + 1));
            gc.drawImage(flowerImage,
                    col * cellWidth  + padW,
                    row * cellHeight + padH,
                    drawW,
                    drawH);
        }

        gc.restore();
    }

    private void drawSolution(GraphicsContext graphicsContext, double cellWidth, double cellHeight) {
        if (solution == null) {
            return;
        }

        graphicsContext.setFill(SOLUTION_COLOR);
        for (AState state : solution.getSolutionPath()) {
            if (state instanceof MazeState mazeState) {
                Position position = mazeState.getPosition();
                double padding = Math.min(cellWidth, cellHeight) * 0.33;
                graphicsContext.fillOval(
                        position.getColumnIndex() * cellWidth + padding,
                        position.getRowIndex() * cellHeight + padding,
                        cellWidth - 2 * padding,
                        cellHeight - 2 * padding
                );
            }
        }
    }

    private void drawGoal(GraphicsContext graphicsContext, double cellWidth, double cellHeight) {
        drawGoalMarker(graphicsContext, maze.getGoalPosition(), cellWidth, cellHeight);
        drawCharacter(graphicsContext, goalImage, maze.getGoalPosition(), cellWidth, cellHeight, Color.web("#7a4b8f"));
    }

    private void drawPlayer(GraphicsContext graphicsContext, double cellWidth, double cellHeight) {
        if (playerPosition == null) {
            return;
        }
        drawCharacter(graphicsContext, playerImage, playerPosition, cellWidth, cellHeight, Color.web("#f6a6bf"));
    }

    private void drawCharacter(GraphicsContext graphicsContext, Image image, Position position,
                               double cellWidth, double cellHeight, Color fallbackColor) {
        double x = position.getColumnIndex() * cellWidth;
        double y = position.getRowIndex() * cellHeight;
        double side = Math.min(cellWidth, cellHeight) * CHARACTER_SCALE;
        double drawX = x + (cellWidth - side) / 2;
        double drawY = y + (cellHeight - side) / 2;

        if (image != null) {
            graphicsContext.drawImage(
                    image,
                    drawX,
                    drawY,
                    side,
                    side
            );
            return;
        }

        graphicsContext.setFill(fallbackColor);
        graphicsContext.fillOval(drawX, drawY, side, side);
    }

    /** Converts a canvas x coordinate to a maze column. */
    public int getColumnForX(double x) {
        if (maze == null || getWidth() <= 0) {
            return -1;
        }
        double boardX = toUnzoomedX(x);
        if (boardX < 0 || boardX >= getWidth()) {
            return -1;
        }
        int column = (int) (boardX / (getWidth() / maze.getColumns()));
        return Math.max(0, Math.min(column, maze.getColumns() - 1));
    }

    /** Converts a canvas y coordinate to a maze row. */
    public int getRowForY(double y) {
        if (maze == null || getHeight() <= 0) {
            return -1;
        }
        double boardY = toUnzoomedY(y);
        if (boardY < 0 || boardY >= getHeight()) {
            return -1;
        }
        int row = (int) (boardY / (getHeight() / maze.getRows()));
        return Math.max(0, Math.min(row, maze.getRows() - 1));
    }

    private double toUnzoomedX(double x) {
        double centerX = getWidth() / 2.0;
        return centerX + (x - centerX) / zoomFactor;
    }

    private double toUnzoomedY(double y) {
        double centerY = getHeight() / 2.0;
        return centerY + (y - centerY) / zoomFactor;
    }

    private void drawGoalMarker(GraphicsContext graphicsContext, Position position, double cellWidth, double cellHeight) {
        double x = position.getColumnIndex() * cellWidth;
        double y = position.getRowIndex() * cellHeight;
        double side = Math.min(cellWidth, cellHeight) * GOAL_MARKER_SCALE;
        double markerX = x + (cellWidth - side) / 2;
        double markerY = y + (cellHeight - side) / 2;

        graphicsContext.setFill(GOAL_MARKER_COLOR);
        graphicsContext.fillOval(markerX, markerY, side, side);
        graphicsContext.setStroke(GOAL_MARKER_STROKE);
        graphicsContext.setLineWidth(Math.max(1.0, side * 0.035));
        graphicsContext.strokeOval(markerX, markerY, side, side);
    }
}
