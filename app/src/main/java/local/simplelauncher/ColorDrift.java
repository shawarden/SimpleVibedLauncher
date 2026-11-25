package local.simplelauncher;

import java.util.Random;

/**
 * Three colour channels (R, G, B), each kept within 0..max and always moving by exactly +/-step,
 * so every resting value is a multiple of step. A move eases smoothly from the old value to the new one.
 * No Android classes in here, so it can be tested on a plain JVM.
 */
final class ColorDrift {
    private final int step;
    private final int levels;           // max / step
    private final Random rnd;
    private final int[] target = new int[3];     // target level (value / step) of the current move
    private final float[] from = new float[3];   // value when the current move started
    private final float[] value = new float[3];  // current value, 0..max
    private final long[] dur = new long[3];
    private final long[] t = new long[3];

    ColorDrift(int max, int step, Random rnd) {
        if (step <= 0 || max < step) throw new IllegalArgumentException("need 0 < step <= max");
        this.step = step;
        this.levels = max / step;
        this.rnd = rnd;
        for (int c = 0; c < 3; c++) {
            target[c] = rnd.nextInt(levels + 1);      // random starting colour
            value[c] = from[c] = target[c] * step;    // dur = t = 0, so no move is in progress
        }
    }

    /**
     * Advance time. With keepGoing, a channel that has finished its move immediately picks another one
     * (taking between minMs and maxMs); without it, channels rest until {@link #nudge}.
     */
    void advance(long dtMs, boolean keepGoing, long minMs, long maxMs) {
        for (int c = 0; c < 3; c++) {
            if (t[c] < dur[c]) {
                t[c] = Math.min(dur[c], t[c] + dtMs);
                float f = (float) t[c] / dur[c];
                f = f * f * (3f - 2f * f);             // ease in and out
                value[c] = from[c] + (target[c] * step - from[c]) * f;
            }
            if (keepGoing && t[c] >= dur[c]) {
                move(c, minMs + (long) (rnd.nextFloat() * (maxMs - minMs)));
            }
        }
    }

    /** Every channel picks a new +/-step target and fades to it over fadeMs. */
    void nudge(long fadeMs) {
        for (int c = 0; c < 3; c++) move(c, fadeMs);
    }

    private void move(int c, long ms) {
        int dir = rnd.nextBoolean() ? 1 : -1;
        int next = target[c] + dir;
        if (next < 0 || next > levels) next = target[c] - dir;   // at an end, the only way is back
        from[c] = value[c];
        target[c] = next;
        t[c] = 0;
        dur[c] = Math.max(1L, ms);
    }

    /** Current colour as opaque ARGB. */
    int color() {
        return 0xFF000000 | (ch(0) << 16) | (ch(1) << 8) | ch(2);
    }

    private int ch(int c) {
        return Math.max(0, Math.min(255, Math.round(value[c])));
    }

    // for tests
    float value(int c) { return value[c]; }
    int target(int c) { return target[c]; }
}
