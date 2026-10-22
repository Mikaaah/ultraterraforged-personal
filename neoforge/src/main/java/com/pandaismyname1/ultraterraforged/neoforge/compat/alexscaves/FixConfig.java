package com.pandaismyname1.ultraterraforged.neoforge.compat.alexscaves;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class FixConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLE_PLACEMENT;
    public static final ModConfigSpec.BooleanValue PROTECT_CAVES;
    public static final ModConfigSpec.BooleanValue FIX_CHAMBERS;
    public static final ModConfigSpec.BooleanValue VERIFY_LOCATOR;
    public static final ModConfigSpec.BooleanValue ADAPT_INLAND_FILTERS;
    public static final ModConfigSpec.IntValue MIN_CAVE_Y;
    public static final ModConfigSpec.IntValue MAX_CAVE_Y;
    public static final ModConfigSpec.DoubleValue DEFAULT_MAX_DEPTH;

    static {
        var b = new ModConfigSpec.Builder();
        b.comment("Only active UltraTerraForged Overworld samplers use this profile. Custom Alex's Caves JSON ranges remain respected.").push("compatibility");
        ENABLE_PLACEMENT = b.define("enableBiomePlacementFix", true);
        FIX_CHAMBERS = b.comment("Keep UltraTerraForged's global surface-structure rule from vetoing Alex's underground chamber generators. Preserve their Voronoi placement and require the matching cave biome.")
                .define("fixCaveChamberPlacement", true);
        ADAPT_INLAND_FILTERS = b.comment("Adapt the port's original inland/ocean continentalness bounds to actual UltraTerraForged terrain. Other custom continentalness ranges are unchanged.")
                .define("adaptDefaultContinentalnessFilters", true);
        DEFAULT_MAX_DEPTH = b.comment("Replacement upper noise-depth bound only for the original Alex's Caves [0.05, 2.0] depth filter. Does not change terrain density or custom ranges.")
                .defineInRange("defaultDepthUpperBound", 8.0, 2.0, 64.0);
        MIN_CAVE_Y = b.comment("Keep cave biome assignment near the port's structure chamber band; this is a block Y coordinate, not climate depth.")
                .defineInRange("minCaveBiomeY", -64, -2032, 2031);
        MAX_CAVE_Y = b.defineInRange("maxCaveBiomeY", 128, -2032, 2031);
        PROTECT_CAVES = b.comment("Prevent the extra UltraTerraForged carving/lining/decorating pass from writing inside tagged Alex's Caves biomes. Ordinary noise caves are unchanged.")
                .define("protectAlexCaveBiomes", true);
        VERIFY_LOCATOR = b.comment("In an active TNKTerraForged world, verify /locateacbiome results against the biome source and include the underground Y coordinate.")
                .define("verifyLocateACBiome", true);
        b.pop();
        SPEC = b.build();
    }
    private FixConfig() {}
}
