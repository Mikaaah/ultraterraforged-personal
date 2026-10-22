package com.pandaismyname1.ultraterraforged.world.worldgen.tile.filter;

import com.pandaismyname1.ultraterraforged.world.worldgen.cell.Cell;
import com.pandaismyname1.ultraterraforged.world.worldgen.heightmap.Heightmap;
import com.pandaismyname1.ultraterraforged.world.worldgen.heightmap.Levels;
import com.pandaismyname1.ultraterraforged.world.worldgen.tile.Size;
import com.pandaismyname1.ultraterraforged.world.worldgen.tile.Tile;

public record PostProcessing(Heightmap heightmap, Levels levels) implements Filter {
     
	@Override
    public void apply(Tile tile, int seedX, int seedZ, int iterations) {
        Size chunkSize = tile.getChunkSize();
        int chunkTotal = chunkSize.total();
        for(int chunkX = 0; chunkX < chunkTotal; chunkX++) {
            for(int chunkZ = 0; chunkZ < chunkTotal; chunkZ++) {
                Tile.Chunk chunk = tile.getChunkWriter(chunkX, chunkZ);
            	
	        	for (int dz = 0; dz < 16; dz++) {
	        		for (int dx = 0; dx < 16; dx++) {
	        			Cell cell = chunk.getCell(dx, dz);
	        			// continentalness, as WorldLookup gives a column it computes itself: without it a tile's cells
	        			// all read as near-inland, and chunks made from them got other biomes than /locate found
	        			this.heightmap.applyPost(cell, chunk.getBlockX() + dx, chunk.getBlockZ() + dz);
	        			chunk.updateHighestPoint(cell);
	        		}
	        	}
            }
        }
    }
}
