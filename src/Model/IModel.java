package Model;

import algorithms.mazeGenerators.Maze;
import algorithms.mazeGenerators.Position;
import algorithms.search.Solution;

import java.io.File;
import java.io.IOException;

/**
 * UI-independent game operations exposed to the ViewModel.
 * The Model owns maze state, persistence, and service communication.
 */
public interface IModel {
    /** Creates a new active maze and resets dependent game state. */
    void generateMaze(int rows, int columns);

    /** Returns the active maze, or null before a maze is generated or loaded. */
    Maze getMaze();

    /** Returns the player's current cell in the active maze. */
    Position getPlayerPosition();

    /** Moves by a row/column delta and returns false for walls or out-of-bounds moves. */
    boolean movePlayer(int rowDelta, int columnDelta);

    /** Returns true when the player is on the goal cell. */
    boolean isMazeSolved();

    /** Solves the active maze and stores the solution for later display. */
    void solveMaze();

    /** Returns the current maze solution, or null when no solution is available. */
    Solution getSolution();

    /** Saves the active maze to the requested file. */
    void saveMaze(File file) throws IOException;

    /** Loads a maze file and resets player and solution state. */
    void loadMaze(File file) throws IOException;

    /** Releases servers and other model-owned resources. */
    void shutdown();

    /** Returns the application configuration as user-readable text. */
    String getPropertiesText();
}
