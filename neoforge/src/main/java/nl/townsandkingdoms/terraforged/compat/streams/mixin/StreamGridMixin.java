package nl.townsandkingdoms.terraforged.compat.streams.mixin;

import dev.streamsreflowing.core.RiverEngine;
import dev.streamsreflowing.core.river.RiverFlowField;
import dev.streamsreflowing.flow.StreamFlowGrid;
import net.minecraft.server.level.ServerLevel;
import nl.townsandkingdoms.terraforged.compat.streams.UtfStreams;
import nl.townsandkingdoms.terraforged.compat.streams.NativeFlowLayers;
import dev.streamsreflowing.worldgen.compat.RTFRiverCourse;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = StreamFlowGrid.class, remap = false)
public abstract class StreamGridMixin {
    @Inject(method = "riverCourse", at = @At("HEAD"), cancellable = true, remap = false)
    private static void utfstreams$nativeCourse(ServerLevel level, CallbackInfoReturnable<RTFRiverCourse> cir) {
        if (UtfStreams.context(level) != null) cir.setReturnValue(null);
    }

    @Inject(method = "withProducerLayers", at = @At("RETURN"), cancellable = true, remap = false)
    private static void utfstreams$compose(ServerLevel level, RiverEngine engine, int cx, int cz,
            byte[] base, CallbackInfoReturnable<byte[]> cir) {
        cir.setReturnValue(NativeFlowLayers.compose(level, engine, cx, cz, cir.getReturnValue()));
    }

    @Inject(method = "imposedFlow", at = @At("HEAD"), cancellable = true, remap = false)
    private static void utfstreams$stillMeansStill(net.minecraft.world.level.BlockGetter level,
            net.minecraft.core.BlockPos pos, net.minecraft.world.level.material.FluidState state,
            byte angle, double speed, net.minecraft.world.phys.Vec3 vanilla,
            CallbackInfoReturnable<net.minecraft.world.phys.Vec3> cir) {
        if (speed == 0.0) cir.setReturnValue(net.minecraft.world.phys.Vec3.ZERO);
    }

    @ModifyVariable(method = {"getOrBuild", "buildProvisional"}, at = @At("HEAD"),
            argsOnly = true, ordinal = 0, remap = false)
    private static boolean utfstreams$replaceGenericRiverPass(boolean original,
            ServerLevel level, RiverEngine engine, RiverFlowField.BiomeClass classifier,
            RiverFlowField.OrientationField orientation, boolean flowRivers, int cx, int cz) {
        return UtfStreams.context(level) == null ? original : false;
    }
}
