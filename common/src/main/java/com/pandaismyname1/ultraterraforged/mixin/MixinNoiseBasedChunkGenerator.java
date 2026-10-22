package com.pandaismyname1.ultraterraforged.mixin;

import java.util.List;
import java.util.function.Predicate;

import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.RandomState;
import com.pandaismyname1.ultraterraforged.world.worldgen.rivermap.RaisedWater;
import net.minecraft.world.level.levelgen.blending.Blender;
import com.pandaismyname1.ultraterraforged.data.preset.settings.WorldSettings;
import com.pandaismyname1.ultraterraforged.world.worldgen.GeneratorContext;
import com.pandaismyname1.ultraterraforged.world.worldgen.UTFRandomState;
import com.pandaismyname1.ultraterraforged.world.worldgen.WorldGenFlags;
import com.pandaismyname1.ultraterraforged.world.worldgen.cell.Cell;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.NoiseUtil;
import com.pandaismyname1.ultraterraforged.world.worldgen.surface.SurfaceRegion;

@Mixin(value = NoiseBasedChunkGenerator.class, priority = 9001 /* we need this so we don't break noisium */)
class MixinNoiseBasedChunkGenerator {
	
	@Shadow
	@Final
    private Holder<NoiseGeneratorSettings> settings;
	

    // Wrap the complete surface call so a thrown exception cannot leave a
    // previous world's WorldGenRegion in this thread-local.
    @WrapMethod(method = "buildSurface")
    private void ultraterraforged$scopedSurface(WorldGenRegion region,
            StructureManager structures, RandomState randomState,
            ChunkAccess chunk, Operation<Void> original) {
        GeneratorContext context = (Object) randomState instanceof UTFRandomState utf
            ? utf.generatorContext() : null;
        if (context == null) {
            original.call(region, structures, randomState, chunk);
            return;
        }
        SurfaceRegion.set(region);
        try {
            RaisedWater.fill(chunk, context);
            original.call(region, structures, randomState, chunk);
            context.caveFeatures.carve(chunk, region.registryAccess(),
                    ((NoiseBasedChunkGenerator)(Object)this).getBiomeSource().possibleBiomes(), structures);
        } finally {
            SurfaceRegion.set(null);
        }
    }

    // Ceiling fittings and cave biomes belong after vanilla carvers, otherwise
    // a later cut can remove the anchor above a hanging chain or vine.
    @Inject(method="applyCarvers", at=@At("TAIL"))
    private void ultraterraforged$decorateCaverns(WorldGenRegion region,long seed,RandomState randomState,
            net.minecraft.world.level.biome.BiomeManager biomeManager,StructureManager structures,
            ChunkAccess chunk,net.minecraft.world.level.levelgen.GenerationStep.Carving step,CallbackInfo callback) {
        if (step != net.minecraft.world.level.levelgen.GenerationStep.Carving.AIR) return;
        if ((Object)randomState instanceof UTFRandomState utf && utf.generatorContext()!=null) {
            utf.generatorContext().caveFeatures.decorate(chunk,region.registryAccess(),
                    ((NoiseBasedChunkGenerator)(Object)this).getBiomeSource().possibleBiomes(), structures);
        }
    }

	@Redirect(
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/levelgen/NoiseSettings;height()I"
		),
		method = "fillFromNoise"
	)
    public int fillFromNoise(NoiseSettings settings, Blender blender, RandomState randomState, StructureManager structureManager, ChunkAccess chunkAccess2) {
		GeneratorContext generatorContext;
		ChunkPos chunkPos = chunkAccess2.getPos();
		if((Object) randomState instanceof UTFRandomState utfRandomState && (generatorContext = utfRandomState.generatorContext()) != null) {
			return generatorContext.lookup.getGenerationHeight(chunkPos.x, chunkPos.z, this.settings.value(), true);
		} else {
    		return settings.height();
    	}
    }

	@Redirect(
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/levelgen/NoiseSettings;height()I"
		),
		require = 2,
		method = "iterateNoiseColumn"
	)
    private int iterateNoiseColumn(NoiseSettings settings, LevelHeightAccessor levelHeightAccessor, RandomState randomState, int blockX, int blockZ, @Nullable MutableObject<NoiseColumn> mutableObject, @Nullable Predicate<BlockState> predicate) {
		GeneratorContext generatorContext;
		if((Object) randomState instanceof UTFRandomState utfRandomState && (generatorContext = utfRandomState.generatorContext()) != null) {
			return generatorContext.lookup.getGenerationHeight(SectionPos.blockToSectionCoord(blockX), SectionPos.blockToSectionCoord(blockZ), this.settings.value(), !WorldGenFlags.fastLookups());
    	} else {
    		return settings.height();
    	}
    }
	
	@Inject(
		at = @At("TAIL"),
		method = "addDebugScreenInfo"
	)
    private void addDebugScreenInfo(List<String> list, RandomState randomState, BlockPos blockPos, CallbackInfo callback) {
		@Nullable
		GeneratorContext generatorContext;
		if((Object) randomState instanceof UTFRandomState utfRandomState && (generatorContext = utfRandomState.generatorContext()) != null) {
			Cell cell = new Cell();
			generatorContext.lookup.apply(cell, blockPos.getX(), blockPos.getZ());

			WorldSettings worldSettings = generatorContext.preset.world();
			WorldSettings.ControlPoints controlPoints = worldSettings.controlPoints;
			
			list.add("");
			list.add("Terrain Type: " + cell.terrain.getName());
			list.add("Terrain Region: " + cell.terrainRegionEdge);
			list.add("Terrain Mask: " + cell.terrainMask);
			list.add("Continent Edge: " + cell.continentEdge);
			list.add("Ground Variance: " + NoiseUtil.map(cell.continentNoise, controlPoints.coast, controlPoints.farInland));
			list.add("River Distance: " + cell.riverDistance);
			list.add("Mountain Chain: " + cell.mountainChainAlpha);
			list.add("Biome Type: " + cell.biomeType.name());
			list.add("Macro Biome: " + cell.macroBiomeId);
			list.add("Temperature: " + cell.regionTemperature);
			list.add("Moisture: " + cell.regionMoisture);
			list.add("");
    	}
    }
}
