package View;

/**
 * Minimal contract for UI actions that other layers may trigger.
 *
 * Uses only primitive/standard types — no Model classes (Maze, Position, etc.)
 * cross this boundary. The controller converts Model data to plain types before
 * exposing them through this interface.
 */
public interface IView {
    /**
     * Renders the maze cell grid.
     * Receives a plain int[][] so no Model class leaks into the View interface.
     * 0 = path, 1 = wall — the same convention used throughout the project.
     */
    void displayMaze(int[][] grid);

    /**
     * Shows a user-facing message using JavaFX UI components.
     */
    void displayMessage(String title, String message);
}
