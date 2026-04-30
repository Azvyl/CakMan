package tech.cumlaude.cakman.cakman.entity;

public abstract class Entity {

    protected double x;
    protected double y;
    protected double speed;
    protected Direction direction;

    protected Entity(double x, double y, double speed) {
        this(x, y, speed, null);
    }

    protected Entity(double x, double y, double speed, Direction direction) {
        this.x = x;
        this.y = y;
        this.speed = speed;
        this.direction = direction;
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

    public abstract void move();

    public abstract void update(long now);

    public enum Direction {
        UP, DOWN, LEFT, RIGHT
    }
}
