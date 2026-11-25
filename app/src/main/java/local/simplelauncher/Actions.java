package local.simplelauncher;

import android.app.ActivityManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.widget.Toast;

/** Things the launcher does to apps and to the TV. */
final class Actions {
    private Actions() {}

    static void launch(Context ctx, AppEntry app) {
        Intent i = Intent.makeMainActivity(app.component)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        start(ctx, i, "Can't open " + app.label);
    }

    /** Opens Android's own App info screen, which has the real Force stop button. */
    static void appInfo(Context ctx, String pkg) {
        Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", pkg, null))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        start(ctx, i, "Can't open App info");
    }

    static void uninstall(Context ctx, String pkg) {
        Intent i = new Intent(Intent.ACTION_DELETE, Uri.fromParts("package", pkg, null))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        start(ctx, i, "Can't uninstall this app");
    }

    /**
     * A normal app is not allowed to call the real forceStopPackage(), so this asks Android to kill the
     * app's background processes, which is as close as it gets. Use App info for a true Force stop.
     */
    static void forceStop(Context ctx, AppEntry app) {
        ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        am.killBackgroundProcesses(app.packageName);
        Toast.makeText(ctx, "Stopped " + app.label + " (use App Info for a full Force stop)", Toast.LENGTH_SHORT).show();
    }

    /** The TV's standard Settings. */
    static void settings(Context ctx) {
        try {
            ctx.startActivity(new Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (ActivityNotFoundException e) {
            Intent tv = new Intent().setComponent(
                    new ComponentName("com.android.tv.settings", "com.android.tv.settings.MainSettings"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            start(ctx, tv, "Can't open Settings");
        }
    }

    /** The TV's network settings; falls back to the main Settings if this TV has no such page. */
    static void networkSettings(Context ctx) {
        String[] actions = { Settings.ACTION_WIFI_SETTINGS, Settings.ACTION_WIRELESS_SETTINGS, Settings.ACTION_SETTINGS };
        for (String action : actions) {
            try {
                ctx.startActivity(new Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                return;
            } catch (ActivityNotFoundException | SecurityException e) {
                // try the next one
            }
        }
        Toast.makeText(ctx, "Can't open network settings", Toast.LENGTH_SHORT).show();
    }

    private static void start(Context ctx, Intent intent, String errorMessage) {
        try {
            ctx.startActivity(intent);
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(ctx, errorMessage, Toast.LENGTH_SHORT).show();
        }
    }
}
