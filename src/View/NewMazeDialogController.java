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
 * Modal form for collecting maze dimensions.
 * Result values remain -1 when the user cancels or enters invalid input.
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

    /** Allows the dialog controller to close its own Stage. */
    public void setDialogStage(Stage stage) {
        this.dialogStage = stage;
    }

    @FXML
    private void initialize() {
        dialogBackground.fitWidthProperty().bind(dialogRoot.widthProperty());
        dialogBackground.fitHeightProperty().bind(dialogRoot.heightProperty());

        var url = getClass().getResource("/Images/wallpaper2.png");
        if (url != null) {
            dialogBackground.setImage(new Image(url.toExternalForm()));
        } else {
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
                // Invalid input is logged before a model request is created.
                logger.warn("Invalid maze dimensions: rows={}, columns={}. Values must be at least 2.",
                        rows, cols);
                showError("Rows and columns must each be at least 2.");
                return;
            }

            resultRows = rows;
            resultCols = cols;
            dialogStage.close();

        } catch (NumberFormatException e) {
            // Non-numeric input is a recoverable user error.
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
