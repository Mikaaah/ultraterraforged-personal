package com.pandaismyname1.ultraterraforged.world.worldgen.cave;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;
import net.minecraft.world.level.block.Blocks;
import com.pandaismyname1.ultraterraforged.test.*;
import com.pandaismyname1.ultraterraforged.world.worldgen.GeneratorContext;

class CavernStyleTest {
    @BeforeAll static void boot() { TestBootstrap.init(); }

    @Test void pillarsFlareAndRemainConnectedBelowTheLake() {
        var c=new GiantCaverns.Cavern(0,0,0,120,40,120,40,100);
        var pillar=new GiantCaverns.Pillar(0,0,5,0.8F,0.9F);
        int waist=0,base=0;
        for (int z=-30;z<=30;z++) for (int x=-30;x<=30;x++) {
            if (GiantCaverns.pillarAt(pillar,x,0,z,c)) waist++;
            if (GiantCaverns.pillarAt(pillar,x,-36,z,c)) base++;
        }
        assertTrue(base>waist*3,"pillar still has an extruded, constant cross-section");
        for(int y=-40;y<=40;y++) assertTrue(GiantCaverns.pillarAt(pillar,0,y,0,c),"unanchored spine at "+y);
    }

    private CaveWorld world() {
        return new CaveWorld(GeneratorContext.makeCached(BuiltinPresetRenderTest.presets().get("default").get(),
                PresetRenderer.SEED,3,6,false));
    }

    @Test void lanternRequiresDryClearanceAndSolidCeiling() {
        var world=world();
        var carving=new CaveCarving(world.chunk(0,0),world.context);
        for(int y=-45;y<=-20;y++) carving.clear(5,y,5,CaveCarving.DRY);
        CavernDecorations.hangLantern(carving,5,-19,5,5);
        assertTrue(world.get(5,-24,5).is(Blocks.LANTERN));
        for(int y=-23;y<=-20;y++) assertTrue(world.get(5,y,5).is(Blocks.CHAIN));
        carving.clear(6,-20,6,CaveCarving.DRY);
        CavernDecorations.hangLantern(carving,6,-20,6,4);
        assertFalse(world.get(6,-24,6).is(Blocks.LANTERN),"lamp hung from air");
        for(int y=-45;y<=-20;y++) carving.clear(7,y,7,-20);
        CavernDecorations.hangLantern(carving,7,-19,7,5);
        assertTrue(world.get(7,-24,7).is(Blocks.WATER),"lamp replaced water");
    }

    @Test void lushChambersGrowMossAndVinesOnExistingRock() {
        var world=world();
        var carving=new CaveCarving(world.chunk(0,0),world.context);
        // A broad dry chamber shelf and ceiling, independent of terrain sampling.
        for(int z=0;z<16;z++) for(int x=0;x<16;x++)
            for(int y=-40;y<=-10;y++) carving.clear(x,y,z,CaveCarving.DRY);
        var c=new GiantCaverns.Cavern(8,-25,8,100,40,100,15,100);
        CavernDecorations.decorate(carving,c,CavernDecorations.Style.LUSH);
        assertTrue(world.count(s->s.is(Blocks.MOSS_BLOCK))>20,"lush style has no moss floor");
        assertTrue(world.count(s->s.is(Blocks.CAVE_VINES))>0,"lush style has no glowberry vines");
    }
    @Test void laterConnectionsCannotSeverProtectedPillarRock() {
        var world=world();
        var carving=new CaveCarving(world.chunk(0,0),world.context);
        carving.protectPillar(5,-30,5);
        carving.hollow(5,-30,5,5,5,5,CaveCarving.DRY,CaveCarving.OPEN);
        assertTrue(world.get(5,-30,5).is(Blocks.STONE));
        assertTrue(world.get(6,-30,5).isAir());
    }

    @Test void overlappingPoolsDoNotFillExistingCavernAir() {
        var world=world();
        var carving=new CaveCarving(world.chunk(0,0),world.context);
        carving.clear(5,-30,5,CaveCarving.DRY);
        carving.clear(5,-30,5,-20);
        assertTrue(world.get(5,-30,5).isAir(),"later pool floated inside an open cavern");
        carving.clear(6,-30,5,-20);
        assertTrue(world.get(6,-30,5).is(Blocks.WATER));
        carving.clear(6,-30,5,CaveCarving.DRY);
        assertTrue(world.get(6,-30,5).isAir(),"later dry cave left a water ball behind");
    }

    @Test void neighboringStructureStartProtectsItsPiecesInThisChunk() {
        var world=world();
        var chunk=world.chunk(0,0);
        var structure=org.mockito.Mockito.mock(net.minecraft.world.level.levelgen.structure.Structure.class);
        var start=org.mockito.Mockito.mock(net.minecraft.world.level.levelgen.structure.StructureStart.class);
        org.mockito.Mockito.when(start.isValid()).thenReturn(true);
        var piece=org.mockito.Mockito.mock(net.minecraft.world.level.levelgen.structure.StructurePiece.class);
        org.mockito.Mockito.when(piece.getBoundingBox()).thenReturn(new net.minecraft.world.level.levelgen.structure.BoundingBox(8,-35,8,10,-25,10));
        org.mockito.Mockito.when(start.getPieces()).thenReturn(java.util.List.of(piece));
        org.mockito.Mockito.when(chunk.getAllReferences()).thenReturn(java.util.Map.of(structure,
                new it.unimi.dsi.fastutil.longs.LongOpenHashSet(new long[]{new net.minecraft.world.level.ChunkPos(-1,0).toLong()})));
        var manager=org.mockito.Mockito.mock(net.minecraft.world.level.StructureManager.class);
        org.mockito.Mockito.when(manager.startsForStructure(org.mockito.ArgumentMatchers.eq(chunk.getPos()),
                org.mockito.ArgumentMatchers.any())).thenReturn(java.util.List.of(start));
        var carving=new CaveCarving(chunk,world.context,null,null,manager);
        carving.clear(9,-30,9,CaveCarving.DRY);
        assertTrue(world.get(9,-30,9).is(Blocks.STONE),"neighboring jigsaw piece was carved out");
        carving.clear(0,-30,0,CaveCarving.DRY);
        assertTrue(world.get(0,-30,0).isAir(),"unrelated cave column was suppressed");
        carving.clear(15,-30,0,CaveCarving.DRY);
        assertTrue(world.get(15,-30,0).isAir(),"aggregate mineshaft bounds suppressed empty space");
    }

    @Test void cavernBiomePaintingRetainsOtherCellsAndProtectedAlexBiomes() {
        var world=world();
        var registry=new net.minecraft.core.MappedRegistry<net.minecraft.world.level.biome.Biome>(
                net.minecraft.core.registries.Registries.BIOME,com.mojang.serialization.Lifecycle.stable());
        var plain=registry.register(net.minecraft.world.level.biome.Biomes.PLAINS,
                org.mockito.Mockito.mock(net.minecraft.world.level.biome.Biome.class),net.minecraft.core.RegistrationInfo.BUILT_IN);
        var lush=registry.register(net.minecraft.world.level.biome.Biomes.LUSH_CAVES,
                org.mockito.Mockito.mock(net.minecraft.world.level.biome.Biome.class),net.minecraft.core.RegistrationInfo.BUILT_IN);
        var alex=registry.register(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BIOME,
                net.minecraft.resources.ResourceLocation.parse("alexscaves:primordial_caves")),
                org.mockito.Mockito.mock(net.minecraft.world.level.biome.Biome.class),net.minecraft.core.RegistrationInfo.BUILT_IN);
        var yung=registry.register(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BIOME,
                net.minecraft.resources.ResourceLocation.parse("yungscavebiomes:frosted_caves")),
                org.mockito.Mockito.mock(net.minecraft.world.level.biome.Biome.class),net.minecraft.core.RegistrationInfo.BUILT_IN);
        registry.freeze();
        var access=new net.minecraft.core.RegistryAccess.ImmutableRegistryAccess(java.util.List.of(registry));
        var chunk=world.chunk(0,0);
        org.mockito.Mockito.when(chunk.getNoiseBiome(org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt(),org.mockito.ArgumentMatchers.anyInt()))
                .thenAnswer(inv->(int)inv.getArgument(0)==1?alex:(int)inv.getArgument(0)==2?yung:plain);
        var carving=new CaveCarving(chunk,world.context,access,java.util.Set.of(plain,lush,alex,yung));
        carving.clear(10,-30,2,CaveCarving.DRY);
        assertTrue(world.get(10,-30,2).is(Blocks.STONE),"YUNG cave volume lost its native blocks");
        carving.paintCavernBiome(2,-18,2,net.minecraft.world.level.biome.Biomes.LUSH_CAVES);
        carving.paintCavernBiome(6,-18,2,net.minecraft.world.level.biome.Biomes.LUSH_CAVES);
        carving.paintCavernBiome(10,-18,2,net.minecraft.world.level.biome.Biomes.LUSH_CAVES);
        var resolver=new java.util.concurrent.atomic.AtomicReference<net.minecraft.world.level.biome.BiomeResolver>();
        org.mockito.Mockito.doAnswer(inv->{resolver.set(inv.getArgument(0));return null;}).when(chunk)
                .fillBiomesFromNoise(org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.isNull());
        carving.decorateCaverns();
        assertSame(lush,resolver.get().getNoiseBiome(0,-5,0,null));
        assertSame(alex,resolver.get().getNoiseBiome(1,-5,0,null));
        assertSame(yung,resolver.get().getNoiseBiome(2,-5,0,null));
        assertSame(plain,resolver.get().getNoiseBiome(3,-5,0,null));
    }
}
