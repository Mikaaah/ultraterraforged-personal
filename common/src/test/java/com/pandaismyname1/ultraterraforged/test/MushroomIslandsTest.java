package com.pandaismyname1.ultraterraforged.test;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.pandaismyname1.ultraterraforged.world.worldgen.biome.Continentalness;
import com.pandaismyname1.ultraterraforged.world.worldgen.cell.Cell;
import com.pandaismyname1.ultraterraforged.world.worldgen.heightmap.Heightmap;
import com.pandaismyname1.ultraterraforged.world.worldgen.heightmap.Levels;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Noises;
import com.pandaismyname1.ultraterraforged.world.worldgen.terrain.TerrainType;
import com.pandaismyname1.ultraterraforged.world.worldgen.terrain.populator.MushroomIslands;
import com.mojang.datafixers.util.Pair;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.OverworldBiomeBuilder;

class MushroomIslandsTest {
    @BeforeAll static void bootstrap() { TestBootstrap.init(); }

    @Test
    @SuppressWarnings("unchecked")
    void isolatedDeepOceanIslandsReachLandAndKeepMushroomBiome() throws ReflectiveOperationException {
        List<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> points = new ArrayList<>();
        Method addBiomes = OverworldBiomeBuilder.class.getDeclaredMethod("addBiomes", Consumer.class);
        addBiomes.setAccessible(true);
        addBiomes.invoke(new OverworldBiomeBuilder(), (Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>>) points::add);
        Climate.ParameterList<ResourceKey<Biome>> biomes = new Climate.ParameterList<>(points);
        Levels levels = new Levels(384, 63);
        MushroomIslands islands = new MushroomIslands((cell, x, z) -> {
            cell.height = levels.water(-25);
            cell.terrain = TerrainType.DEEP_OCEAN;
        }, levels, 0.1F, 12345, Noises.constant(0.5F));
        Heightmap heightmap = new Heightmap(null, null, null, null, null, levels, null, 1, null, null, null);
        Cell cell = new Cell();
        int dry = 0;
        for (int x = -4096; x <= 4096; x += 64) {
            for (int z = -4096; z <= 4096; z += 64) {
                cell.reset();
                cell.continentEdge = 0.0F;
                islands.apply(cell, x, z);
                if (cell.terrain != TerrainType.MUSHROOM_FIELDS) continue;
                assertTrue(cell.height >= levels.water(2));
                heightmap.applyPost(cell, x, z);
                assertTrue(cell.continentalness >= Continentalness.MUSHROOM_FIELDS.min());
                assertTrue(cell.continentalness < Continentalness.DEEP_OCEAN.min());
                assertEquals(Biomes.MUSHROOM_FIELDS, biomes.findValue(Climate.target(0.5F, 0.5F,
                        cell.continentalness, cell.erosion, 0.0F, cell.weirdness)));
                dry++;
            }
        }
        assertTrue(dry > 10, "mushroom islands never rise out of deep water");
        cell.reset();
        cell.continentEdge = 0.2F;
        islands.apply(cell, 0, 0);
        assertEquals(TerrainType.DEEP_OCEAN, cell.terrain, "islands must not spread onto ordinary shelves");
    }
}
