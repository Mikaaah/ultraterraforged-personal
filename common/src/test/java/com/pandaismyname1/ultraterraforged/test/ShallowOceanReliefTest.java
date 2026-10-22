package com.pandaismyname1.ultraterraforged.test;
import static org.junit.jupiter.api.Assertions.*;
import com.pandaismyname1.ultraterraforged.world.worldgen.cell.Cell;
import com.pandaismyname1.ultraterraforged.world.worldgen.heightmap.Levels;
import com.pandaismyname1.ultraterraforged.world.worldgen.terrain.populator.Populators;
import org.junit.jupiter.api.*;
class ShallowOceanReliefTest {
    @BeforeAll static void bootstrap() { TestBootstrap.init(); }
    @Test void shallowOceanBedHasReliefAndRemainsSubmergedAtDifferentWorldHeights() {
        for(int height : new int[]{256,384,512}) {
            Levels levels=new Levels(height,63);
            var ocean=Populators.makeShallowOcean(19842,levels);
            int low=Integer.MAX_VALUE,high=Integer.MIN_VALUE;
            Cell cell=new Cell();
            for(int x=-512;x<=512;x+=16) for(int z=-512;z<=512;z+=16) {
                ocean.apply(cell,x,z);
                int y=levels.scale(cell.height);
                low=Math.min(low,y);high=Math.max(high,y);
                assertTrue(y<levels.waterY && y>=levels.waterY-15);
            }
            assertTrue(high-low>=3,"broad ocean floor must not be a constant-height plane");
        }
    }
}
