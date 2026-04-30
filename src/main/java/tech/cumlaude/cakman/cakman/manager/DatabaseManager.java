package tech.cumlaude.cakman.cakman.manager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {

    private final Path databasePath;

    public DatabaseManager() {
        this(Path.of("pacman_highscores.db"));
    }

    public DatabaseManager(Path databasePath) {
        this.databasePath = databasePath;
        initializeDatabase();
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + databasePath.toAbsolutePath());
    }

    private void initializeDatabase() {
        try {
            Path parent = databasePath.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Tidak dapat menyiapkan folder database", exception);
        }

        String sql = """
                CREATE TABLE IF NOT EXISTS highscores (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    player_name TEXT NOT NULL,
                    score INTEGER NOT NULL,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
                )
                """;

        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        } catch (SQLException exception) {
            throw new IllegalStateException("Gagal membuat tabel high score", exception);
        }
    }

    public void saveHighScore(String name, int score) {
        String safeName = name == null || name.isBlank() ? "PLAYER" : name.trim();
        String sql = "INSERT INTO highscores(player_name, score) VALUES(?, ?)";

        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, safeName);
            statement.setInt(2, score);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Gagal menyimpan high score", exception);
        }
    }

    public List<HighScoreEntry> getTopScores(int limit) {
        List<HighScoreEntry> scores = new ArrayList<>();
        String sql = "SELECT player_name, score, created_at FROM highscores ORDER BY score DESC, created_at ASC LIMIT ?";

        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, Math.max(1, limit));
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    scores.add(new HighScoreEntry(
                            resultSet.getString("player_name"),
                            resultSet.getInt("score"),
                            resultSet.getString("created_at")
                    ));
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Gagal membaca high score", exception);
        }

        return scores;
    }

    public List<HighScoreEntry> getTop5HighScores() {
        return getTopScores(5);
    }
}

