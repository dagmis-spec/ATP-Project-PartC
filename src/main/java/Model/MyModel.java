package Model;

import Client.Client;
import IO.MyCompressorOutputStream;
import IO.MyDecompressorInputStream;
import Server.Configurations;
import Server.Server;
import Server.ServerStrategyGenerateMaze;
import Server.ServerStrategySolveSearchProblem;
import algorithms.mazeGenerators.Maze;
import algorithms.mazeGenerators.Position;
import algorithms.search.Solution;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Model implementation for the maze game.
 *
 * This class owns the game data: the active maze, the player's current position,
 * and the currently displayed solution. It uses the Part B server/client classes
 * from ATPProjectJAR for maze generation and solving.
 *
 * The View and ViewModel should call this class through the IModel interface instead of
 * working directly with maze algorithms or files.
 */
public class MyModel extends java.util.Observable implements IModel {
    private static final Logger logger = LogManager.getLogger(MyModel.class);
    private static final Logger generationLogger = LogManager.getLogger("generation-server");
    private static final Logger solvingLogger = LogManager.getLogger("solving-server");

    /*
     * The two Part B servers run locally for the JavaFX application.
     * The ports must match the client requests in requestMazeFromServer/requestSolutionFromServer.
     */
    private static final int GENERATE_MAZE_PORT = 5400;
    private static final int SOLVE_MAZE_PORT = 5401;
    private static final int SERVER_LISTENING_INTERVAL_MS = 1000;

    /*
     * Runtime state for the current game.
     * maze is null until the user generates or loads a maze.
     * playerPos is reset whenever a new maze becomes active.
     * solution is null until the user asks to solve the active maze.
     */
    private Maze maze;
    private Position playerPos;
    private Solution solution;
    private Server generateMazeServer;
    private Server solveMazeServer;
    private boolean serversStarted;

    /**
     * Starts the local generation and solving servers when the Model is created.
     * The ViewModel owns this Model instance for the lifetime of the game screen.
     */
    public MyModel() {
        startServers();
    }

    /**
     * Creates a new maze with the requested dimensions.
     *
     * The generation request is sent to the Part B generation server from ATPProjectJAR.
     * After generation, this method resets all state that depends on the active maze:
     * player position and solution.
     */
    @Override
    public void generateMaze(int rows, int columns) {
        if (rows <= 0 || columns <= 0) {
            throw new IllegalArgumentException("Maze dimensions must be positive.");
        }

        generationLogger.info("Client requests maze generation: rows={}, columns={}", rows, columns);
        maze = requestMazeFromServer(rows, columns);
        playerPos = maze.getStartPosition();
        solution = null;
        generationLogger.info("Maze generation completed: rows={}, columns={}, start={}, goal={}",
                maze.getRows(), maze.getColumns(), maze.getStartPosition(), maze.getGoalPosition());

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
     * The solve request is sent to the Part B solving server from ATPProjectJAR.
     */
    @Override
    public void solveMaze() {
        if (maze == null) {
            throw new IllegalStateException("Cannot solve a maze before one is generated or loaded.");
        }

        solvingLogger.info("Client requests maze solving: rows={}, columns={}, algorithm={}",
                maze.getRows(), maze.getColumns(), getConfiguredValue("mazeSearchingAlgorithm", "BestFirstSearch"));
        solution = requestSolutionFromServer(maze);
        int solutionLength = solution == null || solution.getSolutionPath() == null
                ? 0
                : solution.getSolutionPath().size();
        solvingLogger.info("Maze solving completed: solutionLength={}", solutionLength);

        // Notify ViewModel that solution is ready
        setChanged();
        notifyObservers("solveMaze");
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

        // Persistence belongs to the Model, so file-system events are logged here.
        logger.info("Saving active maze to file: {}", file.getAbsolutePath());

        /*
         * Maze already knows how to convert itself to bytes.
         * The compressor from the JAR keeps the saved file smaller and matches the format
         * expected by MyDecompressorInputStream when loading the maze back.
         */
        try (MyCompressorOutputStream outputStream =
                     new MyCompressorOutputStream(new FileOutputStream(file))) {
            outputStream.write(maze.toByteArray());
            logger.info("Maze saved successfully: rows={}, columns={}, file={}",
                    maze.getRows(), maze.getColumns(), file.getAbsolutePath());
        } catch (IOException e) {
            logger.error("Failed to save maze to file: {}", file.getAbsolutePath(), e);
            throw e;
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

        // Persistence belongs to the Model, so file-system events are logged here.
        logger.info("Loading maze from file: {}", file.getAbsolutePath());
        byte[] decompressedMazeBytes;

        /*
         * The file is expected to be in the same compressed format produced by saveMaze.
         * readAllBytes() works here because MyDecompressorInputStream is an InputStream.
         */
        try (MyDecompressorInputStream inputStream =
                     new MyDecompressorInputStream(new FileInputStream(file))) {
            decompressedMazeBytes = inputStream.readAllBytes();
        } catch (IOException e) {
            logger.error("Failed to load maze from file: {}", file.getAbsolutePath(), e);
            throw e;
        }

        maze = new Maze(decompressedMazeBytes);
        playerPos = maze.getStartPosition();
        solution = null;
        logger.info("Maze loaded successfully: rows={}, columns={}, start={}, goal={}, file={}",
                maze.getRows(), maze.getColumns(), maze.getStartPosition(), maze.getGoalPosition(),
                file.getAbsolutePath());
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
     */
    @Override
    public void shutdown() {
        if (!serversStarted) {
            return;
        }

        generationLogger.info("Stopping maze generation server on port {}", GENERATE_MAZE_PORT);
        solvingLogger.info("Stopping maze solving server on port {}", SOLVE_MAZE_PORT);
        generateMazeServer.stop();
        solveMazeServer.stop();
        serversStarted = false;
        logger.info("Model servers stopped.");
    }

    /**
     * Creates and starts the two local Part B servers used by this desktop app.
     * Generation and solving are separate services so each can write to its own log file.
     */
    private void startServers() {
        configureServerSettings();

        generateMazeServer = new Server(
                GENERATE_MAZE_PORT,
                SERVER_LISTENING_INTERVAL_MS,
                // Decorate the JAR strategy so generation requests are logged from the server side.
                new LoggingServerStrategy(
                        new ServerStrategyGenerateMaze(),
                        generationLogger,
                        "Maze generation"
                )
        );
        solveMazeServer = new Server(
                SOLVE_MAZE_PORT,
                SERVER_LISTENING_INTERVAL_MS,
                // Decorate the JAR strategy so solving requests are logged from the server side.
                new LoggingServerStrategy(
                        new ServerStrategySolveSearchProblem(),
                        solvingLogger,
                        "Maze solving"
                )
        );

        generationLogger.info("Starting maze generation server on port {}", GENERATE_MAZE_PORT);
        solvingLogger.info("Starting maze solving server on port {}", SOLVE_MAZE_PORT);
        generateMazeServer.start();
        solveMazeServer.start();
        serversStarted = true;
        logger.info("Model servers started.");
    }

    /**
     * Copies values from this project's config.properties into the JAR's singleton
     * configuration object before the servers begin handling client requests.
     */
    private void configureServerSettings() {
        Configurations configurations = Configurations.getInstance();
        configurations.setThreadPoolSize(getConfiguredIntValue("threadPoolSize", 10));
        configurations.setMazeGeneratingAlgorithm(getConfiguredValue("mazeGeneratingAlgorithm", "MyMazeGenerator"));
        configurations.setMazeSearchingAlgorithm(getConfiguredValue("mazeSearchingAlgorithm", "BestFirstSearch"));
        logger.info("Server settings loaded: threadPoolSize={}, mazeGeneratingAlgorithm={}, mazeSearchingAlgorithm={}",
                configurations.getThreadPoolSize(),
                configurations.getMazeGeneratingAlgorithm(),
                configurations.getMazeSearchingAlgorithm());
    }

    private int getConfiguredIntValue(String key, int defaultValue) {
        String value = getConfiguredValue(key, String.valueOf(defaultValue));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            logger.warn("Invalid integer configuration for {}: {}. Using default {}.", key, value, defaultValue);
            return defaultValue;
        }
    }

    /**
     * Sends a maze-size request to the generation server and converts the compressed
     * byte-array response back into a Maze object for the Model state.
     */
    private Maze requestMazeFromServer(int rows, int columns) {
        AtomicReference<Maze> generatedMaze = new AtomicReference<>();
        // The client strategy lambda cannot throw checked exceptions through Client, so keep the failure here.
        AtomicReference<RuntimeException> clientFailure = new AtomicReference<>();

        try {
            Client client = new Client(InetAddress.getLocalHost(), GENERATE_MAZE_PORT, (inputStream, outputStream) -> {
                try {
                    ObjectOutputStream toServer = new ObjectOutputStream(outputStream);
                    ObjectInputStream fromServer = new ObjectInputStream(inputStream);

                    toServer.flush();
                    toServer.writeObject(new int[]{rows, columns});
                    toServer.flush();

                    byte[] compressedMazeBytes = (byte[]) fromServer.readObject();
                    generatedMaze.set(decompressMaze(compressedMazeBytes));
                } catch (IOException | ClassNotFoundException e) {
                    clientFailure.set(new IllegalStateException("Failed to generate maze through the server.", e));
                }
            });
            client.communicateWithServer();
        } catch (UnknownHostException e) {
            throw new IllegalStateException("Could not connect to the local maze generation server.", e);
        }

        if (clientFailure.get() != null) {
            generationLogger.error("Maze generation server request failed.", clientFailure.get());
            throw clientFailure.get();
        }
        if (generatedMaze.get() == null) {
            throw new IllegalStateException("Maze generation server did not return a maze.");
        }

        return generatedMaze.get();
    }

    /**
     * The generation server returns compressed maze bytes, matching the Part B protocol.
     */
    private Maze decompressMaze(byte[] compressedMazeBytes) throws IOException {
        try (MyDecompressorInputStream inputStream =
                     new MyDecompressorInputStream(new ByteArrayInputStream(compressedMazeBytes))) {
            return new Maze(inputStream.readAllBytes());
        }
    }

    /**
     * Sends the active Maze object to the solving server and receives the server's Solution.
     */
    private Solution requestSolutionFromServer(Maze mazeToSolve) {
        AtomicReference<Solution> solvedMaze = new AtomicReference<>();
        // The client strategy lambda cannot throw checked exceptions through Client, so keep the failure here.
        AtomicReference<RuntimeException> clientFailure = new AtomicReference<>();

        try {
            Client client = new Client(InetAddress.getLocalHost(), SOLVE_MAZE_PORT, (inputStream, outputStream) -> {
                try {
                    ObjectOutputStream toServer = new ObjectOutputStream(outputStream);
                    ObjectInputStream fromServer = new ObjectInputStream(inputStream);

                    toServer.flush();
                    toServer.writeObject(mazeToSolve);
                    toServer.flush();

                    solvedMaze.set((Solution) fromServer.readObject());
                } catch (IOException | ClassNotFoundException e) {
                    clientFailure.set(new IllegalStateException("Failed to solve maze through the server.", e));
                }
            });
            client.communicateWithServer();
        } catch (UnknownHostException e) {
            throw new IllegalStateException("Could not connect to the local maze solving server.", e);
        }

        if (clientFailure.get() != null) {
            solvingLogger.error("Maze solving server request failed.", clientFailure.get());
            throw clientFailure.get();
        }
        if (solvedMaze.get() == null) {
            throw new IllegalStateException("Maze solving server did not return a solution.");
        }

        return solvedMaze.get();
    }
}
