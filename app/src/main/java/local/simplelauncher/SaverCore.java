package local.simplelauncher;

import java.util.Random;

/**
 * Everything that happens in the screensaver, apart from drawing it:
 * <ol>
 * <li>FALL: the home screen's pieces are released and fall away under gravity, the background morphs to the
 *     screensaver colours, and the clock (released too, but it bounces) grows to its screensaver size.</li>
 * <li>SAVER: the clock bounces for ever (gravity or clean bounce), stepping the colour on each bounce.</li>
 * <li>RETURN: after a wake-up, the pieces spring back to their places, the clock shrinks and springs home,
 *     and the background morphs back to the home colour.</li>
 * </ol>
 * The plain screensaver (system daydream) skips straight to SAVER. No Android classes in here, so it can be
 * tested on a plain JVM. Units are pixels and seconds.
 */
final class SaverCore {
    enum Phase { FALL, SAVER, RETURN, DONE }

    private static final float SUB = 1f / 240f;    // physics step

    private final boolean gravityMode;
    private final int level;
    private final int homeColor;
    private final ColorDrift drift;
    private final BounceSim sim;
    private final Debris debris;                    // null for the plain screensaver
    private final float screenW, screenH;
    private final float clockHomeX, clockHomeY;

    private Phase phase;
    private float grow;                             // 0 = clock at home size, 1 = at screensaver size
    private float tPhase;                           // seconds spent in this phase
    private int bgColor;
    private int returnFromColor;
    private Spring clockSpring;
    private float msCarry;

    private SaverCore(Random rnd, int saverMax, boolean gravityMode, int level, float screenW, float screenH,
                      int homeColor, Debris debris, float clockHomeX, float clockHomeY) {
        this.gravityMode = gravityMode;
        this.level = level;
        this.screenW = screenW;
        this.screenH = screenH;
        this.homeColor = homeColor;
        this.debris = debris;
        this.clockHomeX = clockHomeX;
        this.clockHomeY = clockHomeY;
        this.drift = new ColorDrift(saverMax, Config.STEP, rnd);
        // The colour only steps on bounces once the transition is over.
        this.sim = new BounceSim(rnd, () -> {
            if (phase == Phase.SAVER) drift.nudge(Config.SAVER_FADE_MS);
        });
    }

    /** The plain screensaver: a bouncing clock on a dim colour, nothing else. */
    static SaverCore plain(Random rnd, int saverMax, boolean gravityMode, int level,
                           float screenW, float screenH, float boxW, float boxH) {
        SaverCore c = new SaverCore(rnd, saverMax, gravityMode, level, screenW, screenH, 0, null, 0f, 0f);
        c.phase = Phase.SAVER;
        c.grow = 1f;
        c.bgColor = c.drift.color();
        c.sim.reset(screenW, screenH, boxW, boxH, gravityMode, level);
        return c;
    }

    /**
     * The screensaver starting from the home screen. The clock box starts at (clockX, clockY) with the given
     * size (its size at home); debris holds the pieces to let fall.
     */
    static SaverCore fromHome(Random rnd, int saverMax, boolean gravityMode, int level,
                              float screenW, float screenH, int homeColor, Debris debris,
                              float clockX, float clockY, float boxW, float boxH) {
        SaverCore c = new SaverCore(rnd, saverMax, gravityMode, level, screenW, screenH,
                homeColor, debris, clockX, clockY);
        c.phase = Phase.FALL;
        c.grow = 0f;
        c.bgColor = homeColor;
        int lv = Math.max(0, Math.min(2, level));
        // Everything falls under the Gravity strength setting, whichever way the clock moves later.
        debris.release(screenW, screenH, Config.GRAVITY * Config.GRAVITY_SCALE[lv] * screenH);
        c.sim.releaseAt(screenW, screenH, boxW, boxH, clockX, clockY, level);
        return c;
    }

    /**
     * Advance by dt seconds. boxW and boxH are the clock's current size, which the caller measures from the
     * text at the current {@link #grow()}.
     */
    void step(float dt, float boxW, float boxH) {
        if (phase == Phase.DONE) return;
        tPhase += dt;
        switch (phase) {
            case FALL:
                grow = Math.min(1f, grow + dt / Config.GROW_S);
                sim.setBox(boxW, boxH);
                for (float left = dt; left > 0f; left -= SUB) {
                    float s = Math.min(left, SUB);
                    sim.step(s);
                    debris.step(s);
                }
                bgColor = blend(homeColor, drift.color(), smooth(Math.min(1f, tPhase / Config.FALL_FADE_S)));
                if (debris.allGone() && tPhase >= Config.FALL_FADE_S && grow >= 1f) {
                    phase = Phase.SAVER;
                    tPhase = 0f;
                    if (!gravityMode) sim.toCleanBounce(level);
                }
                break;

            case SAVER:
                sim.setBox(boxW, boxH);
                for (float left = dt; left > 0f; left -= SUB) sim.step(Math.min(left, SUB));
                msCarry += dt * 1000f;
                long whole = (long) msCarry;
                msCarry -= whole;
                if (whole > 0) drift.advance(whole, false, 0, 0);
                bgColor = drift.color();
                break;

            case RETURN:
                grow = Math.max(0f, grow - dt / Config.RETURN_SHRINK_S);
                for (float left = dt; left > 0f; left -= SUB) {
                    float s = Math.min(left, SUB);
                    debris.step(s);
                    clockSpring.step(s);
                }
                bgColor = blend(returnFromColor, homeColor, smooth(Math.min(1f, tPhase / Config.RETURN_S)));
                boolean settled = debris.allSettled() && clockSpring.settled && grow <= 0f;
                if (tPhase >= Config.RETURN_MAX_S || (settled && tPhase >= Config.RETURN_S)) {
                    debris.snapHome();
                    clockSpring.snap();
                    grow = 0f;
                    bgColor = homeColor;
                    phase = Phase.DONE;
                }
                break;

            default:
                break;
        }
    }

    /** The user is back: start the return to the home screen (the plain screensaver just ends). */
    void wake() {
        if (phase == Phase.RETURN || phase == Phase.DONE) return;
        if (debris == null) {
            phase = Phase.DONE;
            return;
        }
        returnFromColor = bgColor;
        debris.beginReturn();
        float dist = Math.max(60f, (float) Math.hypot(sim.x - clockHomeX, sim.y - clockHomeY));
        float over = Math.max(0.03f, Math.min(0.14f, 0.9f * Math.max(clockHomeY, 0f) / dist));
        clockSpring = new Spring(sim.x, sim.y, sim.vx(), sim.vy(), clockHomeX, clockHomeY,
                (float) (2.0 * Math.PI) * 1.1f, Spring.zetaFor(over));
        phase = Phase.RETURN;
        tPhase = 0f;
    }

    Phase phase() { return phase; }

    int bgColor() { return bgColor; }

    /** 0 while the clock is at its home size, 1 at its screensaver size. */
    float grow() { return grow; }

    /** Top-left of the clock box. */
    float clockX() { return clockSpring != null && onWayHome() ? clockSpring.x : sim.x; }

    float clockY() { return clockSpring != null && onWayHome() ? clockSpring.y : sim.y; }

    Debris debris() { return debris; }

    // for tests
    int saverColor() { return drift.color(); }
    BounceSim sim() { return sim; }

    private boolean onWayHome() { return phase == Phase.RETURN || phase == Phase.DONE; }

    static float smooth(float f) { return f * f * (3f - 2f * f); }

    /** Blend two ARGB colours; e = 0 gives a, e = 1 gives b exactly. */
    static int blend(int a, int b, float e) {
        int out = 0;
        for (int sh = 0; sh <= 24; sh += 8) {
            int ca = (a >>> sh) & 0xFF;
            int cb = (b >>> sh) & 0xFF;
            out |= (Math.round(ca + (cb - ca) * e) & 0xFF) << sh;
        }
        return out;
    }
}
