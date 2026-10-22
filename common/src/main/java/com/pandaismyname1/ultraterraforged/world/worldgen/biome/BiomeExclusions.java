package com.pandaismyname1.ultraterraforged.world.worldgen.biome;

import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;

/** TNK3 natural-generation policy. Registrations, blocks and saved chunks are retained. */
public final class BiomeExclusions {
    public static final Set<ResourceLocation> EXCLUDED = Set.of(
        id("biomesoplenty:cold_desert"), id("biomesoplenty:field"),
        id("biomesoplenty:forested_field"), id("biomesoplenty:gravel_beach"),
        id("biomesoplenty:highland"), id("biomesoplenty:marsh"),
        id("biomesoplenty:moor"), id("biomesoplenty:muskeg"),
        id("biomesoplenty:prairie"), id("biomesoplenty:pumpkin_patch"),
        id("biomesoplenty:rocky_shrubland"), id("biomesoplenty:shrubland"),
        id("biomesoplenty:snowy_fir_clearing"), id("biomesoplenty:tundra"),
        id("biomesoplenty:volcanic_plains"), id("biomesoplenty:volcano"), id("biomesoplenty:wasteland"),
        id("regions_unexplored:shrubland"), id("regions_unexplored:grassland"),
        id("regions_unexplored:prairie"), id("regions_unexplored:gravel_beach"),
        id("regions_unexplored:grassy_beach"), id("regions_unexplored:pumpkin_fields"), id("regions_unexplored:dry_bushland")
    );

    private BiomeExclusions() {}

    /** One stable choice per TNK climate region, preserving most meadows. */
    public static boolean promoteMeadow(float biomeRegionId) {
        return biomeRegionId < 0.4F;
    }

    private static ResourceLocation id(String name) {
        return ResourceLocation.parse(name);
    }

    public static boolean excludes(Holder<Biome> biome) {
        return biome.unwrapKey().map(key -> EXCLUDED.contains(key.location())).orElse(false);
    }

    /** Build a private search tree: TerraBlender must not add its regions back to the fallback. */
    public static Climate.ParameterList<Holder<Biome>> fallback(Climate.ParameterList<Holder<Biome>> vanilla) {
        var allowed = vanilla.values().stream().filter(pair -> !excludes(pair.getSecond())).toList();
        if (allowed.isEmpty()) {
            throw new IllegalStateException("TNK3 biome exclusions left no Overworld fallback biomes");
        }
        return new Climate.ParameterList<>(allowed);
    }

    public static Holder<Biome> select(Holder<Biome> selected, Climate.ParameterList<Holder<Biome>> fallback,
                                      java.util.function.Supplier<Climate.TargetPoint> climate) {
        // Inactive samplers and allowed biomes never perform a second climate lookup.
        return fallback != null && excludes(selected) ? fallback.findValue(climate.get()) : selected;
    }
}
