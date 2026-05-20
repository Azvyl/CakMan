package tech.cumlaude.cakman.cakman.controller;

import javafx.animation.AnimationTimer;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;
import tech.cumlaude.cakman.cakman.entity.CakMan;
import tech.cumlaude.cakman.cakman.entity.Entity;
import tech.cumlaude.cakman.cakman.entity.Ghost;
import tech.cumlaude.cakman.cakman.manager.AudioManager;
import tech.cumlaude.cakman.cakman.world.MapData;
import tech.cumlaude.cakman.cakman.world.MapRenderer;

import java.util.*;

public class GameController {

    // ─── FXML ────────────────────────────────────────────────────────────────
    @FXML private Pane  mazeContainer;
    @FXML private Label lblCountdown;
    @FXML private Label lblScore;
    @FXML private Label lblLives;

    // ─── Konstanta ───────────────────────────────────────────────────────────
    private static final double CAKMAN_SPEED  = 105.0;
    private static final double GHOST_SPEED   = 100.0;
    private static final double GHOST_FRIGHT_SPEED = 85.0;   // lambat saat ketakutan
    private static final double ALIGN_EPS     = 0.75;
    private static final double TURN_WINDOW   = 6.0;

    // Ukuran ghost = TILE_SIZE supaya muat di jalur
    private static final int GHOST_DISPLAY_SIZE = MapData.TILE_SIZE + 4; // 30px, sedikit lebih besar dari tile tapi tidak meluber
    private static final int GHOST_OFFSET       = (GHOST_DISPLAY_SIZE - MapData.TILE_SIZE) / 2;

    private static final int PELLET_SCORE       = 10;
    private static final int POWER_PELLET_SCORE = 50;
    private static final int GHOST_KILL_SCORE   = 200;
    private static final int MAX_LIVES          = 3;
    private static final long FRIGHTEN_MS       = 8_000L;

    // Power pellet [row][col] — 2 lokasi pojok kiri & kanan row 5
    private static final int[][] POWER_PELLET_POS = {{5, 1}, {5, 26}};

    // Ghost spawn di dalam ghost house (row=11, berbagai col)
    private static final int[][] GHOST_SPAWN = {{11, 11}, {14, 11}, {17, 11}};
    // Ghost type images
    private static final String[] GHOST_DIRS = {"usa", "dutch", "japan"};

    // ─── CakMan state ────────────────────────────────────────────────────────
    private CakMan   cakman;
    private ImageView cakmanView;
    private Image imgUp, imgDown, imgRight;
    private Image imgPowerUp, imgPowerDown, imgPowerRight;

    private boolean[][] pelletGrid;
    private boolean[]   powerActive;
    private final List<javafx.scene.Node[]> powerNodes = new ArrayList<>();

    private int score, lives;
    private int curX, curY, tgtX, tgtY;
    private Entity.Direction activeDir, requestedDir;
    private boolean moving;
    private double movePx;
    private long lastNanos = 0L;

    // ─── Ghost state ─────────────────────────────────────────────────────────
    private final List<GhostAgent> ghosts = new ArrayList<>();
    private Image imgFrightened;
    // Audio restore flag when player collides with ghost (non-fatal)
    private boolean restoreMusicWhenMoved = false;
    private double previousMusicVolume = 0.55;

    // ─── Game state ──────────────────────────────────────────────────────────
    private AnimationTimer gameLoop;
    private Stage          gameStage;
    private Scene          gameScene;
    private boolean paused   = false;
    private boolean gameOver = false;
    private boolean victory  = false;
    private StackPane overlayPane;

    // =========================================================================
    //  ENTRY POINT
    // =========================================================================
    public void startGame(Stage stage) {
        this.gameStage = stage;
        new MapRenderer(mazeContainer).render();
        AudioManager.getInstance().playGameMusic();
        loadCakmanAssets();
        loadGhostAssets();
        spawnCakman();
        spawnGhosts();
        renderPowerPellets();
        runCountdown(stage);
    }

    // =========================================================================
    //  ASSET LOADING
    // =========================================================================
    private void loadCakmanAssets() {
        imgUp          = img("/tech/cumlaude/cakman/cakman/images/entity/cakman/up.png");
        imgDown        = img("/tech/cumlaude/cakman/cakman/images/entity/cakman/down.png");
        imgRight       = img("/tech/cumlaude/cakman/cakman/images/entity/cakman/right.png");
        imgPowerUp     = img("/tech/cumlaude/cakman/cakman/images/entity/cakman/power_up.png");
        imgPowerDown   = img("/tech/cumlaude/cakman/cakman/images/entity/cakman/power_down.png");
        imgPowerRight  = img("/tech/cumlaude/cakman/cakman/images/entity/cakman/power_right.png");
    }

    private void loadGhostAssets() {
        imgFrightened = img("/tech/cumlaude/cakman/cakman/images/entity/ghost/frightened.png");
    }

    private Image img(String path) {
        try {
            var url = getClass().getResource(path);
            if (url == null) { System.err.println("Missing: " + path); return null; }
            return new Image(url.toExternalForm());
        } catch (Exception e) { System.err.println("Error loading: " + path); return null; }
    }

    // =========================================================================
    //  SPAWN
    // =========================================================================
    private void spawnCakman() {
        initPellets();
        score = 0; lives = MAX_LIVES;
        int gx = 13, gy = 21;
        cakman = new CakMan(gx, gy, CAKMAN_SPEED);
        curX = gx; curY = gy; tgtX = gx; tgtY = gy;
        activeDir = null; requestedDir = null; moving = false; movePx = 0;

        cakmanView = new ImageView(imgRight);
        cakmanView.setFitWidth(MapData.ENTITY_SIZE);
        cakmanView.setFitHeight(MapData.ENTITY_SIZE);
        cakmanView.setPreserveRatio(true);
        mazeContainer.getChildren().add(cakmanView);
        placeEntityAt(cakmanView, gx, gy, MapData.ENTITY_SIZE, MapData.ENTITY_OFFSET);
        updateHUD();
        consumePellet(gx, gy);
    }

    private void spawnGhosts() {
        ghosts.clear();
        // Spawn dengan delay agar tidak bergerombol
        for (int i = 0; i < GHOST_SPAWN.length; i++) {
            int gx = GHOST_SPAWN[i][0], gy = GHOST_SPAWN[i][1];
            String type = GHOST_DIRS[i % GHOST_DIRS.length];

            Image gUp    = img("/tech/cumlaude/cakman/cakman/images/entity/ghost/" + type + "/up.png");
            Image gDown  = img("/tech/cumlaude/cakman/cakman/images/entity/ghost/" + type + "/down.png");
            Image gRight = img("/tech/cumlaude/cakman/cakman/images/entity/ghost/" + type + "/right.png");

            ImageView iv = new ImageView(gRight != null ? gRight : imgFrightened);
            iv.setFitWidth(GHOST_DISPLAY_SIZE);
            iv.setFitHeight(GHOST_DISPLAY_SIZE);
            iv.setPreserveRatio(true);
            placeEntityAt(iv, gx, gy, GHOST_DISPLAY_SIZE, GHOST_OFFSET);
            mazeContainer.getChildren().add(iv);

            Ghost ghost = new Ghost(gx, gy, GHOST_SPEED);

            // Setiap ghost punya arah initial berbeda untuk menyebar
            Entity.Direction initDir = switch (i) {
                case 0 -> Entity.Direction.LEFT;
                case 1 -> Entity.Direction.UP;
                default -> Entity.Direction.RIGHT;
            };

            GhostAgent ga = new GhostAgent(ghost, iv, gUp, gDown, gRight, gx, gy, initDir, i);
            // Ghost ke-2 dan ke-3 mulai dengan delay (mereka diam dulu)
            ga.releaseDelayFrames = i * 120; // 120 frame ≈ 2 detik per ghost
            ghosts.add(ga);
        }
    }

    // =========================================================================
    //  POWER PELLETS (Bakso & Mie Ayam)
    // =========================================================================
    private void renderPowerPellets() {
        powerActive = new boolean[POWER_PELLET_POS.length];
        powerNodes.clear();
        String[] foodPaths = {
            "/tech/cumlaude/cakman/cakman/images/entity/food/bakso.png",
            "/tech/cumlaude/cakman/cakman/images/entity/food/mie_ayam.png"
        };
        String[] labels = {"© Bakso", "© Mie Ayam"};
        Color[] colors  = {Color.web("#FF6B35"), Color.web("#FFD700")};

        for (int i = 0; i < POWER_PELLET_POS.length; i++) {
            int row = POWER_PELLET_POS[i][0], col = POWER_PELLET_POS[i][1];
            powerActive[i] = true;

            double cx = col * MapData.TILE_SIZE + MapData.TILE_SIZE / 2.0;
            double cy = row * MapData.TILE_SIZE + MapData.TILE_SIZE / 2.0;

            // Glow ring
            Circle glow = new Circle(cx, cy, 11, colors[i].deriveColor(0, 1, 1, 0.35));

            // Load gambar makanan
            Image foodImg = img(foodPaths[i]);
            javafx.scene.Node foodNode;
            if (foodImg != null) {
                ImageView iv = new ImageView(foodImg);
                iv.setFitWidth(22); iv.setFitHeight(22);
                iv.setLayoutX(cx - 11); iv.setLayoutY(cy - 11);
                foodNode = iv;
            } else {
                // Fallback lingkaran berwarna
                Circle c = new Circle(cx, cy, 9, colors[i]);
                c.setStroke(Color.WHITE); c.setStrokeWidth(1.5);
                foodNode = c;
            }

            // Label kecil
            Label lbl = new Label(labels[i]);
            lbl.setStyle("-fx-font-size:7px; -fx-text-fill:white; -fx-font-weight:bold;");
            lbl.setLayoutX(cx - 16); lbl.setLayoutY(cy + 11);

            mazeContainer.getChildren().addAll(glow, foodNode, lbl);
            powerNodes.add(new javafx.scene.Node[]{glow, foodNode, lbl});
        }
    }

    private void checkPowerPellet(int cx, int cy) {
        for (int i = 0; i < POWER_PELLET_POS.length; i++) {
            if (!powerActive[i]) continue;
            if (cx != POWER_PELLET_POS[i][1] || cy != POWER_PELLET_POS[i][0]) continue;
            powerActive[i] = false;
            score += POWER_PELLET_SCORE;
            updateHUD();

            javafx.scene.Node[] nodes = powerNodes.get(i);
            if (nodes != null) mazeContainer.getChildren().removeAll(nodes);

            long now = System.nanoTime();
            cakman.activateSuperMode(now, FRIGHTEN_MS);

            for (GhostAgent ga : ghosts) {
                if (ga.active) ga.ghost.frighten(now, FRIGHTEN_MS);
            }
        }
    }

    // =========================================================================
    //  PELLET HELPERS
    // =========================================================================
    private void initPellets() {
        int rows = MapData.LEVEL_1.length, cols = MapData.LEVEL_1[0].length;
        pelletGrid = new boolean[rows][cols];
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                pelletGrid[r][c] = (MapData.LEVEL_1[r][c] == 60);
    }

    private void consumePellet(int cx, int cy) {
        if (!hasPellet(cx, cy)) return;
        pelletGrid[cy][cx] = false;
        score += PELLET_SCORE;
        updateHUD();
        // play pellet collect SFX
        try { AudioManager.getInstance().playSoundEffect(AudioManager.SFX_COLLECT); } catch (Exception ignored) {}
        removePelletCircle(cx, cy);
        checkVictory();
    }

    private boolean hasPellet(int cx, int cy) {
        return pelletGrid != null && cy >= 0 && cy < pelletGrid.length
                && cx >= 0 && cx < pelletGrid[cy].length && pelletGrid[cy][cx];
    }

    private void removePelletCircle(int cx, int cy) {
        double px = cx * MapData.TILE_SIZE + MapData.TILE_SIZE / 2.0;
        double py = cy * MapData.TILE_SIZE + MapData.TILE_SIZE / 2.0;
        mazeContainer.getChildren().removeIf(n ->
            n instanceof Circle c && c.getRadius() <= 4
            && Math.abs(c.getCenterX() - px) <= 0.2
            && Math.abs(c.getCenterY() - py) <= 0.2);
    }

    // =========================================================================
    //  HUD
    // =========================================================================
    private void updateHUD() {
        if (lblScore != null) lblScore.setText("Score: " + score);
        if (lblLives != null) {
            StringBuilder sb = new StringBuilder("Nyawa: ");
            for (int i = 0; i < lives; i++) sb.append("❤ ");
            lblLives.setText(sb.toString());
        }
    }

    // =========================================================================
    //  VICTORY / GAME OVER
    // =========================================================================
    private void checkVictory() {
        for (boolean[] row : pelletGrid)
            for (boolean v : row) if (v) return;
        victory = true;
        stopLoop();
        showEndOverlay(true);
    }

    private void triggerGameOver() {
        gameOver = true;
        // stop music and play game over SFX
        try {
            AudioManager am = AudioManager.getInstance();
            am.stopCurrentMusic();
            am.playSoundEffect(AudioManager.SFX_GAME_OVER);
        } catch (Exception ignored) {}
        restoreMusicWhenMoved = false;
        stopLoop();
        showEndOverlay(false);
    }

    private void stopLoop() { if (gameLoop != null) gameLoop.stop(); }

    // =========================================================================
    //  OVERLAYS
    // =========================================================================
    private void showEndOverlay(boolean win) {
        removeOverlay();
        overlayPane = buildOverlay(win ? Color.web("#001a00", 0.88) : Color.web("#1a0000", 0.88));

        Label title = new Label(win ? "🎉  MENANG!  🎉" : "💀  GAME OVER  💀");
        title.setStyle("-fx-font-size:48px; -fx-text-fill:" + (win ? "#FFD700" : "#FF4444")
                + "; -fx-font-weight:bold; -fx-effect:dropshadow(gaussian,black,12,0.6,0,2);");

        Label scoreLbl = new Label("Skor: " + score);
        scoreLbl.setStyle("-fx-font-size:28px; -fx-text-fill:white; -fx-font-weight:bold;");

        Button btnReplay = overlayBtn("▶  Main Lagi");
        Button btnMenu   = overlayBtn("🏠  Menu Utama");
        btnReplay.setOnAction(_ -> restartGame());
        btnMenu.setOnAction(_ -> goToMenu());

        addToOverlay(overlayPane, title, scoreLbl, btnReplay, btnMenu);
    }

    private void showPauseOverlay() {
        removeOverlay();
        overlayPane = buildOverlay(Color.web("#000033", 0.82));

        Label title = new Label("⏸  JEDA");
        title.setStyle("-fx-font-size:52px; -fx-text-fill:#00FFFF; -fx-font-weight:bold;");

        Label hint = new Label("Tekan ESC atau P untuk lanjut");
        hint.setStyle("-fx-font-size:16px; -fx-text-fill:#aaaaff;");

        Button btnResume = overlayBtn("▶  Lanjutkan");
        Button btnMenu   = overlayBtn("🏠  Menu Utama");
        btnResume.setOnAction(_ -> resumeGame());
        btnMenu.setOnAction(_ -> goToMenu());

        addToOverlay(overlayPane, title, hint, btnResume, btnMenu);
    }

    private StackPane buildOverlay(Color bg) {
        StackPane sp = new StackPane();
        sp.setPrefSize(728, 728);
        Rectangle rect = new Rectangle(728, 728, bg);
        sp.getChildren().add(rect);
        mazeContainer.getChildren().add(sp);
        return sp;
    }

    private void addToOverlay(StackPane sp, javafx.scene.Node... nodes) {
        VBox box = new VBox(18);
        box.setAlignment(Pos.CENTER);
        box.getChildren().addAll(nodes);
        sp.getChildren().add(box);
    }

    private Button overlayBtn(String text) {
        Button b = new Button(text);
        String base = "-fx-font-size:20px;-fx-font-weight:bold;-fx-text-fill:white;"
                + "-fx-background-color:#1a1a4e;-fx-border-color:#00FFFF;-fx-border-width:2;"
                + "-fx-border-radius:8;-fx-background-radius:8;-fx-padding:10 30 10 30;-fx-cursor:hand;";
        b.setStyle(base);
        b.setOnMouseEntered(_ -> b.setStyle(base + "-fx-background-color:#2a2a9e;"));
        b.setOnMouseExited(_  -> b.setStyle(base));
        return b;
    }

    private void removeOverlay() {
        if (overlayPane != null) { mazeContainer.getChildren().remove(overlayPane); overlayPane = null; }
    }

    // =========================================================================
    //  PAUSE / RESUME
    // =========================================================================
    private void pauseGame() {
        if (gameOver || victory || paused) return;
        paused = true;
        stopLoop();
        showPauseOverlay();
    }

    private void resumeGame() {
        if (!paused) return;
        paused = false;
        removeOverlay();
        lastNanos = 0L;
        gameLoop.start();
    }

    // =========================================================================
    //  NAVIGATION
    // =========================================================================
    private void goToMenu() {
        try {
            stopLoop();
            var loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/tech/cumlaude/cakman/cakman/main-menu.fxml"));
            gameStage.getScene().setRoot(loader.load());
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void restartGame() {
        try {
            stopLoop();
            var loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/tech/cumlaude/cakman/cakman/game.fxml"));
            javafx.scene.layout.Pane root = loader.load();
            gameStage.getScene().setRoot(root);
            GameController ctrl = loader.getController();
            if (ctrl != null) ctrl.startGame(gameStage);
        } catch (Exception e) { e.printStackTrace(); }
    }

    // =========================================================================
    //  COUNTDOWN + INPUT
    // =========================================================================
    private void runCountdown(Stage stage) {
        lblCountdown.setVisible(true);
        lblCountdown.setText("3");
        Timeline tl = new Timeline(
                new KeyFrame(Duration.seconds(1), e -> lblCountdown.setText("2")),
                new KeyFrame(Duration.seconds(2), e -> lblCountdown.setText("1")),
                new KeyFrame(Duration.seconds(3), e -> lblCountdown.setText(""))
        );
        tl.setOnFinished(e -> {
            lblCountdown.setVisible(false);
            gameScene = stage.getScene();
            enableInput(gameScene);
            gameScene.getRoot().requestFocus();
            startLoop();
        });
        tl.playFromStart();
    }

    private void enableInput(Scene scene) {
        if (scene == null) return;
        scene.addEventFilter(KeyEvent.KEY_PRESSED, ev -> {
            KeyCode code = ev.getCode();
            if (code == KeyCode.ESCAPE || code == KeyCode.P) {
                if (paused) resumeGame(); else pauseGame();
                return;
            }
            if (paused || gameOver || victory) return;
            Entity.Direction dir = toDir(code);
            if (dir == null || cakman == null) return;
            requestedDir = dir;
            cakman.requestDirection(dir);
            if (!moving && canStep(curX, curY, dir)) startMoving(dir);
        });
    }

    private Entity.Direction toDir(KeyCode code) {
        return switch (code) {
            case RIGHT, D -> Entity.Direction.RIGHT;
            case LEFT,  A -> Entity.Direction.LEFT;
            case UP,    W -> Entity.Direction.UP;
            case DOWN,  S -> Entity.Direction.DOWN;
            default -> null;
        };
    }

    // =========================================================================
    //  GAME LOOP
    // =========================================================================
    private void startLoop() {
        gameLoop = new AnimationTimer() {
            @Override public void handle(long now) {
                if (lastNanos == 0L) { lastNanos = now; return; }
                double dt = (now - lastNanos) / 1e9;
                lastNanos = now;
                cakman.update(now);
                smoothMoveCakman(dt);
                updateGhosts(now, dt);
                checkGhostCollision(now);
            }
        };
        gameLoop.start();
    }

    // =========================================================================
    //  CAKMAN MOVEMENT
    // =========================================================================
    private void smoothMoveCakman(double dt) {
        if (cakman == null) return;
        if (!moving) {
            if (requestedDir != null && canStep(curX, curY, requestedDir)) startMoving(requestedDir);
            else { snapCakmanToCell(curX, curY); return; }
        }
        if (requestedDir != null && requestedDir == activeDir) {
            requestedDir = null; cakman.clearRequestedDirection();
        }
        movePx += cakman.getSpeed() * dt;
        if (shouldSnap()) movePx = MapData.TILE_SIZE;

        while (moving && movePx >= MapData.TILE_SIZE) {
            movePx -= MapData.TILE_SIZE;
            curX = tgtX; curY = tgtY;
            snapCakmanToCell(curX, curY);
            consumePellet(curX, curY);
            checkPowerPellet(curX, curY);
            Entity.Direction next = pickNextDir();
            if (next == null || !canStep(curX, curY, next)) { stopCakman(); return; }
            startMoving(next);
        }
        if (moving) {
            double t = clamp01(movePx / MapData.TILE_SIZE);
            double rx = lerp(curX * MapData.TILE_SIZE, tgtX * MapData.TILE_SIZE, t);
            double ry = lerp(curY * MapData.TILE_SIZE, tgtY * MapData.TILE_SIZE, t);
            cakman.setPosition(rx, ry);
            cakmanView.setLayoutX(rx - MapData.ENTITY_OFFSET);
            cakmanView.setLayoutY(ry - MapData.ENTITY_OFFSET);
        }
    }

    private boolean shouldSnap() {
        if (requestedDir == null || requestedDir == activeDir) return false;
        if (tgtX == curX && tgtY == curY) return false;
        if (!canStep(tgtX, tgtY, requestedDir)) return false;
        return (MapData.TILE_SIZE - movePx) <= TURN_WINDOW;
    }

    private Entity.Direction pickNextDir() {
        if (requestedDir != null && canStep(curX, curY, requestedDir)) return requestedDir;
        if (activeDir   != null && canStep(curX, curY, activeDir))     return activeDir;
        return null;
    }

    private void startMoving(Entity.Direction dir) {
        if (!canStep(curX, curY, dir)) { stopCakman(); return; }
        // If we need to restore music volume after a collision, do it when movement resumes
        if (restoreMusicWhenMoved) {
            try {
                AudioManager.getInstance().setMusicVolume(previousMusicVolume);
            } catch (Exception ignored) {}
            restoreMusicWhenMoved = false;
        }
        activeDir = dir; moving = true; movePx = 0;
        if (requestedDir == dir) { requestedDir = null; cakman.clearRequestedDirection(); }
        updateCakmanSprite(dir);
        switch (dir) {
            case UP    -> { tgtX = curX;     tgtY = curY - 1; }
            case DOWN  -> { tgtX = curX;     tgtY = curY + 1; }
            case LEFT  -> { tgtX = curX - 1; tgtY = curY; }
            case RIGHT -> { tgtX = curX + 1; tgtY = curY; }
        }
    }

    private void stopCakman() {
        moving = false; activeDir = null; requestedDir = null;
        if (cakman != null) cakman.clearRequestedDirection();
        tgtX = curX; tgtY = curY; movePx = 0;
        snapCakmanToCell(curX, curY);
    }

    private void snapCakmanToCell(int cx, int cy) {
        if (cakman == null || cakmanView == null) return;
        cakman.setPosition(cx * MapData.TILE_SIZE, cy * MapData.TILE_SIZE);
        cakmanView.setLayoutX(cakman.getX() - MapData.ENTITY_OFFSET);
        cakmanView.setLayoutY(cakman.getY() - MapData.ENTITY_OFFSET);
    }

    private void updateCakmanSprite(Entity.Direction dir) {
        if (cakmanView == null || cakman == null) return;
        boolean power = cakman.isSuperMode(System.nanoTime());
        switch (dir) {
            case UP    -> { cakmanView.setImage(power ? imgPowerUp    : imgUp);    cakmanView.setScaleX(1); }
            case DOWN  -> { cakmanView.setImage(power ? imgPowerDown  : imgDown);  cakmanView.setScaleX(1); }
            case LEFT  -> { cakmanView.setImage(power ? imgPowerRight : imgRight); cakmanView.setScaleX(-1); }
            case RIGHT -> { cakmanView.setImage(power ? imgPowerRight : imgRight); cakmanView.setScaleX(1); }
        }
    }

    // Passable untuk CakMan
    private boolean canStep(int cx, int cy, Entity.Direction dir) {
        int nx = cx, ny = cy;
        switch (dir) { case UP -> ny--; case DOWN -> ny++; case LEFT -> nx--; case RIGHT -> nx++; }
        return isCakManPassable(nx, ny);
    }

    private boolean isCakManPassable(int cx, int cy) {
        if (cx < 0 || cy < 0 || cy >= MapData.LEVEL_1.length || cx >= MapData.LEVEL_1[0].length) return false;
        int id = MapData.LEVEL_1[cy][cx];
        return id == 1 || id == 60;
    }

    // =========================================================================
    //  GHOST UPDATES
    // =========================================================================
    private void updateGhosts(long now, double dt) {
        for (GhostAgent ga : ghosts) {
            ga.ghost.update(now);
            ga.updateSprite(now);
            ga.tick(dt);
        }
    }

    private void checkGhostCollision(long now) {
        if (cakman == null) return;
        double px = cakman.getX() + MapData.TILE_SIZE / 2.0;
        double py = cakman.getY() + MapData.TILE_SIZE / 2.0;
        for (GhostAgent ga : ghosts) {
            if (!ga.active) continue;
            double gx = ga.ghost.getX() + MapData.TILE_SIZE / 2.0;
            double gy = ga.ghost.getY() + MapData.TILE_SIZE / 2.0;
            if (Math.hypot(px - gx, py - gy) < MapData.TILE_SIZE * 0.85) {
                if (ga.ghost.isFrightened()) {
                    score += GHOST_KILL_SCORE;
                    updateHUD();
                    int sx = GHOST_SPAWN[ga.index][0];
                    int sy = GHOST_SPAWN[ga.index][1];

                    ga.reset(sx, sy);
                    ga.active = true;
                    ga.view.setVisible(true);
                    ga.releaseDelayFrames = 120;
                } else {
                    // non-fatal collision: temporarily mute music, play collide SFX,
                    // then mark to restore when CakMan moves again
                    try {
                        AudioManager am = AudioManager.getInstance();
                        previousMusicVolume = am.getMusicVolume();
                        am.setMusicVolume(0.0);
                        am.playSoundEffect(AudioManager.SFX_COLLIDE_GHOST);
                        restoreMusicWhenMoved = true;
                    } catch (Exception ignored) {}

                    loseLife();
                    return;
                }
            }
        }
    }

    private void loseLife() {
        lives--;
        updateHUD();
        if (lives <= 0) { triggerGameOver(); return; }
        stopLoop();
        resetPositions();
        new Timeline(new KeyFrame(Duration.seconds(1.5), e -> { lastNanos = 0L; gameLoop.start(); })).play();
    }

    private void resetPositions() {
        int gx = 13, gy = 21;
        curX = gx; curY = gy; tgtX = gx; tgtY = gy;
        activeDir = null; requestedDir = null; moving = false; movePx = 0;
        cakman.setPosition(gx * MapData.TILE_SIZE, gy * MapData.TILE_SIZE);
        snapCakmanToCell(gx, gy);
        for (int i = 0; i < ghosts.size(); i++) {
            GhostAgent ga = ghosts.get(i);
            if (!ga.active) continue;
            int sx = GHOST_SPAWN[i][0], sy = GHOST_SPAWN[i][1];
            ga.reset(sx, sy);
        }
    }

    // =========================================================================
    //  UTILS
    // =========================================================================
    private void placeEntityAt(ImageView iv, int cellX, int cellY, int size, int offset) {
        iv.setLayoutX(cellX * MapData.TILE_SIZE - offset);
        iv.setLayoutY(cellY * MapData.TILE_SIZE - offset);
    }

    private double lerp(double a, double b, double t) { return a + (b - a) * t; }
    private double clamp01(double v) { return Math.max(0, Math.min(1, v)); }

    // =========================================================================
    //  GhostAgent — inner class dengan AI menyebar + flee behaviour
    // =========================================================================
    private class GhostAgent {
        final Ghost     ghost;
        final ImageView view;
        final Image     imgUp, imgDown, imgRight;
        final int       index;
        boolean active = true;

        int    gCurX, gCurY, gTgtX, gTgtY;
        Entity.Direction gDir;
        boolean gMoving = false;
        double  gMovePx = 0;
        int releaseDelayFrames; // delay sebelum bergerak

        final Random rng = new Random();

        GhostAgent(Ghost ghost, ImageView view, Image up, Image down, Image right,
                   int startX, int startY, Entity.Direction initDir, int index) {
            this.ghost = ghost; this.view = view;
            this.imgUp = up; this.imgDown = down; this.imgRight = right;
            this.index = index;
            this.gCurX = startX; this.gCurY = startY;
            this.gTgtX = startX; this.gTgtY = startY;
            this.gDir = initDir;
        }

        void reset(int sx, int sy) {
            gCurX = sx;
            gCurY = sy;
            gTgtX = sx;
            gTgtY = sy;

            gMoving = false;
            gMovePx = 0;

            ghost.setPosition(sx * MapData.TILE_SIZE, sy * MapData.TILE_SIZE);
            ghost.clearFrightened();
            view.setVisible(true);
            view.setLayoutX(sx * MapData.TILE_SIZE - GHOST_OFFSET);
            view.setLayoutY(sy * MapData.TILE_SIZE - GHOST_OFFSET);

            releaseDelayFrames = index * 60;
        }

        void updateSprite(long now) {
            if (!active) return;
            if (ghost.isFrightened()) {
                view.setImage(imgFrightened);
                view.setScaleX(1);
                return;
            }
            if (gDir == null) return;
            switch (gDir) {
                case UP    -> { view.setImage(imgUp != null ? imgUp : imgRight);    view.setScaleX(1); }
                case DOWN  -> { view.setImage(imgDown != null ? imgDown : imgRight); view.setScaleX(1); }
                case LEFT  -> { view.setImage(imgRight); view.setScaleX(-1); }
                case RIGHT -> { view.setImage(imgRight); view.setScaleX(1); }
            }
        }

        void tick(double dt) {
            if (!active) return;
            if (releaseDelayFrames > 0) { releaseDelayFrames--; return; }

            double speed = ghost.isFrightened() ? GHOST_FRIGHT_SPEED : GHOST_SPEED;
            gMovePx += speed * dt;

            if (!gMoving) {
                Entity.Direction next = chooseDir();
                if (next != null) { gDir = next; beginMove(); }
                return;
            }

            if (gMovePx >= MapData.TILE_SIZE) {
                gMovePx -= MapData.TILE_SIZE;
                gCurX = gTgtX; gCurY = gTgtY;
                ghost.setPosition(gCurX * MapData.TILE_SIZE, gCurY * MapData.TILE_SIZE);
                view.setLayoutX(gCurX * MapData.TILE_SIZE - GHOST_OFFSET);
                view.setLayoutY(gCurY * MapData.TILE_SIZE - GHOST_OFFSET);
                gMoving = false;
                Entity.Direction next = chooseDir();
                if (next != null) { gDir = next; beginMove(); }
            } else {
                double t = clamp01(gMovePx / MapData.TILE_SIZE);
                double rx = lerp(gCurX * MapData.TILE_SIZE, gTgtX * MapData.TILE_SIZE, t);
                double ry = lerp(gCurY * MapData.TILE_SIZE, gTgtY * MapData.TILE_SIZE, t);
                ghost.setPosition(rx, ry);
                view.setLayoutX(rx - GHOST_OFFSET);
                view.setLayoutY(ry - GHOST_OFFSET);
            }
        }

        private void beginMove() {
            if (!ghostCanStep(gCurX, gCurY, gDir)) { gMoving = false; return; }
            gMoving = true; gMovePx = 0;
            switch (gDir) {
                case UP    -> { gTgtX = gCurX;     gTgtY = gCurY - 1; }
                case DOWN  -> { gTgtX = gCurX;     gTgtY = gCurY + 1; }
                case LEFT  -> { gTgtX = gCurX - 1; gTgtY = gCurY; }
                case RIGHT -> { gTgtX = gCurX + 1; gTgtY = gCurY; }
            }
        }

        /**
         * AI: saat normal → kejar CakMan dengan variasi per index
         *      saat frightened → lari MENJAUHI CakMan
         */
        private Entity.Direction chooseDir() {
            int px = (cakman != null) ? (int)(cakman.getX() / MapData.TILE_SIZE) : gCurX;
            int py = (cakman != null) ? (int)(cakman.getY() / MapData.TILE_SIZE) : gCurY;

            Entity.Direction opposite = flip(gDir);
            List<Entity.Direction> options = new ArrayList<>();
            for (Entity.Direction d : Entity.Direction.values())
                if (d != opposite && ghostCanStep(gCurX, gCurY, d)) options.add(d);
            if (options.isEmpty() && opposite != null && ghostCanStep(gCurX, gCurY, opposite))
                options.add(opposite); // dead end: balik

            if (options.isEmpty()) return null;

            if (ghost.isFrightened()) {
                // Pilih arah yang PALING JAUH dari CakMan
                return options.stream().max(Comparator.comparingDouble(d -> {
                    int[] n = next(gCurX, gCurY, d);
                    return Math.hypot(n[0] - px, n[1] - py);
                })).orElse(options.get(rng.nextInt(options.size())));
            }

            // Normal: chase dengan tambahan scatter/variasi per ghost index
            // Index 0 (USA)   → pure chase
            // Index 1 (DUTCH) → chase dengan offset target
            // Index 2 (JAPAN) → random 40% + chase 60%
            int targetX = px, targetY = py;
            if (index == 1) {
                // Target 4 tile di depan CakMan
                if (activeDir != null) {
                    int[] ahead = next(px, py, activeDir);
                    int[] ahead2 = next(ahead[0], ahead[1], activeDir);
                    int[] ahead3 = next(ahead2[0], ahead2[1], activeDir);
                    int[] ahead4 = next(ahead3[0], ahead3[1], activeDir);
                    targetX = ahead4[0]; targetY = ahead4[1];
                }
            } else if (index == 2 && rng.nextInt(10) < 4) {
                // 40% acak
                return options.get(rng.nextInt(options.size()));
            }

            final int finalTX = targetX, finalTY = targetY;
            return options.stream().min(Comparator.comparingDouble(d -> {
                int[] n = next(gCurX, gCurY, d);
                return Math.hypot(n[0] - finalTX, n[1] - finalTY);
            })).orElse(options.get(rng.nextInt(options.size())));
        }

        private int[] next(int cx, int cy, Entity.Direction d) {
            return switch (d) {
                case UP    -> new int[]{cx, cy - 1};
                case DOWN  -> new int[]{cx, cy + 1};
                case LEFT  -> new int[]{cx - 1, cy};
                case RIGHT -> new int[]{cx + 1, cy};
            };
        }

        private Entity.Direction flip(Entity.Direction d) {
            if (d == null) return null;
            return switch (d) { case UP -> Entity.Direction.DOWN; case DOWN -> Entity.Direction.UP;
                    case LEFT -> Entity.Direction.RIGHT; case RIGHT -> Entity.Direction.LEFT; };
        }

        // Ghost hanya bisa lewat tile ID 1 dan 60 (bukan 13 = wall ghost house luar)
        private boolean ghostCanStep(int cx, int cy, Entity.Direction d) {
            int[] n = next(cx, cy, d);
            int nx = n[0], ny = n[1];
            if (nx < 0 || ny < 0 || ny >= MapData.LEVEL_1.length || nx >= MapData.LEVEL_1[0].length)
                return false;
            int id = MapData.LEVEL_1[ny][nx];
            return id == 1 || id == 60;
        }
    }
}
