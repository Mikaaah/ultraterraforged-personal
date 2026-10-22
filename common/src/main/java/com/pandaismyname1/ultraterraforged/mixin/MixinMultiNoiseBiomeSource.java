package com.pandaismyname1.ultraterraforged.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.pandaismyname1.ultraterraforged.world.worldgen.biome.UTFClimateSampler;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(MultiNoiseBiomeSource.class)
abstract class MixinMultiNoiseBiomeSource {
    // Wrap the final result, including TerraBlender and optional cave-placement HEAD injections.
    @WrapMethod(method = "getNoiseBiome(IIILnet/minecraft/world/level/biome/Climate$Sampler;)Lnet/minecraft/core/Holder;")
    private Holder<Biome> tnk$excludeBiomes(int x, int y, int z, Climate.Sampler sampler,
                                          Operation<Holder<Biome>> original) {
        Holder<Biome> selected = original.call(x, y, z, sampler);
        return ((UTFClimateSampler) (Object) sampler).filterBiome(selected, x, y, z);
    }
}
