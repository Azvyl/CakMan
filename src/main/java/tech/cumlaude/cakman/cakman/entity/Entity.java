package tech.cumlaude.cakman.cakman.entity;

import tech.cumlaude.cakman.cakman.world.MapData;

public abstract class Entity {

    protected double x, y;
    protected double speed;
    protected Direction direction;

    protected Entity(double gridX, double gridY, double speed) {
        this(gridX, gridY, speed, null);
    }

    protected Entity(double gridX, double gridY, double speed, Direction direction) {
        this.x = gridX * MapData.TILE_SIZE;
        this.y = gridY * MapData.TILE_SIZE;
        this.speed = speed;
        this.direction = direction;

        updateSpritePosition();
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public Direction getDirection() {
        return direction;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    public void clearDirection() {
        this.direction = null;
    }

    protected void translate(double deltaX, double deltaY) {
        x += deltaX;
        y += deltaY;
    }

    protected abstract void updateSpritePosition();

    public abstract void move();

    public abstract void update(long now);

    public enum Direction {
        UP, DOWN, LEFT, RIGHT
    }
}
