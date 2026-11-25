package local.simplelauncher;

import android.content.ComponentName;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;

/** One launchable app. Icons and banners are loaded on demand. */
final class AppEntry {
    final ComponentName component;
    final String packageName;
    final String label;
    /** True for firmware apps that can't be uninstalled. */
    final boolean system;

    private final PackageManager pm;
    private final ResolveInfo info;
    private Drawable icon;

    AppEntry(PackageManager pm, ResolveInfo ri) {
        ActivityInfo ai = ri.activityInfo;
        this.pm = pm;
        this.info = ri;
        this.component = new ComponentName(ai.packageName, ai.name);
        this.packageName = ai.packageName;
        CharSequence l = ri.loadLabel(pm);
        this.label = l != null ? l.toString() : ai.packageName;
        int f = ai.applicationInfo.flags;
        this.system = (f & ApplicationInfo.FLAG_SYSTEM) != 0
                && (f & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0;
    }

    synchronized Drawable icon() {
        if (icon == null) icon = info.loadIcon(pm);
        return icon;
    }

    /** The 16:9 TV banner, or null if the app doesn't have one. */
    Drawable banner() {
        try {
            return info.activityInfo.loadBanner(pm);
        } catch (RuntimeException e) {
            return null;
        }
    }
}
