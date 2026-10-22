package com.pandaismyname1.ultraterraforged.test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;
import com.pandaismyname1.ultraterraforged.world.worldgen.*;
import com.pandaismyname1.ultraterraforged.world.worldgen.cell.Cell;
import com.pandaismyname1.ultraterraforged.world.worldgen.terrain.TerrainType;
import com.pandaismyname1.ultraterraforged.world.worldgen.biome.Continentalness;
class MarineTerrainTest {
 @BeforeAll static void boot(){TestBootstrap.init();}
 @Test void coastAndMarineClimateFollowFinalGroundRatherThanAnOldIslandTag(){
  var preset=BuiltinPresetRenderTest.presets().get("default").get();
  var ctx=GeneratorContext.makeUncached(preset,1234,3,1,6);
  var h=ctx.localHeightmap.get();var l=ctx.levels;
  Cell wet=new Cell();wet.height=l.water(-8);wet.terrain=TerrainType.COASTAL_ISLAND;wet.continentNoise=.8f;
  h.applyPost(wet,0,0);
  assertTrue(wet.continentalness<Continentalness.COAST.min(),"a submerged island needs marine climate");
  Cell dry=new Cell();dry.height=l.water;dry.terrain=TerrainType.DEEP_OCEAN;dry.continentNoise=.01f;
  h.applyPost(dry,0,0);
  assertTrue(dry.continentalness>=Continentalness.COAST.min(),"dry shoreline cannot remain cold ocean");
 }
 @Test void coastHasSubmergedShelvesAndRaisedRockyStonyShore(){
  var levels=new com.pandaismyname1.ultraterraforged.world.worldgen.heightmap.Levels(256,63);
  var coast=com.pandaismyname1.ultraterraforged.world.worldgen.terrain.populator.Populators.makeCoast(1234,levels);
  float low=Float.MAX_VALUE,high=-Float.MAX_VALUE;
  int rock=0,shelf=0;
  Cell cell=new Cell();
  for(int z=-512;z<=512;z+=8)for(int x=-512;x<=512;x+=8){
   coast.apply(cell,x,z);
   float y=cell.height*levels.worldHeight;
   if(y>=levels.waterY+5 && cell.erosion<-.4F) rock++;
   if(y<levels.waterY && cell.erosion>-.4F) shelf++;
   low=Math.min(low,y);high=Math.max(high,y);
  }
  assertTrue(high-low>8.0F,"rocky shore needs actual height above the shelf");
  assertTrue(rock>0,"stony shore has no raised rocky headlands");
  assertTrue(shelf>0,"sandy and submerged shelf disappeared");
 }

}
