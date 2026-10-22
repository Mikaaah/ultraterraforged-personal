package com.pandaismyname1.ultraterraforged.world.worldgen.biome;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import com.pandaismyname1.ultraterraforged.world.worldgen.GeneratorContext;

public interface UTFClimateSampler {
	void setBiomeFallback(Climate.ParameterList<Holder<Biome>> fallback);
	Climate.ParameterList<Holder<Biome>> getBiomeFallback();
	void copyBiomeSelection(UTFClimateSampler source);
	Holder<Biome> getCherryGrove();
	void setGeneratorContext(GeneratorContext context);
	GeneratorContext getGeneratorContext();

	Holder<Biome> filterBiome(Holder<Biome> biome, int x, int y, int z);

	void setSpawnSearchCenter(BlockPos center);
	
	BlockPos getSpawnSearchCenter();
}
