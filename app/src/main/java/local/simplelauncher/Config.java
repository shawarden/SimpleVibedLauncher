package local.simplelauncher;

/** Every tunable number in one place. */
final class Config {
    private Config() {}

    // ---- Home background: each of R, G, B wanders between 0 and BG_MAX, moving +/-STEP at a time.
    static final int BG_MAX = 32;
    static final int STEP = 8;
    /** One +/-STEP move takes a random time between these two values. */
    static final long BG_STEP_MIN_MS = 4000;
    static final long BG_STEP_MAX_MS = 9000;

    // ---- Screensaver
    static final int SAVER_MAX = 64;
    /** Time to fade to the new colour after the clock bounces off an edge. */
    static final long SAVER_FADE_MS = 2500;
    /** Idle time on the home screen before the screensaver starts. */
    static final long IDLE_MS = 2 * 60 * 1000;

    /** Gravity, in screen heights per second squared (bigger = faster fall). */
    static final float GRAVITY = 0.08f;
    /**
     * Starting "energy" of the clock in screen-heights^2 per second^2. It never changes afterwards, so the
     * clock always bounces back up to the same height. Anything above 0 reaches the top of the screen.
     */
    static final float ENERGY_MIN = 0.00f;
    static final float ENERGY_MAX = 0.00f;
    /** Sideways speed, in screen widths per second. */
    static final float VX_MIN = 0.06f;
    static final float VX_MAX = 0.12f;
    /**
     * The Gravity / speed setting: Low, Medium, High. Low is the original values above. In gravity mode the
     * level multiplies the gravity; in clean-bounce mode it multiplies the movement speed.
     */
    static final float[] GRAVITY_SCALE = { 1f, 2.5f, 5f };
    static final float[] SPEED_SCALE = { 1f, 2f, 4f };
    /** Vertical speed for the clean bounce (no gravity), in screen heights per second. */
    static final float VY_CLEAN_MIN = 0.10f;
    static final float VY_CLEAN_MAX = 0.16f;

    // ---- Home screen <-> screensaver transition
    /** Seconds for the background to morph to the screensaver colours while the pieces fall. */
    static final float FALL_FADE_S = 3.0f;
    /** Seconds for the clock to grow from its home size to its screensaver size. */
    static final float GROW_S = 2.5f;
    /** Seconds for the background to morph back, and the shortest time the return takes. */
    static final float RETURN_S = 1.5f;
    /** Everything is snapped to its home position by this time after waking. */
    static final float RETURN_MAX_S = 2.0f;
    /** Seconds for the clock to shrink back to its home size. */
    static final float RETURN_SHRINK_S = 0.9f;
    /** Random sideways speed of the falling pieces, in screen widths per second. */
    static final float DRIFT_MIN = 0.02f;
    static final float DRIFT_MAX = 0.12f;
    /** Each piece lets go after a random delay of up to this many seconds. */
    static final float RELEASE_DELAY_MAX_S = 0.5f;

    // ---- Layout
    static final int FAV_COLUMNS = 4;
    static final int DRAWER_COLUMNS = 5;
}
