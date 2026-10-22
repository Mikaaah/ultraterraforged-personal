package com.pandaismyname1.ultraterraforged.world.worldgen.surface;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;
import com.mojang.serialization.JsonOps;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.SurfaceRules;
import com.pandaismyname1.ultraterraforged.test.TestBootstrap;

class ModSurfaceRulesTest {
    @BeforeAll static void boot() { TestBootstrap.init(); }
    @Test void nativeSurfaceAdaptationPreservesBiomeAndMaterialAndAddsRealGround() {
        var source=SurfaceRules.ifTrue(SurfaceRules.isBiome(Biomes.DESERT),SurfaceRules.ifTrue(
                SurfaceRules.abovePreliminarySurface(),SurfaceRules.state(Blocks.SAND.defaultBlockState())));
        var result=ModSurfaceRules.adaptNative(source);
        var json=SurfaceRules.RuleSource.CODEC.encodeStart(JsonOps.INSTANCE,result).getOrThrow().toString();
        assertTrue(json.contains("minecraft:desert"),json);
        assertTrue(json.contains("minecraft:sand"),json);
        assertTrue(json.contains("above_preliminary_surface"),json);
        assertTrue(json.contains("ultraterraforged:near_ground"),json);
        assertTrue(json.contains("ultraterraforged:any"),json);
        assertTrue(SurfaceRules.RuleSource.CODEC.parse(JsonOps.INSTANCE,
                SurfaceRules.RuleSource.CODEC.encodeStart(JsonOps.INSTANCE,result).getOrThrow()).result().isPresent());
    }
    @Test void optionalModsAbsentLeavesTheOriginalRulesUntouched() {
        try (var mods=org.mockito.Mockito.mockStatic(
                com.pandaismyname1.ultraterraforged.platform.ModLoaderUtil.class)) {
            mods.when(()->com.pandaismyname1.ultraterraforged.platform.ModLoaderUtil.isLoaded(
                    org.mockito.ArgumentMatchers.anyString())).thenReturn(false);
            var original=SurfaceRules.state(Blocks.STONE.defaultBlockState());
            assertSame(original,ModSurfaceRules.wrap(original,org.mockito.Mockito.mock(net.minecraft.core.RegistryAccess.class)));
        }
    }
}
