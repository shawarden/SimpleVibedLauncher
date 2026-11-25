package local.simplelauncher;

import android.content.Context;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** The time and day strings shown on the home screen and the screensaver. */
final class ClockText {
    private ClockText() {}

    /** Time of day, in the launcher's 12/24 hour setting (or the TV's own setting by default). */
    static String time(Context ctx, long nowMs) {
        int format = Prefs.clockFormat(ctx);
        if (format == 1 || format == 2) {
            Locale l = Locale.getDefault();
            String pattern = android.text.format.DateFormat.getBestDateTimePattern(l, format == 2 ? "Hmm" : "hmma");
            return new SimpleDateFormat(pattern, l).format(new Date(nowMs));
        }
        return android.text.format.DateFormat.getTimeFormat(ctx).format(new Date(nowMs));
    }

    /** Day of the week and date, e.g. "Wednesday 25 November". */
    static String day(long nowMs) {
        Locale l = Locale.getDefault();
        String pattern = android.text.format.DateFormat.getBestDateTimePattern(l, "EEEEdMMMM");
        return new SimpleDateFormat(pattern, l).format(new Date(nowMs));
    }
}
