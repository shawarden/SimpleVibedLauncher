package local.simplelauncher;

import android.accessibilityservice.AccessibilityService;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Sees every button press from the remote, in every app, before the system does. A button that has been
 * given a job in the Button mapper is swallowed and its action carried out instead. Android only lets an
 * accessibility service do this, so the service has to be switched on once (see the README).
 * <p>
 * Each button can have three jobs: a single press, a long press and a double press (see {@link Gesture}).
 * Back, Home, the arrows and OK are never touched, so the TV can always be operated.
 */
public class KeyService extends AccessibilityService {
    /** Gets the next button pressed (used by the mapper's "press a button" step). */
    interface Recorder { void onKey(int keyCode, int scanCode); }

    private static volatile KeyService instance;
    private static volatile Recorder recorder;
    /** True while the mapper screen is open: buttons then behave normally so they can be edited. */
    private static volatile boolean suspended;
    /** The app that was last seen coming to the front (used by the App info action). */
    private static volatile String foreground;
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    /** One gesture tracker per button that is mid-press or waiting for a double press. */
    private final Map<String, Gesture> gestures = new HashMap<>();
    private final Runnable pump = this::pump;

    static KeyService instance() { return instance; }

    static boolean isRunning() { return instance != null; }

    static void setSuspended(boolean value) { suspended = value; }

    /** While set, buttons are not acted on but handed to the recorder instead (core buttons still work). */
    static void setRecorder(Recorder r) { recorder = r; }

    /** Package name of the app last seen in front, or null if none has been seen yet. */
    static String currentPackage() { return foreground; }

    @Override protected void onServiceConnected() {
        super.onServiceConnected();
        Tuning.ensureLoaded(this);
        instance = this;
    }

    @Override public boolean onUnbind(android.content.Intent intent) {
        if (instance == this) instance = null;
        stopGestures();
        return super.onUnbind(intent);
    }

    @Override public void onDestroy() {
        if (instance == this) instance = null;
        stopGestures();
        super.onDestroy();
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return;
        CharSequence cs = event.getPackageName();
        if (cs == null) return;
        String pkg = cs.toString();
        // The system's own overlays (volume bar, notification shade, dialogs) are not "the app in front".
        if (pkg.equals("com.android.systemui") || pkg.equals("android")) return;
        foreground = pkg;
    }

    @Override public void onInterrupt() {
        stopGestures();
    }

    private void stopGestures() {
        MAIN.removeCallbacks(pump);
        gestures.clear();
    }

    @Override protected boolean onKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        if (Bindings.isCore(keyCode)) return false;

        final Recorder r = recorder;
        if (r != null) {
            // Recording: swallow the button and report it once, on the key-down.
            if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
                final int scan = event.getScanCode();
                MAIN.post(() -> r.onKey(keyCode, scan));
            }
            return true;
        }
        if (suspended) return false;

        final String id = Bindings.keyId(keyCode, event.getScanCode());
        Bindings.Binding b = Mappings.get(this).find(id);
        if (b == null) return false;

        long now = SystemClock.uptimeMillis();
        pumpAt(now);                    // anything already due (an earlier single press) goes first

        Gesture g = gestures.get(id);
        if (g == null) {
            g = new Gesture(Config.LONG_PRESS_MS, Config.DOUBLE_PRESS_MS);
            gestures.put(id, g);
        }
        Gesture.Press p;
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            p = g.down(now, event.getRepeatCount(), b.has(Bindings.LONG), b.has(Bindings.DOUBLE),
                    ActionCatalog.repeats(b.action[Bindings.SINGLE]));
        } else {
            p = g.up(now);
        }
        perform(b, p);
        if (!g.busy()) gestures.remove(id);
        schedule();
        return true;     // both the press and the release belong to us
    }

    /** Carries out the action for a press, if that press has one. */
    private void perform(Bindings.Binding b, Gesture.Press p) {
        int slot;
        switch (p) {
            case SINGLE: slot = Bindings.SINGLE; break;
            case LONG:   slot = Bindings.LONG; break;
            case DOUBLE: slot = Bindings.DOUBLE; break;
            default:     return;
        }
        if (b.has(slot)) ActionRunner.run(this, b.action[slot]);
    }

    /** Timer: carries out whatever has become due, then waits for the next deadline. */
    private void pump() {
        pumpAt(SystemClock.uptimeMillis());
        schedule();
    }

    private void pumpAt(long now) {
        if (gestures.isEmpty()) return;
        Bindings m = Mappings.get(this);
        for (Iterator<Map.Entry<String, Gesture>> it = gestures.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, Gesture> e = it.next();
            Gesture g = e.getValue();
            Gesture.Press p = g.tick(now);
            if (p != Gesture.Press.NONE) {
                Bindings.Binding b = m.find(e.getKey());
                if (b != null) perform(b, p);
            }
            if (!g.busy()) it.remove();
        }
    }

    private void schedule() {
        MAIN.removeCallbacks(pump);
        long next = -1;
        for (Gesture g : gestures.values()) {
            long d = g.deadline();
            if (d >= 0 && (next < 0 || d < next)) next = d;
        }
        if (next >= 0) MAIN.postDelayed(pump, Math.max(0, next - SystemClock.uptimeMillis()));
    }
}
