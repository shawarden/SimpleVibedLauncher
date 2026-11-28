package local.simplelauncher;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.LauncherApps;
import android.content.pm.ShortcutInfo;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.provider.Settings;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * The button mapper (opened from the launcher settings): pick a button on the remote by pressing it, give it
 * an optional alias, and choose what it does. The buttons themselves are watched by {@link KeyService}.
 */
public class ButtonMapActivity extends Activity {
    private interface Picked { void on(String spec, String label); }

    private BackgroundDriver background;
    private LinearLayout column;
    private int h, rowH;
    private int gap;
    private Dialog dialog;
    private String focusKeyId;          // the binding to put the focus on after the list is rebuilt

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int w = dm.widthPixels;
        h = dm.heightPixels;
        int margin = (int) (w * 0.05f);
        gap = (int) (h * 0.014f);
        rowH = (int) (h * 0.085f);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0xFF000000);
        background = new BackgroundDriver(root);

        int headerTop = (int) (h * 0.04f);
        int headerH = (int) (h * 0.10f);

        TextView title = text(h * 0.05f, 0xFFEDEDED);
        title.setText("Button mapper");
        title.setGravity(Gravity.CENTER_VERTICAL);
        FrameLayout.LayoutParams titleParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, headerH, Gravity.TOP | Gravity.START);
        titleParams.topMargin = headerTop;
        titleParams.leftMargin = margin;
        root.addView(title, titleParams);

        TextView hint = text(h * 0.026f, 0xFF9A9A9A);
        hint.setText("OK on a button to change it");
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
        scroll.setPadding(0, gap, 0, gap);
        column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setClipChildren(false);
        scroll.addView(column, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        FrameLayout.LayoutParams scrollParams = new FrameLayout.LayoutParams(
                (int) (w * 0.70f), ViewGroup.LayoutParams.MATCH_PARENT, Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        scrollParams.topMargin = headerTop + headerH;
        root.addView(scroll, scrollParams);

        setContentView(root);
    }

    private TextView text(float sizePx, int color) {
        TextView t = new TextView(this);
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, sizePx);
        t.setTextColor(color);
        t.setFocusable(false);
        return t;
    }

    @Override protected void onResume() {
        super.onResume();
        KeyService.setSuspended(true);      // while this screen is open, buttons behave normally
        background.start();
        rebuild();
    }

    @Override protected void onPause() {
        KeyService.setSuspended(false);
        KeyService.setRecorder(null);
        background.stop();
        if (dialog != null && dialog.isShowing()) dialog.dismiss();
        super.onPause();
    }

    // ---------------------------------------------------------------- the list

    private void rebuild() {
        column.removeAllViews();
        View focus = null;

        boolean on = KeyService.isRunning();
        addRow(on ? "Button mapper service" : "Button mapper service  (needed for the buttons to work)", null,
                on ? "On" : "Off  -  OK to turn on", on ? 0xFF8BC34A : 0xFFFF8A65, v -> onServiceRow());

        View add = addRow("+  Add a button", null, "Press a button on the remote", 0xFF9A9A9A, v -> startRecording());
        focus = add;

        for (Bindings.Binding b : Mappings.get(this).all()) {
            final Bindings.Binding binding = b;
            String jobs = binding.summary();
            String sub = binding.alias.isEmpty() ? jobs : "Button: " + binding.keyName + "     " + jobs;
            View row = addRow(binding.title(), sub, "", 0xFFFFD54F, v -> editMenu(binding));
            if (binding.keyId.equals(focusKeyId)) focus = row;
        }
        focusKeyId = null;

        addRow("TV IP control key  (for channel buttons)", null,
                TvControl.hasKey(this) ? "Set" : "Not set", TvControl.hasKey(this) ? 0xFF8BC34A : 0xFF9A9A9A,
                v -> editTvKey());

        focus.requestFocus();
    }

    /** One row: a name (and optionally a smaller line under it) on the left, a value on the right. */
    private View addRow(String name, String sub, String value, int valueColor, View.OnClickListener click) {
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
        row.setOnClickListener(click);

        LinearLayout names = new LinearLayout(this);
        names.setOrientation(LinearLayout.VERTICAL);
        TextView nameView = text(h * 0.030f, 0xFFEDEDED);
        nameView.setText(name);
        nameView.setSingleLine(true);
        names.addView(nameView);
        if (sub != null) {
            TextView subView = text(h * 0.021f, 0xFF9A9A9A);
            subView.setText(sub);
            subView.setSingleLine(true);
            names.addView(subView);
        }
        row.addView(names, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView valueView = text(h * 0.028f, valueColor);
        valueView.setText(value);
        valueView.setSingleLine(true);
        valueView.setGravity(Gravity.END);
        row.addView(valueView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, rowH);
        lp.bottomMargin = gap;
        column.addView(row, lp);
        return row;
    }

    // ---------------------------------------------------------------- the service

    private void onServiceRow() {
        if (KeyService.isRunning()) {
            Toast.makeText(this, "The button mapper service is on", Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(this, "Find \"SimpleVibedLauncher buttons\" in the list and switch it on",
                Toast.LENGTH_LONG).show();
        try {
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        } catch (ActivityNotFoundException | SecurityException e) {
            Actions.settings(this);
        }
    }

    // ---------------------------------------------------------------- recording a button

    private void startRecording() {
        final boolean serviceOn = KeyService.isRunning();
        final AlertDialog d = new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK)
                .setTitle("Press a button on the remote")
                .setMessage(serviceOn
                        ? "Press the button you want to change.\n\nBack, Home, the arrows and OK can't be changed."
                        : "Press the button you want to change.\n\nThe button mapper service is off, so buttons that "
                        + "Android handles itself (for example the app buttons) won't be seen. Turn the service on "
                        + "first to record any button.")
                .setNegativeButton("Cancel", null)
                .create();
        final boolean[] done = { false };
        final KeyService.Recorder recorder = (keyCode, scanCode) -> {
            if (done[0]) return;
            done[0] = true;
            d.dismiss();
            onRecorded(keyCode, scanCode);
        };
        d.setOnKeyListener((di, keyCode, event) -> {
            // Buttons the service can't see arrive here. The core buttons keep working (OK presses Cancel).
            if (Bindings.isCore(keyCode)) return false;
            if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
                recorder.onKey(keyCode, event.getScanCode());
            }
            return true;
        });
        d.setOnDismissListener(di -> KeyService.setRecorder(null));
        if (serviceOn) KeyService.setRecorder(recorder);
        dialog = d;
        d.show();
    }

    private void onRecorded(int keyCode, int scanCode) {
        Bindings.Binding existing = Mappings.get(this).find(Bindings.keyId(keyCode, scanCode));
        if (existing != null) {
            Toast.makeText(this, "That button is already set; changing it", Toast.LENGTH_SHORT).show();
            editMenu(existing);
            return;
        }
        final Bindings.Binding fresh = new Bindings.Binding(keyCode, scanCode, keyName(keyCode, scanCode), "");
        // The first job is the single press; the menu then opens so long and double presses can be added.
        pickAction("Set \"" + fresh.keyName + "\" (single press) to", (spec, label) -> {
            commit(fresh, Bindings.SINGLE, spec, label);
            editMenu(fresh);
        });
    }

    private static String keyName(int keyCode, int scanCode) {
        if (keyCode == 0) return "Button (scan code " + scanCode + ")";
        String n = KeyEvent.keyCodeToString(keyCode);
        if (n.startsWith("KEYCODE_")) n = n.substring(8);
        if (n.isEmpty() || Character.isDigit(n.charAt(0))) return "Button " + keyCode;
        return n.replace('_', ' ');
    }

    private void commit(Bindings.Binding b, int press, String spec, String label) {
        b.set(press, spec, label);
        Mappings.get(this).put(b);
        Mappings.save(this);
        focusKeyId = b.keyId;
        rebuild();
        if (ActionCatalog.kind(spec).equals("tv") && !TvControl.hasKey(this)) {
            Toast.makeText(this, "This one needs the TV's IP control key: set it at the bottom of this list",
                    Toast.LENGTH_LONG).show();
        }
    }

    // ---------------------------------------------------------------- editing a button

    private static final String[] PRESS_TITLES = { "Single press", "Long press", "Double press" };

    private void editMenu(final Bindings.Binding b) {
        String[] items = new String[5];
        for (int p = 0; p < 3; p++) {
            items[p] = PRESS_TITLES[p] + ":   " + (b.has(p) ? b.actionLabel[p] : "not set");
        }
        items[3] = b.alias.isEmpty() ? "Add an alias" : "Change or clear the alias";
        items[4] = "Remove this button";
        dialog = Menus.show(this, b.title(), items, which -> {
            if (which < 3) {
                pressMenu(b, which);
            } else if (which == 3) {
                editAlias(b);
            } else {
                Mappings.get(this).remove(b.keyId);
                Mappings.save(this);
                rebuild();
            }
        });
    }

    /** One of a button's three presses: choose what it does, or (if it already does something) change, test or clear it. */
    private void pressMenu(final Bindings.Binding b, final int press) {
        final String title = b.title() + "  >  " + PRESS_TITLES[press].toLowerCase(java.util.Locale.ROOT);
        if (!b.has(press)) {
            pickAction("Set \"" + title + "\" to", (spec, label) -> {
                commit(b, press, spec, label);
                editMenu(b);
            });
            return;
        }
        String[] items = { "Change what it does", "Test it now", "Clear this press", "Back" };
        dialog = Menus.show(this, title + ":  " + b.actionLabel[press], items, which -> {
            if (which == 0) {
                pickAction("Set \"" + title + "\" to", (spec, label) -> {
                    commit(b, press, spec, label);
                    editMenu(b);
                });
            } else if (which == 1) {
                ActionRunner.run(this, b.action[press]);
            } else if (which == 2) {
                b.clear(press);
                if (b.isEmpty()) {
                    Mappings.get(this).remove(b.keyId);     // nothing left: the button goes back to normal
                } else {
                    Mappings.get(this).put(b);
                }
                Mappings.save(this);
                focusKeyId = b.keyId;
                rebuild();
                if (!b.isEmpty()) editMenu(b);
            } else {
                editMenu(b);
            }
        });
    }

    private void editAlias(final Bindings.Binding b) {
        final EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setText(b.alias);
        input.setSelection(b.alias.length());
        input.setHint("For example: Netflix button");
        LinearLayout holder = new LinearLayout(this);
        holder.setPadding(40, 20, 40, 0);
        holder.addView(input, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        AlertDialog d = new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK)
                .setTitle("Alias for " + b.keyName)
                .setView(holder)
                .setPositiveButton("Save", (di, w) -> {
                    String a = input.getText().toString().trim();
                    b.alias = a.length() > 40 ? a.substring(0, 40) : a;
                    Mappings.save(this);
                    focusKeyId = b.keyId;
                    rebuild();
                })
                .setNeutralButton("Clear", (di, w) -> {
                    b.alias = "";
                    Mappings.save(this);
                    focusKeyId = b.keyId;
                    rebuild();
                })
                .setNegativeButton("Cancel", null)
                .create();
        d.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        dialog = d;
        d.show();
    }

    private void editTvKey() {
        final EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        input.setText(Prefs.getString(this, TvControl.PSK, ""));
        input.setHint("Pre-shared key");
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        TextView note = text(h * 0.024f, 0xFFB0B0B0);
        note.setText("Channel up/down and some other TV buttons are sent to the TV itself over the network. "
                + "On the TV, switch on IP control (Settings > Network & Internet > Home network > IP control), "
                + "set Authentication to \"Normal and Pre-Shared Key\", set a Pre-Shared Key, and type the same key here.");
        box.addView(note);
        box.addView(input);
        box.setPadding(40, 10, 40, 0);
        AlertDialog d = new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK)
                .setTitle("TV IP control key")
                .setView(box)
                .setPositiveButton("Save", (di, w) -> {
                    Prefs.setString(this, TvControl.PSK, input.getText().toString().trim());
                    rebuild();
                })
                .setNeutralButton("Clear", (di, w) -> {
                    Prefs.setString(this, TvControl.PSK, "");
                    rebuild();
                })
                .setNegativeButton("Cancel", null)
                .create();
        d.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        dialog = d;
        d.show();
    }

    // ---------------------------------------------------------------- choosing what a button does

    private void pickAction(final String title, final Picked done) {
        final ActionCatalog.Group[] groups = ActionCatalog.GROUPS;
        final int n = groups.length;
        String[] cats = new String[n + 3];
        for (int i = 0; i < n; i++) cats[i] = groups[i].title;
        cats[n] = "Open an app";
        cats[n + 1] = "Open an app shortcut";
        cats[n + 2] = "Nothing (block the button)";
        dialog = Menus.show(this, title, cats, which -> {
            if (which < n) {
                pickFromGroup(groups[which], done);
            } else if (which == n) {
                pickApp(done);
            } else if (which == n + 1) {
                pickShortcut(done);
            } else {
                done.on(ActionCatalog.NONE, ActionCatalog.NONE_LABEL);
            }
        });
    }

    private void pickFromGroup(final ActionCatalog.Group g, final Picked done) {
        String[] labels = new String[g.items.length];
        for (int i = 0; i < labels.length; i++) labels[i] = g.items[i].label;
        dialog = Menus.show(this, g.title, labels, which -> done.on(g.items[which].spec, g.items[which].label));
    }

    private void pickApp(final Picked done) {
        AppRepo.IO.execute(() -> {
            final List<AppEntry> apps = AppRepo.load(this, false);
            runOnUiThread(() -> {
                if (isDestroyed()) return;
                String[] labels = new String[apps.size()];
                for (int i = 0; i < labels.length; i++) labels[i] = apps.get(i).label;
                dialog = Menus.show(this, "Open which app?", labels, which ->
                        done.on(ActionCatalog.APP + apps.get(which).packageName, apps.get(which).label));
            });
        });
    }

    /** Looks through every app for shortcuts it has registered (only possible while this is the default home app). */
    private void pickShortcut(final Picked done) {
        if (Build.VERSION.SDK_INT < 25) {
            Toast.makeText(this, "App shortcuts need Android 7.1 or newer", Toast.LENGTH_LONG).show();
            return;
        }
        AppRepo.IO.execute(() -> {
            final List<String> labels = new ArrayList<>();
            final List<String> specs = new ArrayList<>();
            boolean host = false;
            try {
                LauncherApps la = (LauncherApps) getSystemService(Context.LAUNCHER_APPS_SERVICE);
                host = la.hasShortcutHostPermission();
                if (host) {
                    for (AppEntry app : AppRepo.load(this, false)) {
                        LauncherApps.ShortcutQuery q = new LauncherApps.ShortcutQuery()
                                .setPackage(app.packageName)
                                .setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC
                                        | LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST
                                        | LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED);
                        List<ShortcutInfo> found = la.getShortcuts(q, Process.myUserHandle());
                        if (found == null) continue;
                        for (ShortcutInfo si : found) {
                            if (!si.isEnabled()) continue;
                            CharSequence name = si.getShortLabel();
                            if (name == null) name = si.getId();
                            labels.add(app.label + "  -  " + name);
                            specs.add(ActionCatalog.SHORTCUT + app.packageName + "|" + si.getId());
                        }
                    }
                }
            } catch (SecurityException | IllegalStateException e) {
                host = false;
            }
            final boolean isHost = host;
            runOnUiThread(() -> {
                if (isDestroyed()) return;
                if (!isHost) {
                    dialog = new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK)
                            .setTitle("App shortcuts")
                            .setMessage("Android only lets the default home app list or start app shortcuts. "
                                    + "Make SimpleVibedLauncher the home app first (see the README).")
                            .setPositiveButton("OK", null).show();
                } else if (labels.isEmpty()) {
                    dialog = new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK)
                            .setTitle("App shortcuts")
                            .setMessage("None of the installed apps has registered any shortcuts. "
                                    + "(Most TV apps don't.) \"Open an app\" is the way to start an app.")
                            .setPositiveButton("OK", null).show();
                } else {
                    final String[] names = labels.toArray(new String[0]);
                    dialog = Menus.show(this, "Open which shortcut?", names, which ->
                            done.on(specs.get(which), names[which]));
                }
            });
        });
    }
}
