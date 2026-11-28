package local.simplelauncher;

import java.util.ArrayList;
import java.util.List;

/**
 * The remote buttons that have been given a job. Each binding is a button (identified by its key code, or by
 * its scan code when Android doesn't know the button), an optional alias, and an action written as a short
 * text such as "media:PLAY_PAUSE" or "app:com.netflix.ninja" (see {@link ActionCatalog}).
 * No Android classes in here, so it can be tested on a plain JVM.
 */
final class Bindings {
    /** Which press of the button an action belongs to. */
    static final int SINGLE = 0, LONG = 1, DOUBLE = 2;
    static final String[] PRESS_NAMES = { "Single press", "Long press", "Double press" };
    private static final String[] SHORT_NAMES = { "Press", "Long", "Double" };

    static final class Binding {
        final String keyId;       // "k<keyCode>", or "s<scanCode>" for a button Android has no key code for
        final int keyCode;
        final int scanCode;
        String keyName;           // e.g. "PROG RED", as shown to the user
        String alias;             // "" when there is none
        /** What each press does, indexed by SINGLE, LONG, DOUBLE. An empty action means that press is not set. */
        final String[] action = { "", "", "" };
        /** What to show for each action (kept so apps need no lookup). */
        final String[] actionLabel = { "", "", "" };

        Binding(int keyCode, int scanCode, String keyName, String alias) {
            this.keyCode = keyCode;
            this.scanCode = scanCode;
            this.keyId = keyId(keyCode, scanCode);
            this.keyName = keyName;
            this.alias = alias == null ? "" : alias;
        }

        /** A button with just a single press. */
        Binding(int keyCode, int scanCode, String keyName, String alias, String action, String actionLabel) {
            this(keyCode, scanCode, keyName, alias);
            set(SINGLE, action, actionLabel);
        }

        /** The name to show: the alias if there is one, otherwise the button's own name. */
        String title() { return alias.isEmpty() ? keyName : alias; }

        boolean has(int press) { return !action[press].isEmpty(); }

        void set(int press, String spec, String label) {
            action[press] = spec == null ? "" : spec;
            actionLabel[press] = spec == null ? "" : (label == null ? "" : label);
        }

        void clear(int press) { set(press, "", ""); }

        /** True when no press has an action. */
        boolean isEmpty() { return !has(SINGLE) && !has(LONG) && !has(DOUBLE); }

        /** For example "Press: Play / Pause     Long: Mute". */
        String summary() {
            StringBuilder sb = new StringBuilder();
            for (int p = 0; p < 3; p++) {
                if (!has(p)) continue;
                if (sb.length() > 0) sb.append("     ");
                sb.append(SHORT_NAMES[p]).append(": ").append(actionLabel[p]);
            }
            return sb.toString();
        }
    }

    private final List<Binding> list = new ArrayList<>();

    /** Buttons are matched by key code; a button with no key code (0) is matched by its scan code. */
    static String keyId(int keyCode, int scanCode) {
        return keyCode != 0 ? "k" + keyCode : "s" + scanCode;
    }

    /**
     * Buttons that can't be remapped, because the launcher is operated with them or the system needs them:
     * Back, Home, the arrows, OK/Enter, Escape and the power keys.
     */
    static boolean isCore(int keyCode) {
        switch (keyCode) {
            case 3:     // HOME
            case 4:     // BACK
            case 19:    // DPAD_UP
            case 20:    // DPAD_DOWN
            case 21:    // DPAD_LEFT
            case 22:    // DPAD_RIGHT
            case 23:    // DPAD_CENTER
            case 66:    // ENTER
            case 160:   // NUMPAD_ENTER
            case 111:   // ESCAPE
            case 26:    // POWER
            case 223:   // SLEEP
            case 224:   // WAKEUP
                return true;
            default:
                return false;
        }
    }

    List<Binding> all() { return list; }

    Binding find(String keyId) {
        for (Binding b : list) if (b.keyId.equals(keyId)) return b;
        return null;
    }

    /** Adds the binding, replacing any earlier one for the same button. */
    void put(Binding binding) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).keyId.equals(binding.keyId)) {
                list.set(i, binding);
                return;
            }
        }
        list.add(binding);
    }

    void remove(String keyId) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).keyId.equals(keyId)) {
                list.remove(i);
                return;
            }
        }
    }

    // ---------------------------------------------------------------- saving

    /**
     * One line per binding, fields separated by tabs (tabs, newlines and backslashes inside fields are escaped):
     * key code, scan code, key name, alias, then the action and label of the single, long and double press.
     */
    String serialize() {
        StringBuilder sb = new StringBuilder();
        for (Binding b : list) {
            sb.append(b.keyCode).append('\t').append(b.scanCode).append('\t')
                    .append(esc(b.keyName)).append('\t').append(esc(b.alias));
            for (int p = 0; p < 3; p++) {
                sb.append('\t').append(esc(b.action[p])).append('\t').append(esc(b.actionLabel[p]));
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    /** Reads what {@link #serialize} wrote, and also the older lines that had only one action per button. */
    static Bindings parse(String text) {
        Bindings out = new Bindings();
        if (text == null) return out;
        for (String line : text.split("\n")) {
            if (line.isEmpty()) continue;
            String[] f = line.split("\t", -1);
            if (f.length != 6 && f.length != 10) continue;          // damaged line: skip it
            try {
                Binding b = new Binding(Integer.parseInt(f[0]), Integer.parseInt(f[1]), unesc(f[2]), unesc(f[3]));
                if (f.length == 6) {
                    b.set(SINGLE, unesc(f[4]), unesc(f[5]));
                } else {
                    for (int p = 0; p < 3; p++) b.set(p, unesc(f[4 + 2 * p]), unesc(f[5 + 2 * p]));
                }
                out.put(b);
            } catch (NumberFormatException e) {
                // skip
            }
        }
        return out;
    }

    private static String esc(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\') sb.append("\\\\");
            else if (c == '\t') sb.append("\\t");
            else if (c == '\n') sb.append("\\n");
            else if (c == '\r') sb.append("\\r");
            else sb.append(c);
        }
        return sb.toString();
    }

    private static String unesc(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char n = s.charAt(++i);
                sb.append(n == 't' ? '\t' : n == 'n' ? '\n' : n == 'r' ? '\r' : n);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
