package local.simplelauncher;

import android.content.Context;

/** The saved button bindings, shared by the mapper screen and the key service (same process). */
final class Mappings {
    private Mappings() {}

    private static final String KEY = "bindings";
    private static Bindings cache;

    static synchronized Bindings get(Context c) {
        if (cache == null) cache = Bindings.parse(Prefs.getString(c, KEY, ""));
        return cache;
    }

    /** Call after changing the bindings returned by {@link #get}. */
    static synchronized void save(Context c) {
        if (cache != null) Prefs.setString(c, KEY, cache.serialize());
    }
}
