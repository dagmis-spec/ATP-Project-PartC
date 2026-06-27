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
 * Opening screen controller. Handles couple selection and creates the game scene.
 */
public class WelcomeViewController {

    @FXML private StackPane rootPane;
    @FXML private ImageView backgroundView;
    @FXML private ImageView imgBrideGroom;
    @FXML private ImageView imgBrideBride;
    @FXML private ImageView imgGroomGroom;
    @FXML private VBox card1;
    @FXML private VBox card2;
    @FXML private VBox card3;
    @FXML private Button btnStart;

    private Stage stage;
    private CoupleType selectedCouple = null;
    private MediaPlayer startSoundPlayer;

    /** Provides the primary Stage used for the scene switch. */
    public void setStage(Stage stage) {
        this.stage = stage;
    }

    /** Loads images, sizes the background, and starts welcome music. */
    @FXML
    private void initialize() {
        backgroundView.fitWidthProperty().bind(rootPane.widthProperty());
        backgroundView.fitHeightProperty().bind(rootPane.heightProperty());

        loadInto(backgroundView, "/Images/wallpaper2.png");
        loadInto(imgBrideGroom, "/Images/bride and groom.png");
        loadInto(imgBrideBride, "/Images/2 brides.png");
        loadInto(imgGroomGroom, "/Images/groom and groom.png");

        var soundUrl = getClass().getResource("/sounds/startSound.mp3");
        if (soundUrl != null) {
            startSoundPlayer = new MediaPlayer(new Media(soundUrl.toExternalForm()));
            startSoundPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            startSoundPlayer.setVolume(0.40);
            startSoundPlayer.play();
        }
    }

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

    /** Stores the selected couple and updates card highlighting. */
    private void selectCouple(CoupleType couple, VBox clickedCard) {
        selectedCouple = couple;

        for (VBox card : new VBox[]{ card1, card2, card3 }) {
            card.getStyleClass().remove("couple-card-selected");
        }
        clickedCard.getStyleClass().add("couple-card-selected");

        btnStart.setDisable(false);
    }

    /** Creates the MVVM objects and switches to the game scene. */
    @FXML
    private void onStartGame() {
        if (selectedCouple == null) {
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/View/MyView.fxml"));
            Scene gameScene = new Scene(loader.load(), 900, 650);
            gameScene.getStylesheets().add(
                    getClass().getResource("/View/app.css").toExternalForm()
            );

            MyModel model = new MyModel();
            MyViewModel viewModel = new MyViewModel(model);
            model.addObserver(viewModel);

            MyViewController controller = loader.getController();
            viewModel.addObserver(controller);
            controller.setViewModel(viewModel);
            controller.setCouple(selectedCouple);

            if (startSoundPlayer != null) {
                startSoundPlayer.stop();
            }

            stage.setOnCloseRequest(event -> controller.onExit());
            stage.setScene(gameScene);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onExit() {
        if (startSoundPlayer != null) {
            startSoundPlayer.stop();
        }
        Platform.exit();
        System.exit(0);
    }

    /** Loads a classpath image into an ImageView. */
    private void loadInto(ImageView view, String resourcePath) {
        var url = getClass().getResource(resourcePath);
        if (url != null) {
            view.setImage(new Image(url.toExternalForm()));
        }
    }
}
