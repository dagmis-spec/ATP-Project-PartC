package View;

/**
 * Minimal UI contract used by the controller.
 * Keeps Model classes out of the View interface.
 */
public interface IView {
    /** Renders a maze grid where 0 is path and 1 is wall. */
    void displayMaze(int[][] grid);

    /** Shows a user-facing message. */
    void displayMessage(String title, String message);
}
