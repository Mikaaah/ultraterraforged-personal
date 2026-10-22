package com.pandaismyname1.ultraterraforged.world.worldgen.cave;

import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CaveVines;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;

/** Chamber-scale styles, painted after all carvers and before biome decoration. */
final class CavernDecorations {
    enum Style { LUSH, DRIPSTONE, LANTERN }

    static boolean rock(BlockState state) {
        return state.is(Blocks.STONE) || state.is(BlockTags.BASE_STONE_OVERWORLD)
                || state.is(Blocks.CALCITE) || state.is(Blocks.DRIPSTONE_BLOCK);
    }

    static void decorate(CaveCarving carving, GiantCaverns.Cavern c, Style style) {
        int low = Math.max(carving.bottom+2, (int)(c.y()-c.radiusY()*1.12F));
        int high = Math.min(c.roof(), (int)(c.y()+c.radiusY()*1.12F));
        var biome = style == Style.DRIPSTONE ? Biomes.DRIPSTONE_CAVES : Biomes.LUSH_CAVES;
        for (int z=carving.minZ; z<carving.minZ+16; z++) {
            for (int x=carving.minX; x<carving.minX+16; x++) {
                float nx=(x-c.x())/c.radiusX(), nz=(z-c.z())/c.radiusZ();
                float flat=nx*nx+nz*nz;
                if (flat>1.0F) continue;
                int top=Math.min(high, carving.ground(x,z)-30);
                float patch=CaveFeatures.random(17041,x>>2,z>>2,0);
                for (int y=low; y<=top; y++) {
                    float ny=(y-c.y())/c.radiusY();
                    if (flat+ny*ny>1.15F) continue;
                    BlockState state=carving.get(x,y,z);
                    // Paint only open chamber cells, never the surface or a protected biome.
                    if ((x&3)==2 && (z&3)==2 && (y&3)==2
                            && (state.isAir() || state.is(Blocks.WATER))) {
                        carving.paintCavernBiome(x,y,z,biome);
                    }
                    if (!rock(state)) continue;
                    boolean floor=carving.get(x,y+1,z).isAir();
                    boolean ceiling=carving.get(x,y-1,z).isAir();
                    if (style==Style.LUSH) {
                        if (floor && patch<0.86F) {
                            carving.set(x,y,z,Blocks.MOSS_BLOCK.defaultBlockState());
                            if (chance(x,y,z,1)<0.12F) carving.set(x,y+1,z,Blocks.MOSS_CARPET.defaultBlockState());
                        } else if (carving.get(x,y+1,z).is(Blocks.WATER) && patch<0.55F) {
                            carving.set(x,y,z,Blocks.CLAY.defaultBlockState());
                        }
                        if (ceiling && chance(x,y,z,2)<0.045F) {
                            hangVines(carving,x,y,z,2+(int)(chance(x,y,z,3)*9));
                        }
                    } else if (style==Style.DRIPSTONE) {
                        if ((floor || ceiling) && patch<0.7F) {
                            carving.set(x,y,z,Blocks.DRIPSTONE_BLOCK.defaultBlockState());
                            if (chance(x,y,z,4)<0.08F) {
                                carving.set(x,y+(floor?1:-1),z,Blocks.POINTED_DRIPSTONE.defaultBlockState()
                                        .setValue(PointedDripstoneBlock.TIP_DIRECTION,floor?Direction.UP:Direction.DOWN)
                                        .setValue(PointedDripstoneBlock.THICKNESS,DripstoneThickness.TIP));
                            }
                        }
                    } else {
                        if (floor && patch<0.32F) carving.set(x,y,z,Blocks.MOSSY_COBBLESTONE.defaultBlockState());
                        // One possible fitting per 12x12 cell; no lighting carpets or floating lamps.
                        if (ceiling && Math.floorMod(x,12)==5 && Math.floorMod(z,12)==5
                                && chance(x,y,z,5)<0.65F) {
                            hangLantern(carving,x,y,z,2+(int)(chance(x,y,z,6)*6));
                        }
                    }
                }
            }
        }
    }

    private static float chance(int x,int y,int z,int purpose) {
        return CaveFeatures.random(27913+y*31,x,z,purpose);
    }

    static void hangLantern(CaveCarving carving,int x,int ceiling,int z,int length) {
        if (!carving.canDecorate(x,ceiling,z) || !rock(carving.get(x,ceiling,z)) || length<1 || ceiling-length-3<=carving.bottom) return;
        // Include clearance under the lamp and validate the entire chain before writing.
        for (int i=1;i<=length+3;i++) if (!carving.canDecorate(x,ceiling-i,z) || !carving.get(x,ceiling-i,z).isAir()) return;
        for (int i=1;i<length;i++) carving.set(x,ceiling-i,z,Blocks.CHAIN.defaultBlockState());
        carving.set(x,ceiling-length,z,Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING,true));
    }

    private static void hangVines(CaveCarving carving,int x,int ceiling,int z,int length) {
        if (!carving.canDecorate(x,ceiling,z)) return;
        int free=0;
        while (free<length && ceiling-free-2>carving.bottom && carving.canDecorate(x,ceiling-free-1,z) && carving.get(x,ceiling-free-1,z).isAir()) free++;
        if (free<2) return;
        for (int i=1;i<free;i++) carving.set(x,ceiling-i,z,Blocks.CAVE_VINES_PLANT.defaultBlockState());
        carving.set(x,ceiling-free,z,Blocks.CAVE_VINES.defaultBlockState().setValue(CaveVines.BERRIES,true));
    }
}
