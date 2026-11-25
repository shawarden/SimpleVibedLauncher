package local.simplelauncher;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Finds the installed apps. Call {@link #load} from a background thread. */
final class AppRepo {
    private AppRepo() {}

    static final ExecutorService IO = Executors.newSingleThreadExecutor();

    /**
     * Every app with a TV (leanback) or normal launcher entry, one per package, sorted by name.
     * The TV entry wins when an app has both. This launcher itself is left out.
     */
    static List<AppEntry> load(Context ctx, boolean preloadIcons) {
        PackageManager pm = ctx.getPackageManager();
        Map<String, AppEntry> byPackage = new LinkedHashMap<>();
        String[] categories = { Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER };
        for (String category : categories) {
            Intent query = new Intent(Intent.ACTION_MAIN).addCategory(category);
            for (ResolveInfo ri : pm.queryIntentActivities(query, 0)) {
                String pkg = ri.activityInfo.packageName;
                if (pkg.equals(ctx.getPackageName()) || byPackage.containsKey(pkg)) continue;
                byPackage.put(pkg, new AppEntry(pm, ri));
            }
        }
        List<AppEntry> list = new ArrayList<>(byPackage.values());
        Collections.sort(list, (a, b) -> a.label.compareToIgnoreCase(b.label));
        if (preloadIcons) {
            for (AppEntry a : list) a.icon();
        }
        return list;
    }
}
