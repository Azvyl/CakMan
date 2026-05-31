package tech.cumlaude.cakman.cakman.controller;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import tech.cumlaude.cakman.cakman.manager.AudioManager;
import tech.cumlaude.cakman.cakman.manager.DatabaseManager;
import tech.cumlaude.cakman.cakman.manager.HighScoreEntry;

import java.util.List;

public class LeaderboardController {
    @FXML
    private VBox scoresContainer;
    @FXML
    private Button btnBack;

    private final DatabaseManager databaseManager = new DatabaseManager();

    @FXML
    public void initialize() {
        loadLeaderboard();
        addHoverEffect(btnBack);
        btnBack.setOnAction(_ -> onBackClick());
    }

    private void loadLeaderboard() {
        try {
            List<HighScoreEntry> entries = databaseManager.getTop5HighScores();
            scoresContainer.getChildren().clear();

            if (entries.isEmpty()) {
                Label emptyLabel = new Label("Belum ada data skor");
                emptyLabel.setStyle("-fx-font-size:24px; -fx-text-fill:rgba(255,255,255,0.7);");
                scoresContainer.getChildren().add(emptyLabel);
                return;
            }

            Color[] medalColors = {Color.web("#FFD700"), Color.web("#C0C0C0"), Color.web("#CD7F32")};

            for (int i = 0; i < entries.size(); i++) {
                HighScoreEntry entry = entries.get(i);
                HBox scoreRow = createScoreRow(i + 1, entry, i < medalColors.length ? medalColors[i] : Color.web("#FFFFFF"));
                scoresContainer.getChildren().add(scoreRow);
            }
        } catch (Exception e) {
            Label errorLabel = new Label("Error loading scores: " + e.getMessage());
            errorLabel.setStyle("-fx-font-size:18px; -fx-text-fill:#FF6666;");
            scoresContainer.getChildren().add(errorLabel);
        }
    }

    private HBox createScoreRow(int rank, HighScoreEntry entry, Color medalColor) {
        HBox row = new HBox(15);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 15, 10, 15));
        row.setStyle("-fx-background-color: rgba(0,0,0,0.3); -fx-background-radius: 8; -fx-border-color: rgb(0,255,255); -fx-border-width: 1; -fx-border-radius: 8;");

        // Medal emoji
        String medal = switch (rank) {
            case 1 -> "🥇";
            case 2 -> "🥈";
            case 3 -> "🥉";
            default -> rank + ".";
        };

        Label medalLabel = new Label(medal);
        medalLabel.setStyle("-fx-font-size:28px; -fx-min-width:50;");

        // Rank
        Label rankLabel = new Label(String.valueOf(rank));
        rankLabel.setStyle("-fx-font-size:18px; -fx-text-fill:" + toHexColor(medalColor) + "; -fx-font-weight:bold; -fx-min-width:40;");

        // Player name
        Label nameLabel = new Label(entry.name());
        nameLabel.setStyle("-fx-font-size:18px; -fx-text-fill:white; -fx-font-weight:bold;");
        HBox.setHgrow(nameLabel, Priority.ALWAYS);

        // Score
        Label scoreLabel = new Label(String.valueOf(entry.score()));
        scoreLabel.setStyle("-fx-font-size:18px; -fx-text-fill:" + toHexColor(medalColor) + "; -fx-font-weight:bold; -fx-min-width:100; -fx-text-alignment:right;");

        row.getChildren().addAll(medalLabel, rankLabel, nameLabel, scoreLabel);
        return row;
    }

    private String toHexColor(Color color) {
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }

    private void addHoverEffect(Button button) {
        button.setOnMouseEntered(_ -> {
            button.setScaleX(1.05);
            button.setScaleY(1.05);
            button.setStyle(button.getStyle() + "-fx-background-color:#2a2a9e;");
        });

        button.setOnMouseExited(_ -> {
            button.setScaleX(1.0);
            button.setScaleY(1.0);
            button.setStyle("-fx-font-size:18px; -fx-font-weight:bold; -fx-text-fill:white; -fx-background-color:#1a1a4e; -fx-border-color:#00FFFF; -fx-border-width:2; -fx-border-radius:8; -fx-background-radius:8; -fx-padding:12 40 12 40; -fx-cursor:hand;");
        });
    }

    @FXML
    private void onBackClick() {
        try {
            AudioManager.getInstance().playSoundEffect(AudioManager.SFX_BUTTON_CLICK);
            Stage stage = (Stage) btnBack.getScene().getWindow();
            var loader = new javafx.fxml.FXMLLoader(getClass().getResource("/tech/cumlaude/cakman/cakman/main-menu.fxml"));
            stage.getScene().setRoot(loader.load());
        } catch (Exception e) {
            System.err.println("Failed to go back to menu: " + e.getMessage());
        }
    }
}

