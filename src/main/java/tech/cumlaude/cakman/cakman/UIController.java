package tech.cumlaude.cakman.cakman;

import javafx.fxml.FXML;
import javafx.scene.control.Button;

public class UIController {
    @FXML
    private Button btnScore;
    @FXML
    private Button btnPlay;
    @FXML
    private Button btnExit;
    @FXML
    private Button btnMusic;

    @FXML
    public void initialize() {
        addHoverEffect(btnScore);
        addHoverEffect(btnPlay);
        addHoverEffect(btnExit);
        addHoverEffect(btnMusic);
    }

    private void addHoverEffect(Button button) {
        button.setOnMouseEntered(e -> {
            button.setScaleX(1.1);
            button.setScaleY(1.1);
        });

        button.setOnMouseExited(e -> {
            button.setScaleX(1.0);
            button.setScaleY(1.0);
        });

        button.setOnMousePressed(e -> {
            button.setScaleX(0.9);
            button.setScaleY(0.9);
        });

        button.setOnMouseReleased(e -> {
            button.setScaleX(1.1);
            button.setScaleY(1.1);
        });
    }

    @FXML
    private void onExitClick() {
        System.exit(0);
    }
}
