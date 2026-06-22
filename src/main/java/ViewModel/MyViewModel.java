package ViewModel;

import Model.IModel;
import algorithms.mazeGenerators.Maze;
import algorithms.mazeGenerators.Position;
import algorithms.search.Solution;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Connects the JavaFX View to the plain Java Model.
 *
 * The ViewModel exposes bindable JavaFX properties for the controller and delegates
 * all game work to IModel. It does not know about FXML controls, dialogs, or screen layout.
 */
public class MyViewModel {
    private final IModel model;

    private final StringProperty statusProperty = new SimpleStringProperty("Ready to generate a maze.");
    private final BooleanProperty mazeLoadedProperty = new SimpleBooleanProperty(false);
    private final BooleanProperty mazeSolvedProperty = new SimpleBooleanProperty(false);
    private final ObjectProperty<Maze> mazeProperty = new SimpleObjectProperty<>();
    private final ObjectProperty<Position> playerPositionProperty = new SimpleObjectProperty<>();
    private final ObjectProperty<Solution> solutionProperty = new SimpleObjectProperty<>();

    public MyViewModel(IModel model) {
        this.model = model;
    }

    /**
     * Generates a new maze through the Model and publishes the new state for the View.
     */
    public void generateMaze(int rows, int columns) {
        model.generateMaze(rows, columns);
        refreshStateFromModel();
        statusProperty.set("Maze generated: " + rows + " x " + columns);
    }

    /**
     * Requests a solution for the current maze and exposes it through solutionProperty.
     */
    public void solveMaze() {
        model.solveMaze();
        solutionProperty.set(model.getSolution());
        statusProperty.set("Solution is ready.");
    }

    /**
     * Saves the active maze to the file chosen by the View.
     */
    public void saveMaze(File file) throws IOException {
        model.saveMaze(file);
        statusProperty.set("Maze saved.");
    }

    /**
     * Loads a maze from the file chosen by the View and publishes the loaded state.
     */
    public void loadMaze(File file) throws IOException {
        model.loadMaze(file);
        refreshStateFromModel();
        statusProperty.set("Maze loaded.");
    }

    /**
     * Moves the player and updates bindable state when the move is valid.
     */
    public boolean movePlayer(int rowDelta, int columnDelta) {
        boolean moved = model.movePlayer(rowDelta, columnDelta);
        if (moved) {
            playerPositionProperty.set(model.getPlayerPosition());
            boolean solved = model.isMazeSolved();
            mazeSolvedProperty.set(solved);
            statusProperty.set(solved ? "Maze solved." : "Player moved.");
        }
        return moved;
    }

    /**
     * Reads the project configuration file for display in the Properties dialog.
     */
    public String getPropertiesText() {
        Properties properties = new Properties();

        try (InputStream inputStream = MyViewModel.class.getResourceAsStream("/config.properties")) {
            if (inputStream == null) {
                return "config.properties was not found.";
            }

            properties.load(inputStream);
            return "threadPoolSize = " + properties.getProperty("threadPoolSize", "") + "\n"
                    + "mazeGeneratingAlgorithm = " + properties.getProperty("mazeGeneratingAlgorithm", "") + "\n"
                    + "mazeSearchingAlgorithm = " + properties.getProperty("mazeSearchingAlgorithm", "");
        } catch (IOException e) {
            return "Failed to read config.properties: " + e.getMessage();
        }
    }

    /**
     * Lets the Model close servers or background resources before the app exits.
     */
    public void shutdown() {
        model.shutdown();
    }

    private void refreshStateFromModel() {
        mazeProperty.set(model.getMaze());
        playerPositionProperty.set(model.getPlayerPosition());
        solutionProperty.set(model.getSolution());
        mazeLoadedProperty.set(model.getMaze() != null);
        mazeSolvedProperty.set(model.isMazeSolved());
    }

    public StringProperty statusProperty() {
        return statusProperty;
    }

    public BooleanProperty mazeLoadedProperty() {
        return mazeLoadedProperty;
    }

    public BooleanProperty mazeSolvedProperty() {
        return mazeSolvedProperty;
    }

    public ObjectProperty<Maze> mazeProperty() {
        return mazeProperty;
    }

    public ObjectProperty<Position> playerPositionProperty() {
        return playerPositionProperty;
    }

    public ObjectProperty<Solution> solutionProperty() {
        return solutionProperty;
    }
}
