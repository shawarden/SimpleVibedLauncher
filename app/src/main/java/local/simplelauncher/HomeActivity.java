package local.simplelauncher;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The home screen: the clock on the left; Apps, Inputs, launcher Settings, Wi-Fi and TV Settings icons on
 * the right; favourite apps below. Also a slowly drifting dim background and an idle screensaver.
 */
public class HomeActivity extends Activity implements TileGrid.Callbacks {
    private static final int REQUEST_LOCATION = 1;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private Favourites favourites;
    private FrameLayout root;
    private BackgroundDriver background;
    private ScreensaverView saver;
    private TextView timeView, dayView, emptyView;
    private GlyphView appsButton;
    private LinearLayout clock;
    private final List<View> iconViews = new ArrayList<>();
    private ScrollView scroll;
    private LinearLayout column;

    private int tileW, tileH, gap;
    private int colsAtCreate;      // the Favourites per row setting this screen was laid out with
    private List<TileView> tiles = new ArrayList<>();
    private int focusIndex = 0;
    private Dialog dialog;
    private boolean resumed;
    private boolean pendingWifiDialog;
    /** True until the first favourite has been selected, on launch and each time Home is pressed. */
    private boolean focusFirst = true;
    private int generation;
    private String shownTime = "", shownDay = "";
    /** Something a button asked this screen to do once it is in front: "inputs", "wifi" or "saver". */
    private String pendingCommand;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Tuning.ensureLoaded(this);
        colsAtCreate = Config.FAV_COLUMNS;
        favourites = new Favourites(this);
        if (savedInstanceState == null) pendingCommand = getIntent().getStringExtra(ActionRunner.COMMAND);

        DisplayMetrics dm = getResources().getDisplayMetrics();
        int w = dm.widthPixels;
        int h = dm.heightPixels;
        int margin = (int) (w * 0.05f);
        gap = (int) (w * 0.017f);
        // Four tiles per row, as wide as the screen allows, in the 16:9 shape of TV banners.
        tileW = (w - 2 * margin - (Config.FAV_COLUMNS - 1) * gap) / Config.FAV_COLUMNS;
        tileH = tileW * 9 / 16;

        root = new FrameLayout(this);
        root.setBackgroundColor(0xFF000000);
        background = new BackgroundDriver(root);

        // ---- Top bar: clock in the far left corner, icons on the right.
        int barTop = (int) (h * 0.035f);
        int barH = (int) (h * 0.12f);
        int glyph = (int) (h * 0.10f);
        int iconGap = (int) (w * 0.012f);

        FrameLayout bar = new FrameLayout(this);
        FrameLayout.LayoutParams barParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, barH, Gravity.TOP);
        barParams.topMargin = barTop;
        barParams.leftMargin = margin;
        barParams.rightMargin = margin;
        root.addView(bar, barParams);

        // The clock is display only: not focusable and not clickable, so selecting it does nothing.
        clock = new LinearLayout(this);
        clock.setOrientation(LinearLayout.VERTICAL);
        timeView = label(h * 0.055f, 0xFFEDEDED, Gravity.START);
        dayView = label(h * 0.026f, 0xFFA8A8A8, Gravity.START);
        // Light type with no extra font padding: the screensaver draws the clock itself and has to match.
        Typeface light = Typeface.create("sans-serif-light", Typeface.NORMAL);
        for (TextView t : new TextView[] { timeView, dayView }) {
            t.setTypeface(light);
            t.setIncludeFontPadding(false);
        }
        clock.addView(timeView);
        clock.addView(dayView);
        bar.addView(clock, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.START | Gravity.CENTER_VERTICAL));

        LinearLayout icons = new LinearLayout(this);
        icons.setOrientation(LinearLayout.HORIZONTAL);
        icons.setGravity(Gravity.CENTER_VERTICAL);
        appsButton = icon(icons, GlyphView.Kind.APPS, "Apps", glyph, iconGap,
                v -> startActivity(new Intent(this, DrawerActivity.class)));
        icon(icons, GlyphView.Kind.INPUTS, "Inputs", glyph, iconGap, v -> showInputs());
        icon(icons, GlyphView.Kind.HOME, "Launcher settings", glyph, iconGap,
                v -> startActivity(new Intent(this, SettingsActivity.class)));
        icon(icons, GlyphView.Kind.WIFI, "Network", glyph, iconGap, v -> onWifiClicked());
        icon(icons, GlyphView.Kind.COG, "TV settings", glyph, iconGap, v -> Actions.settings(this));
        bar.addView(icons, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT,
                Gravity.END | Gravity.CENTER_VERTICAL));

        // ---- Favourites fill the rest of the screen: rows centred, and the whole block centred
        // vertically when it is shorter than the space (it scrolls when it is longer).
        scroll = new ScrollView(this);
        scroll.setFocusable(false);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroll.setClipToPadding(false);
        scroll.setClipChildren(false);   // lets the focused tile's zoom and ring show at the edges
        scroll.setPadding(margin, gap, margin, gap);
        column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setGravity(Gravity.CENTER_VERTICAL);
        scroll.addView(column, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        FrameLayout.LayoutParams scrollParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        scrollParams.topMargin = barTop + barH;
        root.addView(scroll, scrollParams);

        emptyView = label(h * 0.03f, 0xFFB0B0B0, Gravity.CENTER_HORIZONTAL);
        emptyView.setText("No favourites yet\nOpen Apps, hold OK on an app and choose Add to Favourites");
        emptyView.setVisibility(View.GONE);
        root.addView(emptyView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));

        // ---- Screensaver, on top of everything and hidden until the idle timer fires.
        saver = new ScreensaverView(this);
        saver.setListener(this::hideSaver);
        saver.setVisibility(View.GONE);
        root.addView(saver, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        setContentView(root);
    }

    private TextView label(float sizePx, int color, int gravity) {
        TextView t = new TextView(this);
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, sizePx);
        t.setTextColor(color);
        t.setGravity(gravity);
        t.setFocusable(false);
        return t;
    }

    private GlyphView icon(LinearLayout parent, GlyphView.Kind kind, String description, int size, int gap,
                           View.OnClickListener onClick) {
        GlyphView g = new GlyphView(this, kind);
        g.setContentDescription(description);
        g.setOnClickListener(onClick);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
        if (parent.getChildCount() > 0) lp.leftMargin = gap;
        parent.addView(g, lp);
        iconViews.add(g);
        return g;
    }

    // ---------------------------------------------------------------- lifecycle

    @Override protected void onResume() {
        super.onResume();
        if (Config.FAV_COLUMNS != colsAtCreate) {      // changed in Advanced settings: lay out again
            recreate();
            return;
        }
        resumed = true;
        background.start();
        updateClock();
        reload();
        armIdle();
        schedulePendingCommand();
        if (pendingWifiDialog) {   // the Location permission question has just been answered
            pendingWifiDialog = false;
            showWifi();
        }
    }

    @Override protected void onPause() {
        resumed = false;
        handler.removeCallbacksAndMessages(null);
        background.stop();
        saver.stop();
        saver.setVisibility(View.GONE);
        if (dialog != null && dialog.isShowing()) dialog.dismiss();
        super.onPause();
    }

    /** The Home button was pressed while this screen was already (or still) the current one. */
    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (dialog != null && dialog.isShowing()) dialog.dismiss();
        if (saver.getVisibility() == View.VISIBLE) hideSaver();
        scroll.scrollTo(0, 0);
        focusIndex = 0;
        focusFirst = true;
        if (!tiles.isEmpty()) tiles.get(0).requestFocus(); else appsButton.requestFocus();
        pendingCommand = intent.getStringExtra(ActionRunner.COMMAND);
        if (resumed) schedulePendingCommand();       // otherwise onResume does it
    }

    /** Runs what a remote button asked for, a moment after the screen is in front so it is laid out. */
    private void schedulePendingCommand() {
        final String command = pendingCommand;
        pendingCommand = null;
        if (command == null) return;
        handler.postDelayed(() -> {
            if (!resumed) return;
            switch (command) {
                case "inputs": showInputs(); break;
                case "wifi":   onWifiClicked(); break;
                case "saver":  showSaver(); break;
                default:       break;
            }
        }, 350);
    }

    /** Back does nothing on a home screen. */
    @Override public void onBackPressed() {
    }

    // ---------------------------------------------------------------- clock

    private void updateClock() {
        long now = System.currentTimeMillis();
        String t = ClockText.time(this, now);
        String d = ClockText.day(now);
        if (!t.equals(shownTime)) { shownTime = t; timeView.setText(t); }
        if (!d.equals(shownDay)) { shownDay = d; dayView.setText(d); }
        handler.removeCallbacks(clockTick);
        handler.postDelayed(clockTick, 60000L - now % 60000L + 50L);
    }

    private final Runnable clockTick = this::updateClock;

    // ---------------------------------------------------------------- screensaver

    private final Runnable idle = () -> {
        if (!resumed) return;
        if (dialog != null && dialog.isShowing()) { armIdle(); return; }
        showSaver();
    };

    private void armIdle() {
        handler.removeCallbacks(idle);
        long ms = Prefs.idleMs(this);
        if (ms > 0) handler.postDelayed(idle, ms);   // 0 = screensaver switched off
    }

    /**
     * Start the screensaver from the home screen: take a picture of every icon and tile, put the overlay
     * over the screen showing exactly the same thing, and let it release them. The real views stay where
     * they are underneath (hiding the focused one would lose the focus), so the return lands back on them.
     */
    private void showSaver() {
        if (saver.getVisibility() == View.VISIBLE) return;
        if (dialog != null && dialog.isShowing()) dialog.dismiss();
        int homeColor = BackgroundDriver.currentColor();
        background.stop();
        root.setBackgroundColor(homeColor);           // exactly what the overlay starts from and returns to

        List<Bitmap> pictures = new ArrayList<>();
        List<float[]> where = new ArrayList<>();      // x, y, scale
        for (View v : iconViews) addPiece(pictures, where, v, 0f);
        float scrollTop = scroll.getTop();
        float scrollBottom = scroll.getBottom();
        for (TileView t : tiles) {
            float[] p = positionInRoot(t);
            if (p[1] + t.getHeight() <= scrollTop || p[1] >= scrollBottom) continue;   // scrolled out of view
            addPiece(pictures, where, t, tileH * 0.08f);
        }
        if (emptyView.getVisibility() == View.VISIBLE) addPiece(pictures, where, emptyView, 0f);

        int n = pictures.size();
        float[] xs = new float[n], ys = new float[n], scales = new float[n];
        for (int i = 0; i < n; i++) {
            xs[i] = where.get(i)[0];
            ys[i] = where.get(i)[1];
            scales[i] = where.get(i)[2];
        }
        float[] clockPos = positionInRoot(clock);
        saver.setVisibility(View.VISIBLE);
        saver.startFromHome(pictures.toArray(new Bitmap[0]), xs, ys, scales, homeColor,
                clockPos[0], clockPos[1], timeView.getTextSize(), dayView.getTextSize());
    }

    /** Adds a picture of the view and where it sits; ignores views that are hidden or have no size. */
    private void addPiece(List<Bitmap> pictures, List<float[]> where, View v, float cornerRadius) {
        int w = v.getWidth(), h = v.getHeight();
        if (v.getVisibility() != View.VISIBLE || w <= 0 || h <= 0) return;
        Bitmap b;
        try {
            b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        } catch (OutOfMemoryError e) {
            return;                                    // that piece simply stays out of the show
        }
        Canvas c = new Canvas(b);
        if (cornerRadius > 0f) {
            Path clip = new Path();
            clip.addRoundRect(new RectF(0, 0, w, h), cornerRadius, cornerRadius, Path.Direction.CW);
            c.clipPath(clip);
        }
        v.draw(c);                                     // draws the view itself, without its scale
        float[] p = positionInRoot(v);
        pictures.add(b);
        where.add(new float[] { p[0], p[1], v.getScaleX() });
    }

    /**
     * Top-left of a view in the root's coordinates, from the layout positions, translations and scrolling of
     * the views above it. (getLocationInWindow would include the focus zoom, which is drawn separately.)
     */
    private float[] positionInRoot(View v) {
        float x = 0f, y = 0f;
        View cur = v;
        while (cur != root) {
            x += cur.getLeft() + cur.getTranslationX();
            y += cur.getTop() + cur.getTranslationY();
            ViewParent parent = cur.getParent();
            if (!(parent instanceof View)) break;
            View pv = (View) parent;
            x -= pv.getScrollX();
            y -= pv.getScrollY();
            cur = pv;
        }
        return new float[] { x, y };
    }

    private void hideSaver() {
        saver.stop();
        saver.setVisibility(View.GONE);
        background.start();
        armIdle();
    }

    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        if (saver.getVisibility() == View.VISIBLE) {
            // The first key press only wakes the screen (everything springs back); it isn't passed on to the
            // buttons underneath, and neither is anything pressed while they are coming back.
            if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) saver.wake();
            return true;
        }
        armIdle();
        return super.dispatchKeyEvent(event);
    }

    // ---------------------------------------------------------------- inputs

    private void showInputs() {
        final List<Inputs.Input> list = Inputs.list(this);
        if (list.isEmpty()) {
            Toast.makeText(this, "No inputs found on this TV", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] names = new String[list.size()];
        for (int i = 0; i < names.length; i++) {
            Inputs.Input in = list.get(i);
            names[i] = in.connected ? in.label + "   (device connected)" : in.label;
        }
        dialog = Menus.show(this, "Inputs", names, which -> {
            Inputs.Input in = list.get(which);
            if (!Inputs.switchTo(this, in.id)) {
                Toast.makeText(this, "Couldn't switch to " + in.label, Toast.LENGTH_LONG).show();
            }
        });
    }

    // ---------------------------------------------------------------- network

    /** First press: show the network details. Second press (the dialog's button has focus): open the settings. */
    private void onWifiClicked() {
        // Android only reveals the Wi-Fi network name to apps that hold the Location permission. Ask once.
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && !Prefs.askedLocation(this)) {
            Prefs.setAskedLocation(this);
            requestPermissions(new String[] { Manifest.permission.ACCESS_FINE_LOCATION }, REQUEST_LOCATION);
            return;
        }
        showWifi();
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION) pendingWifiDialog = true;   // shown from onResume
    }

    private void showWifi() {
        dialog = Menus.info(this, "Network", NetInfo.describe(this), "Network settings",
                () -> Actions.networkSettings(this));
    }

    // ---------------------------------------------------------------- favourites

    private void reload() {
        final int gen = ++generation;
        AppRepo.IO.execute(() -> {
            Map<String, AppEntry> byPackage = new HashMap<>();
            for (AppEntry a : AppRepo.load(this, false)) byPackage.put(a.packageName, a);
            List<AppEntry> favs = new ArrayList<>();
            for (String pkg : favourites.list()) {
                AppEntry a = byPackage.get(pkg);
                if (a == null) continue;          // uninstalled since it was added
                a.icon();                          // load now, off the UI thread
                favs.add(a);
            }
            runOnUiThread(() -> {
                if (gen == generation && !isDestroyed()) showFavourites(favs);
            });
        });
    }

    private void showFavourites(List<AppEntry> favs) {
        View focused = getCurrentFocus();
        boolean restoreFocus = focused == null || focused instanceof TileView;

        tiles = TileGrid.fill(this, column, favs, Config.FAV_COLUMNS, tileW, tileH, gap, true, true, this);
        emptyView.setVisibility(favs.isEmpty() ? View.VISIBLE : View.GONE);

        if (focusFirst && !tiles.isEmpty()) {
            // Android gives the first icon the focus when the window opens, so move it to the first favourite.
            focusFirst = false;
            focusIndex = 0;
            tiles.get(0).requestFocus();
        } else if (restoreFocus) {
            if (tiles.isEmpty()) {
                appsButton.requestFocus();
            } else {
                focusIndex = Math.max(0, Math.min(focusIndex, tiles.size() - 1));
                tiles.get(focusIndex).requestFocus();
            }
        }
    }

    // ---------------------------------------------------------------- TileGrid.Callbacks

    @Override public void onOpen(AppEntry app) {
        Actions.launch(this, app);
    }

    @Override public void onFocused(int index) {
        focusIndex = index;
    }

    @Override public void onMenu(AppEntry app, TileView tile) {
        String[] items = { "Remove from Favourites", "Force Stop", "App Info" };
        dialog = Menus.show(this, app.label, items, which -> {
            if (which == 0) {
                favourites.remove(app.packageName);
                reload();
            } else if (which == 1) {
                Actions.forceStop(this, app);
            } else {
                Actions.appInfo(this, app.packageName);
            }
        });
    }
}
