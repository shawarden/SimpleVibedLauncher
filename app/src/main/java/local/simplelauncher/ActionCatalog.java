package local.simplelauncher;

/**
 * Everything a button can be set to, apart from "open this app" and "open this app shortcut", which are
 * chosen from the installed apps. An action is a short text: "kind:name".
 * <pre>
 *   media:PLAY_PAUSE   media key sent to whatever is playing
 *   vol:UP             TV volume
 *   tv:ChannelUp       a Sony remote code sent to the TV over the network (see TvControl)
 *   global:BACK        a system action done by the accessibility service
 *   sys:BLUETOOTH      a page of the TV's Settings
 *   launcher:APPS      one of this launcher's own screens or functions
 *   app:com.pkg        open an app            shortcut:com.pkg|id   open an app shortcut
 *   none:              swallow the button and do nothing
 * </pre>
 * No Android classes in here, so it can be tested on a plain JVM.
 */
final class ActionCatalog {
    private ActionCatalog() {}

    static final class Item {
        final String spec, label;
        Item(String spec, String label) { this.spec = spec; this.label = label; }
    }

    static final class Group {
        final String title;
        final Item[] items;
        Group(String title, Item... items) { this.title = title; this.items = items; }
    }

    private static Item i(String spec, String label) { return new Item(spec, label); }

    static final Group MEDIA = new Group("Media controls",
            i("media:PLAY_PAUSE", "Play / Pause"),
            i("media:PLAY", "Play"),
            i("media:PAUSE", "Pause"),
            i("media:STOP", "Stop"),
            i("media:NEXT", "Next track"),
            i("media:PREVIOUS", "Previous track"),
            i("media:REWIND", "Rewind"),
            i("media:FAST_FORWARD", "Fast forward"));

    static final Group VOLUME = new Group("Volume",
            i("vol:UP", "Volume up"),
            i("vol:DOWN", "Volume down"),
            i("vol:MUTE", "Mute / unmute"));

    /** These need the TV's IP control switched on and a key set; see TvControl. */
    static final Group TV = new Group("Channels and TV functions (needs IP control)",
            i("tv:ChannelUp", "Channel up"),
            i("tv:ChannelDown", "Channel down"),
            i("tv:Input", "Input select (the TV's own picker)"),
            i("tv:Hdmi1", "Switch to HDMI 1"),
            i("tv:Hdmi2", "Switch to HDMI 2"),
            i("tv:Hdmi3", "Switch to HDMI 3"),
            i("tv:Hdmi4", "Switch to HDMI 4"),
            i("tv:Audio", "Audio language / sound"),
            i("tv:CC", "Subtitles"),
            i("tv:Display", "Info / display"),
            i("tv:Options", "Options"),
            i("tv:Help", "Help"),
            i("tv:Red", "Red"),
            i("tv:Green", "Green"),
            i("tv:Yellow", "Yellow"),
            i("tv:Blue", "Blue"),
            i("tv:Power", "Standby (power)"));

    static final Group ANDROID = new Group("Android TV functions",
            i("global:HOME", "Home"),
            i("global:BACK", "Back"),
            i("global:RECENTS", "Recent apps"),
            i("global:NOTIFICATIONS", "Notifications"),
            i("global:QUICK_SETTINGS", "Quick settings"),
            i("global:POWER_DIALOG", "Power menu"),
            i("global:SCREENSHOT", "Screenshot"),
            i("assist:VOICE", "Voice assistant"),
            i("global:APP_INFO", "App info for the app in front"),
            i("global:DPAD_UP", "D-pad up (Android 13 and newer)"),
            i("global:DPAD_DOWN", "D-pad down (Android 13 and newer)"),
            i("global:DPAD_LEFT", "D-pad left (Android 13 and newer)"),
            i("global:DPAD_RIGHT", "D-pad right (Android 13 and newer)"),
            i("global:DPAD_CENTER", "D-pad OK (Android 13 and newer)"));

    static final Group SYSTEM = new Group("System shortcuts",
            i("sys:SETTINGS", "TV settings"),
            i("sys:NETWORK", "Network settings"),
            i("sys:BLUETOOTH", "Bluetooth settings"),
            i("sys:DISPLAY", "Display settings"),
            i("sys:SOUND", "Sound settings"),
            i("sys:APPS", "Apps settings"),
            i("sys:ACCESSIBILITY", "Accessibility settings"),
            i("sys:KEYBOARD", "Keyboard settings"),
            i("sys:DATE", "Date and time settings"),
            i("sys:LANGUAGE", "Language settings"),
            i("sys:STORAGE", "Storage settings"),
            i("sys:DEVELOPER", "Developer options"));

    static final Group LAUNCHER = new Group("This launcher",
            i("launcher:HOME", "Launcher home screen"),
            i("launcher:APPS", "Apps drawer"),
            i("launcher:INPUTS", "Inputs list"),
            i("launcher:WIFI", "Network information"),
            i("launcher:SETTINGS", "Launcher settings"),
            i("launcher:BUTTONS", "Button mapper"),
            i("launcher:SCREENSAVER", "Start the screensaver now"));

    static final Group[] GROUPS = { MEDIA, VOLUME, TV, ANDROID, SYSTEM, LAUNCHER };

    static final String NONE = "none:";
    static final String NONE_LABEL = "Nothing (the button is blocked)";
    static final String APP = "app:";
    static final String SHORTCUT = "shortcut:";

    /** The text to show for a fixed action, or null if it isn't one (apps and shortcuts store their own). */
    static String labelFor(String spec) {
        if (NONE.equals(spec)) return NONE_LABEL;
        for (Group g : GROUPS) for (Item it : g.items) if (it.spec.equals(spec)) return it.label;
        return null;
    }

    /** Volume buttons should repeat while held; everything else happens once per press. */
    static boolean repeats(String spec) {
        return spec.startsWith("vol:") || spec.equals("tv:ChannelUp") || spec.equals("tv:ChannelDown")
                || spec.equals("media:REWIND") || spec.equals("media:FAST_FORWARD");
    }

    /** "kind" part of an action. */
    static String kind(String spec) {
        int c = spec.indexOf(':');
        return c < 0 ? spec : spec.substring(0, c);
    }

    /** "name" part of an action. */
    static String arg(String spec) {
        int c = spec.indexOf(':');
        return c < 0 ? "" : spec.substring(c + 1);
    }

    // ---------------------------------------------------------------- Sony remote codes

    /**
     * Sony's IRCC codes, copied from Sony's BRAVIA IRCC-IP documentation. The TV can also list its own codes
     * (getRemoteControllerInfo); these are the standard ones.
     */
    static final String[][] SONY_CODES = {
            { "Power", "AAAAAQAAAAEAAAAVAw==" },
            { "Input", "AAAAAQAAAAEAAAAlAw==" },
            { "Hdmi1", "AAAAAgAAABoAAABaAw==" },
            { "Hdmi2", "AAAAAgAAABoAAABbAw==" },
            { "Hdmi3", "AAAAAgAAABoAAABcAw==" },
            { "Hdmi4", "AAAAAgAAABoAAABdAw==" },
            { "CC", "AAAAAgAAAJcAAAAoAw==" },
            { "Red", "AAAAAgAAAJcAAAAlAw==" },
            { "Green", "AAAAAgAAAJcAAAAmAw==" },
            { "Yellow", "AAAAAgAAAJcAAAAnAw==" },
            { "Blue", "AAAAAgAAAJcAAAAkAw==" },
            { "Help", "AAAAAgAAAMQAAABNAw==" },
            { "Display", "AAAAAQAAAAEAAAA6Aw==" },
            { "Options", "AAAAAgAAAJcAAAA2Aw==" },
            { "Audio", "AAAAAQAAAAEAAAAXAw==" },
            { "ChannelUp", "AAAAAQAAAAEAAAAQAw==" },
            { "ChannelDown", "AAAAAQAAAAEAAAARAw==" },
    };

    static String sonyCode(String name) {
        for (String[] c : SONY_CODES) if (c[0].equals(name)) return c[1];
        return null;
    }
}
