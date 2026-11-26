package local.simplelauncher;

import android.content.Context;

import java.util.List;

/** Loads and saves the Advanced settings (see {@link Tunables}); the values live in {@link Config} while running. */
final class Tuning {
    private Tuning() {}

    private static boolean loaded;

    private static String prefKey(Tunables.Item it) { return "t_" + it.key; }

    /** Puts the saved values into Config. Safe to call from anywhere, any number of times; only the first does the work. */
    static synchronized void ensureLoaded(Context c) {
        if (loaded) return;
        loaded = true;
        for (Tunables.Item it : Tunables.ALL) {
            String key = prefKey(it);
            if (Prefs.has(c, key)) it.apply(Prefs.getFloat(c, key, it.def));
        }
        Tunables.enforceAll();
    }

    /** Sets one value (and moves its partner if that would put a lowest/highest pair out of order), and saves. */
    static void set(Context c, Tunables.Item it, float value) {
        ensureLoaded(c);
        it.apply(value);
        Prefs.setFloat(c, prefKey(it), it.value());
        List<Tunables.Item> moved = Tunables.enforce(it);
        for (Tunables.Item other : moved) Prefs.setFloat(c, prefKey(other), other.value());
    }

    /** Back to the original value of one item. */
    static void reset(Context c, Tunables.Item it) {
        ensureLoaded(c);
        it.apply(it.def);
        Prefs.remove(c, prefKey(it));
        for (Tunables.Item other : Tunables.enforce(it)) Prefs.setFloat(c, prefKey(other), other.value());
    }

    static void resetAll(Context c) {
        ensureLoaded(c);
        Tunables.resetAll();
        for (Tunables.Item it : Tunables.ALL) Prefs.remove(c, prefKey(it));
    }
}
