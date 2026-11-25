package local.simplelauncher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.view.View;

import java.util.Random;

/**
 * The screensaver: a dim background whose colour steps each time the clock bounces off an edge of the
 * screen, and a clock that moves around with gravity or in clean straight lines (a setting), showing
 * the time and date or just the time (a setting).
 */
final class ScreensaverView extends View {
    private final Random rnd = new Random();
    private final Paint timePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private ColorDrift drift = new ColorDrift(Config.SAVER_MAX, Config.STEP, rnd);
    private final BounceSim sim = new BounceSim(rnd, new BounceSim.Listener() {
        @Override public void onBounce() {
            drift.nudge(Config.SAVER_FADE_MS);
        }
    });

    private boolean running;
    private boolean needsReset;
    private boolean showDate = true;
    private boolean gravity = true;
    private int level;
    private long last;
    private long minute = -1;
    private String time = "";
    private String day = "";
    private float timeW, dayW, boxW, boxH, timeBase, dayBase;

    ScreensaverView(Context ctx) {
        super(ctx);
        Typeface light = Typeface.create("sans-serif-light", Typeface.NORMAL);
        timePaint.setTypeface(light);
        timePaint.setColor(0xFFB8B8B8);
        dayPaint.setTypeface(light);
        dayPaint.setColor(0xFF8C8C8C);
    }

    /** Start with a fresh random colour and a fresh random throw, using the current settings. */
    void start() {
        Context c = getContext();
        drift = new ColorDrift(Prefs.saverMax(c), Config.STEP, rnd);
        showDate = Prefs.saverShowsDate(c);
        gravity = Prefs.gravityMotion(c);
        level = Prefs.motionLevel(c);
        running = true;
        needsReset = true;
        minute = -1;
        last = SystemClock.uptimeMillis();
        invalidate();
    }

    void stop() {
        running = false;
    }

    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        timePaint.setTextSize(h * 0.13f);
        dayPaint.setTextSize(h * 0.042f);
        minute = -1;
        needsReset = true;
    }

    /** Re-measures the clock text once a minute. */
    private void refreshText() {
        long now = System.currentTimeMillis();
        long m = now / 60000L;
        if (m == minute) return;
        minute = m;
        time = ClockText.time(getContext(), now);
        timeW = timePaint.measureText(time);
        Paint.FontMetrics tm = timePaint.getFontMetrics();
        timeBase = -tm.ascent;
        if (showDate) {
            day = ClockText.day(now);
            dayW = dayPaint.measureText(day);
            Paint.FontMetrics dm = dayPaint.getFontMetrics();
            dayBase = timeBase + tm.descent - dm.ascent;
            boxW = Math.max(timeW, dayW);
            boxH = dayBase + dm.descent;
        } else {
            day = "";
            dayW = 0f;
            boxW = timeW;
            boxH = timeBase + tm.descent;
        }
        sim.setBox(boxW, boxH);
    }

    @Override protected void onDraw(Canvas canvas) {
        if (!running || getWidth() == 0) {
            canvas.drawColor(0xFF000000);
            return;
        }
        long now = SystemClock.uptimeMillis();
        long dtMs = Math.min(now - last, 50L);
        last = now;

        refreshText();
        if (needsReset) {
            sim.reset(getWidth(), getHeight(), boxW, boxH, gravity, level);
            needsReset = false;
        }

        float left = dtMs / 1000f;
        while (left > 0f) {
            float s = Math.min(left, 1f / 240f);
            sim.step(s);
            left -= s;
        }
        drift.advance(dtMs, false, 0, 0);

        canvas.drawColor(drift.color());
        canvas.drawText(time, sim.x + (boxW - timeW) / 2f, sim.y + timeBase, timePaint);
        if (showDate) {
            canvas.drawText(day, sim.x + (boxW - dayW) / 2f, sim.y + dayBase, dayPaint);
        }
        postInvalidateOnAnimation();
    }
}
