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
import java.util.Observable;
import java.util.Observer;

/**
 * Bridges JavaFX controllers and the UI-independent Model.
 * Exposes bindable state and forwards model notifications to the View.
 */
public class MyViewModel extends Observable implements Observer {
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

    /** Generates a new maze and publishes the refreshed model state. */
    public void generateMaze(int rows, int columns) {
        model.generateMaze(rows, columns);
        refreshStateFromModel();
        statusProperty.set("Maze generated: " + rows + " x " + columns);
    }

    /** Requests and exposes a solution for the active maze. */
    public void solveMaze() {
        model.solveMaze();
        solutionProperty.set(model.getSolution());
        statusProperty.set("Solution is ready.");
    }

    /** Saves the active maze to the file selected by the View. */
    public void saveMaze(File file) throws IOException {
        model.saveMaze(file);
        statusProperty.set("Maze saved.");
    }

    /** Loads a maze from the file selected by the View. */
    public void loadMaze(File file) throws IOException {
        model.loadMaze(file);
        refreshStateFromModel();
        statusProperty.set("Maze loaded.");
    }

    /** Moves the player and updates properties only when the move is valid. */
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

    /** Keeps configuration file access inside the Model. */
    public String getPropertiesText() {
        return model.getPropertiesText();
    }

    /** Lets the Model release servers and background resources. */
    public void shutdown() {
        model.shutdown();
    }

    /** Refreshes properties after Model notifications and forwards the command to the View. */
    @Override
    public void update(Observable o, Object arg) {
        if (arg instanceof String command) {
            switch (command) {
                case "generateMaze", "loadMaze" -> refreshStateFromModel();
                case "solveMaze" -> {
                    solutionProperty.set(model.getSolution());
                    statusProperty.set("Solution is ready.");
                }
                case "movePlayer" -> {
                    playerPositionProperty.set(model.getPlayerPosition());
                    boolean solved = model.isMazeSolved();
                    mazeSolvedProperty.set(solved);
                    if (solved) {
                        statusProperty.set("Maze solved!");
                    }
                }
                case "saveMaze" -> statusProperty.set("Maze saved.");
            }
        }
        setChanged();
        notifyObservers(arg);
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
