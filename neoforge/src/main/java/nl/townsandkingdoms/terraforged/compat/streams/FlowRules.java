package nl.townsandkingdoms.terraforged.compat.streams;

/** Small pure rules shared with the focused regression checks. */
public final class FlowRules {
    private FlowRules() {}
    public static boolean uphill(double before, double here, double after) {
        return here > before + 0.5 || after > here + 0.5;
    }
    /** +1 follows the route, -1 reverses a genuinely rising stretch, 0 is a basin. */
    public static int downhill(double before, double here, double after) {
        if (!Double.isFinite(before) || !Double.isFinite(here) || !Double.isFinite(after)) return 0;
        boolean forward = after <= here + 0.5;
        boolean backward = before <= here + 0.5;
        if (!forward && !backward) return 0;
        if (!forward) return -1;
        if (!backward) return 1;
        return before + 0.5 < after ? -1 : 1;
    }
    public static int waterY(float normalized, int scale) {
        return (int)Math.floor((double)normalized * scale + 0.001);
    }
    public static boolean usableVector(double dx, double dz) {
        return Double.isFinite(dx) && Double.isFinite(dz) && dx * dx + dz * dz > 0.01;
    }
}
