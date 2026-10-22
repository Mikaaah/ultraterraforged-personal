package nl.townsandkingdoms.terraforged.compat.streams.client;
public final class ActualWaterClient {
    public static net.minecraft.world.level.Level level() { return net.minecraft.client.Minecraft.getInstance().level; }
    public static void refresh() { var mc=net.minecraft.client.Minecraft.getInstance(); if(mc.level!=null) mc.levelRenderer.allChanged(); }
}
