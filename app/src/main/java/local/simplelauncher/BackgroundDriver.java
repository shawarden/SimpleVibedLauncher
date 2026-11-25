package local.simplelauncher;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;

import java.util.Random;

/**
 * Slowly drifts a view's background colour. The colour state is shared, so the home screen, the apps
 * drawer and the settings screen carry on from the same colour instead of restarting. The brightness
 * limit comes from the settings each time {@link #start} is called.
 */
final class BackgroundDriver {
    private static ColorDrift drift;
    private static int driftMax;
    private static long last;

    private final View view;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean running;
    private int lastColor = 0;

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            long now = SystemClock.uptimeMillis();
            drift.advance(Math.min(now - last, 1000L), true, Config.BG_STEP_MIN_MS, Config.BG_STEP_MAX_MS);
            last = now;
            int color = drift.color();
            if (color != lastColor) {
                lastColor = color;
                view.setBackgroundColor(color);
            }
            if (running) handler.postDelayed(this, 100);
        }
    };

    BackgroundDriver(View view) {
        this.view = view;
    }

    /** The colour the background has right now (the screensaver starts from it and returns to it). */
    static int currentColor() {
        return drift == null ? 0xFF000000 : drift.color();
    }

    void start() {
        if (running) return;
        running = true;
        int max = Prefs.bgMax(view.getContext());
        if (drift == null || max != driftMax) {
            drift = new ColorDrift(max, Config.STEP, new Random());
            driftMax = max;
        }
        last = SystemClock.uptimeMillis();
        handler.post(tick);
    }

    void stop() {
        running = false;
        handler.removeCallbacks(tick);
    }
}
