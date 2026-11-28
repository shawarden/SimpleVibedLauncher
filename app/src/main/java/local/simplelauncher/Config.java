package local.simplelauncher;

/**
 * Every tunable number in one place. The values written here are the defaults. Most of them can be changed
 * from Launcher settings > Advanced settings (see {@link Tunables}); those are plain static fields, set when
 * the launcher starts and whenever a slider moves.
 */
final class Config {
    private Config() {}

    // ---- Home background: each of R, G, B wanders between 0 and BG_MAX, moving +/-STEP at a time.
    /** Default for the Background brightness setting. */
    static final int BG_MAX = 32;
    /** Size of one colour move, on the 0..255 scale. */
    static int STEP = 8;
    /** One +/-STEP move takes a random time between these two values. */
    static long BG_STEP_MIN_MS = 4000;
    static long BG_STEP_MAX_MS = 9000;

    // ---- Screensaver
    /** Default for the Screensaver brightness setting. */
    static final int SAVER_MAX = 64;
    /** Time to fade to the new colour after the clock bounces off an edge. */
    static long SAVER_FADE_MS = 2500;
    /** Default for the Screensaver starts after setting: idle time on the home screen before it starts. */
    static final long IDLE_MS = 2 * 60 * 1000;

    /** Gravity, in screen heights per second squared (bigger = faster fall). */
    static float GRAVITY = 0.08f;
    /**
     * Starting "energy" of the clock in screen-heights^2 per second^2. It never changes afterwards, so the
     * clock always bounces back up to the same height. Anything above 0 reaches the top of the screen.
     */
    static float ENERGY_MIN = 0.01f;
    static float ENERGY_MAX = 0.04f;
    /** Sideways speed, in screen widths per second. */
    static float VX_MIN = 0.06f;
    static float VX_MAX = 0.12f;
    /**
     * The Gravity / speed setting: Low, Medium, High. Low is the original values above. In gravity mode the
     * level multiplies the gravity; in clean-bounce mode it multiplies the movement speed. Entry 0 (Low) is
     * always 1; the other two can be changed.
     */
    static final float[] GRAVITY_SCALE = { 1f, 2.5f, 5f };
    static final float[] SPEED_SCALE = { 1f, 2f, 4f };
    /** Vertical speed for the clean bounce (no gravity), in screen heights per second. */
    static float VY_CLEAN_MIN = 0.10f;
    static float VY_CLEAN_MAX = 0.16f;

    // ---- Home screen <-> screensaver transition
    /** Seconds for the background to morph to the screensaver colours while the pieces fall. */
    static float FALL_FADE_S = 3.0f;
    /** Seconds for the clock to grow from its home size to its screensaver size. */
    static float GROW_S = 2.5f;
    /** Seconds for the background to morph back, and the shortest time the return takes. */
    static float RETURN_S = 1.5f;
    /** Everything is snapped to its home position by this time after waking. */
    static float RETURN_MAX_S = 2.0f;
    /** Seconds for the clock to shrink back to its home size. */
    static float RETURN_SHRINK_S = 0.9f;
    /** Random sideways speed of the falling pieces, in screen widths per second. */
    static float DRIFT_MIN = 0.01f;
    static float DRIFT_MAX = 0.06f;
    /** Each piece lets go after a random delay of up to this many seconds. */
    static float RELEASE_DELAY_MAX_S = 0.5f;

    // ---- Button mapper
    /** Holding a button this long counts as a long press. */
    static long LONG_PRESS_MS = 500;
    /** A second press within this time of the first counts as a double press. */
    static long DOUBLE_PRESS_MS = 300;

    // ---- Layout
    static int FAV_COLUMNS = 4;
    static int DRAWER_COLUMNS = 6;
}
