package local.simplelauncher;

import android.accessibilityservice.AccessibilityService;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.Process;
import android.provider.Settings;
import android.view.KeyEvent;
import android.widget.Toast;

/** Carries out a button's action (see {@link ActionCatalog} for the action texts). */
final class ActionRunner {
    private ActionRunner() {}

    /** Name of the extra that tells the home screen to do something once it is in front. */
    static final String COMMAND = "local.simplelauncher.COMMAND";

    static void run(Context ctx, String spec) {
        Context app = ctx.getApplicationContext();
        String kind = ActionCatalog.kind(spec);
        String arg = ActionCatalog.arg(spec);
        try {
            switch (kind) {
                case "none":     break;
                case "media":    media(app, arg); break;
                case "vol":      volume(app, arg); break;
                case "tv":       TvControl.send(app, arg); break;
                case "global":   global(app, arg); break;
                case "assist":   assist(app); break;
                case "sys":      system(app, arg); break;
                case "launcher": launcher(app, arg); break;
                case "app":      openApp(app, arg); break;
                case "shortcut": shortcut(app, arg); break;
                default:         toast(app, "Unknown button action"); break;
            }
        } catch (RuntimeException e) {
            toast(app, "That button action failed");
        }
    }

    // ---------------------------------------------------------------- media and volume

    private static void media(Context app, String name) {
        int code;
        switch (name) {
            case "PLAY_PAUSE":    code = KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE; break;
            case "PLAY":          code = KeyEvent.KEYCODE_MEDIA_PLAY; break;
            case "PAUSE":         code = KeyEvent.KEYCODE_MEDIA_PAUSE; break;
            case "STOP":          code = KeyEvent.KEYCODE_MEDIA_STOP; break;
            case "NEXT":          code = KeyEvent.KEYCODE_MEDIA_NEXT; break;
            case "PREVIOUS":      code = KeyEvent.KEYCODE_MEDIA_PREVIOUS; break;
            case "REWIND":        code = KeyEvent.KEYCODE_MEDIA_REWIND; break;
            case "FAST_FORWARD":  code = KeyEvent.KEYCODE_MEDIA_FAST_FORWARD; break;
            default: return;
        }
        AudioManager am = (AudioManager) app.getSystemService(Context.AUDIO_SERVICE);
        am.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, code));
        am.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, code));
    }

    private static void volume(Context app, String name) {
        AudioManager am = (AudioManager) app.getSystemService(Context.AUDIO_SERVICE);
        int direction;
        switch (name) {
            case "UP":   direction = AudioManager.ADJUST_RAISE; break;
            case "DOWN": direction = AudioManager.ADJUST_LOWER; break;
            case "MUTE": direction = AudioManager.ADJUST_TOGGLE_MUTE; break;
            default: return;
        }
        am.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI);
    }

    // ---------------------------------------------------------------- system actions (need the service)

    private static void global(Context app, String name) {
        KeyService service = KeyService.instance();
        if (service == null) {
            toast(app, "Turn on the button mapper service first (Launcher settings > Button mapper)");
            return;
        }
        int action;
        switch (name) {
            case "APP_INFO": {
                String pkg = KeyService.currentPackage();
                if (pkg == null) {
                    toast(app, "No app has been seen in front yet; open one first");
                } else {
                    Actions.appInfo(app, pkg);
                }
                return;
            }
            case "HOME":           action = AccessibilityService.GLOBAL_ACTION_HOME; break;
            case "BACK":           action = AccessibilityService.GLOBAL_ACTION_BACK; break;
            case "RECENTS":        action = AccessibilityService.GLOBAL_ACTION_RECENTS; break;
            case "NOTIFICATIONS":  action = AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS; break;
            case "QUICK_SETTINGS": action = AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS; break;
            case "POWER_DIALOG":   action = AccessibilityService.GLOBAL_ACTION_POWER_DIALOG; break;
            case "SCREENSHOT":
                if (Build.VERSION.SDK_INT < 28) { toast(app, "Screenshots need Android 9 or newer"); return; }
                action = AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT;
                break;
            case "DPAD_UP":     action = dpad(app, 16); break;
            case "DPAD_DOWN":   action = dpad(app, 17); break;
            case "DPAD_LEFT":   action = dpad(app, 18); break;
            case "DPAD_RIGHT":  action = dpad(app, 19); break;
            case "DPAD_CENTER": action = dpad(app, 20); break;
            default: return;
        }
        if (action < 0) return;
        if (!service.performGlobalAction(action)) toast(app, "This TV didn't do that");
    }

    /** The D-pad global actions (numbers 16 to 20) only exist from Android 13. */
    private static int dpad(Context app, int code) {
        if (Build.VERSION.SDK_INT < 33) {
            toast(app, "D-pad actions need Android 13 or newer");
            return -1;
        }
        return code;
    }

    private static void assist(Context app) {
        Intent[] tries = {
                new Intent(Intent.ACTION_VOICE_COMMAND),
                new Intent(Intent.ACTION_ASSIST),
                new Intent("android.intent.action.VOICE_ASSIST") };
        for (Intent i : tries) {
            try {
                app.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                return;
            } catch (ActivityNotFoundException | SecurityException e) {
                // try the next one
            }
        }
        toast(app, "No voice assistant found");
    }

    // ---------------------------------------------------------------- Settings pages

    private static void system(Context app, String name) {
        String action;
        switch (name) {
            case "SETTINGS":      Actions.settings(app); return;
            case "NETWORK":       Actions.networkSettings(app); return;
            case "BLUETOOTH":     action = Settings.ACTION_BLUETOOTH_SETTINGS; break;
            case "DISPLAY":       action = Settings.ACTION_DISPLAY_SETTINGS; break;
            case "SOUND":         action = Settings.ACTION_SOUND_SETTINGS; break;
            case "APPS":          action = Settings.ACTION_APPLICATION_SETTINGS; break;
            case "ACCESSIBILITY": action = Settings.ACTION_ACCESSIBILITY_SETTINGS; break;
            case "KEYBOARD":      action = Settings.ACTION_INPUT_METHOD_SETTINGS; break;
            case "DATE":          action = Settings.ACTION_DATE_SETTINGS; break;
            case "LANGUAGE":      action = Settings.ACTION_LOCALE_SETTINGS; break;
            case "STORAGE":       action = Settings.ACTION_INTERNAL_STORAGE_SETTINGS; break;
            case "DEVELOPER":     action = Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS; break;
            default: return;
        }
        try {
            app.startActivity(new Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (ActivityNotFoundException | SecurityException e) {
            toast(app, "This TV has no such settings page");
        }
    }

    // ---------------------------------------------------------------- this launcher

    private static void launcher(Context app, String name) {
        Intent i;
        switch (name) {
            case "APPS":
                i = new Intent(app, DrawerActivity.class);
                break;
            case "SETTINGS":
                i = new Intent(app, SettingsActivity.class);
                break;
            case "BUTTONS":
                i = new Intent(app, ButtonMapActivity.class);
                break;
            case "HOME":
                i = new Intent(app, HomeActivity.class).putExtra(COMMAND, "home");
                break;
            case "INPUTS":
                i = new Intent(app, HomeActivity.class).putExtra(COMMAND, "inputs");
                break;
            case "WIFI":
                i = new Intent(app, HomeActivity.class).putExtra(COMMAND, "wifi");
                break;
            case "SCREENSAVER":
                i = new Intent(app, HomeActivity.class).putExtra(COMMAND, "saver");
                break;
            default:
                return;
        }
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        app.startActivity(i);
    }

    // ---------------------------------------------------------------- apps and shortcuts

    static void openApp(Context app, String pkg) {
        PackageManager pm = app.getPackageManager();
        Intent i = pm.getLeanbackLaunchIntentForPackage(pkg);
        if (i == null) i = pm.getLaunchIntentForPackage(pkg);
        if (i == null) {
            toast(app, "That app isn't installed any more");
            return;
        }
        try {
            app.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED));
        } catch (ActivityNotFoundException | SecurityException e) {
            toast(app, "Can't open that app");
        }
    }

    /** arg is "package|shortcutId". Starting shortcuts only works while this launcher is the default home app. */
    private static void shortcut(Context app, String arg) {
        int bar = arg.indexOf('|');
        if (bar < 0) return;
        String pkg = arg.substring(0, bar);
        String id = arg.substring(bar + 1);
        if (Build.VERSION.SDK_INT < 25) {
            toast(app, "App shortcuts need Android 7.1 or newer");
            return;
        }
        try {
            LauncherApps la = (LauncherApps) app.getSystemService(Context.LAUNCHER_APPS_SERVICE);
            la.startShortcut(pkg, id, null, null, Process.myUserHandle());
        } catch (ActivityNotFoundException | SecurityException | IllegalStateException e) {
            toast(app, "Can't start that shortcut (this launcher must be the default home app)");
        }
    }

    private static void toast(Context app, String text) {
        Toast.makeText(app, text, Toast.LENGTH_LONG).show();
    }
}
