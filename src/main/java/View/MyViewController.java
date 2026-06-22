package View;

import ViewModel.MyViewModel;
import algorithms.mazeGenerators.Maze;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Controller for MyView.fxml.
 *
 * This class belongs to the View layer. It reads values from JavaFX controls,
 * binds visible state to the ViewModel, and forwards user actions to the ViewModel.
 * It must not generate mazes, solve mazes, or work with files directly.
 */
public class MyViewController implements IView {
    private static final DateTimeFormatter SOLVED_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private MyViewModel viewModel;

    @FXML private Label statusLabel;
    @FXML private Label mazeInfoLabel;
    @FXML private TextField txtRows;
    @FXML private TextField txtCols;
    @FXML private Button btnSolve;
    @FXML private Button btnSave;
    @FXML private Button btnSound;
    @FXML private MenuItem menuSave;
    @FXML private StackPane mazeContainer;

    private MazeDisplayer mazeDisplayer;
    private MediaPlayer trailSoundPlayer;
    private MediaPlayer endSoundPlayer;
    private boolean trailSoundEnabled = true;

    /**
     * Runs after JavaFX injects all FXML controls.
     *
     * Only UI defaults belong here. The ViewModel is injected later from Main.
     */
    @FXML
    private void initialize() {
        statusLabel.setText("Ready to generate a maze.");
        mazeInfoLabel.setText("No maze loaded.");

        mazeDisplayer = new MazeDisplayer();
        /*
         * The canvas size is bound to mazeContainer below.
         * Marking it unmanaged prevents a layout feedback loop where the canvas changes
         * the StackPane preferred size, and the StackPane then makes the canvas larger again.
         */
        mazeDisplayer.setManaged(false);
        mazeDisplayer.widthProperty().bind(mazeContainer.widthProperty());
        mazeDisplayer.heightProperty().bind(mazeContainer.heightProperty());
        mazeContainer.setFocusTraversable(true);
        mazeContainer.getChildren().add(mazeDisplayer);
        initializeSoundPlayers();
        registerMouseMovement();
    }

    /**
     * Receives the ViewModel created by Main and connects UI controls to its properties.
     */
    public void setViewModel(MyViewModel viewModel) {
        this.viewModel = viewModel;

        statusLabel.textProperty().bind(viewModel.statusProperty());
        btnSolve.disableProperty().bind(viewModel.mazeLoadedProperty().not());
        btnSave.disableProperty().bind(viewModel.mazeLoadedProperty().not());
        menuSave.disableProperty().bind(viewModel.mazeLoadedProperty().not());

        viewModel.mazeProperty().addListener((observable, oldMaze, newMaze) -> {
            if (newMaze != null) {
                displayMaze(newMaze);
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

        Platform.runLater(this::registerKeyboardMovement);
    }

    /**
     * Creates a new maze after validating the row and column inputs.
     */
    @FXML
    private void onNewMaze() {
        try {
            int rows = Integer.parseInt(txtRows.getText().trim());
            int columns = Integer.parseInt(txtCols.getText().trim());

            if (rows < 2 || columns < 2) {
                displayMessage("Invalid Size", "Rows and columns must each be at least 2.");
                return;
            }

            viewModel.generateMaze(rows, columns);
            playTrailSound();
            mazeContainer.requestFocus();
        } catch (NumberFormatException e) {
            displayMessage("Invalid Input", "Please enter whole numbers for rows and columns.");
        } catch (RuntimeException e) {
            displayMessage("Maze Error", e.getMessage());
        }
    }

    /**
     * Opens a save dialog and asks the ViewModel to save the active maze.
     */
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

    /**
     * Opens a load dialog and asks the ViewModel to load the selected maze.
     */
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
            playTrailSound();
            mazeContainer.requestFocus();
        } catch (IOException | RuntimeException e) {
            displayMessage("Load Error", e.getMessage());
        }
    }

    /**
     * Requests a solution for the active maze.
     */
    @FXML
    private void onSolveMaze() {
        try {
            viewModel.solveMaze();
        } catch (RuntimeException e) {
            displayMessage("Solve Error", e.getMessage());
        }
    }

    /**
     * Shows the values from the project configuration file.
     */
    @FXML
    private void onProperties() {
        displayMessage("Properties", viewModel.getPropertiesText());
    }

    @FXML
    private void onHelp() {
        displayMessage("Help",
                "Use the NumPad to move:\n"
                        + "4 / 6 / 8 / 2 - left, right, up, down\n"
                        + "7 / 9 / 1 / 3 - diagonal movement\n\n"
                        + "Use File > New to generate a maze.");
    }

    @FXML
    private void onAbout() {
        displayMessage("About",
                "ATP Maze Game\n"
                        + "Programmers: Hadas Tourgeman, Shoval Dagmi\n"
                        + "Generation: MyMazeGenerator\n"
                        + "Solver: configured in config.properties");
    }

    /**
     * Performs a clean application exit through the ViewModel.
     */
    @FXML
    public void onExit() {
        stopAllSounds();
        if (viewModel != null) {
            viewModel.shutdown();
        }
        Platform.exit();
    }

    /**
     * Displays basic maze information until the custom MazeDisplayer control is added.
     */
    @Override
    public void displayMaze(Maze maze) {
        mazeDisplayer.setMaze(maze);
        mazeDisplayer.setPlayerPosition(viewModel.playerPositionProperty().get());
        mazeInfoLabel.setText("Maze ready: " + maze.getRows() + " x " + maze.getColumns());
    }

    /**
     * Shows a modal message dialog for errors and information.
     */
    @Override
    public void displayMessage(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Shows the celebration message when the player reaches the goal.
     */
    private void showMazeSolvedMessage() {
        playEndSound();
        String solvedAt = LocalDateTime.now().format(SOLVED_TIME_FORMATTER);
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("SAVE THE DATE!");
        alert.setHeaderText(null);
        alert.setContentText("SAVE THE DATE!\n" + solvedAt);
        alert.setGraphic(createRingGraphic());
        alert.showAndWait();
        stopEndSound();
    }

    private ImageView createRingGraphic() {
        if (getClass().getResourceAsStream("/Images/ring.png") == null) {
            return null;
        }

        ImageView ringGraphic = new ImageView(new Image(getClass().getResourceAsStream("/Images/ring.png")));
        ringGraphic.setFitWidth(72);
        ringGraphic.setFitHeight(72);
        ringGraphic.setPreserveRatio(true);
        ringGraphic.setSmooth(true);
        return ringGraphic;
    }

    private Window getWindow() {
        return statusLabel.getScene().getWindow();
    }

    /**
     * Creates media players for the background trail sound and the ending sound.
     * Sound files are loaded from resources with relative paths.
     */
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

    /**
     * Toggles the trail/background sound from the UI button.
     */
    @FXML
    private void onToggleTrailSound() {
        trailSoundEnabled = !trailSoundEnabled;
        if (trailSoundEnabled) {
            playTrailSound();
            btnSound.setText("Stop Music");
        } else {
            stopTrailSound();
            btnSound.setText("Play Music");
        }
    }

    /**
     * Starts the trail sound when a maze becomes active.
     */
    private void playTrailSound() {
        if (!trailSoundEnabled || trailSoundPlayer == null) {
            return;
        }

        trailSoundPlayer.play();
        btnSound.setText("Stop Music");
    }

    private void stopTrailSound() {
        if (trailSoundPlayer != null) {
            trailSoundPlayer.stop();
        }
    }

    /**
     * Plays the ending sound once when the maze is solved.
     */
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

    /**
     * Enables mouse click/drag movement.
     *
     * The mouse target is translated to a maze cell, then moved one legal step at a time
     * toward that cell. The Model still decides if each step is valid.
     */
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

    /**
     * Registers NumPad movement required by the assignment.
     *
     * The View only translates keys to row/column deltas. The Model decides whether
     * the move is legal according to walls and maze boundaries.
     */
    private void registerKeyboardMovement() {
        /*
         * Use an event filter instead of setOnKeyPressed so movement still works after
         * clicking buttons or text fields. The maze container requests focus after New/Load,
         * but the filter makes the key handling more reliable.
         */
        statusLabel.getScene().addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
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
