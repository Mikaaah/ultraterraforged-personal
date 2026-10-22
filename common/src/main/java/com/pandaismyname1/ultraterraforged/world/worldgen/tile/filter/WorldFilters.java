package com.pandaismyname1.ultraterraforged.world.worldgen.tile.filter;

import java.util.function.IntFunction;

import com.pandaismyname1.ultraterraforged.data.preset.settings.FilterSettings;
import com.pandaismyname1.ultraterraforged.world.worldgen.GeneratorContext;
import com.pandaismyname1.ultraterraforged.world.worldgen.heightmap.Heightmap;
import com.pandaismyname1.ultraterraforged.world.worldgen.tile.Tile;

public class WorldFilters {
    private final GeneratorContext context;
    private Smoothing smoothing;
    private Steepness steepness;
    private BeachDetect beach;
    private PostProcessing processing;
    private WaterRim waterRim;
    private FilterSettings settings;
    private WorldErosion<Erosion> erosion;
    private int erosionIterations;
    private int smoothingIterations;
    
    public WorldFilters(GeneratorContext context, Heightmap heightmap) {
        this.context = context;
        IntFunction<Erosion> factory = Erosion.factory(context);
        this.settings = context.preset.filters();
        this.beach = BeachDetect.make(context);
        this.smoothing = Smoothing.make(context.preset.filters().smoothing, context.levels);
        // the sea floor's own slopes, with sediment on it
        this.steepness = Steepness.make(1, 10.0F, context.levels, context.preset.oceans().sediment);
        this.processing = new PostProcessing(heightmap, context.levels);
        this.waterRim = new WaterRim(context.levels);
        this.erosion = new WorldErosion<>(factory, (e, size) -> e.getSize() == size);
        this.erosionIterations = context.preset.filters().erosion.dropletsPerChunk;
        this.smoothingIterations = context.preset.filters().smoothing.iterations;
    }
    
    public FilterSettings getSettings() {
        return this.settings;
    }
    
    public void apply(Tile tile, boolean optionalFilters) {
        int regionX = tile.getX();
        int regionZ = tile.getZ();
        
        if (optionalFilters) {
            this.applyOptionalFilters(tile, regionX, regionZ);
        }
        this.applyRequiredFilters(tile, regionX, regionZ);
        this.applyPostProcessing(tile, regionX, regionZ);
    }
    
    private void applyRequiredFilters(Tile tile, int seedX, int seedZ) {
        // after erosion and smoothing, which can wear a river's banks below its water
        this.waterRim.apply(tile, seedX, seedZ, 1);
        this.steepness.apply(tile, seedX, seedZ, 1);
        this.beach.apply(tile, seedX, seedZ, 1);
    }
    
    private void applyOptionalFilters(Tile tile, int seedX, int seedZ) {
        Erosion erosion = this.erosion.get(tile.getBlockSize().total());
        erosion.apply(tile, seedX, seedZ, this.erosionIterations);
        this.smoothing.apply(tile, seedX, seedZ, this.smoothingIterations);
    }
    
    public void applyPostProcessing(Tile map, int seedX, int seedZ) {
        this.processing.apply(map, seedX, seedZ, 1);
        if (GeneratorContext.retainRiverData) {
            int size = map.getChunkSize().size();
            int minX = map.getBlockX() >> 4, minZ = map.getBlockZ() >> 4;
            for (int z = 0; z < size; z++) for (int x = 0; x < size; x++) {
                int cx = minX + x, cz = minZ + z;
                this.context.riverData.capture(cx, cz, map.getChunkReader(cx, cz));
            }
        }
    }
}
