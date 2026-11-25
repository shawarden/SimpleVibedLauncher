package local.simplelauncher;

import android.content.Context;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

/** Lays app tiles out in rows inside a vertical LinearLayout. */
final class TileGrid {
    private TileGrid() {}

    interface Callbacks {
        /** OK pressed on a tile. */
        void onOpen(AppEntry app);
        /** OK held on a tile. */
        void onMenu(AppEntry app, TileView tile);
        /** A tile gained focus. */
        void onFocused(int index);
    }

    /**
     * Replaces the contents of column with the tiles and returns them in order.
     * With centre set, a short last row is centred; otherwise it lines up on the left.
     */
    static List<TileView> fill(Context ctx, LinearLayout column, List<AppEntry> apps, int cols,
                               int tileW, int tileH, int gap, boolean banners, boolean centre,
                               Callbacks cb) {
        column.removeAllViews();
        column.setClipChildren(false);   // so a focused (zoomed) tile isn't cut off by its row
        List<TileView> tiles = new ArrayList<>();
        LinearLayout row = null;
        LinearLayout.LayoutParams lastRowParams = null;
        for (int i = 0; i < apps.size(); i++) {
            if (i % cols == 0) {
                row = new LinearLayout(ctx);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setClipChildren(false);
                row.setGravity(centre ? Gravity.CENTER_HORIZONTAL : Gravity.START);
                lastRowParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lastRowParams.bottomMargin = gap;
                column.addView(row, lastRowParams);
            }
            final AppEntry app = apps.get(i);
            final int index = i;
            final TileView tile = new TileView(ctx, app, tileW, tileH, banners);
            tile.setOnClickListener(v -> cb.onOpen(app));
            tile.setOnLongClickListener(v -> {
                cb.onMenu(app, tile);
                return true;
            });
            tile.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) cb.onFocused(index);
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(tileW, tileH);
            boolean lastInRow = (i % cols == cols - 1) || i == apps.size() - 1;
            if (!lastInRow) lp.rightMargin = gap;
            row.addView(tile, lp);
            tiles.add(tile);
        }
        // No space under the final row, so a centred block is truly centred.
        if (lastRowParams != null) lastRowParams.bottomMargin = 0;
        return tiles;
    }
}
