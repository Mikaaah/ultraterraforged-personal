package com.pandaismyname1.ultraterraforged.world.worldgen.rivermap;

import com.pandaismyname1.ultraterraforged.test.TestBootstrap;
import com.pandaismyname1.ultraterraforged.world.worldgen.cell.Cell;
import com.pandaismyname1.ultraterraforged.world.worldgen.GeneratorContext;
import com.pandaismyname1.ultraterraforged.world.worldgen.heightmap.Levels;
import com.pandaismyname1.ultraterraforged.world.worldgen.tile.Tile;
import com.pandaismyname1.ultraterraforged.world.worldgen.tile.TileCache;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RaisedWaterPlacementTest {
    @BeforeAll static void bootstrap() { TestBootstrap.init(); }

    private static class World {
        final ChunkAccess chunk = mock(ChunkAccess.class);
        final Tile.Chunk cells = mock(Tile.Chunk.class);
        final Map<BlockPos, BlockState> writes = new HashMap<>();
        final Set<BlockPos> updates = new HashSet<>();
        final Levels levels = new Levels(384, 63);
        World(int bed, int surface, boolean enclosed, int cx, int cz) {
            var at = new ChunkPos(cx, cz);
            when(chunk.getPos()).thenReturn(at);
            when(chunk.getMinBuildHeight()).thenReturn(-64);
            when(cells.getCell(anyInt(), anyInt())).thenAnswer(a -> {
                Cell cell = new Cell();
                if (a.<Integer>getArgument(0) == 8 && a.<Integer>getArgument(1) == 8) {
                    cell.waterLevel = (surface + 0.01f) / levels.worldHeight;
                }
                return cell;
            });
            when(chunk.getBlockState(any())).thenAnswer(a -> {
                BlockPos p = a.getArgument(0);
                assertEquals(cx, p.getX() >> 4, "cross-chunk read");
                assertEquals(cz, p.getZ() >> 4, "cross-chunk read");
                if (writes.containsKey(p)) return writes.get(p);
                boolean column = (p.getX() & 15) == 8 && (p.getZ() & 15) == 8;
                return p.getY() <= bed || (enclosed && !column) ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState();
            });
            when(chunk.setBlockState(any(), any(), eq(false))).thenAnswer(a -> {
                writes.put(a.<BlockPos>getArgument(0).immutable(), a.getArgument(1));
                return Blocks.AIR.defaultBlockState();
            });
            doAnswer(a -> { updates.add(a.<BlockPos>getArgument(0).immutable()); return null; })
                .when(chunk).markPosForPostprocessing(any());
        }
        void fill() {
            GeneratorContext context = mock(GeneratorContext.class);
            context.levels = levels;
            context.cache = mock(TileCache.class);
            Tile tile = mock(Tile.class);
            var at = chunk.getPos();
            when(context.cache.provideAtChunk(at.x, at.z)).thenReturn(tile);
            when(tile.getChunkReader(at.x, at.z)).thenReturn(cells);
            RaisedWater.fill(chunk, context);
        }
    }

    @Test void unsupportedRaisedWaterDoesNotCreateFloatingSourceSlab() {
        World world = new World(65, 100, false, 0, 0);
        world.fill();
        assertTrue(world.writes.isEmpty(), "air gap below maximum fill depth must not get floating sources");
    }

    @Test void entireExposedFaceGetsFluidUpdatesIncludingNegativeChunks() {
        World world = new World(92, 100, false, -2, -3);
        world.fill();
        assertEquals(8, world.writes.size());
        assertEquals(world.writes.keySet(), world.updates, "lower exposed sources need updates too");
    }

    @Test void enclosedPoolDoesNotScheduleUnneededFluidWork() {
        World world = new World(92, 100, true, 0, 0);
        world.fill();
        assertEquals(8, world.writes.size());
        assertTrue(world.updates.isEmpty());
    }
}
