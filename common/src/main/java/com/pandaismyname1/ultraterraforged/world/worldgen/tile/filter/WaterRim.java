package com.pandaismyname1.ultraterraforged.world.worldgen.tile.filter;

import com.pandaismyname1.ultraterraforged.world.worldgen.cell.Cell;
import com.pandaismyname1.ultraterraforged.world.worldgen.heightmap.Levels;
import com.pandaismyname1.ultraterraforged.world.worldgen.terrain.TerrainType;
import com.pandaismyname1.ultraterraforged.world.worldgen.tile.Tile;

/**
 * Bounds raised water against the finished banks after landforms, erosion and smoothing: dry ground right beside
 * it that's lower than the water, where a bank was worn down, is raised to the water's surface, and single spots of
 * the bed left standing out of the water are sunk back under it.
 */
public record WaterRim(Levels levels) implements Filter {
    private static final int MAX_FILL = 2;

    // Remove only truly isolated surface pools; diagonal river connections count.
    private void removeSingleCellPools(Tile tile, int size) {
        java.util.BitSet isolated = new java.util.BitSet();
        for (int z = 1; z < size - 1; z++) for (int x = 1; x < size - 1; x++) {
            Cell cell = tile.getCellRaw(x, z);
            if (!this.wet(cell) && cell.height >= this.levels.water) continue;
            boolean joined = false;
            for (int dz = -1; dz <= 1 && !joined; dz++) for (int dx = -1; dx <= 1; dx++) {
                if (dx == 0 && dz == 0) continue;
                Cell next = tile.getCellRaw(x + dx, z + dz);
                if (this.wet(next) || next.height < this.levels.water) { joined = true; break; }
            }
            if (!joined) isolated.set(z * size + x);
        }
        for (int i = isolated.nextSetBit(0); i >= 0; i = isolated.nextSetBit(i + 1)) {
            int x = i % size, z = i / size;
            Cell cell = tile.getCellRaw(x, z);
            float rim = Float.MAX_VALUE;
            for (int dz = -1; dz <= 1; dz++) for (int dx = -1; dx <= 1; dx++) {
                if (dx != 0 || dz != 0) rim = Math.min(rim, tile.getCellRaw(x + dx, z + dz).height);
            }
            cell.height = Math.max(cell.height, Math.min(rim, Math.max(this.levels.water, cell.waterLevel)));
            cell.waterLevel = 0;
        }
    }

	@Override
	public void apply(Tile tile, int regionX, int regionZ, int iterationsPerChunks) {
		int size = tile.getBlockSize().total();
        this.removeSingleCellPools(tile, size);
		for (int z = 0; z < size; z++) {
			for (int x = 0; x < size; x++) {
				Cell cell = tile.getCellRaw(x, z);
				int y = this.levels.scale(cell.height);
				if (cell.waterLevel > 0.0F && !this.wet(cell) && x > 0 && z > 0 && x < size - 1 && z < size - 1) {
					int around = this.wetCount(tile.getCellRaw(x - 1, z)) + this.wetCount(tile.getCellRaw(x + 1, z)) + this.wetCount(tile.getCellRaw(x, z - 1)) + this.wetCount(tile.getCellRaw(x, z + 1));
					if (around >= 3 && cell.height - cell.waterLevel <= MAX_FILL * this.levels.unit) {
						cell.height = Math.min(cell.height, cell.waterLevel - this.levels.unit);
						continue;
					}
				}
				// wet ground, and ground under the sea, stay as they are; so does the lip of a cirque, where its lake spills
				// over down the mountain
				if (this.wet(cell) || y < this.levels.waterY || cell.terrain == TerrainType.CIRQUE) {
					continue;
				}
				float water = 0.0F;
				if (x > 0) water = Math.max(water, this.surface(tile.getCellRaw(x - 1, z)));
				if (x < size - 1) water = Math.max(water, this.surface(tile.getCellRaw(x + 1, z)));
				if (z > 0) water = Math.max(water, this.surface(tile.getCellRaw(x, z - 1)));
				if (z < size - 1) water = Math.max(water, this.surface(tile.getCellRaw(x, z + 1)));
				if (y < this.levels.scale(water) && water - (Float.isNaN(cell.riverBankBase) ? cell.height : cell.riverBankBase) <= MAX_FILL * this.levels.unit + 1.0E-6F) {
					cell.height = water;
				}
			}
		}
	}

	private boolean wet(Cell cell) {
		return cell.waterLevel > 0.0F && this.levels.scale(cell.height) < this.levels.scale(cell.waterLevel);
	}

	private int wetCount(Cell cell) {
		return this.wet(cell) ? 1 : 0;
	}

	// the water surface over a cell, or 0 if it's dry
	private float surface(Cell cell) {
		return this.wet(cell) ? cell.waterLevel : 0.0F;
	}
}
