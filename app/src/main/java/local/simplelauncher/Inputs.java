package local.simplelauncher;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.media.tv.TvContract;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** The TV's external inputs (HDMI 1, HDMI 2, ...) and a way to switch to one. */
final class Inputs {
    private Inputs() {}

    static final class Input {
        final String id;
        final String label;
        final boolean connected;

        Input(String id, String label, boolean connected) {
            this.id = id;
            this.label = label;
            this.connected = connected;
        }
    }

    /** Every "passthrough" input the TV reports (HDMI, composite, ...), in the order the TV gives them. */
    static List<Input> list(Context ctx) {
        List<Input> out = new ArrayList<>();
        TvInputManager tim = (TvInputManager) ctx.getSystemService(Context.TV_INPUT_SERVICE);
        if (tim == null) return out;
        List<TvInputInfo> infos;
        try {
            infos = tim.getTvInputList();
        } catch (RuntimeException e) {
            return out;
        }
        if (infos == null) return out;
        for (TvInputInfo info : infos) {
            if (!info.isPassthroughInput()) continue;
            String label = null;
            try {
                CharSequence custom = info.loadCustomLabel(ctx);
                CharSequence normal = info.loadLabel(ctx);
                if (custom != null && custom.length() > 0) label = custom.toString();
                else if (normal != null && normal.length() > 0) label = normal.toString();
            } catch (RuntimeException e) {
                // fall through to the id
            }
            if (label == null) label = info.getId();
            boolean connected = false;
            try {
                connected = tim.getInputState(info.getId()) == TvInputManager.INPUT_STATE_CONNECTED;
            } catch (RuntimeException e) {
                // leave as not connected
            }
            out.add(new Input(info.getId(), label, connected));
        }
        return Collections.unmodifiableList(out);
    }

    /** Asks the TV to show this input. This is the standard Android TV request; false if nothing handled it. */
    static boolean switchTo(Context ctx, String inputId) {
        Intent i = new Intent(Intent.ACTION_VIEW, TvContract.buildChannelUriForPassthroughInput(inputId))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            ctx.startActivity(i);
            return true;
        } catch (ActivityNotFoundException | SecurityException e) {
            return false;
        }
    }
}
