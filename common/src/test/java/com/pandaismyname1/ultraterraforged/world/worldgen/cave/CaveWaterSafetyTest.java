package com.pandaismyname1.ultraterraforged.world.worldgen.cave;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import java.util.*;
import org.junit.jupiter.api.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import com.pandaismyname1.ultraterraforged.test.TestBootstrap;
import com.pandaismyname1.ultraterraforged.world.worldgen.GeneratorContext;
import com.pandaismyname1.ultraterraforged.world.worldgen.heightmap.Levels;

class CaveWaterSafetyTest {
    @BeforeAll static void bootstrap() { TestBootstrap.init(); }

    @Test void exposedSpringDoesNotFloodOpenHillsideOrNeighboringCave() {
        ChunkAccess chunk = mock(ChunkAccess.class);
        when(chunk.getPos()).thenReturn(new ChunkPos(-2, -3));
        when(chunk.getMinBuildHeight()).thenReturn(-64);
        when(chunk.getMaxBuildHeight()).thenReturn(320);
        when(chunk.getAllStarts()).thenReturn(Map.of());
        when(chunk.getHeight(any(), anyInt(), anyInt())).thenReturn(100);
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        Set<BlockPos> updates = new HashSet<>();
        BlockPos source = new BlockPos(-23, 90, -40);
        blocks.put(source, Blocks.WATER.defaultBlockState());
        when(chunk.getBlockState(any())).thenAnswer(a -> {
            BlockPos pos = a.getArgument(0);
            assertEquals(-2, pos.getX() >> 4);
            assertEquals(-3, pos.getZ() >> 4);
            return blocks.getOrDefault(pos, pos.getY() > 100 || (pos.getX() >= -22 && pos.getY() > 70)
                    ? Blocks.AIR.defaultBlockState() : Blocks.STONE.defaultBlockState());
        });
        when(chunk.setBlockState(any(), any(), anyBoolean())).thenAnswer(a -> {
            blocks.put(a.<BlockPos>getArgument(0).immutable(), a.getArgument(1));
            return Blocks.STONE.defaultBlockState();
        });
        doAnswer(a -> { updates.add(a.<BlockPos>getArgument(0).immutable()); return null; })
                .when(chunk).markPosForPostprocessing(any());
        GeneratorContext context = mock(GeneratorContext.class);
        context.levels = new Levels(384, 63);
        CaveCarving carving = new CaveCarving(chunk, context);
        carving.clear(-24, 90, -40, CaveCarving.DRY);
        int before = blocks.size();
        carving.reconcileWater();
        assertEquals(before, blocks.size(), "reconciliation must not synthesize source water");
        assertTrue(chunk.getBlockState(new BlockPos(-17, 80, -40)).isAir(), "open hillside must stay air");
        assertTrue(chunk.getBlockState(new BlockPos(-24, 90, -40)).isAir(), "new cave must stay air");
        assertTrue(updates.contains(source), "exposed source needs a fluid update");
    }
    @Test void isolatedWallPocksCloseWhileLargerOpeningsRemain() {
        ChunkAccess chunk = mock(ChunkAccess.class);
        when(chunk.getPos()).thenReturn(new ChunkPos(0,0));
        when(chunk.getMinBuildHeight()).thenReturn(-64);
        when(chunk.getMaxBuildHeight()).thenReturn(320);
        when(chunk.getAllStarts()).thenReturn(Map.of());
        when(chunk.getHeight(any(),anyInt(),anyInt())).thenReturn(100);
        Map<BlockPos,BlockState> blocks = new HashMap<>();
        blocks.put(new BlockPos(8,80,7), Blocks.AIR.defaultBlockState());
        when(chunk.getBlockState(any())).thenAnswer(a -> {
            BlockPos pos = a.getArgument(0);
            assertEquals(0,pos.getX()>>4); assertEquals(0,pos.getZ()>>4);
            return blocks.getOrDefault(pos,Blocks.STONE.defaultBlockState());
        });
        when(chunk.setBlockState(any(),any(),anyBoolean())).thenAnswer(a -> {
            blocks.put(a.<BlockPos>getArgument(0).immutable(),a.getArgument(1));
            return Blocks.STONE.defaultBlockState();
        });
        GeneratorContext context = mock(GeneratorContext.class);
        context.levels = new Levels(384,63);
        CaveCarving carving = new CaveCarving(chunk,context);
        carving.clear(7,80,7,CaveCarving.DRY);
        for(int x=4;x<=5;x++) for(int y=82;y<=83;y++) carving.clear(x,y,5,CaveCarving.DRY);
        carving.repairSingleBlockHoles();
        assertTrue(chunk.getBlockState(new BlockPos(7,80,7)).is(Blocks.STONE));
        for(int x=4;x<=5;x++) for(int y=82;y<=83;y++) assertTrue(chunk.getBlockState(new BlockPos(x,y,5)).isAir());
    }

}
