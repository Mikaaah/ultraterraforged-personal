package com.pandaismyname1.ultraterraforged.world.worldgen.tile.filter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import org.junit.jupiter.api.Test;
import com.pandaismyname1.ultraterraforged.world.worldgen.cell.Cell;
import com.pandaismyname1.ultraterraforged.world.worldgen.heightmap.Levels;
import com.pandaismyname1.ultraterraforged.world.worldgen.tile.*;
import com.pandaismyname1.ultraterraforged.world.worldgen.terrain.TerrainType;

class WaterRimSafetyTest {
    @org.junit.jupiter.api.BeforeAll static void bootstrap() { com.pandaismyname1.ultraterraforged.test.TestBootstrap.init(); }
    private final Levels levels = new Levels(384, 63);
    private Cell[][] cells;
    private Tile tile(int bed, int water, int bank) {
        cells = new Cell[32][32];
        for(int z=0; z<32; z++) for(int x=0; x<32; x++) {
            Cell cell = cells[z][x] = new Cell();
            boolean river = x >= 10 && x <= 20;
            cell.height = (river ? bed : bank) / 384f;
            cell.waterLevel = river ? (water + .01f) / 384f : 0;
            cell.terrain = river ? TerrainType.RIVER : TerrainType.PLAINS;
        }
        Tile tile = mock(Tile.class);
        when(tile.getBlockSize()).thenReturn(Size.make(32,0));
        when(tile.getCellRaw(anyInt(),anyInt())).thenAnswer(a -> cells[a.<Integer>getArgument(1)][a.<Integer>getArgument(0)]);
        return tile;
    }
    @Test void lowFinalTerrainCannotBecomeATallRiverWall() {
        Tile tile = tile(82, 100, 70);
        new WaterRim(levels).apply(tile,0,0,1);
        for(int z=1; z<31; z++) for(int x=0; x<32; x++) {
            Cell cell = cells[z][x];
            if(x < 10 || x > 20) assertTrue(levels.scale(cell.height) <= 72, "bank repair exceeded two blocks");
        }
        assertEquals(100, levels.scale(cells[16][10].waterLevel), "a local bank check must not erase a connected river profile");
    }
    @Test void properlyContainedMountainRiverKeepsItsWater() {
        Tile tile = tile(94,100,104);
        new WaterRim(levels).apply(tile,0,0,1);
        assertEquals(100,levels.scale(cells[16][16].waterLevel));
        assertEquals(104,levels.scale(cells[16][9].height));
    }
    @Test void smallWornBankStillGetsRepaired() {
        Tile tile = tile(94,100,99);
        new WaterRim(levels).apply(tile,0,0,1);
        assertEquals(100,levels.scale(cells[16][9].height));
    }
    @Test void isolatedOneBlockPoolIsFilledButDiagonalStreamSurvives() {
        Tile tile = tile(94,100,104);
        for (Cell[] row : cells) for (Cell cell : row) { cell.height = 104 / 384f; cell.waterLevel = 0; }
        cells[6][6].height = 61 / 384f;
        cells[8][8].height = 99 / 384f;
        cells[8][8].waterLevel = 100.01f / 384f;
        cells[16][16].height = cells[17][17].height = 99 / 384f;
        cells[16][16].waterLevel = cells[17][17].waterLevel = 100.01f / 384f;
        new WaterRim(levels).apply(tile,0,0,1);
        assertEquals(62, levels.scale(cells[6][6].height), "isolated sea-level pit must also be filled");
        assertEquals(0, cells[8][8].waterLevel);
        assertEquals(100, levels.scale(cells[8][8].height));
        assertEquals(100, levels.scale(cells[16][16].waterLevel));
        assertEquals(100, levels.scale(cells[17][17].waterLevel));
    }

}
