package tech.cumlaude.cakman.cakman.controller;

import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import tech.cumlaude.cakman.cakman.manager.AudioManager;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainMenuController {
    @FXML
    private Button btnPlay, btnScore, btnExit, btnMusic;
    @FXML
    private TextField txtPlayerName;

    private Image imgMusicOn;
    private Image imgMusicOff;
    private List<Button> focusableButtons;
    private int focusedButtonIndex = -1;

    @FXML
    public void initialize() {
        focusableButtons = new ArrayList<>();
        Button[] buttons = {btnPlay, btnScore, btnExit, btnMusic};

        imgMusicOn = loadImage("/tech/cumlaude/cakman/cakman/images/button/music_on.png");
        imgMusicOff = loadImage("/tech/cumlaude/cakman/cakman/images/button/music_off.png");

        for (Button b : buttons) {
            if (b != null) {
                focusableButtons.add(b);
                addHoverEffect(b);
            }
        }

        AudioManager audioManager = AudioManager.getInstance();
        audioManager.playMenuMusic();
        updateMusicButtonIcon(audioManager.isMusicEnabled());

        if (txtPlayerName != null) {
            txtPlayerName.setText("PLAYER");
        }

        // Add keyboard navigation
        setupKeyboardNavigation();
    }

    private void setupKeyboardNavigation() {
        // Get the scene for keyboard events
        var scene = btnPlay.getScene();
        if (scene != null) {
            scene.setOnKeyPressed(this::handleKeyPress);
        }
    }

    private void handleKeyPress(KeyEvent e) {
        if (e.getCode() == KeyCode.TAB) {
            e.consume();
            if (e.isShiftDown()) {
                focusPreviousButton();
            } else {
                focusNextButton();
            }
        } else if (e.getCode() == KeyCode.ENTER && focusedButtonIndex >= 0) {
            e.consume();
            focusableButtons.get(focusedButtonIndex).fire();
        }
    }

    private void focusNextButton() {
        focusedButtonIndex++;
        if (focusedButtonIndex >= focusableButtons.size()) {
            focusedButtonIndex = 0;
        }
        updateFocusedButton();
    }

    private void focusPreviousButton() {
        focusedButtonIndex--;
        if (focusedButtonIndex < 0) {
            focusedButtonIndex = focusableButtons.size() - 1;
        }
        updateFocusedButton();
    }

    private void updateFocusedButton() {
        for (int i = 0; i < focusableButtons.size(); i++) {
            Button btn = focusableButtons.get(i);
            if (i == focusedButtonIndex) {
                btn.setStyle(btn.getStyle() + "-fx-border-color:#00FF00; -fx-border-width:3;");
                btn.setScaleX(1.1);
                btn.setScaleY(1.1);
            } else {
                // Reset to normal style
                resetButtonStyle(btn);
            }
        }
    }

    private void resetButtonStyle(Button btn) {
        btn.setScaleX(1.0);
        btn.setScaleY(1.0);
        btn.setStyle("-fx-background-color: transparent;");
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
            focusedButtonIndex = focusableButtons.indexOf(button);
        });

        button.setOnMouseExited(_ -> {
            button.setScaleX(1.0);
            button.setScaleY(1.0);
            button.setEffect(null);
            focusedButtonIndex = -1;
        });

        button.setOnMousePressed(_ -> {
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
        AudioManager.getInstance().playSoundEffect(AudioManager.SFX_BUTTON_CLICK);
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
    private void onScoreClick() {
        AudioManager.getInstance().playSoundEffect(AudioManager.SFX_BUTTON_CLICK);
        try {
            Stage stage = (Stage) btnScore.getScene().getWindow();

            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/tech/cumlaude/cakman/cakman/leaderboard.fxml"));
            Pane leaderboardRoot = loader.load();

            Scene scene = stage.getScene();
            scene.setRoot(leaderboardRoot);
        } catch (Exception e) {
            System.err.println("Failed to open leaderboard: " + e.getMessage());
        }
    }

    @FXML
    private void onExitClick() {
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Konfirmasi");
        confirmDialog.setHeaderText("Keluar Aplikasi");
        confirmDialog.setContentText("Yakin ingin keluar dari aplikasi?");
        confirmDialog.getButtonTypes().clear();

        javafx.scene.control.ButtonType yesButton = new javafx.scene.control.ButtonType("Ya");
        javafx.scene.control.ButtonType noButton = new javafx.scene.control.ButtonType("Tidak");
        confirmDialog.getButtonTypes().addAll(yesButton, noButton);

        var result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == yesButton) {
            System.exit(0);
        }
    }

    @FXML
    private void onMusicClick() {
        AudioManager.getInstance().playSoundEffect(AudioManager.SFX_BUTTON_CLICK);
        AudioManager audioManager = AudioManager.getInstance();
        audioManager.toggleMusic();
        updateMusicButtonIcon(audioManager.isMusicEnabled());
    }
}
