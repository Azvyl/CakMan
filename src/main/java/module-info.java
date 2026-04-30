module tech.cumlaude.cakman.cakman {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;
    requires java.sql;


    opens tech.cumlaude.cakman.cakman to javafx.fxml;
    exports tech.cumlaude.cakman.cakman;
    exports tech.cumlaude.cakman.cakman.entity;
    opens tech.cumlaude.cakman.cakman.entity to javafx.fxml;
    exports tech.cumlaude.cakman.cakman.controller;
    opens tech.cumlaude.cakman.cakman.controller to javafx.fxml;
}