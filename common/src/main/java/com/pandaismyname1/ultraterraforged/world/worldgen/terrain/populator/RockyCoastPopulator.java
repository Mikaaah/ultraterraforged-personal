package com.pandaismyname1.ultraterraforged.world.worldgen.terrain.populator;

import com.pandaismyname1.ultraterraforged.world.worldgen.cell.Cell;
import com.pandaismyname1.ultraterraforged.world.worldgen.cell.CellPopulator;
import com.pandaismyname1.ultraterraforged.world.worldgen.heightmap.Levels;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Noise;
import com.pandaismyname1.ultraterraforged.world.worldgen.terrain.TerrainType;

/** A sloping shelf with occasional raised rocky headlands, rather than a paved strip at sea level. */
public record RockyCoastPopulator(Levels levels, Noise relief, Noise headlands) implements CellPopulator {
    @Override
    public void apply(Cell cell, float x, float z) {
        float rock = Math.max(0.0F, Math.min(1.0F, (this.headlands.compute(x, z, 0) - 0.54F) / 0.22F));
        rock = rock * rock * (3.0F - 2.0F * rock);
        cell.terrain = TerrainType.COAST;
        cell.height = Math.max(0.0F, this.levels.water(-2) + this.relief.compute(x, z, 0)
                + rock * this.levels.scale(18));
        // Vanilla distinguishes the rocky shore from the beach using erosion. Keep the climate and height together.
        cell.erosion = 0.15F - rock * 0.95F;
        cell.weirdness = rock * 0.45F;
    }

    @Override
    public RockyCoastPopulator mapNoise(Noise.Visitor visitor) {
        return new RockyCoastPopulator(this.levels, this.relief.mapAll(visitor), this.headlands.mapAll(visitor));
    }
}
