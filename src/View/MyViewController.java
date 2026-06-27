package View;

import ViewModel.MyViewModel;
import algorithms.mazeGenerators.Maze;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Observable;
import java.util.Observer;

/**
 * Game screen controller. Binds UI controls to the ViewModel and forwards user actions.
 */
public class MyViewController implements IView, Observer {
    private static final DateTimeFormatter SOLVED_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final double MIN_ZOOM = 0.5;
    private static final double MAX_ZOOM = 2.5;
    private static final double ZOOM_STEP = 0.1;

    private MyViewModel viewModel;

    // Root node used to keep the background image fitted to the window.
    @FXML private StackPane gameRoot;
    @FXML private ImageView gameBackgroundView;

    @FXML private Label statusLabel;
    @FXML private Label mazeInfoLabel;
    @FXML private Label soundIcon;       // music play/pause icon inside btnSound's graphic
    @FXML private Button btnNewMaze;
    @FXML private Button btnSolve;
    @FXML private Button btnSave;
    @FXML private Button btnSound;
    @FXML private MenuItem menuSave;
    @FXML private StackPane mazeContainer;

    private MazeDisplayer mazeDisplayer;
    private MediaPlayer trailSoundPlayer;
    private MediaPlayer endSoundPlayer;
    private boolean trailSoundEnabled = true;
    private boolean solutionVisible = false;
    private boolean initialInstructionsShown = false;
    private CoupleType selectedCouple = CoupleType.BRIDE_GROOM;
    private double currentZoom = 1.0;

    /** Creates view-only controls and default UI state. */
    @FXML
    private void initialize() {
        statusLabel.setText("Ready to generate a maze.");
        mazeInfoLabel.setText("No maze loaded.");

        // Stretch the game background photo to fill the whole window
        gameBackgroundView.fitWidthProperty().bind(gameRoot.widthProperty());
        gameBackgroundView.fitHeightProperty().bind(gameRoot.heightProperty());
        var bgUrl = getClass().getResource("/Images/background.jpg");
        if (bgUrl != null) gameBackgroundView.setImage(new Image(bgUrl.toExternalForm()));

        mazeDisplayer = new MazeDisplayer();
        /*
         * The canvas follows its parent size without contributing its own preferred size.
         */
        mazeDisplayer.setManaged(false);
        mazeDisplayer.widthProperty().bind(mazeContainer.widthProperty());
        mazeDisplayer.heightProperty().bind(mazeContainer.heightProperty());
        mazeContainer.setFocusTraversable(true);
        mazeContainer.getChildren().add(mazeDisplayer);
        initializeSoundPlayers();
        registerMouseMovement();
        registerZoomControls();
        applyCustomToolbarIcons();
    }

    /** Replaces text placeholders with drawn toolbar icons. */
    private void applyCustomToolbarIcons() {
        replaceToolbarIcon(btnNewMaze, ToolbarIconFactory.plusIcon(42), "new maze");
        replaceToolbarIcon(btnSolve,   ToolbarIconFactory.questionIcon(42), "solve");
    }

    private void replaceToolbarIcon(Button btn, Canvas icon, String text) {
        if (btn == null) return;
        Label lbl = new Label(text);
        lbl.getStyleClass().add("toolbar-text");
        VBox box = new VBox(3, icon, lbl);
        box.setAlignment(Pos.CENTER);
        btn.setGraphic(box);
    }

    /** Connects UI controls to ViewModel properties and notifications. */
    public void setViewModel(MyViewModel viewModel) {
        this.viewModel = viewModel;

        statusLabel.textProperty().bind(viewModel.statusProperty());
        btnSolve.disableProperty().bind(viewModel.mazeLoadedProperty().not());
        btnSave.disableProperty().bind(viewModel.mazeLoadedProperty().not());
        menuSave.disableProperty().bind(viewModel.mazeLoadedProperty().not());

        viewModel.mazeProperty().addListener((observable, oldMaze, newMaze) -> {
            if (newMaze != null) {
                mazeDisplayer.setMaze(newMaze);
                displayMaze(toGrid(newMaze));
            }
        });
        viewModel.playerPositionProperty().addListener((observable, oldPosition, newPosition) ->
                mazeDisplayer.setPlayerPosition(newPosition));
        viewModel.solutionProperty().addListener((observable, oldSolution, newSolution) ->
                mazeDisplayer.setSolution(newSolution));
        viewModel.mazeSolvedProperty().addListener((observable, wasSolved, isSolved) -> {
            if (!wasSolved && isSolved) {
                showMazeSolvedMessage();
            }
        });

        Platform.runLater(() -> {
            registerKeyboardMovement();
            showInitialInstructions();
        });
    }

    /** Applies ViewModel notifications on the JavaFX application thread. */
    @Override
    public void update(Observable o, Object arg) {
        Platform.runLater(() -> {
            if (arg instanceof String command) {
                switch (command) {
                    case "generateMaze", "loadMaze" -> {
                        Maze newMaze = viewModel.mazeProperty().get();
                        if (newMaze != null) {
                            mazeDisplayer.setMaze(newMaze);
                            displayMaze(toGrid(newMaze));
                        }
                    }
                    case "solveMaze" ->
                        mazeDisplayer.setSolution(viewModel.solutionProperty().get());
                    case "movePlayer" ->
                        mazeDisplayer.setPlayerPosition(viewModel.playerPositionProperty().get());
                }
            }
        });
    }

    /** Opens the new-maze dialog and generates a maze when input is valid. */
    @FXML
    private void onNewMaze() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/View/NewMazeDialog.fxml"));
            Scene dialogScene = new Scene(loader.load());
            dialogScene.getStylesheets().add(
                    getClass().getResource("/View/app.css").toExternalForm());

            Stage dialogStage = new Stage();
            dialogStage.setTitle("New Maze");
            dialogStage.setScene(dialogScene);
            dialogStage.setResizable(false);
            dialogStage.setMaximized(false);
            dialogStage.setWidth(440);
            dialogStage.setHeight(330);
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(getWindow());

            NewMazeDialogController dialogController = loader.getController();
            dialogController.setDialogStage(dialogStage);

            dialogStage.centerOnScreen();
            dialogStage.showAndWait();

            int rows = dialogController.getResultRows();
            int cols = dialogController.getResultCols();

            if (rows > 0 && cols > 0) {
                viewModel.generateMaze(rows, cols);
                resetSolveButton();
                playTrailSound();
                mazeContainer.requestFocus();
            }
        } catch (IOException e) {
            displayMessage("Error", "Could not open the new-maze dialog: " + e.getMessage());
        } catch (RuntimeException e) {
            displayMessage("Maze Error", e.getMessage());
        }
    }

    /** Saves the active maze to a user-selected file. */
    @FXML
    private void onSaveMaze() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Maze");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Maze Files", "*.maze"));

        File file = fileChooser.showSaveDialog(getWindow());
        if (file == null) {
            return;
        }

        try {
            viewModel.saveMaze(file);
        } catch (IOException | RuntimeException e) {
            displayMessage("Save Error", e.getMessage());
        }
    }

    /** Loads a maze from a user-selected file. */
    @FXML
    private void onLoadMaze() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Load Maze");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Maze Files", "*.maze"));

        File file = fileChooser.showOpenDialog(getWindow());
        if (file == null) {
            return;
        }

        try {
            viewModel.loadMaze(file);
            resetSolveButton();
            playTrailSound();
            mazeContainer.requestFocus();
        } catch (IOException | RuntimeException e) {
            displayMessage("Load Error", e.getMessage());
        }
    }

    /** Toggles the solution path on the active maze. */
    @FXML
    private void onSolveMaze() {
        try {
            if (solutionVisible) {
                mazeDisplayer.clearSolution();
                solutionVisible = false;
                replaceToolbarIcon(btnSolve, ToolbarIconFactory.questionIcon(42), "solve");
            } else {
                viewModel.solveMaze();
                mazeDisplayer.setSolution(viewModel.solutionProperty().get());
                solutionVisible = true;
                replaceToolbarIcon(btnSolve, ToolbarIconFactory.hideSolutionIcon(42), "hide");
            }
        } catch (RuntimeException e) {
            displayMessage("Solve Error", e.getMessage());
        }
    }

    /** Restores the Solve button to its initial state for a new or loaded maze. */
    private void resetSolveButton() {
        solutionVisible = false;
        if (mazeDisplayer != null) {
            mazeDisplayer.clearSolution();
        }
        replaceToolbarIcon(btnSolve, ToolbarIconFactory.questionIcon(42), "solve");
    }

    /** Shows the current configuration values. */
    @FXML
    private void onProperties() {
        displayMessage("Properties", viewModel.getPropertiesText());
    }

    @FXML
    private void onHelp() {
        showInstructionsWindow();
    }

    @FXML
    private void onAbout() {
        displayMessage("About",
                "ATP Maze Game\n"
                        + "Programmers: Hadas Tourgeman, Shoval Dagmi\n"
                        + "Generation: MyMazeGenerator\n"
                        + "Solver: configured in config.properties");
    }

    /** Applies the selected character images to the maze control. */
    public void setCouple(CoupleType couple) {
        if (couple != null) {
            selectedCouple = couple;
        }
        if (mazeDisplayer != null) {
            mazeDisplayer.setCouple(couple);
        }
    }

    /** Stops sounds, shuts down the model, and exits the application. */
    @FXML
    public void onExit() {
        stopAllSounds();
        if (viewModel != null) {
            viewModel.shutdown();
        }
        Platform.exit();
    }

    /** Updates maze metadata shown in the status area. */
    @Override
    public void displayMaze(int[][] grid) {
        mazeInfoLabel.setText("Maze ready: " + grid.length + " × " + grid[0].length);
        mazeDisplayer.setPlayerPosition(viewModel.playerPositionProperty().get());
    }

    /** Converts the maze object to the grid format used by IView. */
    private int[][] toGrid(Maze maze) {
        int rows = maze.getRows();
        int cols = maze.getColumns();
        int[][] grid = new int[rows][cols];
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                grid[r][c] = maze.getCellValue(r, c);
        return grid;
    }

    /** Shows a modal message dialog. */
    @Override
    public void displayMessage(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /** Shows the game instructions once when the game screen is first opened. */
    private void showInitialInstructions() {
        if (!initialInstructionsShown) {
            initialInstructionsShown = true;
            showInstructionsWindow();
        }
    }

    /** Opens a styled instructions window that matches the New Maze dialog design. */
    private void showInstructionsWindow() {
        Stage instructionsStage = new Stage();
        instructionsStage.setTitle("How to Play");
        instructionsStage.initModality(Modality.WINDOW_MODAL);
        instructionsStage.initOwner(getWindow());
        instructionsStage.setResizable(false);
        instructionsStage.setWidth(620);
        instructionsStage.setHeight(650);

        StackPane root = new StackPane();
        root.setPrefSize(600, 610);

        ImageView background = new ImageView();
        background.setPreserveRatio(false);
        background.setSmooth(true);
        background.fitWidthProperty().bind(root.widthProperty());
        background.fitHeightProperty().bind(root.heightProperty());
        var backgroundUrl = getClass().getResource("/Images/wallpaper2.png");
        if (backgroundUrl == null) {
            backgroundUrl = getClass().getResource("/Images/background.jpg");
        }
        if (backgroundUrl != null) {
            background.setImage(new Image(backgroundUrl.toExternalForm()));
        }

        Region overlay = new Region();
        overlay.getStyleClass().add("welcome-overlay");

        Label title = new Label("How to Play");
        title.getStyleClass().add("dialog-title");

        Label instructions = new Label(getInstructionsText());
        instructions.getStyleClass().add("instructions-text");
        instructions.setWrapText(true);
        instructions.setMaxWidth(500);

        ScrollPane instructionsScroll = new ScrollPane(instructions);
        instructionsScroll.getStyleClass().add("instructions-scroll");
        instructionsScroll.setFitToWidth(true);
        instructionsScroll.setMaxWidth(520);
        instructionsScroll.setPrefViewportHeight(390);

        Button okButton = new Button("OK");
        okButton.getStyleClass().add("welcome-start-btn");
        okButton.setOnAction(event -> instructionsStage.close());

        VBox content = new VBox(18, title, instructionsScroll, okButton);
        content.setAlignment(Pos.CENTER);
        content.setMaxWidth(500);
        content.setPadding(new Insets(34, 42, 34, 42));

        root.getChildren().addAll(background, overlay, content);

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/View/app.css").toExternalForm());
        instructionsStage.setScene(scene);
        instructionsStage.centerOnScreen();
        instructionsStage.showAndWait();
    }

    private String getInstructionsText() {
        String goalText = switch (selectedCouple) {
            case BRIDE_GROOM -> "Move the bride through the garden maze until she reaches the groom.";
            case BRIDE_BRIDE -> "Move the first bride through the garden maze until she reaches the second bride.";
            case GROOM_GROOM -> "Move the first groom through the garden maze until he reaches the second groom.";
        };

        return """
                Goal:
                %s

                Movement:
                Use the NumPad keys:
                4 / 6 / 8 / 2 - left, right, up, down
                7 / 9 / 1 / 3 - diagonal movement
                You can also click or drag with the mouse on the maze.
                Ctrl + mouse wheel, or +/- keys, zooms the maze.

                Toolbar buttons:
                New Maze - opens the maze-size window. Choose rows and columns, then create a new maze.
                Save - saves the current maze to a .maze file on your computer. It is enabled only after a maze exists.
                Load - opens a saved .maze file and displays it on the board.
                Solve - shows the solution path. Press the same button again when it says hide to remove the solution.
                Music - turns the background music on or off while you play.

                Menu:
                File contains New Maze, Save, Load, and Exit.
                Options > Properties shows the current configuration.
                Help > Help opens this instructions window again.
                """.formatted(goalText);
    }

    /** Shows the solved-maze celebration popup. */
    private void showMazeSolvedMessage() {
        playEndSound();
        String solvedAt = LocalDateTime.now().format(SOLVED_TIME_FORMATTER);
        MazeSolvedPopup.show(getWindow(), solvedAt);
        stopEndSound();
    }

    private Window getWindow() {
        return statusLabel.getScene().getWindow();
    }

    /** Creates media players for background and ending sounds. */
    private void initializeSoundPlayers() {
        trailSoundPlayer = createMediaPlayer("/sounds/trailSound.mp3");
        if (trailSoundPlayer != null) {
            trailSoundPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            trailSoundPlayer.setVolume(0.35);
            trailSoundPlayer.setOnReady(() -> {
                if (trailSoundEnabled) {
                    playTrailSound();
                }
            });
        }

        endSoundPlayer = createMediaPlayer("/sounds/endSound.mp3");
        if (endSoundPlayer != null) {
            endSoundPlayer.setVolume(0.70);
        }
    }

    private MediaPlayer createMediaPlayer(String resourcePath) {
        if (getClass().getResource(resourcePath) == null) {
            return null;
        }

        Media media = new Media(getClass().getResource(resourcePath).toExternalForm());
        return new MediaPlayer(media);
    }

    /** Toggles background sound from the toolbar button. */
    @FXML
    private void onToggleTrailSound() {
        trailSoundEnabled = !trailSoundEnabled;
        if (trailSoundEnabled) {
            playTrailSound();
        } else {
            stopTrailSound();
            setSoundIcon("▶");
        }
    }

    /** Starts background sound when a maze becomes active. */
    private void playTrailSound() {
        if (!trailSoundEnabled || trailSoundPlayer == null) {
            return;
        }

        trailSoundPlayer.play();
        setSoundIcon("⏸");
    }

    /** Updates the music icon label. */
    private void setSoundIcon(String symbol) {
        if (soundIcon != null) soundIcon.setText(symbol);
    }

    private void stopTrailSound() {
        if (trailSoundPlayer != null) {
            trailSoundPlayer.stop();
        }
    }

    /** Plays the ending sound once when the maze is solved. */
    private void playEndSound() {
        if (endSoundPlayer == null) {
            return;
        }

        stopTrailSound();
        endSoundPlayer.stop();
        endSoundPlayer.play();
    }

    private void stopEndSound() {
        if (endSoundPlayer != null) {
            endSoundPlayer.stop();
        }
    }

    private void stopAllSounds() {
        stopTrailSound();
        stopEndSound();
    }

    /** Registers Ctrl+mouse-wheel zoom for the maze board. */
    private void registerZoomControls() {
        mazeContainer.addEventFilter(javafx.scene.input.ScrollEvent.SCROLL, event -> {
            if (!event.isControlDown()) {
                return;
            }

            changeMazeZoom(event.getDeltaY() > 0 ? ZOOM_STEP : -ZOOM_STEP);
            event.consume();
        });
    }

    /** Applies a bounded visual zoom without changing maze state. */
    private void changeMazeZoom(double delta) {
        currentZoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, currentZoom + delta));
        mazeDisplayer.setZoomFactor(currentZoom);
    }

    private boolean handleKeyboardZoom(javafx.scene.input.KeyEvent event) {
        boolean zoomIn = switch (event.getCode()) {
            case PLUS, ADD, EQUALS -> true;
            default -> false;
        };
        boolean zoomOut = switch (event.getCode()) {
            case MINUS, SUBTRACT -> true;
            default -> false;
        };

        if (zoomIn || zoomOut) {
            changeMazeZoom(zoomIn ? ZOOM_STEP : -ZOOM_STEP);
            return true;
        }
        return false;
    }

    /** Enables mouse click and drag movement over the maze control. */
    private void registerMouseMovement() {
        mazeDisplayer.setOnMousePressed(event -> moveBrideTowardMouse(event.getX(), event.getY()));
        mazeDisplayer.setOnMouseDragged(event -> moveBrideTowardMouse(event.getX(), event.getY()));
    }

    private void moveBrideTowardMouse(double mouseX, double mouseY) {
        if (viewModel == null || !viewModel.mazeLoadedProperty().get()
                || viewModel.playerPositionProperty().get() == null) {
            return;
        }

        int targetRow = mazeDisplayer.getRowForY(mouseY);
        int targetColumn = mazeDisplayer.getColumnForX(mouseX);
        if (targetRow < 0 || targetColumn < 0) {
            return;
        }

        int currentRow = viewModel.playerPositionProperty().get().getRowIndex();
        int currentColumn = viewModel.playerPositionProperty().get().getColumnIndex();
        int rowDelta = Integer.compare(targetRow, currentRow);
        int columnDelta = Integer.compare(targetColumn, currentColumn);

        if (rowDelta != 0 || columnDelta != 0) {
            viewModel.movePlayer(rowDelta, columnDelta);
        }
    }

    /** Registers keyboard movement and delegates legality checks to the model. */
    private void registerKeyboardMovement() {
        /* Event filters keep movement active after focus moves to toolbar controls. */
        statusLabel.getScene().addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
            if (handleKeyboardZoom(event)) {
                event.consume();
                return;
            }

            if (viewModel == null || !viewModel.mazeLoadedProperty().get()) {
                return;
            }

            boolean handled = switch (event.getCode()) {
                case NUMPAD8, DIGIT8 -> viewModel.movePlayer(-1, 0);
                case NUMPAD2, DIGIT2 -> viewModel.movePlayer(1, 0);
                case NUMPAD4, DIGIT4 -> viewModel.movePlayer(0, -1);
                case NUMPAD6, DIGIT6 -> viewModel.movePlayer(0, 1);
                case NUMPAD7, DIGIT7 -> viewModel.movePlayer(-1, -1);
                case NUMPAD9, DIGIT9 -> viewModel.movePlayer(-1, 1);
                case NUMPAD1, DIGIT1 -> viewModel.movePlayer(1, -1);
                case NUMPAD3, DIGIT3 -> viewModel.movePlayer(1, 1);
                default -> false;
            };

            if (handled) {
                event.consume();
            }
        });
    }
}
