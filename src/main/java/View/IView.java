package View;

import algorithms.mazeGenerators.Maze;

/**
 * Minimal contract for UI actions that other layers may trigger.
 *
 * The controller implements this interface so presentation actions are explicit:
 * drawing a maze and showing messages are View responsibilities. Maze creation,
 * solving, saving, and loading should not be implemented here.
 */
public interface IView {
    /**
     * Renders the maze that came from the Model layer.
     *
     * When the custom maze control is added, this method should pass the Maze object to that
     * control instead of drawing cells directly inside the controller.
     */
    void displayMaze(Maze maze);

    /**
     * Shows a user-facing message using JavaFX UI components.
     *
     * Part C requires errors and messages to be displayed with alerts instead of console
     * prints. Keeping this method in the View contract gives us one place to standardize
     * those messages.
     */
    void displayMessage(String title, String message);
}
