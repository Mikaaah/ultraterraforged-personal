package nl.townsandkingdoms.terraforged.compat.streams;

import com.mojang.logging.LogUtils;
import com.pandaismyname1.ultraterraforged.world.worldgen.GeneratorContext;
import com.pandaismyname1.ultraterraforged.world.worldgen.UTFRandomState;
import dev.streamsreflowing.flow.StreamFlowGrid;
import dev.streamsreflowing.platform.Services;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;

public final class UtfStreams {
    public static final Logger LOG = LogUtils.getLogger();
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED, SKIP_UPHILL, ACTUAL_WATER, FAST_RIVER_SCAN;
    public static final ModConfigSpec.DoubleValue SPEED;
    public static final ModConfigSpec.IntValue MAX_DEPTH, SCAN_RADIUS;
    static {
        var b = new ModConfigSpec.Builder();
        ENABLED = b.define("enabled", true);
        FAST_RIVER_SCAN = b.comment("Reuse finalized live tile river flags to skip terrain recomputation off rivers. Always fall back to exact sampling when missing.")
                .define("fastRiverScan", true);
        SKIP_UPHILL = b.comment("Calm local basins where both route directions rise. Rising stretches are directed downhill; terrain is unchanged.")
                .define("skipUphillSegments", true);
        SPEED = b.defineInRange("nativeRiverSpeed", 1.0, 0.1, 2.0);
        MAX_DEPTH = b.defineInRange("maxCurrentDepth", 64, 1, 256);
        ACTUAL_WATER = b.comment("Validate final baked-fluid and boat currents against connected water in loaded chunks.").define("correctActualWaterFlow", true);
        SCAN_RADIUS = b.defineInRange("actualWaterScanRadius", 16, 4, 32);
        SPEC = b.build();
    }
    /** Initialize within TNKTerraForged, only when Streams Reflowing is installed. */
    public static void initialize(IEventBus bus, ModContainer container) {
        GeneratorContext.retainRiverData = true;
        container.registerConfig(ModConfig.Type.COMMON, SPEC, "tnkterraforged/streams.toml");
        bus.addListener(UtfStreams::setup);
        NeoForge.EVENT_BUS.addListener(UtfStreams::commands);
        bus.addListener(ActualWaterState::register);
        NeoForge.EVENT_BUS.addListener(ActualWaterState::login);
        NeoForge.EVENT_BUS.addListener(ActualWaterState::dimension);
        NeoForge.EVENT_BUS.addListener(ActualWaterState::respawn);
        IntegrationChecks.register();
    }
    private static void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // Native layers are composed by StreamGridMixin, ahead of conflicting generic layers.
            LOG.info("TNKTerraForged integrated native currents for Streams 2.15.2; terrain is not modified.");
        });
    }
    public static GeneratorContext context(ServerLevel level) {
        if (level == null || !level.dimension().equals(Level.OVERWORLD) || !ENABLED.get()) return null;
        // All hooks must agree on activation. Preserve the original baked-fluid path
        // when ordinary-water currents or natural-river currents are disabled.
        if (!Services.PLATFORM.vanillaWaterFlow() || !Services.PLATFORM.flowVanillaRivers()) return null;
        Object random = level.getChunkSource().randomState();
        GeneratorContext context = random instanceof UTFRandomState state && state.usesCells()
                ? state.generatorContext() : null;
        if (context == null) return null;
        // These options make UTF initialize the native world-space route frames.
        // Leave the original Streams path untouched on unsupported plain presets.
        var rivers = context.preset.rivers();
        return rivers.raisedWater || rivers.winding ? context : null;
    }
    private static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("utfstreams").requires(s -> s.hasPermission(2))
                .then(Commands.literal("status").executes(c -> {
                    boolean active = context(c.getSource().getLevel()) != null;
                    c.getSource().sendSuccess(() -> Component.literal("TNKTerraForged Streams: active=" + active
                            + "; native columns=" + NativeFlowLayers.COLUMNS.sum()
                            + "; basin columns calmed=" + NativeFlowLayers.UPHILL.sum()
                            + "; layer builds=" + NativeFlowLayers.BUILDS.sum()
                            + "; reused tile masks=" + NativeFlowLayers.FAST_CHUNKS.sum()
                            + "; avoided samples=" + NativeFlowLayers.FAST_SKIPS.sum()
                            + "; actual water searches=" + ActualWaterFlow.SEARCHES.sum()
                            + "; actual corrections=" + ActualWaterFlow.CORRECTIONS.sum()), false);
                    return active ? 1 : 0;
                }))
                .then(Commands.literal("inspect").executes(c -> {
                    var level=c.getSource().getLevel();
                    var pos=net.minecraft.core.BlockPos.containing(c.getSource().getPosition());
                    for(int i=0;i<3 && !level.getFluidState(pos).is(net.minecraft.tags.FluidTags.WATER);i++) pos=pos.below();
                    var state=level.getFluidState(pos);
                    if(!state.is(net.minecraft.tags.FluidTags.WATER)) {
                        c.getSource().sendFailure(Component.literal("Stand in or directly above the affected water.")); return 0;
                    }
                    var flow=state.getFlow(level,pos);
                    byte[] grid=StreamFlowGrid.peek(pos.getX()>>4,pos.getZ()>>4);
                    byte raw=StreamFlowGrid.angleOf(grid,pos.getX(),pos.getZ(),pos.getY());
                    final String info="TNKTerraForged Streams at "+pos.toShortString()+"; fluid="
                        +net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(state.getType())
                        +"; grid angle="+raw+"; actual flow="+flow+"; runtime fix="+ACTUAL_WATER.get();
                    c.getSource().sendSuccess(()->Component.literal(info),false); return 1;
                })));

    }
}
