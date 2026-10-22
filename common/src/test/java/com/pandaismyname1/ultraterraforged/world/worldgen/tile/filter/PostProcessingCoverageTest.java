package com.pandaismyname1.ultraterraforged.world.worldgen.tile.filter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import com.pandaismyname1.ultraterraforged.concurrent.SimpleResource;
import com.pandaismyname1.ultraterraforged.world.worldgen.cell.Cell;
import com.pandaismyname1.ultraterraforged.world.worldgen.heightmap.*;
import com.pandaismyname1.ultraterraforged.world.worldgen.tile.*;

class PostProcessingCoverageTest {
    @org.junit.jupiter.api.BeforeAll static void bootstrap() { com.pandaismyname1.ultraterraforged.test.TestBootstrap.init(); }
    @Test void processesEveryCellOnceIncludingBorderAtNegativeWorldCoordinates() {
        Size blocks = Size.blocks(1,1), chunks = Size.chunks(1,1);
        Cell[] cells = new Cell[blocks.arraySize()];
        Arrays.setAll(cells, ignored -> new Cell());
        Tile tile = new Tile(-2,3,1,1,blocks,chunks,
            new SimpleResource<>(cells, ignored -> {}),
            new SimpleResource<>(new Tile.Chunk[chunks.arraySize()], ignored -> {}));
        Heightmap heightmap = mock(Heightmap.class);
        Set<Cell> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        doAnswer(a -> {
            Cell cell = a.getArgument(0);
            int x = Math.round(a.<Float>getArgument(1)), z = Math.round(a.<Float>getArgument(2));
            assertTrue(visited.add(cell), "cell processed twice");
            assertSame(tile.getCellRaw(x - tile.getBlockX() + blocks.border(), z - tile.getBlockZ() + blocks.border()), cell);
            cell.height = 0.5F;
            return null;
        }).when(heightmap).applyPost(any(Cell.class), anyFloat(), anyFloat());
        new PostProcessing(heightmap,new Levels(384,63)).apply(tile,0,0,1);
        assertEquals(cells.length,visited.size(), "border cells missed their final biome classification");
        for (int z=0;z<chunks.total();z++) for(int x=0;x<chunks.total();x++)
            assertEquals(0.5F,tile.getChunkWriter(x,z).getHighestPoint());
        tile.close();
    }
}
