module atp.project.atpprojectpartc {
    requires javafx.controls;
    requires javafx.fxml;


    opens atp.project.atpprojectpartc to javafx.fxml;
    exports atp.project.atpprojectpartc;
}