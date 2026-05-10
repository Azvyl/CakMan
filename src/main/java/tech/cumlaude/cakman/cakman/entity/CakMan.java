package tech.cumlaude.cakman.cakman.entity;

public class CakMan extends Entity {
    private Direction requestedDirection;
    private boolean superMode;
    private long superModeEndsAtNanos;

    public CakMan(double x, double y, double speed) {
        super(x, y, speed);
    }

    @Override
    protected void updateSpritePosition() {

    }

    public void requestDirection(Direction direction) {
        this.requestedDirection = direction;
    }

    public Direction getRequestedDirection() {
        return requestedDirection;
    }

    public boolean hasRequestedDirection() {
        return requestedDirection != null && requestedDirection != direction;
    }

    public void clearRequestedDirection() {
        requestedDirection = null;
    }

    public void activateSuperMode(long now, long durationMillis) {
        superMode = true;
        long durationNanos = durationMillis * 1_000_000L;
        superModeEndsAtNanos = Math.max(superModeEndsAtNanos, now) + durationNanos;
    }

    public boolean isSuperMode() {
        return isSuperMode(System.nanoTime());
    }

    public boolean isSuperMode(long now) {
        return superMode && now < superModeEndsAtNanos;
    }

    public long getRemainingSuperModeMillis(long now) {
        if (!isSuperMode(now)) {
            return 0L;
        }
        return Math.max(0L, (superModeEndsAtNanos - now) / 1_000_000L);
    }

    @Override
    public void move() {
        // Movement is managed by GameController in this simplified implementation.
        // Keep method no-op to satisfy abstract contract.
    }

    @Override
    public void update(long now) {
        if (superMode && now >= superModeEndsAtNanos) {
            superMode = false;
            superModeEndsAtNanos = 0L;
        }
    }
}
