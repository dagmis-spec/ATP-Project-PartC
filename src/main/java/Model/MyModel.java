package Model;

import algorithms.mazeGenerators.Maze;
import algorithms.mazeGenerators.MyMazeGenerator;
import algorithms.mazeGenerators.Position;
import algorithms.search.BestFirstSearch;
import algorithms.search.BreadthFirstSearch;
import algorithms.search.DepthFirstSearch;
import algorithms.search.ISearchingAlgorithm;
import algorithms.search.SearchableMaze;
import algorithms.search.Solution;
import IO.MyCompressorOutputStream;
import IO.MyDecompressorInputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Model implementation for the maze game.
 *
 * This class owns the game data: the active maze, the player's current position,
 * and the currently displayed solution. It uses classes from ATPProjectJAR for maze
 * generation, solving, compression, and decompression.
 *
 * The View and ViewModel should call this class through the IModel interface instead of
 * working directly with maze algorithms or files.
 */
public class MyModel extends java.util.Observable implements IModel {
    /*
     * Runtime state for the current game.
     * maze is null until the user generates or loads a maze.
     * playerPos is reset whenever a new maze becomes active.
     * solution is null until the user asks to solve the active maze.
     */
    private Maze maze;
    private Position playerPos;
    private Solution solution;

    /**
     * Creates a new maze with the requested dimensions.
     *
     * MyMazeGenerator comes from ATPProjectJAR. After generation, this method resets
     * all state that depends on the active maze: player position and solution.
     */
    @Override
    public void generateMaze(int rows, int columns) {
        if (rows <= 0 || columns <= 0) {
            throw new IllegalArgumentException("Maze dimensions must be positive.");
        }

        MyMazeGenerator mazeGenerator = new MyMazeGenerator();
        maze = mazeGenerator.generate(rows, columns);
        playerPos = maze.getStartPosition();
        solution = null;
        // Notify ViewModel that a new maze is ready
        setChanged();
        notifyObservers("generateMaze");
    }

    /**
     * Returns the active maze object.
     *
     * The caller can inspect this object for drawing, but the Model remains responsible
     * for changing which maze is active.
     */
    @Override
    public Maze getMaze() {
        return maze;
    }

    /**
     * Returns the player's current position in the active maze.
     *
     * This value is controlled by generate/load/move operations in the Model.
     */
    @Override
    public Position getPlayerPosition() {
        return playerPos;
    }

    /**
     * Moves the player inside the active maze if the requested target cell is legal.
     *
     * The View sends movement as row/column deltas, for example:
     * (-1, 0) for up, (1, 0) for down, and (-1, 1) for diagonal up-right.
     *
     * The Model checks the maze boundaries and wall cells before updating playerPos.
     * Returning false lets the ViewModel/View ignore invalid movement without crashing.
     */
    @Override
    public boolean movePlayer(int rowDelta, int columnDelta) {
        if (maze == null || playerPos == null) {
            return false;
        }

        int targetRow = playerPos.getRowIndex() + rowDelta;
        int targetColumn = playerPos.getColumnIndex() + columnDelta;

        if (!isInsideMaze(targetRow, targetColumn)) {
            return false;
        }

        if (maze.getCellValue(targetRow, targetColumn) != 0) {
            return false;
        }

        playerPos = new Position(targetRow, targetColumn);
        setChanged();
        notifyObservers("movePlayer");
        return true;
    }

    /**
     * Checks whether a row/column pair is inside the current maze boundaries.
     *
     * This helper assumes maze is not null. Public methods must check that before calling it.
     */
    private boolean isInsideMaze(int row, int column) {
        return row >= 0 && row < maze.getRows() && column >= 0 && column < maze.getColumns();
    }

    /**
     * Checks whether the player has reached the maze goal cell.
     *
     * The goal position comes from the Maze object created by the generator or loaded from file.
     */
    @Override
    public boolean isMazeSolved() {
        if (maze == null || playerPos == null) {
            return false;
        }

        Position goalPosition = maze.getGoalPosition();
        return playerPos.getRowIndex() == goalPosition.getRowIndex()
                && playerPos.getColumnIndex() == goalPosition.getColumnIndex();
    }

    /**
     * Solves the active maze and stores the solution path.
     *
     * The algorithm is chosen from config.properties. The Maze is first adapted to
     * SearchableMaze because the JAR search algorithms solve generic searchable problems,
     * not Maze objects directly.
     */
    @Override
    public void solveMaze() {
        if (maze == null) {
            throw new IllegalStateException("Cannot solve a maze before one is generated or loaded.");
        }

        /*
         * The search algorithms from the JAR work on ISearchable problems, not directly on Maze.
         * SearchableMaze adapts our Maze object to the interface expected by the algorithm.
         */
        SearchableMaze searchableMaze = new SearchableMaze(maze);
        ISearchingAlgorithm searchingAlgorithm = createSearchAlgorithmFromConfig();
        solution = searchingAlgorithm.solve(searchableMaze);
        // Notify ViewModel that solution is ready
        setChanged();
        notifyObservers("solveMaze");
    }

    /**
     * Creates the search algorithm selected in src/main/resources/config.properties.
     *
     * Keeping the algorithm name in a configuration file means we can switch between
     * BestFirstSearch, BreadthFirstSearch, and DepthFirstSearch without changing code.
     */
    private ISearchingAlgorithm createSearchAlgorithmFromConfig() {
        String algorithmName = getConfiguredValue("mazeSearchingAlgorithm", "BestFirstSearch");

        return switch (algorithmName) {
            case "BreadthFirstSearch" -> new BreadthFirstSearch();
            case "DepthFirstSearch" -> new DepthFirstSearch();
            case "BestFirstSearch" -> new BestFirstSearch();
            default -> throw new IllegalArgumentException("Unsupported maze searching algorithm: " + algorithmName);
        };
    }

    /**
     * Reads a value from the application configuration file.
     *
     * The file is loaded from the classpath, so src/main/resources/config.properties becomes
     * available as /config.properties after Maven copies resources to target/classes.
     * If the file or key is missing, the provided default keeps the model usable.
     */
    private String getConfiguredValue(String key, String defaultValue) {
        Properties properties = new Properties();

        try (InputStream inputStream = MyModel.class.getResourceAsStream("/config.properties")) {
            if (inputStream == null) {
                return defaultValue;
            }

            properties.load(inputStream);
            return properties.getProperty(key, defaultValue).trim();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read application configuration.", e);
        }
    }

    /**
     * Returns the last solution calculated for the active maze.
     *
     * A null result means the maze has not been solved yet, or a new maze was generated/loaded
     * after the previous solution.
     */
    @Override
    public Solution getSolution() {
        return solution;
    }

    /**
     * Saves the active maze to disk using the same compressed format as Part B.
     *
     * The selected file comes from the View layer, usually through a FileChooser.
     * The Model only receives the destination and writes the maze bytes.
     */
    @Override
    public void saveMaze(File file) throws IOException {
        if (maze == null) {
            throw new IllegalStateException("Cannot save a maze before one is generated or loaded.");
        }

        if (file == null) {
            throw new IllegalArgumentException("Save file cannot be null.");
        }

        /*
         * Maze already knows how to convert itself to bytes.
         * The compressor from the JAR keeps the saved file smaller and matches the format
         * expected by MyDecompressorInputStream when loading the maze back.
         */
        try (MyCompressorOutputStream outputStream =
                     new MyCompressorOutputStream(new FileOutputStream(file))) {
            outputStream.write(maze.toByteArray());
        }
        setChanged();
        notifyObservers("saveMaze");
    }

    /**
     * Loads a maze from disk and makes it the active game.
     *
     * The file must have been saved with saveMaze or another compatible compressor.
     * Loading a new maze resets runtime state: the player returns to the start position
     * and the previous solution is cleared because it belongs to the old maze.
     */
    @Override
    public void loadMaze(File file) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("Load file cannot be null.");
        }

        byte[] decompressedMazeBytes;

        /*
         * The file is expected to be in the same compressed format produced by saveMaze.
         * readAllBytes() works here because MyDecompressorInputStream is an InputStream.
         */
        try (MyDecompressorInputStream inputStream =
                     new MyDecompressorInputStream(new FileInputStream(file))) {
            decompressedMazeBytes = inputStream.readAllBytes();
        }

        maze = new Maze(decompressedMazeBytes);
        playerPos = maze.getStartPosition();
        solution = null;
        setChanged();
        notifyObservers("loadMaze");
    }

    /**
     * Reads the application configuration file and returns its content as a formatted string.
     * File reading belongs in the Model — the ViewModel must not access the file system.
     */
    @Override
    public String getPropertiesText() {
        return getConfiguredValue("threadPoolSize", "(not set)") == null ? "" :
                "threadPoolSize = " + getConfiguredValue("threadPoolSize", "") + "\n"
                + "mazeGeneratingAlgorithm = " + getConfiguredValue("mazeGeneratingAlgorithm", "") + "\n"
                + "mazeSearchingAlgorithm = " + getConfiguredValue("mazeSearchingAlgorithm", "");
    }

    /**
     * Releases resources owned by the model before the application exits.
     *
     * This model currently does not keep servers or background threads open. When server
     * instances are added later, this is the method that should stop them cleanly.
     */
    @Override
    public void shutdown() {
        // No open model resources yet.
    }
}
