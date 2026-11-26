package local.simplelauncher;

import android.accessibilityservice.AccessibilityService;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

/**
 * Sees every button press from the remote, in every app, before the system does. A button that has been
 * given a job in the Button mapper is swallowed and its action carried out instead. Android only lets an
 * accessibility service do this, so the service has to be switched on once (see the README).
 * <p>
 * Back, Home, the arrows and OK are never touched, so the TV can always be operated.
 */
public class KeyService extends AccessibilityService {
    /** Gets the next button pressed (used by the mapper's "press a button" step). */
    interface Recorder { void onKey(int keyCode, int scanCode); }

    private static volatile KeyService instance;
    private static volatile Recorder recorder;
    /** True while the mapper screen is open: buttons then behave normally so they can be edited. */
    private static volatile boolean suspended;
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    static KeyService instance() { return instance; }

    static boolean isRunning() { return instance != null; }

    static void setSuspended(boolean value) { suspended = value; }

    /** While set, buttons are not acted on but handed to the recorder instead (core buttons still work). */
    static void setRecorder(Recorder r) { recorder = r; }

    @Override protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
    }

    @Override public boolean onUnbind(android.content.Intent intent) {
        if (instance == this) instance = null;
        return super.onUnbind(intent);
    }

    @Override public void onDestroy() {
        if (instance == this) instance = null;
        super.onDestroy();
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        // Not interested in what is on screen; only in the buttons.
    }

    @Override public void onInterrupt() {
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

        Bindings.Binding b = Mappings.get(this).find(Bindings.keyId(keyCode, event.getScanCode()));
        if (b == null) return false;

        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            boolean first = event.getRepeatCount() == 0;
            if (first || ActionCatalog.repeats(b.action)) ActionRunner.run(this, b.action);
        }
        return true;     // both the press and the release belong to us
    }
}
