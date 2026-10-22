package com.pandaismyname1.ultraterraforged.mixin;

import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import com.pandaismyname1.ultraterraforged.world.worldgen.biome.BiomeExclusions;
import com.pandaismyname1.ultraterraforged.world.worldgen.GeneratorContext;
import com.pandaismyname1.ultraterraforged.world.worldgen.cell.Cell;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.biome.Climate;
import com.pandaismyname1.ultraterraforged.world.worldgen.biome.UTFClimateSampler;

@Mixin(Climate.Sampler.class)
@Implements(@Interface(iface = UTFClimateSampler.class, prefix = "ultraterraforged$UTFClimateSampler$"))
class MixinClimateSampler {
	private Climate.ParameterList<Holder<Biome>> tnk$biomeFallback;
	private GeneratorContext tnk$generatorContext;
	private Holder<Biome> tnk$cherryGrove;
	private static final ThreadLocal<Cell> tnk$cell = ThreadLocal.withInitial(Cell::new);

	public void ultraterraforged$UTFClimateSampler$setBiomeFallback(Climate.ParameterList<Holder<Biome>> fallback) {
		this.tnk$biomeFallback = fallback;
		this.tnk$cherryGrove = fallback == null ? null : fallback.values().stream()
			.map(com.mojang.datafixers.util.Pair::getSecond)
			.filter(biome -> biome.is(net.minecraft.world.level.biome.Biomes.CHERRY_GROVE))
			.findFirst().orElse(null);
	}

	public Climate.ParameterList<Holder<Biome>> ultraterraforged$UTFClimateSampler$getBiomeFallback() {
		return this.tnk$biomeFallback;
	}

	public Holder<Biome> ultraterraforged$UTFClimateSampler$getCherryGrove() {
		return this.tnk$cherryGrove;
	}

	public void ultraterraforged$UTFClimateSampler$copyBiomeSelection(
			com.pandaismyname1.ultraterraforged.world.worldgen.biome.UTFClimateSampler source) {
		this.tnk$biomeFallback = source.getBiomeFallback();
		this.tnk$cherryGrove = source.getCherryGrove();
		this.tnk$generatorContext = source.getGeneratorContext();
	}

	public void ultraterraforged$UTFClimateSampler$setGeneratorContext(GeneratorContext context) {
		this.tnk$generatorContext = context;
	}

	public GeneratorContext ultraterraforged$UTFClimateSampler$getGeneratorContext() {
		return this.tnk$generatorContext;
	}

	public Holder<Biome> ultraterraforged$UTFClimateSampler$filterBiome(Holder<Biome> selected, int x, int y, int z) {
		if (this.tnk$biomeFallback == null) return selected;
		if (BiomeExclusions.excludes(selected)) return BiomeExclusions.select(selected, this.tnk$biomeFallback,
			() -> ((Climate.Sampler) (Object) this).sample(x, y, z));
		if (this.tnk$cherryGrove != null && this.tnk$generatorContext != null
				&& selected.is(net.minecraft.world.level.biome.Biomes.MEADOW)) {
			Cell cell = this.tnk$cell.get().reset();
			this.tnk$generatorContext.lookup.apply(cell, QuartPos.toBlock(x), QuartPos.toBlock(z));
			if (BiomeExclusions.promoteMeadow(cell.biomeRegionId)) return this.tnk$cherryGrove;
		}
		return selected;
	}

	private BlockPos spawnSearchCenter = BlockPos.ZERO;
	
	public void ultraterraforged$UTFClimateSampler$setSpawnSearchCenter(BlockPos spawnSearchCenter) {
		this.spawnSearchCenter = spawnSearchCenter;
	}
	
	public BlockPos ultraterraforged$UTFClimateSampler$getSpawnSearchCenter() {
		return this.spawnSearchCenter;
	}
}
