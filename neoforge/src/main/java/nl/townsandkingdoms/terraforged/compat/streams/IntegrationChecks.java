package nl.townsandkingdoms.terraforged.compat.streams;

import com.google.gson.GsonBuilder;
import com.pandaismyname1.ultraterraforged.world.worldgen.heightmap.Levels;
import com.pandaismyname1.ultraterraforged.world.worldgen.rivermap.WaterLevels;
import com.pandaismyname1.ultraterraforged.world.worldgen.GeneratorContext;
import com.pandaismyname1.ultraterraforged.world.worldgen.cell.Cell;
import com.pandaismyname1.ultraterraforged.world.worldgen.rivermap.Rivermap;
import com.pandaismyname1.ultraterraforged.world.worldgen.rivermap.RiverRoutes;
import com.pandaismyname1.ultraterraforged.world.worldgen.rivermap.river.Network;
import com.pandaismyname1.ultraterraforged.world.worldgen.util.PosUtil;
import dev.streamsreflowing.core.RiverEngine;
import dev.streamsreflowing.flow.StreamFlowGrid;
import dev.streamsreflowing.flow.ClientFlowGrids;
import dev.streamsreflowing.neoforge.net.StreamFlowNet;
import dev.streamsreflowing.worldgen.RiverFlowFields;
import dev.streamsreflowing.worldgen.StreamsWorldgen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

import java.nio.file.Files;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.HashSet;

/** Runs and stops the test server only when the developer explicitly opts in. */
public final class IntegrationChecks {
    private IntegrationChecks() {}
    public static void register() {
        if (!System.getProperty("utfstreams.integrationTest", "false").equals("false"))
        {
            NeoForge.EVENT_BUS.addListener(IntegrationChecks::prepare);
            NeoForge.EVENT_BUS.addListener(IntegrationChecks::started);
        }
    }
    private static void prepare(net.neoforged.neoforge.event.server.ServerAboutToStartEvent event) {
        event.getServer().getWorldData().getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_SPAWN_CHUNK_RADIUS).set(0, null);
    }
    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
    private static void started(ServerStartedEvent event) {
        var server = event.getServer();
        var report = new LinkedHashMap<String, Object>();
        try {
            ServerLevel level = server.overworld();
            var context = UtfStreams.context(level);
            if (System.getProperty("utfstreams.integrationTest").equals("vanilla")) {
                require(context == null, "Vanilla Overworld activated the adapter");
                report.put("vanillaControlInactive", true);
            } else {
                require(context != null, "No active UTF context");
                if (Boolean.getBoolean("utfstreams.natureSmoke")) {
                    natureSpiritSurface(level,context,report);
                } else {
                generatedCoastalWater(level, context, report);
                NativeCapture.Sample high = findElevatedMain(context);
                require(high != null, "No elevated main-river sample found within test search");
                final var s = high;
                report.put("highMainRiver", Map.of("x", s.x(), "z", s.z(), "surfaceY", s.surfaceY(),
                        "bedY", s.bedY(), "dx", s.dx(), "dz", s.dz()));
                int cx = s.x() >> 4, cz = s.z() >> 4, i = (s.x() & 15) + (s.z() & 15) * 16;
                var position = new BlockPos(s.x(), s.surfaceY(), s.z());
                var chunk = level.getChunkAt(position);
                require(chunk.getFluidState(position).is(FluidTags.WATER), "Native predicted surface is not actual water in the generated chunk");
                report.put("generatedWaterAtPredictedHeight", true);
                Cell retained = new Cell();
                require(context.riverData.apply(retained, s.x(), s.z()) && retained.riverCarver != null,
                        "Generated river did not retain its compact current data");
                report.put("generatedRiverSnapshotRetained", true);
                long started = System.nanoTime();
                var layers = NativeFlowLayers.layers(level, null, cx, cz);
                report.put("sampleLayerBuildMs", (System.nanoTime() - started) / 1_000_000.0);
                require(layers.size() == 1 && layers.getFirst().angle()[i] != -1, "No current at elevated native main river");
                var nativeLayer = layers.getFirst();
                require(nativeLayer.lo()[i] <= s.surfaceY() && nativeLayer.hi()[i] == s.surfaceY(), "Wrong native Y band");
                report.put("elevatedLayerProduced", true);
                actualWaterRegression(level, s.x(), s.z(), report);

                // Verify the public extra-layer API keeps an existing Streams layer first.
                byte[] own = new byte[StreamFlowGrid.LAYER];
                Arrays.fill(own, 0, 256, (byte)-1);
                own[i] = 64;
                own[256 + i] = StreamFlowGrid.packSpeed(1);
                StreamFlowGrid.putBand(own, 0, i, s.surfaceY() - 2, s.surfaceY());
                var append = StreamFlowGrid.class.getDeclaredMethod("withProducerLayers",
                        ServerLevel.class, RiverEngine.class, int.class, int.class, byte[].class);
                append.setAccessible(true);
                byte[] combined = (byte[])append.invoke(null, level, null, cx, cz, own);
                require(StreamFlowGrid.layerCount(combined) >= 2, "Native layer was not registered through the API");
                require(StreamFlowGrid.layerAt(combined, i, s.surfaceY()) == 0
                        && StreamFlowGrid.angleOf(combined, s.x(), s.z(), s.surfaceY()) == StreamFlowGrid.encode(s.dx(), s.dz()),
                        "Native layer did not correct a conflicting current");
                report.put("nativeRiverDirectionHasPriority", true);
                require(own[i] == 64, "Composition mutated the original Streams grid");
                report.put("originalGridPreserved", true);

                RiverEngine engine = StreamsWorldgen.engine(level);
                require(engine != null, "No Streams engine for full pipeline test");
                var classifier = RiverFlowFields.classifierFor(level);
                var orientation = RiverFlowFields.orientationFor(level);
                StreamFlowGrid.invalidateArea(cx << 4, cz << 4, 16);
                byte[] grid = StreamFlowGrid.getOrBuild(level, engine, classifier, orientation, true, cx, cz);
                require(grid != null && StreamFlowGrid.angleOf(grid, s.x(), s.z(), s.surfaceY()) != -1,
                        "Native current missing from cached full grid");
                require(StreamFlowGrid.angleOf(grid, s.x(), s.z(), s.surfaceY() + 32) == -1,
                        "Current leaked into water above native river band");
                require(StreamFlowGrid.angleForPhysics(level, s.x(), s.surfaceY(), s.z()) != -1,
                        "Server physics did not see elevated native current");
                report.put("fullGridAndServerPhysicsPassed", true);
                report.put("verticalBandIsolationPassed", true);
                report.put("networkGridBytes", grid.length);
                StreamFlowGrid.invalidateArea(cx << 4, cz << 4, 16);
                byte[] provisional = StreamFlowGrid.buildProvisional(level, engine, classifier, orientation, true, cx, cz);
                require(provisional != null && StreamFlowGrid.angleOf(provisional, s.x(), s.z(), s.surfaceY()) != -1,
                        "Provisional grid omitted the native current");
                report.put("provisionalGridPassed", true);

                // Round-trip the actual network codec, then check client-side data lookup.
                var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess());
                try {
                    StreamFlowNet.FlowGridPayload.CODEC.encode(buffer,
                            new StreamFlowNet.FlowGridPayload(cx, cz, grid));
                    var decoded = StreamFlowNet.FlowGridPayload.CODEC.decode(buffer);
                    require(decoded.cx() == cx && decoded.cz() == cz && Arrays.equals(decoded.grid(), grid),
                            "Streams packet codec changed the height-aware current grid");
                    ClientFlowGrids.put(cx, cz, decoded.grid());
                    require(ClientFlowGrids.angleAt(s.x(), s.surfaceY(), s.z()) != -1,
                            "Client grid lookup lost the elevated current");
                    require(ClientFlowGrids.angleAt(s.x(), s.surfaceY() + 32, s.z()) == -1,
                            "Client current leaked above its height band");
                } finally { ClientFlowGrids.clear(); buffer.release(); }
                report.put("packetCodecAndClientLookupPassed", true);

                // Exercise the actual native leveling method with an inland rise in terrain.
                var step = WaterLevels.class.getDeclaredMethod("step", int.class, float.class, float.class, Levels.class);
                step.setAccessible(true);
                Levels levels = context.levels;
                int a = (int)step.invoke(null, Integer.MAX_VALUE, 120f / levels.worldHeight, 40f / levels.worldHeight, levels);
                int b = (int)step.invoke(null, a, 100f / levels.worldHeight, 40f / levels.worldHeight, levels);
                int c = (int)step.invoke(null, b, 180f / levels.worldHeight, 40f / levels.worldHeight, levels);
                require(c > b, "Native leveling rise counterexample did not reproduce");
                report.put("nativeUphillCounterexample", java.util.List.of(a, b, c));
                require(FlowRules.downhill(b, b, c) == -1, "Downhill rule did not reverse the native rise");
                report.put("uphillReversalPassed", true);
                report.put("statusCommandPassed", server.getCommands().getDispatcher().execute("utfstreams status", server.createCommandSourceStack()) == 1);
                }
            }
            for (var key : java.util.List.of(Level.NETHER, Level.END))
                require(UtfStreams.context(server.getLevel(key)) == null, "Non-Overworld adapter activation");
            report.put("netherAndEndInactive", true);
            report.put("passed", true);
        } catch (Throwable failure) {
            report.put("passed", false); report.put("error", failure.toString());
            UtfStreams.LOG.error("UTF Streams integration failed", failure);
        }
        try { Files.writeString(FMLPaths.GAMEDIR.get().resolve("utfstreams-integration-results.json"),
                new GsonBuilder().setPrettyPrinting().create().toJson(report));
        } catch (Exception e) { UtfStreams.LOG.error("Cannot write integration results", e); }
        UtfStreams.LOG.info("UTF Streams integration complete: {}", report);
        server.halt(false);
    }

    private static void natureSpiritSurface(ServerLevel level, GeneratorContext context, Map<String,Object> report) {
        var nativeSource=com.pandaismyname1.ultraterraforged.world.worldgen.surface.ModSurfaceRules.wrap(
                net.minecraft.world.level.levelgen.SurfaceRules.state(net.minecraft.world.level.block.Blocks.STONE.defaultBlockState()),
                level.registryAccess());
        String nativeJson=net.minecraft.world.level.levelgen.SurfaceRules.RuleSource.CODEC
                .encodeStart(com.mojang.serialization.JsonOps.INSTANCE,nativeSource).getOrThrow().toString();
        require(nativeJson.contains("natures_spirit:white_chalk"),"Native Nature's Spirit special materials were not imported");
        require(nativeJson.contains("ultraterraforged:near_ground"),"Native surface height adaptation missing");
        report.put("nativeNatureSpiritMaterialsAndHeightChecks",true);
        int soil=0, tested=0;
        var samples=new java.util.ArrayList<Object>();
        Cell cell=new Cell();
        for (int radius=256; radius<=8192 && soil<80; radius+=256) {
            for (int side=0;side<16 && soil<80;side++) {
            double angle=side*Math.PI/8;
            int x=(int)(Math.cos(angle)*radius),z=(int)(Math.sin(angle)*radius);
            context.lookup.compute(cell.reset(),x,z);
            if (cell.height<context.levels.water(6) || cell.height>context.levels.water(80)
                    || cell.terrain.isRiver()) continue;
            var chunk=level.getChunk(x>>4,z>>4);
            require(level.getBiome(new BlockPos(x,context.levels.scale(cell.height),z)).unwrapKey()
                    .map(key->key.location().toString().equals("natures_spirit:cypress_fields")).orElse(false),
                    "Nature smoke world did not use the fixed cypress biome");
            int found=0;
            for (int dz=0;dz<16;dz++) for (int dx=0;dx<16;dx++) {
                int bx=(x&~15)+dx,bz=(z&~15)+dz;
                context.lookup.apply(cell.reset(),bx,bz,true);
                int y=context.levels.scale(cell.height);
                for (int dy=8;dy>=-8;dy--) {
                    var pos=new BlockPos(bx,y+dy,bz);
                    if (chunk.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)
                            && chunk.getBlockState(pos.below()).is(net.minecraft.world.level.block.Blocks.DIRT)) {
                        found++;break;
                    }
                }
            }
            tested++;
            soil+=found;
            samples.add(Map.of("x",x,"z",z,"grassAndDirtColumns",found));
            }
        }
        require(soil>=80,"Cypress fields still lack grass/dirt surfaces: "+soil+" columns in "+tested+" chunks");
        report.put("naturesSpiritCypressGrassAndDirt",true);
        report.put("cypressSamples",samples);
        report.put("grassAndDirtColumns",soil);
    }

    private static void generatedCoastalWater(ServerLevel level, GeneratorContext context, Map<String,Object> report) {
        Cell cell = new Cell();
        int checked = 0;
        for (int radius=256; radius<=4096 && checked<2; radius+=256) {
            for (int side=0;side<8 && checked<2;side++) {
                double angle=side*Math.PI/4;
                int x=(int)(Math.cos(angle)*radius), z=(int)(Math.sin(angle)*radius);
                context.lookup.compute(cell.reset(),x,z);
                if (cell.height < context.levels.water(-6) || cell.height >= context.levels.water(-1)
                        || cell.continentalness >= com.pandaismyname1.ultraterraforged.world.worldgen.biome.Continentalness.COAST.min()) continue;
                context.lookup.apply(cell,x,z,true);
                if (cell.height >= context.levels.water(-1) || cell.waterLevel>0) continue;
                BlockPos surface=new BlockPos(x,context.levels.waterY,z);
                var chunk=level.getChunkAt(surface);
                require(chunk.getFluidState(surface).is(FluidTags.WATER),
                        "Coastal floor is dry at " + surface + " predicted bed="+context.levels.scale(cell.height));
                require(cell.continentalness < com.pandaismyname1.ultraterraforged.world.worldgen.biome.Continentalness.COAST.min(),
                        "Submerged coastal bed retained inland biome climate");
                report.put("coastalColumn"+checked,Map.of("x",x,"z",z,"bedY",context.levels.scale(cell.height),"waterY",surface.getY(),"terrain",cell.terrain.getName()));
                checked++;
            }
        }
        require(checked>0,"No coastal water sample found in the smoke seed");
        report.put("generatedCoastalWaterColumns",checked);
    }

    private static void actualWaterRegression(ServerLevel level, int wx, int wz, Map<String,Object> report) {
        int x=(wx & ~15)+8, z=(wz & ~15)+8, low=230;
        int east=0;
        for(int i=0;i<8;i++) if(dev.streamsreflowing.neoforge.fluid.StreamLiquidBlock.DIRS[i].x>0.9) east=i;
        for(int ax=-6;ax<=6;ax++)for(int az=-2;az<=2;az++) {
            int top=ax<0?low:low+3;
            for(int y=low-1;y<=low+5;y++) {
                BlockPos p=new BlockPos(x+ax,y,z+az);
                boolean wall=Math.abs(az)==2 || Math.abs(ax)==6 || y==low-1;
                var block=wall?net.minecraft.world.level.block.Blocks.STONE.defaultBlockState()
                    :y<=top?dev.streamsreflowing.neoforge.fluid.StreamFluids.STREAM_BLOCK.get().defaultBlockState()
                        .setValue(dev.streamsreflowing.neoforge.fluid.StreamLiquidBlock.FLOW,east)
                    :net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
                level.setBlock(p,block,2);
            }
        }
        BlockPos test=new BlockPos(x+3,low+3,z);
        ActualWaterFlow.clear();
        boolean enabled=UtfStreams.ACTUAL_WATER.get();
        net.minecraft.world.phys.Vec3 uncorrected;
        UtfStreams.ACTUAL_WATER.set(false);
        try { uncorrected=level.getFluidState(test).getFlow(level,test); }
        finally { UtfStreams.ACTUAL_WATER.set(enabled); }
        require(uncorrected.x>0.5,"Test fixture did not reproduce an uphill baked stream before correction: "+uncorrected);
        report.put("uncorrectedBakedUphillVector",java.util.List.of(uncorrected.x,uncorrected.z));
        var flow=level.getFluidState(test).getFlow(level,test);
        require(flow.x < -0.5, "Baked stream still flowed uphill despite a real westward drop: " + flow);
        byte a=StreamFlowGrid.angleForEntityPush(level,test.getX(),test.getY(),test.getZ());
        require(a!=-1 && StreamFlowGrid.vx(a)<-0.5, "Boat current did not follow actual downhill water");
        report.put("bakedUphillCurrentReversedAgainstActualWater",true);
        report.put("actualWaterBoatDirection",true);
        report.put("actualWaterCorrectedVector",java.util.List.of(flow.x,flow.z));
    }

    private static NativeCapture.Sample findElevatedMain(GeneratorContext context) throws Exception {
        var networksField = Rivermap.class.getDeclaredField("networks");
        networksField.setAccessible(true);
        var seen = new HashSet<Long>();
        Cell probe = new Cell();
        for (int radius = 0; radius <= 8192; radius += 2048) {
            for (int z = -radius; z <= radius; z += 2048) for (int x = -radius; x <= radius; x += 2048) {
                if (radius != 0 && Math.abs(x) != radius && Math.abs(z) != radius) continue;
                context.lookup.compute(probe.reset(), x, z);
                long key = (long)probe.continentX << 32 ^ (probe.continentZ & 0xffffffffL);
                if (!seen.add(key)) continue;
                var map = Rivermap.get(probe, null, context.localHeightmap.get());
                UtfStreams.LOG.info("UTF Streams: inspecting native main routes in continent {},{}", probe.continentX, probe.continentZ);
                for (Network network : (Network[])networksField.get(map)) {
                    var carver = network.riverCarver();
                    if (!carver.main || carver.frame == null) continue;
                    for (int step = 2; step <= 62; step++) {
                        long point = RiverRoutes.channelInWorld(carver, step / 64f);
                        int wx = (int)Math.floor(PosUtil.unpackLeftf(point));
                        int wz = (int)Math.floor(PosUtil.unpackRightf(point));
                        var sample = NativeCapture.sample(context, wx, wz);
                        if (sample != null && sample.main() && !sample.uphill()
                                && sample.surfaceY() >= context.levels.waterY + 8) return sample;
                    }
                }
            }
        }
        return null;
    }
}
