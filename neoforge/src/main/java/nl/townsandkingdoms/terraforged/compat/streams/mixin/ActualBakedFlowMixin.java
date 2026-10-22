package nl.townsandkingdoms.terraforged.compat.streams.mixin;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.streamsreflowing.neoforge.fluid.StreamFlowingFluid;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import nl.townsandkingdoms.terraforged.compat.streams.ActualWaterFlow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(value=StreamFlowingFluid.class,remap=false,priority=400)
public abstract class ActualBakedFlowMixin {
    @ModifyReturnValue(method="resolve",at=@At("RETURN"),remap=false)
    private static Vec3 utfstreams$actualBaked(Vec3 original,Vec3 natural,BlockGetter level,BlockPos pos){return ActualWaterFlow.apply(original,level,pos,level.getFluidState(pos));}
}
