package Model;

import algorithms.mazeGenerators.Maze;
import algorithms.mazeGenerators.Position;
import algorithms.search.Solution;

import java.io.File;
import java.io.IOException;

/**
 * Defines the application operations that are independent of the JavaFX UI.
 *
 * The Model owns the current maze, player location, solution data, persistence, and
 * communication with the maze generation/solving services from the previous project parts.
 * Nothing in this interface should expose JavaFX controls or scene objects.
 */
public interface IModel {
    /**
     * Creates a new maze and stores it as the active game.
     *
     * The implementation can use the Part B generation server or the generator classes
     * from ATPProjectJAR. After generation, the model should also reset the player position
     * to the maze start position and clear any old solution.
     */
    void generateMaze(int rows, int columns);

    /**
     * Returns the active maze object for display or gameplay logic.
     *
     * The View should treat a {@code null} value as "no maze has been generated or loaded
     * yet" and keep actions such as Solve/Save disabled.
     */
    Maze getMaze();

    /**
     * Returns the current location of the player inside the active maze.
     *
     * This uses the Position class from the JAR so movement logic and maze coordinates
     * stay consistent with the previous project parts.
     */
    Position getPlayerPosition();

    /**
     * Attempts to move the player by the requested row and column change.
     *
     * Examples:
     * rowDelta = -1, columnDelta = 0 means move up.
     * rowDelta = 0, columnDelta = 1 means move right.
     * rowDelta = -1, columnDelta = -1 means move diagonally up-left.
     *
     * The implementation should return false when the move is outside the maze or into a wall.
     */
    boolean movePlayer(int rowDelta, int columnDelta);

    /**
     * Returns true when the player position equals the maze goal position.
     */
    boolean isMazeSolved();

    /**
     * Solves the currently active maze and stores the resulting solution.
     *
     * The implementation can use ServerStrategySolveSearchProblem through the server flow,
     * or directly use SearchableMaze and one of the search algorithms from the JAR.
     */
    void solveMaze();

    /**
     * Returns the last solution calculated for the active maze.
     *
     * A null value means the user has not requested a solution yet, or the current maze has
     * changed since the previous solution was calculated.
     */
    Solution getSolution();

    /**
     * Saves the active maze to the requested file.
     *
     * The implementation should use the JAR's maze byte representation and compression
     * streams instead of inventing a new file format.
     */
    void saveMaze(File file) throws IOException;

    /**
     * Loads a maze from the requested file and makes it the active game.
     *
     * After loading, the model should reset the player position to the loaded maze start
     * position and clear any old solution.
     */
    void loadMaze(File file) throws IOException;

    /**
     * Releases resources owned by the model, such as running servers or open background
     * threads. The View should call this during a clean application exit.
     */
    void shutdown();
}
