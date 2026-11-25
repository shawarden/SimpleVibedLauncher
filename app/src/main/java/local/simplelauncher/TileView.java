package local.simplelauncher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * One app tile. Shows the app's TV banner when asked (and it has one), otherwise its icon and name.
 * Focusable, with a white ring and a slight zoom when highlighted.
 */
final class TileView extends FrameLayout {
    private final StarView star;

    TileView(Context ctx, AppEntry app, int w, int h, boolean wantBanner) {
        super(ctx);
        final float radius = h * 0.08f;

        GradientDrawable fill = new GradientDrawable();
        fill.setColor(0x26FFFFFF);
        fill.setCornerRadius(radius);
        setBackground(fill);

        // The ring is the foreground so a banner image can't cover it.
        GradientDrawable ring = new GradientDrawable();
        ring.setColor(0x00000000);
        ring.setCornerRadius(radius);
        ring.setStroke(Math.max(3, h / 40), 0xFFFFFFFF);
        StateListDrawable foreground = new StateListDrawable();
        foreground.addState(new int[] { android.R.attr.state_focused }, ring);
        setForeground(foreground);

        setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radius);
            }
        });
        setClipToOutline(true);
        setFocusable(true);

        Drawable banner = wantBanner ? app.banner() : null;
        if (banner != null) {
            ImageView image = new ImageView(ctx);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            image.setImageDrawable(banner);
            addView(image, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        } else {
            int iconSize = (int) (h * 0.46f);
            ImageView icon = new ImageView(ctx);
            icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            icon.setImageDrawable(app.icon());
            LayoutParams ilp = new LayoutParams(iconSize, iconSize, Gravity.TOP | Gravity.CENTER_HORIZONTAL);
            ilp.topMargin = (int) (h * 0.12f);
            addView(icon, ilp);

            TextView label = new TextView(ctx);
            label.setText(app.label);
            label.setTextColor(0xFFE6E6E6);
            label.setTextSize(TypedValue.COMPLEX_UNIT_PX, h * 0.115f);
            label.setSingleLine(true);
            label.setEllipsize(TextUtils.TruncateAt.END);
            label.setGravity(Gravity.CENTER);
            LayoutParams llp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT,
                    Gravity.BOTTOM);
            llp.bottomMargin = (int) (h * 0.09f);
            llp.leftMargin = llp.rightMargin = (int) (h * 0.06f);
            addView(label, llp);
        }

        star = new StarView(ctx);
        int starSize = Math.max(16, (int) (h * 0.14f));
        LayoutParams slp = new LayoutParams(starSize, starSize, Gravity.TOP | Gravity.END);
        slp.topMargin = (int) (h * 0.05f);
        slp.setMarginEnd((int) (h * 0.05f));
        star.setVisibility(GONE);
        addView(star, slp);
    }

    /** Shows or hides the small star that marks a favourite. */
    void setFavourite(boolean favourite) {
        star.setVisibility(favourite ? VISIBLE : GONE);
    }

    @Override protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        float scale = gainFocus ? 1.06f : 1f;
        animate().scaleX(scale).scaleY(scale).setDuration(120).start();
    }

    /** A small five-pointed star, drawn with a path so it doesn't depend on any font. */
    private static final class StarView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();

        StarView(Context ctx) {
            super(ctx);
            paint.setColor(0xFFFFD54F);
        }

        @Override protected void onDraw(Canvas canvas) {
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            float outer = Math.min(getWidth(), getHeight()) / 2f;
            float inner = outer * 0.42f;
            path.rewind();
            for (int i = 0; i < 10; i++) {
                double a = -Math.PI / 2 + i * Math.PI / 5;
                float r = (i % 2 == 0) ? outer : inner;
                float px = cx + (float) (Math.cos(a) * r);
                float py = cy + (float) (Math.sin(a) * r);
                if (i == 0) path.moveTo(px, py); else path.lineTo(px, py);
            }
            path.close();
            canvas.drawPath(path, paint);
        }
    }
}
