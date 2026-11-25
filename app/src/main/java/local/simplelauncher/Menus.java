package local.simplelauncher;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.widget.Button;

/** The pop-up dialogs: option lists (long OK press, inputs) and the network info panel. */
final class Menus {
    private Menus() {}

    interface Choice { void on(int which); }

    static Dialog show(Activity activity, String title, String[] items, Choice choice) {
        AlertDialog dialog = new AlertDialog.Builder(activity, AlertDialog.THEME_DEVICE_DEFAULT_DARK)
                .setTitle(title)
                .setItems(items, (d, which) -> choice.on(which))
                .create();
        dialog.show();
        return dialog;
    }

    /**
     * A message with an action button and a Close button. The action button starts with the focus,
     * so pressing OK again triggers it.
     */
    static Dialog info(Activity activity, String title, String message, String actionLabel, Runnable action) {
        AlertDialog dialog = new AlertDialog.Builder(activity, AlertDialog.THEME_DEVICE_DEFAULT_DARK)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(actionLabel, (d, which) -> action.run())
                .setNegativeButton("Close", null)
                .create();
        dialog.show();
        Button primary = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        if (primary != null) primary.requestFocus();
        return dialog;
    }
}
