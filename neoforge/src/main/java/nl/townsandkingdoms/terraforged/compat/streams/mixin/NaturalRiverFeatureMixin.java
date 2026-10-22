package nl.townsandkingdoms.terraforged.compat.streams.mixin;

import dev.streamsreflowing.worldgen.StreamRiverFlowFeature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import nl.townsandkingdoms.terraforged.compat.streams.UtfStreams;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = StreamRiverFlowFeature.class, remap = false)
public abstract class NaturalRiverFeatureMixin {
    @Inject(method = "place", at = @At("HEAD"), cancellable = true, remap = false)
    private void utfstreams$keepNativeWater(FeaturePlaceContext<NoneFeatureConfiguration> context,
                                           CallbackInfoReturnable<Boolean> cir) {
        // Do not bake the generic, sea-level river orientation into native UTF water blocks.
        if (UtfStreams.context(context.level().getLevel()) != null) cir.setReturnValue(false);
    }
}
