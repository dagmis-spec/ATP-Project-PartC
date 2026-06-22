module atp.project.atpprojectpartc {
    // Required by the JavaFX controls declared in FXML and created in controller code.
    requires javafx.controls;

    // Required so FXMLLoader can parse MyView.fxml and instantiate its controller.
    requires javafx.fxml;
    requires javafx.media;
    requires ATPProjectJAR;

    // FXMLLoader creates View.MyViewController reflectively from MyView.fxml.
    opens View to javafx.fxml;

    // Expose the project layers inside this named module.
    exports View;
    exports ViewModel;
    exports Model;
}
