package local.simplelauncher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.View;

/** A focusable icon button drawn in code, so no image files are needed. */
final class GlyphView extends View {
    enum Kind { APPS, INPUTS, HOME, WIFI, COG }

    private static final int COLOR = 0xFFE8E8E8;

    private final Kind kind;
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF rect = new RectF();
    private final GradientDrawable focusRing = new GradientDrawable();

    GlyphView(Context ctx, Kind kind) {
        super(ctx);
        this.kind = kind;
        fill.setColor(COLOR);
        stroke.setColor(COLOR);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeCap(Paint.Cap.ROUND);
        stroke.setStrokeJoin(Paint.Join.ROUND);
        path.setFillType(Path.FillType.EVEN_ODD);   // so inner shapes (cog hole, house door) become holes

        focusRing.setColor(0x33FFFFFF);
        GradientDrawable idle = new GradientDrawable();
        idle.setColor(0x00000000);
        StateListDrawable background = new StateListDrawable();
        background.addState(new int[] { android.R.attr.state_focused }, focusRing);
        background.addState(new int[] {}, idle);
        setBackground(background);
        setFocusable(true);
    }

    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        focusRing.setCornerRadius(Math.min(w, h) * 0.22f);
        focusRing.setStroke(Math.max(3, Math.min(w, h) / 25), 0xFFFFFFFF);
    }

    @Override protected void onDraw(Canvas canvas) {
        float s = Math.min(getWidth(), getHeight());
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        switch (kind) {
            case APPS:   drawApps(canvas, s, cx, cy); break;
            case INPUTS: drawInputs(canvas, s, cx, cy); break;
            case HOME:   drawHome(canvas, s, cx, cy); break;
            case WIFI:   drawWifi(canvas, s, cx, cy); break;
            case COG:    drawCog(canvas, s, cx, cy); break;
        }
    }

    /** Nine rounded squares. */
    private void drawApps(Canvas canvas, float s, float cx, float cy) {
        float sq = s * 0.15f;
        float gap = s * 0.06f;
        float total = 3 * sq + 2 * gap;
        float left = cx - total / 2f;
        float top = cy - total / 2f;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                float l = left + col * (sq + gap);
                float t = top + row * (sq + gap);
                rect.set(l, t, l + sq, t + sq);
                canvas.drawRoundRect(rect, sq * 0.25f, sq * 0.25f, fill);
            }
        }
    }

    /** An arrow going into a box. */
    private void drawInputs(Canvas canvas, float s, float cx, float cy) {
        stroke.setStrokeWidth(s * 0.07f);
        path.rewind();
        // the box, open on the left
        path.moveTo(cx, cy - 0.28f * s);
        path.lineTo(cx + 0.28f * s, cy - 0.28f * s);
        path.lineTo(cx + 0.28f * s, cy + 0.28f * s);
        path.lineTo(cx, cy + 0.28f * s);
        // the arrow
        path.moveTo(cx - 0.31f * s, cy);
        path.lineTo(cx + 0.12f * s, cy);
        path.moveTo(cx - 0.02f * s, cy - 0.13f * s);
        path.lineTo(cx + 0.12f * s, cy);
        path.lineTo(cx - 0.02f * s, cy + 0.13f * s);
        canvas.drawPath(path, stroke);
    }

    /** A house with a door. */
    private void drawHome(Canvas canvas, float s, float cx, float cy) {
        float[] p = {
                0f, -0.31f,  0.33f, -0.02f,  0.22f, -0.02f,  0.22f, 0.28f,
                -0.22f, 0.28f,  -0.22f, -0.02f,  -0.33f, -0.02f };
        path.rewind();
        for (int i = 0; i < p.length; i += 2) {
            float x = cx + p[i] * s;
            float y = cy + p[i + 1] * s;
            if (i == 0) path.moveTo(x, y); else path.lineTo(x, y);
        }
        path.close();
        path.addRect(cx - 0.07f * s, cy + 0.06f * s, cx + 0.07f * s, cy + 0.28f * s, Path.Direction.CW);
        canvas.drawPath(path, fill);
    }

    /** Three arcs and a dot. */
    private void drawWifi(Canvas canvas, float s, float cx, float cy) {
        float ox = cx;
        float oy = cy + 0.21f * s;
        stroke.setStrokeWidth(s * 0.065f);
        float[] radius = { 0.14f, 0.27f, 0.40f };
        for (float r : radius) {
            float rr = r * s;
            rect.set(ox - rr, oy - rr, ox + rr, oy + rr);
            canvas.drawArc(rect, 225f, 90f, false, stroke);   // 270 degrees is straight up
        }
        canvas.drawCircle(ox, oy, 0.045f * s, fill);
    }

    /** A toothed wheel with a hole. */
    private void drawCog(Canvas canvas, float s, float cx, float cy) {
        float outer = s * 0.30f;
        float inner = s * 0.235f;
        float hole = s * 0.11f;
        path.rewind();
        for (int tooth = 0; tooth < 8; tooth++) {
            double a = Math.PI * 2 * tooth / 8;
            double[] angle = { a - 0.22, a - 0.12, a + 0.12, a + 0.22 };
            float[] radius = { inner, outer, outer, inner };
            for (int k = 0; k < 4; k++) {
                float px = cx + (float) (Math.cos(angle[k]) * radius[k]);
                float py = cy + (float) (Math.sin(angle[k]) * radius[k]);
                if (tooth == 0 && k == 0) path.moveTo(px, py); else path.lineTo(px, py);
            }
        }
        path.close();
        path.addCircle(cx, cy, hole, Path.Direction.CW);
        canvas.drawPath(path, fill);
    }
}
