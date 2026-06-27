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
 * Owns game state, persistence, and communication with the maze services.
 */
public class MyModel extends java.util.Observable implements IModel {
    private static final Logger logger = LogManager.getLogger(MyModel.class);
    private static final Logger generationLogger = LogManager.getLogger("generation-server");
    private static final Logger solvingLogger = LogManager.getLogger("solving-server");

    /* Local service ports used by the model's client requests. */
    private static final int GENERATE_MAZE_PORT = 5400;
    private static final int SOLVE_MAZE_PORT = 5401;
    private static final int SERVER_LISTENING_INTERVAL_MS = 1000;

    /* Runtime state for the active game. */
    private Maze maze;
    private Position playerPos;
    private Solution solution;
    private Server generateMazeServer;
    private Server solveMazeServer;
    private boolean serversStarted;

    /** Starts local generation and solving services for this model instance. */
    public MyModel() {
        startServers();
    }

    /** Creates a maze through the generation service and resets dependent state. */
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

        setChanged();
        notifyObservers("generateMaze");
    }

    /** Returns the active maze object. */
    @Override
    public Maze getMaze() {
        return maze;
    }

    /** Returns the player's current maze position. */
    @Override
    public Position getPlayerPosition() {
        return playerPos;
    }

    /** Applies a row/column movement delta after checking walls and boundaries. */
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

    /** Assumes maze is not null; public callers check that first. */
    private boolean isInsideMaze(int row, int column) {
        return row >= 0 && row < maze.getRows() && column >= 0 && column < maze.getColumns();
    }

    /** Checks whether the player has reached the maze goal cell. */
    @Override
    public boolean isMazeSolved() {
        if (maze == null || playerPos == null) {
            return false;
        }

        Position goalPosition = maze.getGoalPosition();
        return playerPos.getRowIndex() == goalPosition.getRowIndex()
                && playerPos.getColumnIndex() == goalPosition.getColumnIndex();
    }

    /** Solves the active maze through the solving service. */
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

        setChanged();
        notifyObservers("solveMaze");
    }

    /** Reads a configuration value from the classpath resource. */
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

    /** Returns the current solution, or null when no solution is available. */
    @Override
    public Solution getSolution() {
        return solution;
    }

    /** Saves the active maze with the same compressed format used for loading. */
    @Override
    public void saveMaze(File file) throws IOException {
        if (maze == null) {
            throw new IllegalStateException("Cannot save a maze before one is generated or loaded.");
        }

        if (file == null) {
            throw new IllegalArgumentException("Save file cannot be null.");
        }

        logger.info("Saving active maze to file: {}", file.getAbsolutePath());

        /*
         * Use the maze byte format so saved files can be loaded by the matching decompressor.
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

    /** Loads a maze file and resets player and solution state. */
    @Override
    public void loadMaze(File file) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("Load file cannot be null.");
        }

        logger.info("Loading maze from file: {}", file.getAbsolutePath());
        byte[] decompressedMazeBytes;

        /*
         * Loaded files are expected to use the compressed maze format written by saveMaze.
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

    /** Formats configuration values for display. */
    @Override
    public String getPropertiesText() {
        return getConfiguredValue("threadPoolSize", "(not set)") == null ? "" :
                "threadPoolSize = " + getConfiguredValue("threadPoolSize", "") + "\n"
                + "mazeGeneratingAlgorithm = " + getConfiguredValue("mazeGeneratingAlgorithm", "") + "\n"
                + "mazeSearchingAlgorithm = " + getConfiguredValue("mazeSearchingAlgorithm", "");
    }

    /** Stops model-owned background services. */
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

    /** Starts separate local services for maze generation and solving. */
    private void startServers() {
        configureServerSettings();

        generateMazeServer = new Server(
                GENERATE_MAZE_PORT,
                SERVER_LISTENING_INTERVAL_MS,
                new LoggingServerStrategy(
                        new ServerStrategyGenerateMaze(),
                        generationLogger,
                        "Maze generation"
                )
        );
        solveMazeServer = new Server(
                SOLVE_MAZE_PORT,
                SERVER_LISTENING_INTERVAL_MS,
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

    /** Applies config.properties values to the shared server configuration. */
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

    /** Requests a generated maze and converts the compressed response into a Maze. */
    private Maze requestMazeFromServer(int rows, int columns) {
        AtomicReference<Maze> generatedMaze = new AtomicReference<>();
        // Client strategy lambdas cannot propagate checked exceptions directly.
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

    /** Converts compressed maze bytes into a Maze instance. */
    private Maze decompressMaze(byte[] compressedMazeBytes) throws IOException {
        try (MyDecompressorInputStream inputStream =
                     new MyDecompressorInputStream(new ByteArrayInputStream(compressedMazeBytes))) {
            return new Maze(inputStream.readAllBytes());
        }
    }

    /** Sends the active maze to the solving service and returns its solution. */
    private Solution requestSolutionFromServer(Maze mazeToSolve) {
        AtomicReference<Solution> solvedMaze = new AtomicReference<>();
        // Client strategy lambdas cannot propagate checked exceptions directly.
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
