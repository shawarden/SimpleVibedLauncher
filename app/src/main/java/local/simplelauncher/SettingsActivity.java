package local.simplelauncher;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * The launcher's own settings (the House icon). Each row is a list of choices: press OK to step to the
 * next one, or Left / Right to step either way. Changes are saved as soon as they are made.
 */
public class SettingsActivity extends Activity {
    private BackgroundDriver background;
    private int h;
    private int rowH;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Tuning.ensureLoaded(this);
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int w = dm.widthPixels;
        h = dm.heightPixels;
        int margin = (int) (w * 0.05f);
        int gap = (int) (h * 0.014f);
        rowH = (int) (h * 0.07f);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0xFF000000);
        background = new BackgroundDriver(root);

        int headerTop = (int) (h * 0.04f);
        int headerH = (int) (h * 0.10f);

        TextView title = text(h * 0.05f, 0xFFEDEDED);
        title.setText("Launcher settings");
        title.setGravity(Gravity.CENTER_VERTICAL);
        FrameLayout.LayoutParams titleParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, headerH, Gravity.TOP | Gravity.START);
        titleParams.topMargin = headerTop;
        titleParams.leftMargin = margin;
        root.addView(title, titleParams);

        TextView hint = text(h * 0.026f, 0xFF9A9A9A);
        hint.setText("OK or Left / Right to change");
        hint.setGravity(Gravity.CENTER_VERTICAL);
        FrameLayout.LayoutParams hintParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, headerH, Gravity.TOP | Gravity.END);
        hintParams.topMargin = headerTop;
        hintParams.rightMargin = margin;
        root.addView(hint, hintParams);

        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        FrameLayout.LayoutParams columnParams = new FrameLayout.LayoutParams(
                (int) (w * 0.62f), ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        columnParams.topMargin = headerTop + headerH + gap;
        root.addView(column, columnParams);

        long min = 60 * 1000L;
        View first = addRow(column, gap, "Clock format (home and screensaver)", Prefs.CLOCK_FORMAT, 0, false,
                new String[] { "Same as TV", "12 hour", "24 hour" },
                new long[] { 0, 1, 2 });
        addRow(column, gap, "Screensaver starts after", Prefs.IDLE_MS, Config.IDLE_MS, false,
                new String[] { "Never", "1 minute", "2 minutes", "5 minutes", "10 minutes", "30 minutes" },
                new long[] { 0, min, 2 * min, 5 * min, 10 * min, 30 * min });
        addRow(column, gap, "Screensaver clock moves", Prefs.MOTION, 0, false,
                new String[] { "With gravity", "Clean bounce" },
                new long[] { 0, 1 });
        addRow(column, gap, "Gravity strength / speed", Prefs.MOTION_LEVEL, 0, false,
                new String[] { "Low  (default)", "Medium", "High" },
                new long[] { 0, 1, 2 });
        addRow(column, gap, "Screensaver clock shows", Prefs.SAVER_DATE, 1, false,
                new String[] { "Time and date", "Time only" },
                new long[] { 1, 0 });
        addRow(column, gap, "Screensaver brightness", Prefs.SAVER_MAX, Config.SAVER_MAX, false,
                new String[] { "32", "64  (default)", "96", "128" },
                new long[] { 32, 64, 96, 128 });
        addRow(column, gap, "Background brightness", Prefs.BG_MAX, Config.BG_MAX, true,
                new String[] { "16  (very dim)", "32  (default)", "48", "64" },
                new long[] { 16, 32, 48, 64 });

        addLinkRow(column, gap, "Advanced settings (sliders)", "Open  >",
                () -> startActivity(new Intent(this, TuningActivity.class)));
        addLinkRow(column, gap, "Button mapper", "Open  >",
                () -> startActivity(new Intent(this, ButtonMapActivity.class)));

        setContentView(root);
        first.requestFocus();
    }

    private TextView text(float sizePx, int color) {
        TextView t = new TextView(this);
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, sizePx);
        t.setTextColor(color);
        t.setFocusable(false);
        return t;
    }

    /** A row that opens another screen when OK is pressed. */
    private void addLinkRow(LinearLayout parent, int gap, String name, String value, Runnable open) {
        LinearLayout row = makeRow(name);
        TextView valueView = text(h * 0.032f, 0xFFFFD54F);
        valueView.setText(value);
        row.addView(valueView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        row.setOnClickListener(v -> open.run());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, rowH);
        lp.bottomMargin = gap;
        parent.addView(row, lp);
    }

    /** The shared look of a settings row: rounded, with a white outline when it has the focus; holds the name. */
    private LinearLayout makeRow(String name) {
        final LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        int pad = (int) (h * 0.035f);
        row.setPadding(pad, 0, pad, 0);

        float radius = rowH * 0.12f;
        GradientDrawable idle = new GradientDrawable();
        idle.setColor(0x1AFFFFFF);
        idle.setCornerRadius(radius);
        GradientDrawable focused = new GradientDrawable();
        focused.setColor(0x33FFFFFF);
        focused.setCornerRadius(radius);
        focused.setStroke(Math.max(3, rowH / 30), 0xFFFFFFFF);
        StateListDrawable rowBackground = new StateListDrawable();
        rowBackground.addState(new int[] { android.R.attr.state_focused }, focused);
        rowBackground.addState(new int[] {}, idle);
        row.setBackground(rowBackground);
        row.setFocusable(true);

        TextView nameView = text(h * 0.032f, 0xFFEDEDED);
        nameView.setText(name);
        row.addView(nameView, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        return row;
    }

    /** One row: a name on the left, the current choice on the right. */
    private View addRow(LinearLayout parent, int gap, String name, final String key, long fallback,
                        final boolean restartBackground, final String[] labels, final long[] values) {
        final LinearLayout row = makeRow(name);

        final TextView valueView = text(h * 0.032f, 0xFFFFD54F);
        row.addView(valueView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        long saved = Prefs.get(this, key, fallback);
        int start = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i] == saved) start = i;
        }
        final int[] index = { start };
        valueView.setText("<  " + labels[index[0]] + "  >");

        final Runnable[] stepper = new Runnable[1];
        final int[] direction = { 1 };
        stepper[0] = () -> {
            int n = values.length;
            index[0] = (index[0] + direction[0] + n) % n;
            Prefs.set(this, key, values[index[0]]);
            valueView.setText("<  " + labels[index[0]] + "  >");
            if (restartBackground) {         // show the new brightness straight away
                background.stop();
                background.start();
            }
        };
        row.setOnClickListener(v -> { direction[0] = 1; stepper[0].run(); });
        row.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
            if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) { direction[0] = 1; stepper[0].run(); return true; }
            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) { direction[0] = -1; stepper[0].run(); return true; }
            return false;
        });

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, rowH);
        lp.bottomMargin = gap;
        parent.addView(row, lp);
        return row;
    }

    @Override protected void onResume() {
        super.onResume();
        background.start();
    }

    @Override protected void onPause() {
        background.stop();
        super.onPause();
    }
}
