package View;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * Controller for NewMazeDialog.fxml.
 *
 * The dialog is opened modally by MyViewController.onNewMaze().
 * After showAndWait() returns, the caller reads getResultRows() / getResultCols().
 * Values are -1 if the user cancelled or entered invalid input.
 */
public class NewMazeDialogController {

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
        try {
            int rows = Integer.parseInt(txtRows.getText().trim());
            int cols = Integer.parseInt(txtCols.getText().trim());

            if (rows < 2 || cols < 2) {
                showError("Rows and columns must each be at least 2.");
                return;
            }

            resultRows = rows;
            resultCols = cols;
            dialogStage.close();

        } catch (NumberFormatException e) {
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
