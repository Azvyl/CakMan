package tech.cumlaude.cakman.cakman.world;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import java.util.Objects;

public class MapRenderer {
    private final Pane mazeContainer;

    private Image frameStraight, frameCorner, frameBranch;
    private Image straight, corner1, corner2, full;
    private Image empty;

    public MapRenderer(Pane mazeContainer) {
        this.mazeContainer = mazeContainer;
        loadAssets();
    }

    private void loadAssets() {
        try {
            frameStraight = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/tech/cumlaude/cakman/cakman/images/wall/frame_straight.png")));
            frameCorner = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/tech/cumlaude/cakman/cakman/images/wall/frame_corner.png")));
            frameBranch = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/tech/cumlaude/cakman/cakman/images/wall/frame_branch.png")));

            straight = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/tech/cumlaude/cakman/cakman/images/wall/straight.png")));
            corner1 = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/tech/cumlaude/cakman/cakman/images/wall/corner_1.png")));
            corner2 = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/tech/cumlaude/cakman/cakman/images/wall/corner_2.png")));
            full = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/tech/cumlaude/cakman/cakman/images/wall/full.png")));

            //TODO: thin wall untuk base ghost

            empty = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/tech/cumlaude/cakman/cakman/images/wall/empty.png")));
        } catch (Exception e) {
            System.err.println("Aset tidak ditemukan! Pastikan nama file sesuai.");
        }
    }

    public void render() {
        mazeContainer.getChildren().clear();

        for (int row = 0; row < MapData.GRID_SIZE; row++) {
            for (int col = 0; col < MapData.GRID_SIZE; col++) {
                System.out.printf("Memproses tile di baris %d, kolom %d%n", row, col);
                int id = MapData.LEVEL_1[row][col];
                double x = col * MapData.TILE_SIZE;
                double y = row * MapData.TILE_SIZE;

                processTile(id, x, y);
            }
        }
    }

    private void processTile(int id, double x, double y) {
        switch (id) {
            case 1 -> draw(x, y, empty, 0, false);

            // --- WALL LUAR ---
            case 11 -> draw(x, y, frameStraight, 0, false);
            case 12 -> draw(x, y, frameStraight, 90, false);
            case 13 -> draw(x, y, frameStraight, 180, false);
            case 14 -> draw(x, y, frameStraight, 270, false);

            case 15 -> draw(x, y, frameCorner, 0, false);
            case 16 -> draw(x, y, frameCorner, 90, false);
            case 17 -> draw(x, y, frameCorner, 180, false);
            case 18 -> draw(x, y, frameCorner, 270, false);

            case 19 -> draw(x, y, frameBranch, 0, false);
            case 20 -> draw(x, y, frameBranch, 90, false);
            case 21 -> draw(x, y, frameBranch, 180, false);
            case 22 -> draw(x, y, frameBranch, 270, false);

            case 23 -> draw(x, y, frameBranch, 0, true);
            case 24 -> draw(x, y, frameBranch, 90, true);
            case 25 -> draw(x, y, frameBranch, 180, true);
            case 26 -> draw(x, y, frameBranch, 270, true);

            // --- WALL DALAM ---
            case 31 -> draw(x, y, straight, 0, false);
            case 32 -> draw(x, y, straight, 90, false);
            case 33 -> draw(x, y, straight, 180, false);
            case 34 -> draw(x, y, straight, 270, false);

            case 35 -> draw(x, y, corner2, 0, false);
            case 36 -> draw(x, y, corner2, 90, false);
            case 37 -> draw(x, y, corner2, 180, false);
            case 38 -> draw(x, y, corner2, 270, false);

            case 39 -> draw(x, y, corner2, 0, true);
            case 40 -> draw(x, y, corner2, 90, true);
            case 41 -> draw(x, y, corner2, 180, true);
            case 42 -> draw(x, y, corner2, 270, true);

            case 43 -> draw(x, y, full, 0, false);

            case 44 -> draw(x, y, corner1, 0, false);
            case 45 -> draw(x, y, corner1, 90, false);
            case 46 -> draw(x, y, corner1, 180, false);
            case 47 -> draw(x, y, corner1, 270, false);

            case 50 -> draw(x, y, frameCorner, 0, false);
            case 51 -> draw(x, y, frameCorner, 90, false);
            case 52 -> draw(x, y, frameCorner, 180, false);
            case 53 -> draw(x, y, frameCorner, 270, false);

            case 54 -> draw(x, y, frameStraight, 0, false);
            case 55 -> draw(x, y, frameStraight, 90, false);
            case 56 -> draw(x, y, frameStraight, 180, false);
            case 57 -> draw(x, y, frameStraight, 270, false);

            case 58 -> draw(x, y, corner2, 0, false);

            // --- ITEMS ---
            case 60 -> drawPellet(x, y);
        }
    }

    private void draw(double x, double y, Image img, double rotation, boolean mirror) {
        if (img == null) return;
        ImageView iv = new ImageView(img);
        iv.setFitWidth(MapData.TILE_SIZE);
        iv.setFitHeight(MapData.TILE_SIZE);
        iv.setX(x);
        iv.setY(y);
        iv.setRotate(rotation);
        if (mirror) iv.setScaleX(-1);

        mazeContainer.getChildren().add(iv);
    }

    private void drawPellet(double x, double y) {
        draw(x, y, empty, 0, false);
        Circle c = new Circle(x + 13, y + 13, 3, Color.web("#FFD700"));
        mazeContainer.getChildren().add(c);
    }
}