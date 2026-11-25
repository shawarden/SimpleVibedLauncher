package local.simplelauncher;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

/** The favourite apps, kept as an ordered list of package names. */
final class Favourites {
    private static final String KEY = "packages";
    private final SharedPreferences prefs;

    Favourites(Context ctx) {
        prefs = ctx.getSharedPreferences("favourites", Context.MODE_PRIVATE);
    }

    List<String> list() {
        List<String> out = new ArrayList<>();
        for (String s : prefs.getString(KEY, "").split("\n")) {
            if (!s.isEmpty()) out.add(s);
        }
        return out;
    }

    boolean has(String pkg) {
        return list().contains(pkg);
    }

    void add(String pkg) {
        List<String> l = list();
        if (!l.contains(pkg)) {
            l.add(pkg);
            save(l);
        }
    }

    void remove(String pkg) {
        List<String> l = list();
        if (l.remove(pkg)) save(l);
    }

    private void save(List<String> l) {
        StringBuilder sb = new StringBuilder();
        for (String s : l) {
            if (sb.length() > 0) sb.append('\n');
            sb.append(s);
        }
        prefs.edit().putString(KEY, sb.toString()).apply();
    }
}
