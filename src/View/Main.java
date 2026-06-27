package View;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * JavaFX entry point. Loads the welcome screen and delegates later scene changes
 * to the welcome controller.
 */
public class Main extends Application {
what is
    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader welcomeLoader = new FXMLLoader(Main.class.getResource("/View/WelcomeView.fxml"));
        Scene welcomeScene = new Scene(welcomeLoader.load(), 900, 650);
        welcomeScene.getStylesheets().add(
                Main.class.getResource("/View/app.css").toExternalForm()
        );

        WelcomeViewController welcomeController = welcomeLoader.getController();
        welcomeController.setStage(primaryStage);

        primaryStage.setTitle("ATP Maze Game");
        primaryStage.setScene(welcomeScene);
        primaryStage.setMinWidth(700);
        primaryStage.setMinHeight(500);
        primaryStage.show();
    }
}
