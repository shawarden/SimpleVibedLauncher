package local.simplelauncher;

/**
 * Tells single, long and double presses of one remote button apart. Feed it the button going down and up and
 * the passing of time; it says when to carry out which press. No Android classes in here, so it can be tested
 * on a plain JVM. Times are in milliseconds from any steady clock.
 * <p>
 * To stay quick, it never waits longer than it has to:
 * <ul>
 * <li>a button with only a single press acts the moment it goes down (and repeats while held, if asked);</li>
 * <li>with a long press set, a single press acts when the button is let go, and the long press acts as soon as
 *     the button has been held for the long-press time;</li>
 * <li>with a double press set, a single press acts once the double-press window has passed without a second press;
 *     a second press inside the window acts as the double press straight away.</li>
 * </ul>
 */
final class Gesture {
    enum Press { NONE, SINGLE, LONG, DOUBLE }

    private enum State {
        IDLE,
        SIMPLE_DOWN,     // a button with only a single press: already acted, waiting for the release
        FIRST_DOWN,      // first press held; a long press may fire
        LONG_DONE,       // the long press fired; waiting for the release
        WAIT_SECOND,     // released once; waiting to see whether a second press follows
        SECOND_DOWN      // the double press fired; waiting for the release
    }

    private final long longMs, doubleMs;
    private State state = State.IDLE;
    private boolean hasLong, hasDouble;
    private long deadline = -1;

    Gesture(long longMs, long doubleMs) {
        this.longMs = longMs;
        this.doubleMs = doubleMs;
    }

    /**
     * The button went down (repeatCount is 0 for the first event of a press, more while it is held).
     * hasLong and hasDouble say whether those presses have an action; repeating is true if the single press
     * should repeat while the button is held (volume).
     */
    Press down(long now, int repeatCount, boolean hasLong, boolean hasDouble, boolean repeating) {
        if (repeatCount > 0) {
            // held down
            if (state == State.SIMPLE_DOWN) return repeating ? Press.SINGLE : Press.NONE;
            if (state == State.FIRST_DOWN && deadline >= 0 && now >= deadline) return tick(now);
            return Press.NONE;
        }
        if (state == State.WAIT_SECOND && now < deadline) {
            state = State.SECOND_DOWN;
            deadline = -1;
            return Press.DOUBLE;
        }
        // a new press (anything left over from an earlier press that never ended is forgotten)
        this.hasLong = hasLong;
        this.hasDouble = hasDouble;
        if (!hasLong && !hasDouble) {
            state = State.SIMPLE_DOWN;
            deadline = -1;
            return Press.SINGLE;
        }
        state = State.FIRST_DOWN;
        deadline = hasLong ? now + longMs : -1;
        return Press.NONE;
    }

    /** The button was let go. */
    Press up(long now) {
        switch (state) {
            case FIRST_DOWN:
                if (hasLong && deadline >= 0 && now >= deadline) {      // held long enough but the timer hasn't run yet
                    state = State.IDLE;
                    deadline = -1;
                    return Press.LONG;
                }
                if (hasDouble) {
                    state = State.WAIT_SECOND;
                    deadline = now + doubleMs;
                    return Press.NONE;
                }
                state = State.IDLE;
                deadline = -1;
                return Press.SINGLE;
            case WAIT_SECOND:
                return Press.NONE;                                   // a release with no press: ignore
            default:                                                 // SIMPLE_DOWN, LONG_DONE, SECOND_DOWN, IDLE
                state = State.IDLE;
                deadline = -1;
                return Press.NONE;
        }
    }

    /** Time has passed: returns the press that has now become due, if any. */
    Press tick(long now) {
        if (state == State.FIRST_DOWN && hasLong && deadline >= 0 && now >= deadline) {
            state = State.LONG_DONE;
            deadline = -1;
            return Press.LONG;
        }
        if (state == State.WAIT_SECOND && now >= deadline) {
            state = State.IDLE;
            deadline = -1;
            return Press.SINGLE;
        }
        return Press.NONE;
    }

    /** When {@link #tick} next needs to be called, or -1 if nothing is waiting. */
    long deadline() { return deadline; }

    /** True while the button is mid-press or a double press is being waited for. */
    boolean busy() { return state != State.IDLE; }

    void reset() {
        state = State.IDLE;
        deadline = -1;
    }
}
