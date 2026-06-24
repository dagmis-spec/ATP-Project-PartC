package View;

import algorithms.mazeGenerators.Maze;
import algorithms.mazeGenerators.Position;
import algorithms.search.AState;
import algorithms.search.MazeState;
import algorithms.search.Solution;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

/**
 * Reusable JavaFX control that draws the maze board.
 *
 * This class belongs to the View layer because it only handles presentation:
 * wall graphics, path cells, player image, goal image, and solution overlay.
 * It does not generate mazes, solve mazes, or decide if movement is legal.
 */
public class MazeDisplayer extends Canvas {
    private static final double BACKGROUND_OPACITY = 1.0;
    private static final double PATH_OPACITY = 0.70;
    private static final double WALL_OVERLAP_PIXELS = 0.35;
    private static final double CHARACTER_SCALE = 2.25;
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

    private final Image wallImage = loadImage("/Images/wall.png");
    private final Image brideImage = loadImage("/Images/bride.png");
    private final Image groomImage = loadImage("/Images/groom.png");
    private final Image backgroundImage = loadImage("/Images/background.jpg");

    public MazeDisplayer() {
        /*
         * Canvas does not redraw automatically when its size changes.
         * These listeners keep the board responsive when the window is resized.
         */
        widthProperty().addListener((observable, oldValue, newValue) -> redraw());
        heightProperty().addListener((observable, oldValue, newValue) -> redraw());
    }

    /**
     * Sets the maze to draw and clears old visual state that belongs to another maze.
     */
    public void setMaze(Maze maze) {
        this.maze = maze;
        this.solution = null;
        redraw();
    }

    /**
     * Updates the player location and redraws the bride image in the new cell.
     */
    public void setPlayerPosition(Position playerPosition) {
        this.playerPosition = playerPosition;
        redraw();
    }

    /**
     * Stores the solution path that should be painted over the maze.
     */
    public void setSolution(Solution solution) {
        this.solution = solution;
        redraw();
    }

    /**
     * Draws the full board according to the current maze state.
     */
    public void redraw() {
        GraphicsContext graphicsContext = getGraphicsContext2D();
        double width = getWidth();
        double height = getHeight();

        graphicsContext.setImageSmoothing(true);
        graphicsContext.clearRect(0, 0, width, height);

        if (width <= 0 || height <= 0) {
            return;
        }

        if (maze == null) {
            drawBackground(graphicsContext, width, height);
            return;
        }

        drawBackground(graphicsContext, width, height);

        double cellWidth = width / maze.getColumns();
        double cellHeight = height / maze.getRows();

        drawMazeCells(graphicsContext, cellWidth, cellHeight);
        drawSolution(graphicsContext, cellWidth, cellHeight);
        drawGoal(graphicsContext, cellWidth, cellHeight);
        drawPlayer(graphicsContext, cellWidth, cellHeight);
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

        // No white overlay here: the background should stay clear and natural.
    }

    /**
     * Draws an image like CSS background-size: cover.
     *
     * The image fills the whole target area without stretching. If the image ratio is
     * different from the board ratio, the extra part is cropped from the center.
     */
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

        // Use the image only for mazes up to 50×50; larger mazes get a solid color.
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

        // Very soft center highlight gives paths a calm garden-floor feel.
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
        drawCharacter(graphicsContext, groomImage, maze.getGoalPosition(), cellWidth, cellHeight, Color.web("#7a4b8f"));
    }

    private void drawPlayer(GraphicsContext graphicsContext, double cellWidth, double cellHeight) {
        if (playerPosition == null) {
            return;
        }
        drawCharacter(graphicsContext, brideImage, playerPosition, cellWidth, cellHeight, Color.web("#f6a6bf"));
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

    /**
     * Converts a mouse x coordinate on the canvas into a maze column.
     */
    public int getColumnForX(double x) {
        if (maze == null || getWidth() <= 0) {
            return -1;
        }
        int column = (int) (x / (getWidth() / maze.getColumns()));
        return Math.max(0, Math.min(column, maze.getColumns() - 1));
    }

    /**
     * Converts a mouse y coordinate on the canvas into a maze row.
     */
    public int getRowForY(double y) {
        if (maze == null || getHeight() <= 0) {
            return -1;
        }
        int row = (int) (y / (getHeight() / maze.getRows()));
        return Math.max(0, Math.min(row, maze.getRows() - 1));
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
