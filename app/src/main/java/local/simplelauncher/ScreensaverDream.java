package local.simplelauncher;

import android.service.dreams.DreamService;

/**
 * The same screensaver as a system "Daydream", for TVs that let you pick a screen saver in Settings.
 * (Google TV models often don't, which is why the launcher also starts the screensaver by itself.)
 */
public class ScreensaverDream extends DreamService {
    private ScreensaverView view;

    @Override public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setInteractive(false);
        setFullscreen(true);
        view = new ScreensaverView(this);
        setContentView(view);
    }

    @Override public void onDreamingStarted() {
        super.onDreamingStarted();
        view.start();
    }

    @Override public void onDreamingStopped() {
        view.stop();
        super.onDreamingStopped();
    }
}
