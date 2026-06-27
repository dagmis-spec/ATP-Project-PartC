module atp.project.atpprojectpartc {
    // JavaFX controls used by FXML files and controllers.
    requires javafx.controls;

    // FXML loading, media playback, project services, and logging.
    requires javafx.fxml;
    requires javafx.media;
    requires ATPProjectJAR;
    requires org.apache.logging.log4j;

    // FXMLLoader creates View controllers reflectively.
    opens View to javafx.fxml;

    // Expose project layers to the application runtime.
    exports View;
    exports ViewModel;
    exports Model;
}
