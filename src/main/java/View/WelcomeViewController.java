package View;

import Model.MyModel;
import ViewModel.MyViewModel;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Stage;

/**
 * Controller for WelcomeView.fxml — the game opening screen.
 *
 * Responsibilities:
 *   - Load and display the couple selection images at runtime (filenames contain spaces,
 *     so they are loaded here rather than embedded as FXML attributes).
 *   - Track which couple card the user clicks and highlight it.
 *   - On "Start Game", wire the full MVVM stack and switch the scene to the game screen.
 *
 * This controller intentionally keeps no game state (maze, player, solution).
 * Game state belongs entirely in MyModel and flows through MyViewModel.
 */
public class WelcomeViewController {

    @FXML private StackPane rootPane;
    @FXML private ImageView backgroundView;

    // Couple preview images (shown on the welcome cards)
    @FXML private ImageView imgBrideGroom;
    @FXML private ImageView imgBrideBride;
    @FXML private ImageView imgGroomGroom;

    // The three clickable cards
    @FXML private VBox card1;
    @FXML private VBox card2;
    @FXML private VBox card3;

    // The Start button — enabled only after a couple is selected
    @FXML private Button btnStart;

    private Stage stage;
    private CoupleType selectedCouple = null;
    private MediaPlayer startSoundPlayer;

    /** Called by Main after loading this FXML so the controller can switch scenes. */
    public void setStage(Stage stage) {
        this.stage = stage;
    }

    /**
     * Runs after JavaFX injects FXML fields.
     *
     * Binds the background ImageView to fill the full pane (can't be done in FXML),
     * and loads all couple images from the classpath.
     */
    @FXML
    private void initialize() {
        // Stretch the background to always fill the entire pane
        backgroundView.fitWidthProperty().bind(rootPane.widthProperty());
        backgroundView.fitHeightProperty().bind(rootPane.heightProperty());

        loadInto(backgroundView, "/Images/wallpaper2.png");

        // Couple preview images — file names contain spaces so they must be loaded here
        loadInto(imgBrideGroom, "/Images/bride and groom.png");
        loadInto(imgBrideBride, "/Images/2 brides.png");
        loadInto(imgGroomGroom, "/Images/groom and groom.png");

        // Background music for the welcome / couple-selection screen
        var soundUrl = getClass().getResource("/sounds/startSound.mp3");
        if (soundUrl != null) {
            startSoundPlayer = new MediaPlayer(new Media(soundUrl.toExternalForm()));
            startSoundPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            startSoundPlayer.setVolume(0.40);
            startSoundPlayer.play();
        }
    }

    // -----------------------------------------------------------------------
    // Couple selection
    // -----------------------------------------------------------------------

    @FXML
    private void onSelectBrideGroom() {
        selectCouple(CoupleType.BRIDE_GROOM, card1);
    }

    @FXML
    private void onSelectBrideBride() {
        selectCouple(CoupleType.BRIDE_BRIDE, card2);
    }

    @FXML
    private void onSelectGroomGroom() {
        selectCouple(CoupleType.GROOM_GROOM, card3);
    }

    /**
     * Records the chosen couple, moves the visual selection highlight to the clicked card,
     * and enables the Start Game button.
     */
    private void selectCouple(CoupleType couple, VBox clickedCard) {
        selectedCouple = couple;

        // Remove highlight from every card, then add it only to the chosen one
        for (VBox card : new VBox[]{ card1, card2, card3 }) {
            card.getStyleClass().remove("couple-card-selected");
        }
        clickedCard.getStyleClass().add("couple-card-selected");

        btnStart.setDisable(false);
    }

    // -----------------------------------------------------------------------
    // Button actions
    // -----------------------------------------------------------------------

    /**
     * Transitions to the game screen.
     *
     * Loads MyView.fxml, wires the MVVM chain (Model → ViewModel → Controller),
     * passes the selected couple to the MazeDisplayer, and swaps the scene on the Stage.
     */
    @FXML
    private void onStartGame() {
        if (selectedCouple == null) {
            return; // button should be disabled, but guard anyway
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/View/MyView.fxml"));
            Scene gameScene = new Scene(loader.load(), 900, 650);
            gameScene.getStylesheets().add(
                    getClass().getResource("/Styles/app.css").toExternalForm()
            );

            // Wire MVVM layers: Model → ViewModel → View (slide 18 compliance)
            MyModel model = new MyModel();
            MyViewModel viewModel = new MyViewModel(model);
            model.addObserver(viewModel);

            MyViewController controller = loader.getController();
            viewModel.addObserver(controller);
            controller.setViewModel(viewModel);
            controller.setCouple(selectedCouple); // apply the couple the user chose

            // Stop welcome-screen music before the game starts
            if (startSoundPlayer != null) startSoundPlayer.stop();

            // Clean application exit from the game screen should shut the model down
            stage.setOnCloseRequest(event -> controller.onExit());

            stage.setScene(gameScene);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onExit() {
        if (startSoundPlayer != null) startSoundPlayer.stop();
        Platform.exit();
        System.exit(0);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Loads a classpath image into an ImageView via its URL string.
     *
     * Using getResource().toExternalForm() gives JavaFX the full URL including
     * the file extension, which is required for the platform image decoder to
     * recognise formats such as AVIF that are not detected from a raw stream.
     */
    private void loadInto(ImageView view, String resourcePath) {
        var url = getClass().getResource(resourcePath);
        if (url != null) {
            view.setImage(new Image(url.toExternalForm()));
        }
    }
}
