package local.simplelauncher;

/**
 * A damped spring pulling a point to a target: it rushes there, overshoots a little and settles.
 * No Android classes in here, so it can be tested on a plain JVM. Units are pixels and seconds.
 */
final class Spring {
    float x, y, vx, vy;
    boolean settled;

    private final float tx, ty;
    private final float omega;   // angular frequency, radians per second
    private final float zeta;    // damping ratio: 1 = no overshoot, smaller = bouncier

    Spring(float x, float y, float vx, float vy, float tx, float ty, float omega, float zeta) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.tx = tx;
        this.ty = ty;
        this.omega = omega;
        this.zeta = zeta;
    }

    /** Advance by dt seconds; keep dt small (a few milliseconds). */
    void step(float dt) {
        if (settled) return;
        float k = omega * omega;
        float c = 2f * zeta * omega;
        vx += (-k * (x - tx) - c * vx) * dt;
        vy += (-k * (y - ty) - c * vy) * dt;
        x += vx * dt;
        y += vy * dt;
        if (Math.abs(x - tx) < 1f && Math.abs(y - ty) < 1f && Math.abs(vx) < 25f && Math.abs(vy) < 25f) snap();
    }

    /** Jump straight to the target and stop. */
    void snap() {
        x = tx;
        y = ty;
        vx = 0f;
        vy = 0f;
        settled = true;
    }

    /** The damping ratio that overshoots the target by this fraction of the distance travelled (0.1 = 10%). */
    static float zetaFor(float overshoot) {
        double l = Math.log(Math.max(0.001f, Math.min(0.9f, overshoot)));
        return (float) (-l / Math.sqrt(Math.PI * Math.PI + l * l));
    }
}
