package local.simplelauncher;

import java.util.Random;

/**
 * The pieces of the home screen (icons and tiles). They are released to fall under gravity with a little
 * random sideways drift and are gone once they leave the screen; later they spring back to their homes.
 * No Android classes in here, so it can be tested on a plain JVM. Units are pixels and seconds, +y is down.
 */
final class Debris {
    private final int n;
    private final float[] homeX, homeY;     // where each piece belongs (top-left)
    private final float[] w, h;
    private final float[] x, y, vx, vy;
    private final float[] delay;            // seconds until released, or until it re-enters on the way back
    private final boolean[] gone;
    private final Spring[] spring;
    private final Random rnd;
    private float screenW, screenH, g;
    private boolean returning;

    Debris(float[] homeX, float[] homeY, float[] w, float[] h, Random rnd) {
        n = homeX.length;
        this.homeX = homeX;
        this.homeY = homeY;
        this.w = w;
        this.h = h;
        this.rnd = rnd;
        x = homeX.clone();
        y = homeY.clone();
        vx = new float[n];
        vy = new float[n];
        delay = new float[n];
        gone = new boolean[n];
        spring = new Spring[n];
    }

    int count() { return n; }

    /** Let go of everything: each piece falls under gravity g after a short random delay, drifting sideways. */
    void release(float screenW, float screenH, float g) {
        this.screenW = screenW;
        this.screenH = screenH;
        this.g = g;
        returning = false;
        for (int i = 0; i < n; i++) {
            float dir = rnd.nextBoolean() ? 1f : -1f;
            vx[i] = dir * (Config.DRIFT_MIN + (Config.DRIFT_MAX - Config.DRIFT_MIN) * rnd.nextFloat()) * screenW;
            vy[i] = 0f;
            delay[i] = rnd.nextFloat() * Config.RELEASE_DELAY_MAX_S;
            gone[i] = offScreen(i);          // a piece that was never on screen has nothing to fall from
        }
    }

    /** Advance by dt seconds; keep dt small (a few milliseconds). */
    void step(float dt) {
        if (returning) {
            for (int i = 0; i < n; i++) {
                if (delay[i] > 0f) { delay[i] -= dt; continue; }
                spring[i].step(dt);
            }
            return;
        }
        for (int i = 0; i < n; i++) {
            if (gone[i]) continue;
            if (delay[i] > 0f) { delay[i] -= dt; continue; }
            x[i] += vx[i] * dt;
            y[i] += vy[i] * dt + 0.5f * g * dt * dt;
            vy[i] += g * dt;
            if (offScreen(i)) gone[i] = true;   // no bounce: it keeps going and is not seen again
        }
    }

    /**
     * Start bringing every piece home. A piece still on screen springs back from where it is, with the
     * speed it has; a piece that has left comes up from below the screen.
     */
    void beginReturn() {
        returning = true;
        for (int i = 0; i < n; i++) {
            float sx = x[i], sy = y[i], svx = vx[i], svy = vy[i];
            if (gone[i]) {
                sx = homeX[i] + (rnd.nextFloat() - 0.5f) * 80f;
                sy = screenH + 30f;
                svx = 0f;
                svy = 0f;
                delay[i] = rnd.nextFloat() * 0.25f;      // staggered, so they don't all arrive together
            } else {
                delay[i] = 0f;
            }
            // Overshoot above home by up to 14% of the distance, but not far past the top of the screen.
            float dist = Math.max(60f, (float) Math.hypot(sx - homeX[i], sy - homeY[i]));
            float over = Math.max(0.03f, Math.min(0.14f, 0.9f * Math.max(homeY[i], 0f) / dist));
            float omega = (float) (2.0 * Math.PI) * (0.95f + 0.3f * rnd.nextFloat());
            spring[i] = new Spring(sx, sy, svx, svy, homeX[i], homeY[i], omega, Spring.zetaFor(over));
        }
    }

    /** Put everything exactly home. */
    void snapHome() {
        returning = true;
        for (int i = 0; i < n; i++) {
            if (spring[i] == null) spring[i] = new Spring(homeX[i], homeY[i], 0f, 0f, homeX[i], homeY[i], 1f, 1f);
            spring[i].snap();
        }
    }

    boolean allGone() {
        for (int i = 0; i < n; i++) if (!gone[i]) return false;
        return true;
    }

    boolean allSettled() {
        if (!returning) return false;
        for (int i = 0; i < n; i++) if (!spring[i].settled) return false;
        return true;
    }

    /** Whether piece i should be drawn. */
    boolean visible(int i) { return returning || !gone[i]; }

    float x(int i) { return returning ? spring[i].x : x[i]; }

    float y(int i) { return returning ? spring[i].y : y[i]; }

    private boolean offScreen(int i) {
        return y[i] >= screenH || x[i] + w[i] <= 0f || x[i] >= screenW || y[i] + h[i] <= 0f;
    }
}
