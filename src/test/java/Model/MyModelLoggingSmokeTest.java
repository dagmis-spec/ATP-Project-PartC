package Model;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * End-to-end logging smoke test for model operations that use local services.
 * The application must be closed because the services use fixed local ports.
 */
class MyModelLoggingSmokeTest {
    @Test
    void modelOperationsCreateInfoAndErrorLogEvents() throws Exception {
        assumeTrue(portsAreAvailable(5400, 5401),
                "Close the JavaFX app before running this smoke test; ports 5400/5401 are already in use.");

        Path applicationLog = Path.of("logs", "application.log");
        Files.deleteIfExists(applicationLog);

        MyModel model = new MyModel();
        File savedMaze = File.createTempFile("atp-maze-smoke", ".maze");
        Path tempDirectory = Files.createTempDirectory("atp-maze-smoke-dir");
        File missingMazeFile = tempDirectory.resolve("missing.maze").toFile();

        try {
            // Uses the real generation server, which should write INFO entries.
            model.generateMaze(5, 5);
            assertNotNull(model.getMaze());

            // Uses the real solving server, which should also write INFO entries.
            model.solveMaze();
            assertNotNull(model.getSolution());

            model.saveMaze(savedMaze);
            model.loadMaze(savedMaze);
            assertNotNull(model.getMaze());

            // Saving to a directory is invalid and should produce an ERROR log entry.
            assertThrows(Exception.class, () -> model.saveMaze(tempDirectory.toFile()));

            // Loading a missing file is invalid and should produce another ERROR log entry.
            assertThrows(Exception.class, () -> model.loadMaze(missingMazeFile));

            String logText = readLogFile(applicationLog);
            assertTrue(logText.contains("INFO"), "Expected INFO logs for normal server operations.");
            assertTrue(logText.contains("ERROR"), "Expected ERROR logs for invalid save/load operations.");
            assertTrue(logText.contains("Failed to save maze to file"),
                    "Expected failed-save message in application.log.");
            assertTrue(logText.contains("Failed to load maze from file"),
                    "Expected failed-load message in application.log.");
        } finally {
            model.shutdown();
            if (savedMaze.exists()) {
                savedMaze.delete();
            }
            Files.deleteIfExists(missingMazeFile.toPath());
            Files.deleteIfExists(tempDirectory);
        }
    }

    private String readLogFile(Path logPath) throws Exception {
        for (int attempt = 0; attempt < 10 && !Files.exists(logPath); attempt++) {
            Thread.sleep(100);
        }
        return Files.readString(logPath, StandardCharsets.UTF_8);
    }

    private boolean portsAreAvailable(int... ports) {
        for (int port : ports) {
            try (ServerSocket ignored = new ServerSocket(port)) {
                // Opening and closing the socket proves the port is currently free for the real test server.
            } catch (Exception e) {
                return false;
            }
        }
        return true;
    }
}
