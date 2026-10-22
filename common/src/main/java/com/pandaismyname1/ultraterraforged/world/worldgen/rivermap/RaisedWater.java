package com.pandaismyname1.ultraterraforged.world.worldgen.rivermap;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import com.pandaismyname1.ultraterraforged.world.worldgen.GeneratorContext;
import com.pandaismyname1.ultraterraforged.world.worldgen.cell.Cell;
import com.pandaismyname1.ultraterraforged.world.worldgen.heightmap.Levels;
import com.pandaismyname1.ultraterraforged.world.worldgen.tile.Tile;

/**
 * Puts the water of rivers, lakes and wetlands above the sea into a chunk once its terrain is filled in, before the
 * surface is built on it: the noise only fills water up to sea level. Where the water steps down to a lower stretch
 * of river, or might meet another chunk's, it's left to flow.
 */
public final class RaisedWater {
	// water is filled at most this deep below its surface; anything deeper is a cave under the river bed
	private static final int MAX_DEPTH = 24;
	private static final int NONE = Integer.MIN_VALUE;

	private RaisedWater() {
	}

	public static void fill(ChunkAccess chunk, GeneratorContext context) {
		ChunkPos chunkPos = chunk.getPos();
		Tile.Chunk cells = context.cache.provideAtChunk(chunkPos.x, chunkPos.z).getChunkReader(chunkPos.x, chunkPos.z);
        fill(chunk, cells, context.levels);
    }

    // Kept separate so actual block placement and fluid postprocessing can be regression-tested.
    static void fill(ChunkAccess chunk, Tile.Chunk cells, Levels levels) {
        ChunkPos chunkPos = chunk.getPos();
		int[] surfaces = new int[256];
		boolean any = false;
		for (int dz = 0; dz < 16; dz++) {
			for (int dx = 0; dx < 16; dx++) {
				Cell cell = cells.getCell(dx, dz);
				int surface = cell.waterLevel > 0.0F ? levels.scale(cell.waterLevel) : NONE;
				// the noise has filled the sea already
				if (surface <= levels.waterY) {
					surface = NONE;
				}
				surfaces[dz << 4 | dx] = surface;
				any |= surface != NONE;
			}
		}
		if (!any) {
			return;
		}
		int minX = chunkPos.getMinBlockX();
		int minZ = chunkPos.getMinBlockZ();
		int bottom = chunk.getMinBuildHeight();
		BlockState water = Blocks.WATER.defaultBlockState();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int[] bottoms = new int[256];
        java.util.Arrays.fill(bottoms, NONE);
		// the top water block of each column, or NONE
		int[] tops = new int[256];
		for (int i = 0; i < 256; i++) {
			tops[i] = NONE;
			int surface = surfaces[i];
			if (surface == NONE) {
				continue;
			}
			int x = minX + (i & 15);
			int z = minZ + (i >> 4);
            // Locate a real bed before writing anything. The old top-down fill left
            // a MAX_DEPTH-high floating source-water slab when terrain was farther below.
            int floor = Math.max(bottom, surface - MAX_DEPTH);
            int bed = surface;
            while (bed >= floor) {
                BlockState state = chunk.getBlockState(pos.set(x, bed, z));
                if (!state.isAir() && state.getFluidState().isEmpty()) break;
                bed--;
            }
            if (bed < floor) continue;
            for (int y = bed + 1; y <= surface; y++) {
				BlockState state = chunk.getBlockState(pos.set(x, y, z));
				if (state.isAir()) {
					chunk.setBlockState(pos, water, false);
                    bottoms[i] = bottoms[i] == NONE ? y : Math.min(bottoms[i], y);
					tops[i] = Math.max(tops[i], y);
				} else if (state.is(Blocks.WATER)) {
                    // Pre-existing water is not an empty adjacent column.
                    bottoms[i] = bottoms[i] == NONE ? y : Math.min(bottoms[i], y);
                    tops[i] = Math.max(tops[i], y);
				} else if (state.getFluidState().isEmpty()) {
					break;
				}
			}
		}
		// water that can spill: beside a column with less or no water, or at the edge of the chunk
		for (int i = 0; i < 256; i++) {
			int top = tops[i];
			if (top == NONE) {
				continue;
			}
			int dx = i & 15;
			int dz = i >> 4;
            // Every exposed source on a face needs postprocessing, not just its top.
            // Read only this chunk; edge sources are reconciled once neighbors exist.
            for (int y = bottoms[i]; y <= top; y++) {
                boolean exposed = dx == 0 || dz == 0 || dx == 15 || dz == 15
                    || chunk.getBlockState(pos.set(minX + dx - 1, y, minZ + dz)).isAir()
                    || chunk.getBlockState(pos.set(minX + dx + 1, y, minZ + dz)).isAir()
                    || chunk.getBlockState(pos.set(minX + dx, y, minZ + dz - 1)).isAir()
                    || chunk.getBlockState(pos.set(minX + dx, y, minZ + dz + 1)).isAir();
                if (exposed) chunk.markPosForPostprocessing(pos.set(minX + dx, y, minZ + dz));
            }
		}
	}
}
