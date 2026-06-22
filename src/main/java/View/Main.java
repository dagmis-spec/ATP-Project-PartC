package View;

import Model.MyModel;
import ViewModel.MyViewModel;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Starts the desktop client for Part C.
 *
 * This class is intentionally small: it only creates the JavaFX window and loads the
 * root FXML file. All screen behavior belongs in {@link MyViewController}, and all maze
 * logic belongs behind the ViewModel/Model layers.
 */
public class Main extends Application {
    /**
     * Creates the primary scene from MyView.fxml.
     * The assignment requires a resizable window, so width and height here are only
     * initial values. The minimum size prevents the menu and future maze board from
     * collapsing into an unusable layout.
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        // The FXML stays under resources/View to keep all UI layout files in the View layer.
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("/View/MyView.fxml"));

        Scene scene = new Scene(fxmlLoader.load(), 900, 650);
        scene.getStylesheets().add(Main.class.getResource("/Styles/app.css").toExternalForm());
        MyViewController controller = fxmlLoader.getController();
        MyViewModel viewModel = new MyViewModel(new MyModel());
        controller.setViewModel(viewModel);

        primaryStage.setTitle("ATP Maze Game");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(700);
        primaryStage.setMinHeight(500);
        primaryStage.setOnCloseRequest(event -> controller.onExit());
        primaryStage.show();
    }
}
