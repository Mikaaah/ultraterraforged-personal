package nl.townsandkingdoms.terraforged.compat.streams.mixin;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.material.*;
import net.minecraft.world.phys.Vec3;
import nl.townsandkingdoms.terraforged.compat.streams.ActualWaterFlow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(value=FlowingFluid.class,priority=400)
public abstract class ActualWaterMixin {
    @ModifyReturnValue(method="getFlow",at=@At("RETURN"))
    private Vec3 utfstreams$actualWater(Vec3 original,BlockGetter level,BlockPos pos,FluidState state){return ActualWaterFlow.apply(original,level,pos,state);}
}
