package com.pandaismyname1.ultraterraforged.world.worldgen.cave;

import com.pandaismyname1.ultraterraforged.world.worldgen.landform.SiteCache;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.NoiseUtil;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Noise;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Noises;

/** Rare boat-sized sea entrances opening into a sheltered, flooded cliff grotto. */
final class PirateCoves implements CaveFeatures.Feature {
    private static final int GRID=256;
    private final int seed;
    private final float frequency;
    private final Noise walls;
    private final SiteCache<Cove> sites=SiteCache.make();
    record Cove(float mouthX,float mouthZ,float x,float z,float radius,float height) {}
    private static final Cove NONE=new Cove(0,0,0,0,0,0);

    PirateCoves(int seed,float frequency) {
        this.seed=seed;
        this.frequency=frequency;
        this.walls=Noises.perlin(seed+1,19,2);
    }

    @Override public void carve(CaveCarving carving) {
        int gx=Math.floorDiv(carving.minX,GRID),gz=Math.floorDiv(carving.minZ,GRID);
        for (int x=gx-1;x<=gx+1;x++) for (int z=gz-1;z<=gz+1;z++) {
            if (CaveFeatures.random(this.seed,x,z,0)>this.frequency*0.22F) continue;
            Cove c=this.sites.get(x,z,(sx,sz)->find(sx,sz,carving));
            if (c==NONE || !carving.reaches(Math.min(c.x()-c.radius(),c.mouthX()-16),
                    Math.min(c.z()-c.radius(),c.mouthZ()-16),Math.max(c.x()+c.radius(),c.mouthX()+16),
                    Math.max(c.z()+c.radius(),c.mouthZ()+16))) continue;
            hollow(c,carving);
        }
    }

    void hollow(Cove c,CaveCarving carving) {
        int sea=carving.levels.waterY;
        carving.hollow(c.x(),sea+6,c.z(),c.radius(),c.height(),c.radius()*0.86F,sea,8,this.walls);
        CaveFeatures.along(new float[]{c.mouthX(),c.x()},new float[]{sea+3,sea+6},
                new float[]{c.mouthZ(),c.z()},5,(px,py,pz,t)->
                carving.hollow(px,py,pz,14,15,14,sea,6,this.walls));
    }

    Cove find(int gx,int gz,CaveCarving carving) {
        int sea=carving.levels.waterY;
        for (int attempt=0;attempt<4;attempt++) {
            float x=gx*GRID+64+CaveFeatures.random(this.seed,gx,gz,1+attempt*2)*128;
            float z=gz*GRID+64+CaveFeatures.random(this.seed,gx,gz,2+attempt*2)*128;
            if (carving.terrainHeight(x,z)<sea+24) continue;
            for (int d=16;d<=64;d+=8) for (int angle=0;angle<12;angle++) {
                float dx=NoiseUtil.cos(angle*NoiseUtil.PI2/12),dz=NoiseUtil.sin(angle*NoiseUtil.PI2/12);
                float mx=x+dx*d,mz=z+dz*d;
                var cell=carving.cell(mx,mz);
                if (carving.levels.scale(cell.height)>=sea-3
                        || cell.continentalness>=com.pandaismyname1.ultraterraforged.world.worldgen.biome.Continentalness.COAST.min()
                        || cell.terrain.isRiver()
                        || cell.terrain.isLake() || cell.terrain.isWetland()) continue;
                float cx=x-dx*20,cz=z-dz*20;
                float radius=32+CaveFeatures.random(this.seed,gx,gz,15)*16;
                // Enough inland cover for a chamber, with no sinkhole through its roof.
                if (carving.terrainHeight(cx,cz)<sea+30) continue;
                return new Cove(mx,mz,cx,cz,radius,22+CaveFeatures.random(this.seed,gx,gz,16)*8);
            }
        }
        return NONE;
    }
}
