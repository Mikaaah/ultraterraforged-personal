package nl.townsandkingdoms.terraforged.compat.streams;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
public final class ActualWaterState {
    public static volatile boolean clientActive;
    public record State(boolean active) implements CustomPacketPayload {
        public static final Type<State> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("ultraterraforged","streams_actual_water"));
        public static final StreamCodec<RegistryFriendlyByteBuf,State> CODEC=new StreamCodec<>() {
            public State decode(RegistryFriendlyByteBuf b){return new State(b.readBoolean());}
            public void encode(RegistryFriendlyByteBuf b,State s){b.writeBoolean(s.active);}
        };
        public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public static void register(RegisterPayloadHandlersEvent e){e.registrar("1").playToClient(State.TYPE,State.CODEC,(p,c)->c.enqueueWork(()->{clientActive=p.active;ActualWaterFlow.clear();nl.townsandkingdoms.terraforged.compat.streams.client.ActualWaterClient.refresh();}));}
    private static void sync(ServerPlayer p){PacketDistributor.sendToPlayer(p,new State(UtfStreams.context(p.serverLevel())!=null && UtfStreams.ACTUAL_WATER.get()));}
    public static void login(PlayerEvent.PlayerLoggedInEvent e){if(e.getEntity() instanceof ServerPlayer p)sync(p);}
    public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e){if(e.getEntity() instanceof ServerPlayer p)sync(p);}
    public static void respawn(PlayerEvent.PlayerRespawnEvent e){if(e.getEntity() instanceof ServerPlayer p)sync(p);}
}
