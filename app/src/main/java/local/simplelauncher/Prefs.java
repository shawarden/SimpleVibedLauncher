package local.simplelauncher;

import android.content.Context;
import android.content.SharedPreferences;

/** The launcher's own settings (the House icon screen). Defaults come from {@link Config}. */
final class Prefs {
    private Prefs() {}

    static final String IDLE_MS = "idle_ms";
    static final String BG_MAX = "bg_max";
    static final String SAVER_MAX = "saver_max";
    /** 0 = follow the TV's 12/24 hour setting, 1 = 12 hour, 2 = 24 hour. Home screen and screensaver. */
    static final String CLOCK_FORMAT = "clock_format";
    /** 0 = gravity, 1 = clean bounce. */
    static final String MOTION = "motion";
    /** 0 = low, 1 = medium, 2 = high: gravity strength in gravity mode, movement speed in clean mode. */
    static final String MOTION_LEVEL = "motion_level";
    /** 1 = in gravity mode the background breathes with the clock's height; 0 = colour steps on each bounce. */
    static final String BREATHE = "breathe";
    /** 1 = time and date, 0 = time only. */
    static final String SAVER_DATE = "saver_date";
    private static final String ASKED_LOCATION = "asked_location";

    private static SharedPreferences sp(Context c) {
        return c.getApplicationContext().getSharedPreferences("settings", Context.MODE_PRIVATE);
    }

    /** Idle time before the screensaver starts; 0 means never. */
    static long idleMs(Context c) {
        return Math.max(0L, sp(c).getLong(IDLE_MS, Config.IDLE_MS));
    }

    /** Highest value any background colour channel may reach. Always a multiple of the step. */
    static int bgMax(Context c) {
        return level(sp(c).getLong(BG_MAX, Config.BG_MAX));
    }

    /** Highest value any screensaver colour channel may reach. Always a multiple of the step. */
    static int saverMax(Context c) {
        return level(sp(c).getLong(SAVER_MAX, Config.SAVER_MAX));
    }

    static int clockFormat(Context c) {
        return (int) sp(c).getLong(CLOCK_FORMAT, 0);
    }

    /** True for the gravity effect, false for the clean (constant speed) bounce. */
    static boolean gravityMotion(Context c) {
        return sp(c).getLong(MOTION, 0) == 0;
    }

    static int motionLevel(Context c) {
        return (int) Math.max(0, Math.min(2, sp(c).getLong(MOTION_LEVEL, 0)));
    }

    static boolean breathe(Context c) {
        return sp(c).getLong(BREATHE, 0) != 0;
    }

    static boolean saverShowsDate(Context c) {
        return sp(c).getLong(SAVER_DATE, 1) != 0;
    }

    static long get(Context c, String key, long fallback) {
        return sp(c).getLong(key, fallback);
    }

    static void set(Context c, String key, long value) {
        sp(c).edit().putLong(key, value).apply();
    }

    static boolean has(Context c, String key) {
        return sp(c).contains(key);
    }

    static float getFloat(Context c, String key, float fallback) {
        try {
            return sp(c).getFloat(key, fallback);
        } catch (ClassCastException e) {
            return fallback;
        }
    }

    static void setFloat(Context c, String key, float value) {
        sp(c).edit().putFloat(key, value).apply();
    }

    static void remove(Context c, String key) {
        sp(c).edit().remove(key).apply();
    }

    static String getString(Context c, String key, String fallback) {
        return sp(c).getString(key, fallback);
    }

    static void setString(Context c, String key, String value) {
        sp(c).edit().putString(key, value).apply();
    }

    static boolean askedLocation(Context c) {
        return sp(c).getBoolean(ASKED_LOCATION, false);
    }

    static void setAskedLocation(Context c) {
        sp(c).edit().putBoolean(ASKED_LOCATION, true).apply();
    }

    private static int level(long v) {
        int step = Config.STEP;
        long top = 255L / step * step;
        return (int) Math.min(top, Math.max(step, v / step * step));
    }
}
