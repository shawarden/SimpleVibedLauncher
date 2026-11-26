package local.simplelauncher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * The numbers in {@link Config} that can be adjusted from the Advanced settings screen: for each one its name,
 * its range, its step and how to show it. Times kept in milliseconds in Config are shown and set in seconds.
 * Values are snapped to the step and kept inside the range, and "lowest / highest" pairs are kept in order.
 * No Android classes in here, so it can be tested on a plain JVM.
 */
final class Tunables {
    private Tunables() {}

    interface Get { float get(); }
    interface Put { void put(float value); }

    static final class Item {
        final String key, group, title, prefix, suffix;
        final float min, max, step;
        final float scale;          // shown value = value * scale (for example 100 to show a fraction as a percent)
        final int decimals;
        final float def;            // the value in Config when the program started
        private final Get get;
        private final Put put;

        Item(String group, String key, String title, float min, float max, float step, float scale, int decimals,
             String prefix, String suffix, Get get, Put put) {
            this.group = group;
            this.key = key;
            this.title = title;
            this.min = min;
            this.max = max;
            this.step = step;
            this.scale = scale;
            this.decimals = decimals;
            this.prefix = prefix;
            this.suffix = suffix;
            this.get = get;
            this.put = put;
            this.def = get.get();
        }

        float value() { return get.get(); }

        /** Sets the value, rounded to the step and kept inside the range. Returns what was actually set. */
        float apply(float v) {
            float s = snap(v);
            put.put(s);
            return s;
        }

        float snap(float v) {
            double c = Math.max(min, Math.min(max, v));
            double n = Math.round((c - min) / step);
            double r = Math.round((min + n * step) * 100000.0) / 100000.0;
            return (float) Math.max(min, Math.min(max, r));
        }

        /** 0 at the lowest value, 1 at the highest. */
        float fraction() { return (value() - min) / (max - min); }

        String format(float v) { return prefix + number(v * scale, decimals) + suffix; }

        String formatValue() { return format(value()); }
    }

    /** A number with at most the given decimals and no trailing zeros: 3, 0.5, 0.035. */
    static String number(double d, int decimals) {
        if (decimals <= 0) return Long.toString(Math.round(d));
        String s = String.format(Locale.US, "%." + decimals + "f", d);
        if (s.indexOf('.') >= 0) {
            int end = s.length();
            while (end > 0 && s.charAt(end - 1) == '0') end--;
            if (end > 0 && s.charAt(end - 1) == '.') end--;
            s = s.substring(0, end);
        }
        return s.equals("-0") ? "0" : s;
    }

    private static final String HOME_BG = "Home background";
    private static final String SAVER = "Screensaver";
    private static final String GOING_IN = "Going into the screensaver";
    private static final String COMING_BACK = "Coming back to the home screen";
    private static final String LAYOUT = "Layout";

    private static final List<Item> ITEMS = new ArrayList<>();

    private static Item add(Item it) {
        ITEMS.add(it);
        return it;
    }

    // ---- Home background
    static final Item STEP = add(new Item(HOME_BG, "step", "Size of each colour change (0 to 255 scale)",
            1, 16, 1, 1, 0, "", "", () -> Config.STEP, v -> Config.STEP = Math.round(v)));
    static final Item BG_STEP_MIN = add(new Item(HOME_BG, "bg_step_min", "Fastest colour change",
            1, 30, 1, 1, 0, "", " s",
            () -> Config.BG_STEP_MIN_MS / 1000f, v -> Config.BG_STEP_MIN_MS = Math.round(v * 1000f)));
    static final Item BG_STEP_MAX = add(new Item(HOME_BG, "bg_step_max", "Slowest colour change",
            1, 60, 1, 1, 0, "", " s",
            () -> Config.BG_STEP_MAX_MS / 1000f, v -> Config.BG_STEP_MAX_MS = Math.round(v * 1000f)));

    // ---- Screensaver
    static final Item SAVER_FADE = add(new Item(SAVER, "saver_fade", "Colour fade after each bounce",
            0, 10, 0.5f, 1, 1, "", " s",
            () -> Config.SAVER_FADE_MS / 1000f, v -> Config.SAVER_FADE_MS = Math.round(v * 1000f)));
    static final Item GRAVITY = add(new Item(SAVER, "gravity", "Gravity at Low (screen heights per second squared)",
            0.01f, 0.5f, 0.01f, 100, 0, "", " %", () -> Config.GRAVITY, v -> Config.GRAVITY = v));
    static final Item GRAVITY_MEDIUM = add(new Item(SAVER, "gravity_medium", "Gravity at Medium, times Low",
            1, 10, 0.5f, 1, 1, "x ", "",
            () -> Config.GRAVITY_SCALE[1], v -> Config.GRAVITY_SCALE[1] = v));
    static final Item GRAVITY_HIGH = add(new Item(SAVER, "gravity_high", "Gravity at High, times Low",
            1, 20, 0.5f, 1, 1, "x ", "",
            () -> Config.GRAVITY_SCALE[2], v -> Config.GRAVITY_SCALE[2] = v));
    static final Item ENERGY_MIN = add(new Item(SAVER, "energy_min", "Bounce energy, lowest (speed at the top of each arc)",
            0.000f, 0.1f, 0.01f, 1, 3, "", "", () -> Config.ENERGY_MIN, v -> Config.ENERGY_MIN = v));
    static final Item ENERGY_MAX = add(new Item(SAVER, "energy_max", "Bounce energy, highest",
            0.000f, 0.2f, 0.01f, 1, 3, "", "", () -> Config.ENERGY_MAX, v -> Config.ENERGY_MAX = v));
    static final Item VX_MIN = add(new Item(SAVER, "vx_min", "Sideways speed, slowest (screen widths per second)",
            0.01f, 0.4f, 0.01f, 100, 0, "", " %", () -> Config.VX_MIN, v -> Config.VX_MIN = v));
    static final Item VX_MAX = add(new Item(SAVER, "vx_max", "Sideways speed, fastest",
            0.01f, 0.6f, 0.01f, 100, 0, "", " %", () -> Config.VX_MAX, v -> Config.VX_MAX = v));
    static final Item VY_MIN = add(new Item(SAVER, "vy_min", "Clean bounce up-down speed, slowest (screen heights per second)",
            0.02f, 0.5f, 0.01f, 100, 0, "", " %", () -> Config.VY_CLEAN_MIN, v -> Config.VY_CLEAN_MIN = v));
    static final Item VY_MAX = add(new Item(SAVER, "vy_max", "Clean bounce up-down speed, fastest",
            0.02f, 0.6f, 0.01f, 100, 0, "", " %", () -> Config.VY_CLEAN_MAX, v -> Config.VY_CLEAN_MAX = v));
    static final Item SPEED_MEDIUM = add(new Item(SAVER, "speed_medium", "Clean bounce speed at Medium, times Low",
            1, 8, 0.5f, 1, 1, "x ", "",
            () -> Config.SPEED_SCALE[1], v -> Config.SPEED_SCALE[1] = v));
    static final Item SPEED_HIGH = add(new Item(SAVER, "speed_high", "Clean bounce speed at High, times Low",
            1, 16, 0.5f, 1, 1, "x ", "",
            () -> Config.SPEED_SCALE[2], v -> Config.SPEED_SCALE[2] = v));

    // ---- Going into the screensaver
    static final Item FALL_FADE = add(new Item(GOING_IN, "fall_fade", "Background colour change while falling",
            0.5f, 15, 0.5f, 1, 1, "", " s", () -> Config.FALL_FADE_S, v -> Config.FALL_FADE_S = v));
    static final Item GROW = add(new Item(GOING_IN, "grow", "Clock growing to screensaver size",
            0.5f, 10, 0.5f, 1, 1, "", " s", () -> Config.GROW_S, v -> Config.GROW_S = v));
    static final Item DRIFT_MIN = add(new Item(GOING_IN, "drift_min", "Sideways drift of falling icons, least (screen widths per second)",
            0f, 0.2f, 0.005f, 100, 1, "", " %", () -> Config.DRIFT_MIN, v -> Config.DRIFT_MIN = v));
    static final Item DRIFT_MAX = add(new Item(GOING_IN, "drift_max", "Sideways drift of falling icons, most",
            0f, 0.3f, 0.005f, 100, 1, "", " %", () -> Config.DRIFT_MAX, v -> Config.DRIFT_MAX = v));
    static final Item RELEASE_DELAY = add(new Item(GOING_IN, "release_delay", "Icons let go over up to",
            0, 3, 0.1f, 1, 1, "", " s", () -> Config.RELEASE_DELAY_MAX_S, v -> Config.RELEASE_DELAY_MAX_S = v));

    // ---- Coming back
    static final Item RETURN_FADE = add(new Item(COMING_BACK, "return_fade", "Background colour change on return",
            0.5f, 10, 0.5f, 1, 1, "", " s", () -> Config.RETURN_S, v -> Config.RETURN_S = v));
    static final Item RETURN_MAX = add(new Item(COMING_BACK, "return_max", "Everything back in place by",
            0.5f, 10, 0.5f, 1, 1, "", " s", () -> Config.RETURN_MAX_S, v -> Config.RETURN_MAX_S = v));
    static final Item RETURN_SHRINK = add(new Item(COMING_BACK, "return_shrink", "Clock shrinking to home size",
            0.1f, 5, 0.1f, 1, 1, "", " s", () -> Config.RETURN_SHRINK_S, v -> Config.RETURN_SHRINK_S = v));

    // ---- Layout
    static final Item FAV_COLUMNS = add(new Item(LAYOUT, "fav_columns", "Favourites per row",
            2, 8, 1, 1, 0, "", "", () -> Config.FAV_COLUMNS, v -> Config.FAV_COLUMNS = Math.round(v)));
    static final Item DRAWER_COLUMNS = add(new Item(LAYOUT, "drawer_columns", "Apps per row in the Apps drawer",
            3, 10, 1, 1, 0, "", "", () -> Config.DRAWER_COLUMNS, v -> Config.DRAWER_COLUMNS = Math.round(v)));

    static final List<Item> ALL = Collections.unmodifiableList(ITEMS);

    /** Pairs of (lowest, highest) that must stay in order. */
    private static final Item[][] PAIRS = {
            { BG_STEP_MIN, BG_STEP_MAX },
            { ENERGY_MIN, ENERGY_MAX },
            { VX_MIN, VX_MAX },
            { VY_MIN, VY_MAX },
            { DRIFT_MIN, DRIFT_MAX },
            { RETURN_FADE, RETURN_MAX },       // the return can't finish before its colour change does
    };

    /**
     * Call after changing an item: if it broke the order of a pair, the other one of the pair is moved to
     * match. Returns the items that were moved (the one passed in is not included).
     */
    static List<Item> enforce(Item changed) {
        List<Item> moved = new ArrayList<>();
        for (Item[] p : PAIRS) {
            if (changed == p[0] && p[0].value() > p[1].value()) {
                p[1].apply(p[0].value());
                moved.add(p[1]);
            } else if (changed == p[1] && p[1].value() < p[0].value()) {
                p[0].apply(p[1].value());
                moved.add(p[0]);
            }
        }
        return moved;
    }

    /** Puts every pair in order (after loading saved values). */
    static void enforceAll() {
        for (Item[] p : PAIRS) {
            if (p[0].value() > p[1].value()) p[1].apply(p[0].value());
        }
    }

    static void resetAll() {
        for (Item it : ITEMS) it.apply(it.def);
    }

    static Item byKey(String key) {
        for (Item it : ITEMS) if (it.key.equals(key)) return it;
        return null;
    }
}
