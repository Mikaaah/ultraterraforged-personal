package nl.townsandkingdoms.terraforged.compat.streams.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.streamsreflowing.neoforge.client.StreamParticleScanner;
import net.minecraft.client.multiplayer.ClientLevel;
import nl.townsandkingdoms.terraforged.compat.streams.ActualWaterFlow;
import org.spongepowered.asm.mixin.Mixin;

/** Surface particles receive a column-only cached angle upstream; correct it at the known surface height. */
@Mixin(value=StreamParticleScanner.class,remap=false,priority=400)
public abstract class ActualParticleMixin {
    @WrapMethod(method="emitSurfaceAt",remap=false)
    private static void utfstreams$downhillParticles(ClientLevel level,int x,int y,int z,byte angle,
            double drift,boolean debris,Operation<Void> original) {
        byte corrected=ActualWaterFlow.angle(level,x,y,z,angle);
        if(corrected!=-1) original.call(level,x,y,z,corrected,drift,debris);
    }
}
