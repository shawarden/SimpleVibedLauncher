package local.simplelauncher;

import java.util.Random;

/**
 * A box moving around a rectangle with perfectly elastic walls and no resistance, so it never loses
 * energy and never settles. Two kinds of motion:
 * <ul>
 * <li>gravity: constant downward acceleration, so it speeds up falling and slows rising;</li>
 * <li>clean bounce: no gravity, a constant speed in a straight line, reflecting off each edge.</li>
 * </ul>
 * No Android classes in here, so it can be tested on a plain JVM. Units are pixels and seconds, +y is down.
 */
final class BounceSim {
    interface Listener { void onBounce(); }

    /** Top-left corner of the box. */
    float x, y;

    private float vx, vy;
    private float g;                 // 0 for a clean bounce
    private float energy;            // 0.5*vy^2 - g*y; constant while flying (gravity mode only)
    private float fieldW, fieldH, maxX, maxY;
    private final Random rnd;
    private final Listener listener;

    BounceSim(Random rnd, Listener listener) {
        this.rnd = rnd;
        this.listener = listener;
    }

    /**
     * Start from a random place, heading in a random direction. Level 0, 1, 2 is Low, Medium, High:
     * with gravity it sets how strong the gravity is, without gravity it sets how fast the clock moves.
     */
    void reset(float fieldW, float fieldH, float boxW, float boxH, boolean gravity, int level) {
        int lv = Math.max(0, Math.min(2, level));
        this.fieldW = fieldW;
        this.fieldH = fieldH;
        g = gravity ? Config.GRAVITY * Config.GRAVITY_SCALE[lv] * fieldH : 0f;
        float speedScale = gravity ? 1f : Config.SPEED_SCALE[lv];
        setBox(boxW, boxH);
        x = rnd.nextFloat() * maxX;
        y = rnd.nextFloat() * maxY;
        vx = sign() * lerp(Config.VX_MIN, Config.VX_MAX, rnd.nextFloat()) * fieldW * speedScale;
        if (gravity) {
            energy = lerp(Config.ENERGY_MIN, Config.ENERGY_MAX, rnd.nextFloat()) * fieldH * fieldH;
            vy = sign() * (float) Math.sqrt(2f * (energy + g * y));
        } else {
            energy = 0f;
            vy = sign() * lerp(Config.VY_CLEAN_MIN, Config.VY_CLEAN_MAX, rnd.nextFloat()) * fieldH * speedScale;
        }
    }

    /**
     * Let the box go from rest at (x0, y0) under gravity (strength set by level), with a random sideways
     * speed. This is the clock being released from its place on the home screen.
     */
    void releaseAt(float fieldW, float fieldH, float boxW, float boxH, float x0, float y0, int level) {
        int lv = Math.max(0, Math.min(2, level));
        this.fieldW = fieldW;
        this.fieldH = fieldH;
        g = Config.GRAVITY * Config.GRAVITY_SCALE[lv] * fieldH;
        setBox(boxW, boxH);
        x = Math.max(0f, Math.min(x0, maxX));
        y = Math.max(0f, Math.min(y0, maxY));
        vx = sign() * lerp(Config.VX_MIN, Config.VX_MAX, rnd.nextFloat()) * fieldW;
        vy = 0f;
        energy = -g * y;      // at rest at y: it will fall and rise back to this height each time
    }

    /** Stop using gravity: keep the direction of travel but move at a constant speed (set by level). */
    void toCleanBounce(int level) {
        int lv = Math.max(0, Math.min(2, level));
        float k = Config.SPEED_SCALE[lv];
        g = 0f;
        energy = 0f;
        vx = (vx < 0f ? -1f : 1f) * lerp(Config.VX_MIN, Config.VX_MAX, rnd.nextFloat()) * fieldW * k;
        vy = (vy < 0f ? -1f : 1f) * lerp(Config.VY_CLEAN_MIN, Config.VY_CLEAN_MAX, rnd.nextFloat()) * fieldH * k;
    }

    /** The box changed size (the time text changed width). */
    void setBox(float boxW, float boxH) {
        maxX = Math.max(0f, fieldW - boxW);
        maxY = Math.max(0f, fieldH - boxH);
        x = Math.min(x, maxX);
        y = Math.min(y, maxY);
    }

    /** Advance by dt seconds; keep dt small (a few milliseconds). */
    void step(float dt) {
        boolean hit = false;

        x += vx * dt;
        if (x < 0f) { x = -x; vx = -vx; hit = true; }
        else if (x > maxX) { x = 2f * maxX - x; vx = -vx; hit = true; }

        y += vy * dt + 0.5f * g * dt * dt;
        vy += g * dt;
        if (y < 0f) { y = -y; vy = -vy; hit = true; }
        else if (y > maxY) { y = 2f * maxY - y; vy = -vy; hit = true; }

        if (g > 0f) {
            // Re-derive the speed from the conserved energy so rounding errors never add up.
            float v2 = 2f * (energy + g * y);
            vy = Math.copySign(v2 > 0f ? (float) Math.sqrt(v2) : 0f, vy);
        }

        if (hit) listener.onBounce();
    }

    private float sign() { return rnd.nextBoolean() ? 1f : -1f; }

    private static float lerp(float a, float b, float f) { return a + (b - a) * f; }

    // for tests
    float energyNow() { return 0.5f * vy * vy - g * y; }
    float energyTarget() { return energy; }
    float maxX() { return maxX; }
    float maxY() { return maxY; }
    float vx() { return vx; }
    float vy() { return vy; }
}
