package View;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Entry point for the ATP Maze desktop application.
 *
 * Main loads the welcome screen first. From there WelcomeViewController handles the
 * scene transition to the game screen after the user selects a couple and clicks Start.
 *
 * The class is intentionally minimal: no MVVM wiring happens here because the
 * welcome controller owns the lifecycle of both screens.
 */
public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Load the opening / welcome screen
        FXMLLoader welcomeLoader = new FXMLLoader(Main.class.getResource("/View/WelcomeView.fxml"));
        Scene welcomeScene = new Scene(welcomeLoader.load(), 900, 650);
        welcomeScene.getStylesheets().add(
                Main.class.getResource("/Styles/app.css").toExternalForm()
        );

        // Give the controller a reference to the Stage so it can swap scenes later
        WelcomeViewController welcomeController = welcomeLoader.getController();
        welcomeController.setStage(primaryStage);

        primaryStage.setTitle("ATP Maze Game");
        primaryStage.setScene(welcomeScene);
        primaryStage.setMinWidth(700);
        primaryStage.setMinHeight(500);
        primaryStage.show();
    }
}
