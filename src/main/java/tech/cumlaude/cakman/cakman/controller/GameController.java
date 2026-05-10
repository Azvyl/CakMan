package tech.cumlaude.cakman.cakman.controller;

import javafx.animation.AnimationTimer;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.util.Duration;
import tech.cumlaude.cakman.cakman.entity.CakMan;
import tech.cumlaude.cakman.cakman.entity.Entity;
import tech.cumlaude.cakman.cakman.world.MapData;
import tech.cumlaude.cakman.cakman.world.MapRenderer;

import java.util.Objects;

public class GameController {
	@FXML
	private Pane mazeContainer;

	@FXML
	private Label lblCountdown;

	private static final double MOVE_SPEED_PX_PER_SEC = 104.0;
	private static final double ALIGN_EPSILON_PX = 0.75;
	private static final double TURN_WINDOW_PX = 6.0;

	private CakMan cakman;
	private Image imgUp;
	private Image imgDown;
	private Image imgRight;
	private ImageView cakmanView;

	private long lastNowNanos = 0L;

	private int currentCellX;
	private int currentCellY;
	private int targetCellX;
	private int targetCellY;
	private Entity.Direction activeDirection;
	private Entity.Direction requestedDirection;
	private boolean moving;
	private double moveProgressPx;

	public void startGame(Stage stage) {
		new MapRenderer(mazeContainer).render();
		loadAssets();
		spawnCakman();
		if (cakman != null) {
			cakman.setSpeed(MOVE_SPEED_PX_PER_SEC);
		}
		runCountdownThenStart(stage);
	}

	private void loadAssets() {
		imgUp = loadImage("/tech/cumlaude/cakman/cakman/images/entity/cakman/up.png");
		imgDown = loadImage("/tech/cumlaude/cakman/cakman/images/entity/cakman/down.png");
		imgRight = loadImage("/tech/cumlaude/cakman/cakman/images/entity/cakman/right.png");
	}

	private Image loadImage(String resourcePath) {
		return new Image(Objects.requireNonNull(getClass().getResource(resourcePath), "Missing resource: " + resourcePath).toExternalForm());
	}

	private void spawnCakman() {
		int gridX = 13;
		int gridY = 21;
		cakman = new CakMan(gridX, gridY, MOVE_SPEED_PX_PER_SEC);
		currentCellX = gridX;
		currentCellY = gridY;
		targetCellX = gridX;
		targetCellY = gridY;
		activeDirection = null;
		requestedDirection = null;
		moving = false;
		moveProgressPx = 0.0;

		cakmanView = new ImageView(imgRight);
		cakmanView.setFitWidth(MapData.ENTITY_SIZE);
		cakmanView.setFitHeight(MapData.ENTITY_SIZE);
		mazeContainer.getChildren().add(cakmanView);
		updateModelAndSpriteAtCell(currentCellX, currentCellY);
	}

	private void updateModelAndSpriteAtCell(int cellX, int cellY) {
		if (cakman == null || cakmanView == null) return;
		cakman.setPosition(cellX * MapData.TILE_SIZE, cellY * MapData.TILE_SIZE);
		cakmanView.setLayoutX(cakman.getX() - MapData.ENTITY_OFFSET);
		cakmanView.setLayoutY(cakman.getY() - MapData.ENTITY_OFFSET);
	}

	private void runCountdownThenStart(Stage stage) {
		lblCountdown.setVisible(true);
		lblCountdown.setText("3");

		Timeline timeline = new Timeline(
				new KeyFrame(Duration.seconds(1), event -> {
					Objects.requireNonNull(event);
					lblCountdown.setText("2");
				}),
				new KeyFrame(Duration.seconds(2), event -> {
					Objects.requireNonNull(event);
					lblCountdown.setText("1");
				}),
				new KeyFrame(Duration.seconds(3), event -> {
					Objects.requireNonNull(event);
					lblCountdown.setText("");
				})
		);

		timeline.setOnFinished(event -> {
			Objects.requireNonNull(event);
			lblCountdown.setVisible(false);
			enableKeyboardInput(stage.getScene());
			requestGameFocus(stage.getScene());
			startGameLoop();
		});

		timeline.playFromStart();
	}

	private void requestGameFocus(Scene scene) {
		if (scene == null || scene.getRoot() == null) return;
		scene.getRoot().setFocusTraversable(true);
		scene.getRoot().requestFocus();
		if (mazeContainer != null) {
			mazeContainer.setFocusTraversable(true);
			mazeContainer.requestFocus();
		}
	}

	private void enableKeyboardInput(Scene scene) {
		if (scene == null) return;
		scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
			Entity.Direction direction = toDirection(event.getCode());
			if (direction == null || cakman == null) return;

			requestedDirection = direction;
			cakman.requestDirection(direction);

			if (!moving && canStepFrom(currentCellX, currentCellY, direction)) {
				startMoving(direction);
			}
		});
	}

	private Entity.Direction toDirection(KeyCode code) {
		return switch (code) {
			case RIGHT, D -> Entity.Direction.RIGHT;
			case LEFT, A -> Entity.Direction.LEFT;
			case UP, W -> Entity.Direction.UP;
			case DOWN, S -> Entity.Direction.DOWN;
			default -> null;
		};
	}

	private void startGameLoop() {
		AnimationTimer loop = new AnimationTimer() {
			@Override
			public void handle(long now) {
				if (lastNowNanos == 0L) {
					lastNowNanos = now;
					return;
				}
				double deltaSec = (now - lastNowNanos) / 1_000_000_000.0;
				lastNowNanos = now;
				smoothMove(deltaSec);
			}
		};
		loop.start();
	}

	private void smoothMove(double deltaSec) {
		if (cakman == null) return;

		if (!moving) {
			if (requestedDirection != null && canStepFrom(currentCellX, currentCellY, requestedDirection)) {
				startMoving(requestedDirection);
			} else {
				updateModelAndSpriteAtCell(currentCellX, currentCellY);
				return;
			}
		}

		if (requestedDirection != null && requestedDirection == activeDirection) {
			requestedDirection = null;
			cakman.clearRequestedDirection();
		}

		double stepPx = cakman.getSpeed() * deltaSec;
		moveProgressPx += stepPx;

		if (moving && shouldTurnSoon()) {
			moveProgressPx = MapData.TILE_SIZE;
		}

		while (moving && moveProgressPx >= MapData.TILE_SIZE) {
			moveProgressPx -= MapData.TILE_SIZE;
			currentCellX = targetCellX;
			currentCellY = targetCellY;
			updateModelAndSpriteAtCell(currentCellX, currentCellY);

			Entity.Direction nextDirection = chooseNextDirection();

			if (nextDirection == null || !canStepFrom(currentCellX, currentCellY, nextDirection)) {
				stopAtCurrentCell();
				return;
			}

			startMoving(nextDirection);
		}

		if (moving) {
			double t = clamp01(moveProgressPx / MapData.TILE_SIZE);
			double renderedX = lerp(currentCellX * MapData.TILE_SIZE, targetCellX * MapData.TILE_SIZE, t);
			double renderedY = lerp(currentCellY * MapData.TILE_SIZE, targetCellY * MapData.TILE_SIZE, t);
			cakman.setPosition(renderedX, renderedY);
			cakmanView.setLayoutX(renderedX - MapData.ENTITY_OFFSET);
			cakmanView.setLayoutY(renderedY - MapData.ENTITY_OFFSET);

			if (isAtCellCenter()) {
				Entity.Direction nextDirection = chooseNextDirection();
				if (nextDirection != null && nextDirection != activeDirection && canStepFrom(currentCellX, currentCellY, nextDirection)) {
					startMoving(nextDirection);
				}
			}
		}
	}

	private boolean shouldTurnSoon() {
		if (requestedDirection == null || requestedDirection == activeDirection) {
			return false;
		}
		if (targetCellX == currentCellX && targetCellY == currentCellY) {
			return false;
		}
		if (!canStepFrom(targetCellX, targetCellY, requestedDirection)) {
			return false;
		}

		double remainingPx = MapData.TILE_SIZE - moveProgressPx;
		return remainingPx <= TURN_WINDOW_PX;
	}

	private Entity.Direction chooseNextDirection() {
		if (requestedDirection != null && canStepFrom(currentCellX, currentCellY, requestedDirection)) {
			return requestedDirection;
		}
		if (activeDirection != null && canStepFrom(currentCellX, currentCellY, activeDirection)) {
			return activeDirection;
		}
		return null;
	}

	private double lerp(double from, double to, double t) {
		return from + (to - from) * t;
	}

	private double clamp01(double value) {
		return value < 0.0 ? 0.0 : Math.min(value, 1.0);
	}

	private boolean isAtCellCenter() {
		if (cakman == null) return false;
		double cellOriginX = currentCellX * MapData.TILE_SIZE;
		double cellOriginY = currentCellY * MapData.TILE_SIZE;
		return Math.abs(cakman.getX() - cellOriginX) <= ALIGN_EPSILON_PX
				&& Math.abs(cakman.getY() - cellOriginY) <= ALIGN_EPSILON_PX;
	}

	private void startMoving(Entity.Direction direction) {
		if (cakman == null) return;
		if (!canStepFrom(currentCellX, currentCellY, direction)) {
			stopAtCurrentCell();
			return;
		}

		Entity.Direction pendingDirection = requestedDirection;

		activeDirection = direction;
		moving = true;
		moveProgressPx = 0.0;
		if (pendingDirection != null && pendingDirection == direction) {
			requestedDirection = null;
			cakman.clearRequestedDirection();
		}
		updateSpriteForDirection(direction);

		switch (direction) {
			case UP -> {
				targetCellX = currentCellX;
				targetCellY = currentCellY - 1;
			}
			case DOWN -> {
				targetCellX = currentCellX;
				targetCellY = currentCellY + 1;
			}
			case LEFT -> {
				targetCellX = currentCellX - 1;
				targetCellY = currentCellY;
			}
			case RIGHT -> {
				targetCellX = currentCellX + 1;
				targetCellY = currentCellY;
			}
		}
	}

	private void stopAtCurrentCell() {
		moving = false;
		activeDirection = null;
		requestedDirection = null;
		cakman.clearRequestedDirection();
		targetCellX = currentCellX;
		targetCellY = currentCellY;
		moveProgressPx = 0.0;
		updateModelAndSpriteAtCell(currentCellX, currentCellY);
	}

	private boolean canStepFrom(int cellX, int cellY, Entity.Direction direction) {
		int nextX = cellX;
		int nextY = cellY;
		switch (direction) {
			case UP -> nextY -= 1;
			case DOWN -> nextY += 1;
			case LEFT -> nextX -= 1;
			case RIGHT -> nextX += 1;
		}
		return isTilePassable(nextX, nextY);
	}

	private boolean isTilePassable(int cellX, int cellY) {
		if (cellX < 0 || cellY < 0 || cellY >= MapData.LEVEL_1.length || cellX >= MapData.LEVEL_1[0].length) {
			return false;
		}

		int id = MapData.LEVEL_1[cellY][cellX];
		return id == 1 || id == 60;
	}

	private void updateSpriteForDirection(Entity.Direction direction) {
		if (cakmanView == null) return;
		switch (direction) {
			case UP -> {
				cakmanView.setImage(imgUp);
				cakmanView.setScaleX(1.0);
			}
			case DOWN -> {
				cakmanView.setImage(imgDown);
				cakmanView.setScaleX(1.0);
			}
			case LEFT -> {
				cakmanView.setImage(imgRight);
				cakmanView.setScaleX(-1.0);
			}
			case RIGHT -> {
				cakmanView.setImage(imgRight);
				cakmanView.setScaleX(1.0);
			}
		}
	}
}

