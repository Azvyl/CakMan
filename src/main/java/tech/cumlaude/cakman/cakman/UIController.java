package tech.cumlaude.cakman.cakman;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class UIController {
    @FXML
    private Button btnPlay, btnScore, btnExit, btnMusic;

    @FXML
    public void initialize() {
        Button[] buttons = {btnPlay, btnScore, btnExit, btnMusic};

        for (Button b : buttons) {
            if (b != null) addHoverEffect(b);
        }
    }

    private void addHoverEffect(Button button) {
        ColorAdjust bright = new ColorAdjust();
        bright.setBrightness(0.2);

        button.setOnMouseEntered(e -> {
            button.setScaleX(1.1);
            button.setScaleY(1.1);
            button.setEffect(bright);
        });

        button.setOnMouseExited(e -> {
            button.setScaleX(1.0);
            button.setScaleY(1.0);
            button.setEffect(null);
        });

        //TODO: Audio Feedback
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

    //TODO: Music Manager
    private boolean isMuted = false;
    private Image imgMusicOn = new Image(getClass().getResourceAsStream("images/music_on.png"));
    private Image imgMusicOff = new Image(getClass().getResourceAsStream("images/music_off.png"));

    @FXML
    private void onMusicClick() {
        isMuted = !isMuted;
        ImageView view = (ImageView) btnMusic.getGraphic();
        view.setImage(isMuted ? imgMusicOff : imgMusicOn);
    }
}
