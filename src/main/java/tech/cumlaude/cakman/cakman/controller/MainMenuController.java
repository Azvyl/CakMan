package tech.cumlaude.cakman.cakman.controller;

import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import tech.cumlaude.cakman.cakman.manager.AudioManager;

import java.net.URL;

public class MainMenuController {
    @FXML
    private Button btnPlay, btnScore, btnExit, btnMusic;
    @FXML
    private TextField txtPlayerName;

    private Image imgMusicOn;
    private Image imgMusicOff;

    @FXML
    public void initialize() {
        Button[] buttons = {btnPlay, btnScore, btnExit, btnMusic};

        imgMusicOn = loadImage("/tech/cumlaude/cakman/cakman/images/button/music_on.png");
        imgMusicOff = loadImage("/tech/cumlaude/cakman/cakman/images/button/music_off.png");

        for (Button b : buttons) {
            if (b != null) addHoverEffect(b);
        }

        AudioManager audioManager = AudioManager.getInstance();
        audioManager.playMenuMusic();
        updateMusicButtonIcon(audioManager.isMusicEnabled());

        if (txtPlayerName != null) {
            txtPlayerName.setText("PLAYER");
        }
    }

    private void updateMusicButtonIcon(boolean musicEnabled) {
        if (btnMusic == null || btnMusic.getGraphic() == null || imgMusicOn == null || imgMusicOff == null) {
            return;
        }

        ImageView view = (ImageView) btnMusic.getGraphic();
        view.setImage(musicEnabled ? imgMusicOn : imgMusicOff);
    }

    private Image loadImage(String path) {
        URL url = getClass().getResource(path);
        return url == null ? null : new Image(url.toExternalForm());
    }

    private void addHoverEffect(Button button) {
        ColorAdjust bright = new ColorAdjust();
        bright.setBrightness(0.2);

        button.setOnMouseEntered(_ -> {
            button.setScaleX(1.1);
            button.setScaleY(1.1);
            button.setEffect(bright);
        });

        button.setOnMouseExited(_ -> {
            button.setScaleX(1.0);
            button.setScaleY(1.0);
            button.setEffect(null);
        });

        button.setOnMousePressed(_ -> {
            AudioManager.getInstance().playSoundEffect(AudioManager.SFX_BUTTON_CLICK);
            button.setScaleX(0.9);
            button.setScaleY(0.9);
        });

        button.setOnMouseReleased(_ -> {
            button.setScaleX(1.1);
            button.setScaleY(1.1);
        });
    }

    @FXML
    private void onPlayClick() {
        try {
            Stage stage = (Stage) btnPlay.getScene().getWindow();

            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/tech/cumlaude/cakman/cakman/game.fxml"));
            Pane gameRoot = loader.load();

            Scene scene = stage.getScene();
            scene.setRoot(gameRoot);

            GameController controller = loader.getController();
            if (controller != null) controller.startGame(stage, resolvePlayerName());
        } catch (Exception e) {
            System.err.println("Failed to open game scene: " + e.getMessage());
        }
    }

    private String resolvePlayerName() {
        if (txtPlayerName == null || txtPlayerName.getText() == null) {
            return "PLAYER";
        }
        String value = txtPlayerName.getText().trim();
        return value.isEmpty() ? "PLAYER" : value;
    }

    @FXML
    private void onExitClick() {
        System.exit(0);
    }

    @FXML
    private void onMusicClick() {
        AudioManager audioManager = AudioManager.getInstance();
        audioManager.toggleMusic();
        updateMusicButtonIcon(audioManager.isMusicEnabled());
    }
}
