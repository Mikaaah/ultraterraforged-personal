package com.pandaismyname1.ultraterraforged.world.worldgen.cave;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.*;
import com.pandaismyname1.ultraterraforged.test.*;
import com.pandaismyname1.ultraterraforged.world.worldgen.GeneratorContext;
import com.pandaismyname1.ultraterraforged.world.worldgen.cell.Cell;
import com.pandaismyname1.ultraterraforged.world.worldgen.heightmap.WorldLookup;
import com.pandaismyname1.ultraterraforged.world.worldgen.terrain.TerrainType;
import net.minecraft.world.level.block.Blocks;

class PirateCoveTest {
    @BeforeAll static void boot() { TestBootstrap.init(); }
    @Test void coveConnectsToSeaWithHeadroomAndRetainsCliffRoof() {
        var context=GeneratorContext.makeUncached(BuiltinPresetRenderTest.presets().get("default").get(),1234,3,1,6);
        context.lookup=mock(WorldLookup.class);
        org.mockito.stubbing.Answer<Boolean> terrain=invocation->{
            Cell cell=invocation.getArgument(0);int x=invocation.getArgument(1);
            cell.height=context.levels.water(x<128?-12:55);
            cell.continentalness=x<128?-0.5F:0.3F;
            cell.terrain=x<128?TerrainType.SHALLOW_OCEAN:TerrainType.PLAINS;
            return true;
        };
        when(context.lookup.compute(any(Cell.class),anyInt(),anyInt())).thenAnswer(terrain);
        when(context.lookup.apply(any(Cell.class),anyInt(),anyInt(),anyBoolean())).thenAnswer(terrain);
        var world=new CaveWorld(context);
        var generator=new PirateCoves(4321,1);
        PirateCoves.Cove cove=null;
        for(int gz=-5;gz<=5;gz++) {
            var c=generator.find(0,gz,world.probe());
            if(c.radius()>0) {cove=c;break;}
        }
        assertNotNull(cove,"no viable cove along synthetic ocean cliff");
        var selected=cove;
        int x=Math.round(cove.x()),z=Math.round(cove.z()),sea=context.levels.waterY;
        world.carve(x,z,x,z,carving->generator.hollow(selected,carving));
        assertTrue(world.get(x,sea-2,z).is(Blocks.WATER),"cove lacks sea-level water");
        assertTrue(world.get(x,sea+8,z).isAir(),"cove lacks boat headroom");
        assertTrue(world.get(x,sea+49,z).is(Blocks.STONE),"cove broke through the cliff roof");
        for(float t=0;t<=1;t+=.1F) {
            int px=Math.round(cove.mouthX()+(cove.x()-cove.mouthX())*t);
            int pz=Math.round(cove.mouthZ()+(cove.z()-cove.mouthZ())*t);
            world.carve(px,pz,px,pz,carving->generator.hollow(selected,carving));
            assertTrue(world.get(px,sea,pz).is(Blocks.WATER),"sea connection cut off at "+t);
            assertTrue(world.get(px,sea+3,pz).isAir(),"entrance blocked at "+t);
        }
    }
}
