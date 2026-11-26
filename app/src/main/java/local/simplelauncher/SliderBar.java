package local.simplelauncher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

/** A slider drawn for display only (the row that holds it handles the Left / Right keys). */
final class SliderBar extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private float fraction;
    private boolean changed;

    SliderBar(Context ctx) {
        super(ctx);
        setFocusable(false);
    }

    /** fraction is 0 to 1; changed is true when the value is not the original one. */
    void set(float fraction, boolean changed) {
        this.fraction = Math.max(0f, Math.min(1f, fraction));
        this.changed = changed;
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        float r = h * 0.5f;                       // thumb radius
        float trackH = h * 0.22f;
        float x0 = r;
        float x1 = w - r;
        float cy = h / 2f;

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0x33FFFFFF);
        rect.set(x0, cy - trackH / 2f, x1, cy + trackH / 2f);
        canvas.drawRoundRect(rect, trackH / 2f, trackH / 2f, paint);

        float x = x0 + (x1 - x0) * fraction;
        int accent = changed ? 0xFFFFD54F : 0xFFB0B0B0;
        paint.setColor(accent);
        rect.set(x0, cy - trackH / 2f, x, cy + trackH / 2f);
        canvas.drawRoundRect(rect, trackH / 2f, trackH / 2f, paint);
        canvas.drawCircle(x, cy, r * 0.8f, paint);
    }
}
