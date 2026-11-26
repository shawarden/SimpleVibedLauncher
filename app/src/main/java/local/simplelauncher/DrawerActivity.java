package local.simplelauncher;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** The apps drawer: every installed app, with a long OK press for the options menu. */
public class DrawerActivity extends Activity implements TileGrid.Callbacks {
    private static final String ADD = "Add to Favourites";
    private static final String REMOVE = "Remove from Favourites";
    private static final String FORCE_STOP = "Force Stop";
    private static final String APP_INFO = "App Info";
    private static final String UNINSTALL = "Uninstall";

    private final Handler handler = new Handler(Looper.getMainLooper());

    private Favourites favourites;
    private BackgroundDriver background;
    private LinearLayout column;
    private int tileW, tileH, gap;

    private List<AppEntry> apps = new ArrayList<>();
    private List<TileView> tiles = new ArrayList<>();
    private int focusIndex = 0;
    private Dialog dialog;
    private boolean resumed;
    private int generation;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Tuning.ensureLoaded(this);
        favourites = new Favourites(this);

        DisplayMetrics dm = getResources().getDisplayMetrics();
        int w = dm.widthPixels;
        int h = dm.heightPixels;
        int margin = (int) (w * 0.05f);
        gap = (int) (w * 0.0125f);
        tileW = (w - 2 * margin - (Config.DRAWER_COLUMNS - 1) * gap) / Config.DRAWER_COLUMNS;
        tileH = (int) (tileW * 0.85f);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0xFF000000);
        background = new BackgroundDriver(root);

        int headerTop = (int) (h * 0.04f);
        int headerH = (int) (h * 0.10f);

        TextView title = text(h * 0.05f, 0xFFEDEDED);
        title.setText("Apps");
        FrameLayout.LayoutParams titleParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, headerH, Gravity.TOP | Gravity.START);
        titleParams.topMargin = headerTop;
        titleParams.leftMargin = margin;
        title.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(title, titleParams);

        TextView hint = text(h * 0.026f, 0xFF9A9A9A);
        hint.setText("Hold OK on an app for options");
        FrameLayout.LayoutParams hintParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, headerH, Gravity.TOP | Gravity.END);
        hintParams.topMargin = headerTop;
        hintParams.rightMargin = margin;
        hint.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(hint, hintParams);

        ScrollView scroll = new ScrollView(this);
        scroll.setFocusable(false);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroll.setClipToPadding(false);
        scroll.setClipChildren(false);   // lets the focused tile's zoom and ring show at the edges
        scroll.setPadding(margin, gap, margin, gap);
        column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(column, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        FrameLayout.LayoutParams scrollParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
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

    // ---------------------------------------------------------------- lifecycle

    @Override protected void onResume() {
        super.onResume();
        resumed = true;
        background.start();
        reload();       // also picks up an app that was just uninstalled
        armIdle();
    }

    @Override protected void onPause() {
        resumed = false;
        handler.removeCallbacksAndMessages(null);
        background.stop();
        if (dialog != null && dialog.isShowing()) dialog.dismiss();
        super.onPause();
    }

    // After the idle time, go back to the home screen, which then starts the screensaver.
    private final Runnable idle = () -> {
        if (!resumed) return;
        if (dialog != null && dialog.isShowing()) { armIdle(); return; }
        finish();
    };

    private void armIdle() {
        handler.removeCallbacks(idle);
        long ms = Prefs.idleMs(this);
        if (ms > 0) handler.postDelayed(idle, ms);   // 0 = screensaver switched off
    }

    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        armIdle();
        return super.dispatchKeyEvent(event);
    }

    // ---------------------------------------------------------------- app list

    private void reload() {
        final int gen = ++generation;
        AppRepo.IO.execute(() -> {
            List<AppEntry> list = AppRepo.load(this, true);   // icons are loaded here, off the UI thread
            runOnUiThread(() -> {
                if (gen == generation && !isDestroyed()) show(list);
            });
        });
    }

    private void show(List<AppEntry> list) {
        View focused = getCurrentFocus();
        boolean restoreFocus = focused == null || focused instanceof TileView;

        apps = list;
        tiles = TileGrid.fill(this, column, apps, Config.DRAWER_COLUMNS, tileW, tileH, gap, false, false, this);
        markFavourites();

        if (restoreFocus && !tiles.isEmpty()) {
            focusIndex = Math.max(0, Math.min(focusIndex, tiles.size() - 1));
            tiles.get(focusIndex).requestFocus();
        }
    }

    private void markFavourites() {
        Set<String> favs = new HashSet<>(favourites.list());
        for (int i = 0; i < tiles.size(); i++) {
            tiles.get(i).setFavourite(favs.contains(apps.get(i).packageName));
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
        final List<String> items = new ArrayList<>();
        items.add(favourites.has(app.packageName) ? REMOVE : ADD);
        items.add(FORCE_STOP);
        items.add(APP_INFO);
        if (!app.system) items.add(UNINSTALL);   // firmware apps can't be uninstalled

        dialog = Menus.show(this, app.label, items.toArray(new String[0]), which -> {
            String choice = items.get(which);
            if (ADD.equals(choice)) {
                favourites.add(app.packageName);
                tile.setFavourite(true);
            } else if (REMOVE.equals(choice)) {
                favourites.remove(app.packageName);
                tile.setFavourite(false);
            } else if (FORCE_STOP.equals(choice)) {
                Actions.forceStop(this, app);
            } else if (APP_INFO.equals(choice)) {
                Actions.appInfo(this, app.packageName);
            } else if (UNINSTALL.equals(choice)) {
                Actions.uninstall(this, app.packageName);
            }
        });
    }
}
