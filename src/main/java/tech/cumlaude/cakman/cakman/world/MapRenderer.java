package tech.cumlaude.cakman.cakman.world;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import java.util.Objects;

public class MapRenderer {
    private final Pane mazeContainer;

    private Image outStraight, outCorner, outBranch;
    private Image inStraight, inEnd, inFull, inBranch;
    private Image empty;

    public MapRenderer(Pane mazeContainer) {
        this.mazeContainer = mazeContainer;
        loadAssets();
    }

    private void loadAssets() {
        try {
            outStraight = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/tech/cumlaude/cakman/cakman/images/wall/outer_straight.png")));
            outCorner = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/tech/cumlaude/cakman/cakman/images/wall/outer_corner.png")));
            outBranch = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/tech/cumlaude/cakman/cakman/images/wall/outer_branch.png")));

            inStraight = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/tech/cumlaude/cakman/cakman/images/wall/inner_straight.png")));
            inEnd = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/tech/cumlaude/cakman/cakman/images/wall/inner_end.png")));
            inFull = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/tech/cumlaude/cakman/cakman/images/wall/inner_full.png")));
            inBranch = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/tech/cumlaude/cakman/cakman/images/wall/inner_branch.png")));

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
            case 11 -> draw(x, y, outStraight, 0, false);
            case 12 -> draw(x, y, outStraight, 90, false);
            case 13 -> draw(x, y, outStraight, 180, false);
            case 14 -> draw(x, y, outStraight, 270, false);

            case 15 -> draw(x, y, outCorner, 0, false);
            case 16 -> draw(x, y, outCorner, 90, false);
            case 17 -> draw(x, y, outCorner, 180, false);
            case 18 -> draw(x, y, outCorner, 270, false);

            case 19 -> draw(x, y, outBranch, 0, false);
            case 20 -> draw(x, y, outBranch, 90, false);
            case 21 -> draw(x, y, outBranch, 180, false);
            case 22 -> draw(x, y, outBranch, 270, false);

            case 23 -> draw(x, y, outBranch, 0, true);
            case 24 -> draw(x, y, outBranch, 90, true);
            case 25 -> draw(x, y, outBranch, 180, true);
            case 26 -> draw(x, y, outBranch, 270, true);

            // --- WALL DALAM ---
            case 31 -> draw(x, y, inStraight, 0, false);
            case 32 -> draw(x, y, inStraight, 90, false);
            case 33 -> draw(x, y, inStraight, 180, false);
            case 34 -> draw(x, y, inStraight, 270, false);

            case 35 -> draw(x, y, inEnd, 0, false);
            case 36 -> draw(x, y, inEnd, 90, false);
            case 37 -> draw(x, y, inEnd, 180, false);
            case 38 -> draw(x, y, inEnd, 270, false);

            case 39 -> draw(x, y, inEnd, 0, true);
            case 40 -> draw(x, y, inEnd, 90, true);
            case 41 -> draw(x, y, inEnd, 180, true);
            case 42 -> draw(x, y, inEnd, 270, true);

            case 43 -> draw(x, y, inFull, 0, false);

            case 44 -> draw(x, y, inBranch, 0, false);
            case 45 -> draw(x, y, inBranch, 90, false);
            case 46 -> draw(x, y, inBranch, 180, false);
            case 47 -> draw(x, y, inBranch, 270, false);

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