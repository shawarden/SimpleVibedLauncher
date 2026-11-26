package local.simplelauncher;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Advanced settings: a slider for each adjustable number in {@link Config} (see {@link Tunables}). Left / Right
 * moves a slider (hold for bigger jumps), OK puts that one back to its original value. Saved straight away.
 */
public class TuningActivity extends Activity {
    private BackgroundDriver background;
    private int w, h, rowH, gap;
    private LinearLayout column;
    private final List<Runnable> refreshers = new ArrayList<>();

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Tuning.ensureLoaded(this);
        DisplayMetrics dm = getResources().getDisplayMetrics();
        w = dm.widthPixels;
        h = dm.heightPixels;
        int margin = (int) (w * 0.05f);
        gap = (int) (h * 0.012f);
        rowH = (int) (h * 0.068f);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0xFF000000);
        background = new BackgroundDriver(root);

        int headerTop = (int) (h * 0.04f);
        int headerH = (int) (h * 0.10f);

        TextView title = text(h * 0.05f, 0xFFEDEDED);
        title.setText("Advanced settings");
        title.setGravity(Gravity.CENTER_VERTICAL);
        FrameLayout.LayoutParams titleParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, headerH, Gravity.TOP | Gravity.START);
        titleParams.topMargin = headerTop;
        titleParams.leftMargin = margin;
        root.addView(title, titleParams);

        TextView hint = text(h * 0.024f, 0xFF9A9A9A);
        hint.setText("Left / Right to change (hold for faster)    OK = back to original");
        hint.setGravity(Gravity.CENTER_VERTICAL);
        FrameLayout.LayoutParams hintParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, headerH, Gravity.TOP | Gravity.END);
        hintParams.topMargin = headerTop;
        hintParams.rightMargin = margin;
        root.addView(hint, hintParams);

        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroll.setClipToPadding(false);
        scroll.setClipChildren(false);
        scroll.setPadding(0, gap, 0, gap * 4);
        column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setClipChildren(false);
        scroll.addView(column, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        FrameLayout.LayoutParams scrollParams = new FrameLayout.LayoutParams(
                (int) (w * 0.84f), ViewGroup.LayoutParams.MATCH_PARENT, Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        scrollParams.topMargin = headerTop + headerH;
        root.addView(scroll, scrollParams);

        View first = null;
        String lastGroup = null;
        for (Tunables.Item it : Tunables.ALL) {
            if (!it.group.equals(lastGroup)) {
                addHeader(it.group);
                lastGroup = it.group;
            }
            View row = addSliderRow(it);
            if (first == null) first = row;
        }
        addResetRow();

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

    private void addHeader(String name) {
        TextView t = text(h * 0.024f, 0xFF8AB4F8);
        t.setText(name.toUpperCase());
        t.setLetterSpacing(0.08f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = column.getChildCount() == 0 ? 0 : gap * 3;
        lp.bottomMargin = gap;
        lp.leftMargin = (int) (h * 0.035f);
        column.addView(t, lp);
    }

    /** A rounded row that shows a white outline when it has the focus. */
    private LinearLayout rowShell() {
        LinearLayout row = new LinearLayout(this);
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
        StateListDrawable bg = new StateListDrawable();
        bg.addState(new int[] { android.R.attr.state_focused }, focused);
        bg.addState(new int[] {}, idle);
        row.setBackground(bg);
        row.setFocusable(true);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, rowH);
        lp.bottomMargin = gap;
        column.addView(row, lp);
        return row;
    }

    private View addSliderRow(final Tunables.Item it) {
        LinearLayout row = rowShell();

        TextView name = text(h * 0.025f, 0xFFEDEDED);
        name.setText(it.title);
        name.setSingleLine(true);
        name.setEllipsize(TextUtils.TruncateAt.END);
        row.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        final SliderBar bar = new SliderBar(this);
        LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams((int) (w * 0.16f), (int) (h * 0.030f));
        barParams.leftMargin = (int) (h * 0.02f);
        row.addView(bar, barParams);

        final TextView value = text(h * 0.027f, 0xFFFFD54F);
        value.setGravity(Gravity.END);
        LinearLayout.LayoutParams valueParams = new LinearLayout.LayoutParams(
                (int) (w * 0.075f), ViewGroup.LayoutParams.WRAP_CONTENT);
        valueParams.leftMargin = (int) (h * 0.015f);
        row.addView(value, valueParams);

        refreshers.add(() -> {
            boolean changed = Math.abs(it.value() - it.def) > it.step * 0.01f;
            bar.set(it.fraction(), changed);
            value.setText(it.formatValue());
            value.setTextColor(changed ? 0xFFFFD54F : 0xFFB0B0B0);
        });

        row.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
            if (keyCode != KeyEvent.KEYCODE_DPAD_LEFT && keyCode != KeyEvent.KEYCODE_DPAD_RIGHT) return false;
            int repeat = event.getRepeatCount();
            int jump = repeat >= 20 ? 5 : repeat >= 6 ? 2 : 1;       // holding the key speeds it up
            float direction = keyCode == KeyEvent.KEYCODE_DPAD_RIGHT ? 1f : -1f;
            Tuning.set(this, it, it.value() + direction * it.step * jump);
            refreshAll();
            return true;
        });
        row.setOnClickListener(v -> {
            Tuning.reset(this, it);
            refreshAll();
        });
        refreshAll();
        return row;
    }

    private void addResetRow() {
        LinearLayout row = rowShell();
        ((LinearLayout.LayoutParams) row.getLayoutParams()).topMargin = gap * 3;
        TextView name = text(h * 0.027f, 0xFFFF8A65);
        name.setText("Put every advanced setting back to its original value");
        row.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.setOnClickListener(v -> new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK)
                .setTitle("Reset advanced settings?")
                .setMessage("Every slider on this screen goes back to its original value.")
                .setPositiveButton("Reset", (d, which) -> {
                    Tuning.resetAll(this);
                    refreshAll();
                })
                .setNegativeButton("Cancel", null)
                .show());
    }

    private void refreshAll() {
        for (Runnable r : refreshers) r.run();
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
