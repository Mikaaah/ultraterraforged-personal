package com.pandaismyname1.ultraterraforged.world.worldgen.terrain.populator;

import com.pandaismyname1.ultraterraforged.world.worldgen.cell.Cell;
import com.pandaismyname1.ultraterraforged.world.worldgen.cell.CellPopulator;
import com.pandaismyname1.ultraterraforged.world.worldgen.heightmap.Levels;
import com.pandaismyname1.ultraterraforged.world.worldgen.landform.Landform;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.NoiseUtil;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Noise;
import com.pandaismyname1.ultraterraforged.world.worldgen.terrain.TerrainType;

/** Rare isolated islands in deep water. The above-water core is assigned vanilla's Mushroom Fields climate. */
public record MushroomIslands(CellPopulator ocean, Levels levels, float deepOcean, int seed, Noise shore) implements CellPopulator {
    private static final int GRID = 2048;

    @Override
    public void apply(Cell cell, float x, float z) {
        this.ocean.apply(cell, x, z);
        // No island can be placed on a continental shelf or alongside a mainland shore.
        if (cell.continentEdge >= this.deepOcean - 0.03F) return;
        int gridX = NoiseUtil.floor(x / GRID);
        int gridZ = NoiseUtil.floor(z / GRID);
        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                int gx = gridX + dx;
                int gz = gridZ + dz;
                if (Landform.random(this.seed, gx, gz, 0) >= 0.38F) continue;
                float cx = (gx + 0.15F + 0.7F * Landform.random(this.seed, gx, gz, 1)) * GRID;
                float cz = (gz + 0.15F + 0.7F * Landform.random(this.seed, gx, gz, 2)) * GRID;
                float radius = 170.0F + 85.0F * Landform.random(this.seed, gx, gz, 3);
                float wobble = (this.shore.compute(x, z, 0) - 0.5F) * 35.0F;
                float distance = (float) Math.hypot(x - cx, z - cz) + wobble;
                if (distance >= radius) continue;
                float rise = Landform.smoothstep(1.0F - distance / radius, 0.0F, 1.0F);
                // A long submerged slope rises into a low island. Blending
                // twice made a steep rim and a broad, unnaturally high plateau.
                float islandHeight = this.levels.water(18);
                float height = cell.height + (islandHeight - cell.height) * rise;
                if (height > cell.height) cell.height = height;
                if (cell.height >= this.levels.water(2)) {
                    cell.terrain = TerrainType.MUSHROOM_FIELDS;
                    cell.erosion = 0.4F;
                    cell.weirdness = 0.2F;
                }
            }
        }
    }

    @Override
    public MushroomIslands mapNoise(Noise.Visitor visitor) {
        return new MushroomIslands(this.ocean.mapNoise(visitor), this.levels, this.deepOcean, this.seed,
                this.shore.mapAll(visitor));
    }
}
