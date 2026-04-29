module tech.cumlaude.cakman.cakman {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;


    opens tech.cumlaude.cakman.cakman to javafx.fxml;
    exports tech.cumlaude.cakman.cakman;
}