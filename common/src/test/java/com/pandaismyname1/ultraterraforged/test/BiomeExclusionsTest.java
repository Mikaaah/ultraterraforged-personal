package com.pandaismyname1.ultraterraforged.test;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import com.mojang.datafixers.util.Pair;
import com.pandaismyname1.ultraterraforged.world.worldgen.biome.BiomeExclusions;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import org.junit.jupiter.api.Test;

class BiomeExclusionsTest {
    private static Holder<Biome> biome(String name) {
        return Holder.Reference.createStandAlone(new HolderOwner<Biome>() {},
            ResourceKey.create(Registries.BIOME, ResourceLocation.parse(name)));
    }

    @Test
    void meadowPromotionUsesStableRegionIdentity() {
        assertTrue(BiomeExclusions.promoteMeadow(0.1F));
        assertFalse(BiomeExclusions.promoteMeadow(0.6F));
    }

    @Test
    void finalPackListKeepsBopGrasslandAndOtherDimensions() {
        assertEquals(24, BiomeExclusions.EXCLUDED.size());
        assertEquals(17, BiomeExclusions.EXCLUDED.stream().filter(id -> id.getNamespace().equals("biomesoplenty")).count());
        assertEquals(7, BiomeExclusions.EXCLUDED.stream().filter(id -> id.getNamespace().equals("regions_unexplored")).count());
        for (var id : BiomeExclusions.EXCLUDED) assertTrue(BiomeExclusions.excludes(biome(id.toString())), id.toString());
        for (String kept : List.of("biomesoplenty:grassland", "biomesoplenty:erupting_inferno",
                "regions_unexplored:redwoods", "minecraft:plains", "alexscaves:toxic_caves")) {
            assertFalse(BiomeExclusions.excludes(biome(kept)), kept);
        }
    }

    @Test
    void excludedSelectionUsesClimateAndInactiveSamplersAreUntouched() {
        var cold = biome("minecraft:snowy_plains");
        var warm = biome("minecraft:desert");
        var blocked = biome("biomesoplenty:volcano");
        var fallback = BiomeExclusions.fallback(new Climate.ParameterList<>(List.of(
            Pair.of(Climate.parameters(-1, 0, 0, 0, 0, 0, 0), cold),
            Pair.of(Climate.parameters(1, 0, 0, 0, 0, 0, 0), warm),
            Pair.of(Climate.parameters(0, 0, 0, 0, 0, 0, 0), blocked))));
        assertEquals(2, fallback.values().size());
        assertSame(cold, BiomeExclusions.select(blocked, fallback, () -> Climate.target(-1, 0, 0, 0, 0, 0)));
        assertSame(warm, BiomeExclusions.select(blocked, fallback, () -> Climate.target(1, 0, 0, 0, 0, 0)));
        assertSame(blocked, BiomeExclusions.select(blocked, null, () -> { throw new AssertionError("inactive sampler"); }));
        assertSame(cold, BiomeExclusions.select(cold, fallback, () -> { throw new AssertionError("allowed biome"); }));
    }

    @Test
    void entirelyBlockedFallbackFailsClearly() {
        var blocked = biome("regions_unexplored:grassland");
        assertThrows(IllegalStateException.class, () -> BiomeExclusions.fallback(new Climate.ParameterList<>(
            List.of(Pair.of(Climate.parameters(0, 0, 0, 0, 0, 0, 0), blocked)))));
    }
}
