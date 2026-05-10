package tech.cumlaude.cakman.cakman.entity;

import java.util.Random;

public class Ghost extends Entity {

    private final Random random = new Random();
    private final long decisionIntervalNanos = 220_000_000L;
    private final double chaseRadius = 8 * 24.0;
    private State state = State.NORMAL;
    private long frightenedEndsAtNanos;
    private long lastDecisionNanos;

    public Ghost(double x, double y, double speed) {
        super(x, y, speed);
        setDirection(random.nextBoolean() ? Direction.RIGHT : Direction.LEFT);
    }

    @Override
    protected void updateSpritePosition() {

    }

    public State getState() {
        return state;
    }

    public boolean isFrightened() {
        return state == State.FRIGHTENED;
    }

    public void frighten(long now, long durationMillis) {
        state = State.FRIGHTENED;
        frightenedEndsAtNanos = now + durationMillis * 1_000_000L;
    }

    public void clearFrightened() {
        state = State.NORMAL;
        frightenedEndsAtNanos = 0L;
    }

    @Override
    public void update(long now) {
        if (state == State.FRIGHTENED && now >= frightenedEndsAtNanos) {
            clearFrightened();
        }
    }

    @Override
    public void move() {
        //TODO
    }

    public enum State {
        NORMAL,
        FRIGHTENED
    }

    public enum Type {
        USA,
        DUTCH,
        JAPAN;

        public String getImagePath() {
            return switch (this) {
                case USA -> "/tech/cumlaude/cakman/cakman/images/ghost/usa";
                case DUTCH -> "/tech/cumlaude/cakman/cakman/images/ghost/dutch";
                case JAPAN -> "/tech/cumlaude/cakman/cakman/images/ghost/japan";
            };
        }
    }
}
