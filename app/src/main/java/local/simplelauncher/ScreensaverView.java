package local.simplelauncher;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.view.View;

import java.util.Random;

/**
 * The screensaver, drawn over everything else. Started from the home screen it shows the home screen's
 * pieces (pictures taken of them) falling away, the background morphing to the screensaver colours and the
 * clock growing and bouncing; on {@link #wake} everything springs back. The motion itself is in
 * {@link SaverCore}; this class only draws it. Also used on its own as the system daydream.
 */
final class ScreensaverView extends View {
    interface Listener {
        /** The return to the home screen has finished; the overlay can be hidden. */
        void onReturned();
    }

    // The clock's colours at home and in the screensaver; it blends between them as it grows.
    private static final int HOME_TIME_COLOR = 0xFFEDEDED;
    private static final int HOME_DAY_COLOR = 0xFFA8A8A8;
    private static final int SAVER_TIME_COLOR = 0xFFB8B8B8;
    private static final int SAVER_DAY_COLOR = 0xFF8C8C8C;

    private final Random rnd = new Random();
    private final Paint timePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bitmapPaint = new Paint(Paint.FILTER_BITMAP_FLAG);

    private Listener listener;
    private SaverCore core;
    private boolean running;
    private boolean pendingStart;      // build the core on the first frame, when the size is known
    private boolean notified;

    // settings, read when the screensaver starts
    private boolean showDate = true;
    private boolean gravity = true;
    private int level;
    private int saverMax = Config.SAVER_MAX;

    // what the home screen looked like (empty for the plain screensaver)
    private boolean fromHome;
    private Bitmap[] bitmaps;
    private float[] pieceX, pieceY, pieceScale;
    private int homeColor;
    private float clockHomeX, clockHomeY, homeTimePx, homeDayPx;

    private float saverTimePx, saverDayPx;
    private long last;
    private long minute = -1;
    private String time = "";
    private String day = "";
    private boolean dayVisible = true;
    private float timeW, dayW, boxW, boxH, timeBase, dayBase, align;

    ScreensaverView(Context ctx) {
        super(ctx);
        Tuning.ensureLoaded(ctx);
        Typeface light = Typeface.create("sans-serif-light", Typeface.NORMAL);
        timePaint.setTypeface(light);
        dayPaint.setTypeface(light);
    }

    void setListener(Listener l) {
        listener = l;
    }

    /** The plain screensaver: a bouncing clock on a dim colour (used by the system daydream). */
    void start() {
        readSettings();
        fromHome = false;
        releaseBitmaps();
        begin();
    }

    /**
     * The screensaver starting from the home screen. Each piece is a picture of an icon or tile with its
     * top-left position (in this view's coordinates) and the scale it is shown at. The clock text is at
     * (clockX, clockY) at home, in the given text sizes.
     */
    void startFromHome(Bitmap[] pictures, float[] x, float[] y, float[] scale, int backgroundColor,
                       float clockX, float clockY, float timePx, float dayPx) {
        readSettings();
        releaseBitmaps();
        fromHome = true;
        bitmaps = pictures;
        pieceX = x;
        pieceY = y;
        pieceScale = scale;
        homeColor = backgroundColor;
        clockHomeX = clockX;
        clockHomeY = clockY;
        homeTimePx = timePx;
        homeDayPx = dayPx;
        begin();
    }

    /** The user is back: start the return to the home screen. */
    void wake() {
        if (!running) return;
        if (core == null) {                 // woken before the first frame: nothing to animate back
            finish();
            return;
        }
        core.wake();
    }

    boolean isRunning() {
        return running;
    }

    /** Stop at once and let go of the pictures. */
    void stop() {
        running = false;
        core = null;
        releaseBitmaps();
    }

    private void readSettings() {
        Context c = getContext();
        saverMax = Prefs.saverMax(c);
        showDate = Prefs.saverShowsDate(c);
        gravity = Prefs.gravityMotion(c);
        level = Prefs.motionLevel(c);
    }

    private void begin() {
        running = true;
        pendingStart = true;
        notified = false;
        core = null;
        minute = -1;
        last = SystemClock.uptimeMillis();
        invalidate();
    }

    private void releaseBitmaps() {
        if (bitmaps != null) {
            for (Bitmap b : bitmaps) if (b != null && !b.isRecycled()) b.recycle();
        }
        bitmaps = null;
    }

    private void finish() {
        if (notified) return;
        notified = true;
        post(() -> {
            if (listener != null) listener.onReturned();
        });
    }

    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        saverTimePx = h * 0.13f;
        saverDayPx = h * 0.042f;
        if (running && oldw != 0) {        // the screen changed size while running: start over
            pendingStart = true;
            core = null;
        }
    }

    /** Refreshes the time and date text once a minute. */
    private void refreshStrings() {
        long now = System.currentTimeMillis();
        long m = now / 60000L;
        if (m == minute) return;
        minute = m;
        time = ClockText.time(getContext(), now);
        day = ClockText.day(now);
    }

    /**
     * Sizes and measures the clock for the given amount of growth: 0 is its size on the home screen,
     * 1 its screensaver size. The text is left-aligned at home and centred in the screensaver.
     */
    private void layoutText(float grow) {
        float e = SaverCore.smooth(grow);
        float tPx = homeTimePx + (saverTimePx - homeTimePx) * e;
        float dPx = homeDayPx + (saverDayPx - homeDayPx) * e;
        if (!fromHome) {
            tPx = saverTimePx;
            dPx = saverDayPx;
        }
        timePaint.setTextSize(tPx);
        dayPaint.setTextSize(dPx);
        timePaint.setColor(SaverCore.blend(HOME_TIME_COLOR, SAVER_TIME_COLOR, e));
        dayPaint.setColor(SaverCore.blend(HOME_DAY_COLOR, SAVER_DAY_COLOR, e));

        // With "time only" the date fades out as the clock grows; its space is dropped once it is gone.
        dayVisible = showDate || grow < 1f;
        if (!showDate) dayPaint.setAlpha(Math.round(255f * (1f - e)));

        timeW = timePaint.measureText(time);
        Paint.FontMetrics tm = timePaint.getFontMetrics();
        timeBase = -tm.ascent;
        if (dayVisible) {
            dayW = dayPaint.measureText(day);
            Paint.FontMetrics dm = dayPaint.getFontMetrics();
            dayBase = timeBase + tm.descent - dm.ascent;
            boxW = Math.max(timeW, dayW);
            boxH = dayBase + dm.descent;
        } else {
            dayW = 0f;
            boxW = timeW;
            boxH = timeBase + tm.descent;
        }
        align = 0.5f * e;
    }

    private void buildCore() {
        float w = getWidth();
        float h = getHeight();
        layoutText(fromHome ? 0f : 1f);
        if (fromHome && bitmaps != null && bitmaps.length > 0) {
            float[] pw = new float[bitmaps.length];
            float[] ph = new float[bitmaps.length];
            for (int i = 0; i < bitmaps.length; i++) {
                pw[i] = bitmaps[i].getWidth();
                ph[i] = bitmaps[i].getHeight();
            }
            Debris debris = new Debris(pieceX.clone(), pieceY.clone(), pw, ph, rnd);
            core = SaverCore.fromHome(rnd, saverMax, gravity, level, w, h, homeColor, debris,
                    clockHomeX, clockHomeY, boxW, boxH);
        } else if (fromHome) {
            // nothing on the home screen to drop (no favourites and no icons laid out): just the clock
            Debris none = new Debris(new float[0], new float[0], new float[0], new float[0], rnd);
            core = SaverCore.fromHome(rnd, saverMax, gravity, level, w, h, homeColor, none,
                    clockHomeX, clockHomeY, boxW, boxH);
        } else {
            core = SaverCore.plain(rnd, saverMax, gravity, level, w, h, boxW, boxH);
        }
    }

    @Override protected void onDraw(Canvas canvas) {
        if (!running || getWidth() == 0) {
            canvas.drawColor(0xFF000000);
            return;
        }
        long now = SystemClock.uptimeMillis();
        float dt = Math.min(now - last, 50L) / 1000f;
        last = now;

        refreshStrings();
        if (pendingStart) {
            buildCore();
            pendingStart = false;
        }
        layoutText(core.grow());
        core.step(dt, boxW, boxH);

        canvas.drawColor(core.bgColor());

        Debris debris = core.debris();
        if (debris != null && bitmaps != null) {
            for (int i = 0; i < bitmaps.length; i++) {
                if (!debris.visible(i)) continue;
                Bitmap b = bitmaps[i];
                float s = pieceScale[i];
                float halfW = b.getWidth() / 2f;
                float halfH = b.getHeight() / 2f;
                canvas.save();
                canvas.translate(debris.x(i) + halfW, debris.y(i) + halfH);
                canvas.scale(s, s);
                canvas.drawBitmap(b, -halfW, -halfH, bitmapPaint);
                canvas.restore();
            }
        }

        float cx = core.clockX();
        float cy = core.clockY();
        canvas.drawText(time, cx + (boxW - timeW) * align, cy + timeBase, timePaint);
        if (dayVisible) {
            canvas.drawText(day, cx + (boxW - dayW) * align, cy + dayBase, dayPaint);
        }

        if (core.phase() == SaverCore.Phase.DONE) {
            finish();            // the last frame drawn is exactly the home screen again
        } else {
            postInvalidateOnAnimation();
        }
    }
}
