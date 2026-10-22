import nl.townsandkingdoms.terraforged.compat.streams.FlowRules;

public class FlowRulesTest {
    private static int checks;
    private static void check(boolean value) { checks++; if (!value) throw new AssertionError("check " + checks); }
    public static void main(String[] args) {
        check(!FlowRules.uphill(120, 120, 120));
        check(!FlowRules.uphill(123, 120, 117));
        check(FlowRules.uphill(117, 120, 120));
        check(FlowRules.uphill(120, 120, 123));
        check(FlowRules.waterY((120.01f / 512), 512) == 120);
        check(FlowRules.waterY(62f / 512, 512) == 62);
        check(FlowRules.waterY(-20.2f / 512, 512) == -21);
        check(FlowRules.usableVector(1, 0));
        check(FlowRules.usableVector(-1, -1));
        check(!FlowRules.usableVector(0, 0));
        check(!FlowRules.usableVector(Double.NaN, 1));
        check(!FlowRules.usableVector(Double.POSITIVE_INFINITY, 1));
        check(FlowRules.downhill(123, 120, 117) == 1);
        check(FlowRules.downhill(117, 120, 123) == -1);
        check(FlowRules.downhill(120, 120, 120) == 1);
        check(FlowRules.downhill(123, 120, 123) == 0);
        check(FlowRules.downhill(Double.NaN, 120, 117) == 0);
        check(FlowRules.downhill(117, 120, 120) == -1);
        System.out.println(checks + " flow rules passed");
    }
}
