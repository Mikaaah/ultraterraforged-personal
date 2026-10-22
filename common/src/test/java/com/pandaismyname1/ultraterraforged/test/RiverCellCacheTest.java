package com.pandaismyname1.ultraterraforged.test;

import com.pandaismyname1.ultraterraforged.world.worldgen.GeneratorContext;
import com.pandaismyname1.ultraterraforged.world.worldgen.cell.Cell;
import com.pandaismyname1.ultraterraforged.world.worldgen.heightmap.Levels;
import com.pandaismyname1.ultraterraforged.world.worldgen.heightmap.WorldLookup;
import com.pandaismyname1.ultraterraforged.world.worldgen.terrain.TerrainType;
import com.pandaismyname1.ultraterraforged.world.worldgen.terrain.populator.RiverPopulator;
import com.pandaismyname1.ultraterraforged.world.worldgen.tile.Tile;
import com.pandaismyname1.ultraterraforged.world.worldgen.tile.TileCache;
import com.pandaismyname1.ultraterraforged.world.worldgen.rivermap.RiverDataCache;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RiverCellCacheTest {
    @Test void compactSnapshotsSurviveTileReuseAndRememberDryColumns() {
        RiverDataCache cache = new RiverDataCache();
        Tile.Chunk chunk = mock(Tile.Chunk.class);
        Cell river = new Cell();
        river.terrain = TerrainType.RIVER;
        river.riverCarver = mock(RiverPopulator.class);
        var carver = river.riverCarver;
        river.riverT = .25f;
        river.height = .2f;
        river.waterLevel = .3f;
        when(chunk.getCell(anyInt(), anyInt())).thenAnswer(a ->
            a.<Integer>getArgument(0) == 8 && a.<Integer>getArgument(1) == 8 ? river : new Cell());
        cache.capture(-2, -3, chunk);
        river.reset(); // pooled tiles are wiped and reused after generation
        Cell out = new Cell();
        assertTrue(cache.apply(out, -24, -40));
        assertSame(carver, out.riverCarver);
        assertEquals(.25f, out.riverT);
        assertEquals(.2f, out.height);
        assertTrue(cache.apply(out, -25, -40), "known dry columns also hit the cache");
        assertNull(out.riverCarver);
        assertFalse(cache.apply(out, 1000, 1000), "unknown chunks must use the non-blocking fallback");
    }

    @Test void cachedLookupRetainsNativeRiverWithoutRecomputingTerrain() {
        GeneratorContext context = mock(GeneratorContext.class);
        context.levels = new Levels(384, 63);
        context.cache = mock(TileCache.class);
        Tile tile = mock(Tile.class);
        Cell source = new Cell();
        source.terrain = TerrainType.RIVER;
        source.riverCarver = mock(RiverPopulator.class);
        source.riverT = .375f;
        source.waterLevel = .3f;
        when(context.cache.provideIfPresent(0, 0)).thenReturn(tile);
        when(tile.lookup(8, 8)).thenReturn(source);
        WorldLookup lookup = spy(new WorldLookup(context));
        Cell result = new Cell();
        assertTrue(lookup.apply(result, 8, 8));
        assertSame(source.riverCarver, result.riverCarver);
        assertEquals(source.riverT, result.riverT);
        verify(lookup, never()).compute(any(), anyInt(), anyInt());
        result.reset();
        assertNull(result.riverCarver, "pooled cells must release their previous river");
        assertEquals(0, result.riverT);
    }
}
