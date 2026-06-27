package View;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Controller for NewMazeDialog.fxml.
 *
 * The dialog is opened modally by MyViewController.onNewMaze().
 * After showAndWait() returns, the caller reads getResultRows() / getResultCols().
 * Values are -1 if the user cancelled or entered invalid input.
 */
public class NewMazeDialogController {
    private static final Logger logger = LogManager.getLogger(NewMazeDialogController.class);

    @FXML private StackPane dialogRoot;
    @FXML private ImageView dialogBackground;
    @FXML private TextField txtRows;
    @FXML private TextField txtCols;

    private Stage dialogStage;
    private int resultRows = -1;
    private int resultCols = -1;

    /** Injected by MyViewController so the dialog can close itself. */
    public void setDialogStage(Stage stage) {
        this.dialogStage = stage;
    }

    @FXML
    private void initialize() {
        // Stretch background to fill the dialog pane
        dialogBackground.fitWidthProperty().bind(dialogRoot.widthProperty());
        dialogBackground.fitHeightProperty().bind(dialogRoot.heightProperty());

        // Load the same wallpaper as the welcome screen
        var url = getClass().getResource("/Images/wallpaper2.avif");
        if (url != null) {
            dialogBackground.setImage(new Image(url.toExternalForm()));
        } else {
            // Fall back to the game background if AVIF is unavailable
            var fallback = getClass().getResource("/Images/background.jpg");
            if (fallback != null) dialogBackground.setImage(new Image(fallback.toExternalForm()));
        }
    }

    @FXML
    private void onCreate() {
        String rowsText = txtRows.getText().trim();
        String colsText = txtCols.getText().trim();

        try {
            int rows = Integer.parseInt(rowsText);
            int cols = Integer.parseInt(colsText);

            if (rows < 2 || cols < 2) {
                // This is user input validation in the View layer, before any Model/server request exists.
                logger.warn("Invalid maze dimensions: rows={}, columns={}. Values must be at least 2.",
                        rows, cols);
                showError("Rows and columns must each be at least 2.");
                return;
            }

            resultRows = rows;
            resultCols = cols;
            dialogStage.close();

        } catch (NumberFormatException e) {
            // Non-numeric input is expected user error, so log it as warning and keep showing the Alert.
            logger.warn("Invalid maze dimensions input: rows='{}', columns='{}'", rowsText, colsText);
            showError("Please enter whole numbers for rows and columns.");
        }
    }

    @FXML
    private void onCancel() {
        dialogStage.close();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Invalid Input");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public int getResultRows() { return resultRows; }
    public int getResultCols() { return resultCols; }
}
