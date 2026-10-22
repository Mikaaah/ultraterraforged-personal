package nl.townsandkingdoms.terraforged.compat.streams;

import com.pandaismyname1.ultraterraforged.world.worldgen.GeneratorContext;
import dev.streamsreflowing.core.RiverEngine;
import dev.streamsreflowing.flow.StreamFlowGrid;
import dev.streamsreflowing.platform.Services;
import net.minecraft.server.level.ServerLevel;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.LinkedHashMap;
import java.util.concurrent.atomic.LongAdder;

/** Server-produced layers use Streams' existing height-aware cache and network path. */
public final class NativeFlowLayers {
    public static final LongAdder SAMPLES = new LongAdder(), COLUMNS = new LongAdder(),
            UPHILL = new LongAdder(), BUILDS = new LongAdder(), NANOS = new LongAdder(),
            FAST_SKIPS = new LongAdder(), FAST_CHUNKS = new LongAdder();
    private static final Map<GeneratorContext, Map<Key, List<StreamFlowGrid.Layer>>> CACHE = new WeakHashMap<>();
    private record Key(int cx, int cz, double speed, int depth, boolean skip) {}
    private NativeFlowLayers() {}

    /** Compose a bounded native band first; leave other elevations and non-native columns untouched. */
    public static byte[] compose(ServerLevel level, RiverEngine engine, int cx, int cz, byte[] base) {
        var nativeLayers = layers(level, engine, cx, cz);
        if (nativeLayers.isEmpty()) return base;
        var layer = nativeLayers.getFirst();
        int oldSize = base.length == 0 ? 0 : Math.max(base.length, StreamFlowGrid.LAYER);
        byte[] out = new byte[StreamFlowGrid.LAYER + oldSize];
        System.arraycopy(layer.angle(), 0, out, 0, 256);
        System.arraycopy(layer.speed(), 0, out, 256, 256);
        for (int i = 0; i < 256; i++) {
            int bottom = layer.lo()[i];
            // Streams reserves a 12-block band span for generic surface estimates.
            if (layer.hi()[i] - bottom == 12) bottom--;
            StreamFlowGrid.putBand(out, 0, i, bottom, layer.hi()[i]);
        }
        if (oldSize != 0) {
            Arrays.fill(out, StreamFlowGrid.LAYER, StreamFlowGrid.LAYER + 256, (byte)-1);
            System.arraycopy(base, 0, out, StreamFlowGrid.LAYER, base.length);
            if (base.length < StreamFlowGrid.LAYER)
                for (int i = 0; i < 256; i++) StreamFlowGrid.putBand(out, 1, i, Short.MIN_VALUE, Short.MAX_VALUE);
        }
        return out;
    }

    public static List<StreamFlowGrid.Layer> layers(ServerLevel level, RiverEngine engine, int cx, int cz) {
        GeneratorContext context = UtfStreams.context(level);
        if (context == null || !Services.PLATFORM.flowVanillaRivers()
                || !Services.PLATFORM.vanillaWaterFlow()) return List.of();
        Key key = new Key(cx, cz, UtfStreams.SPEED.get(), UtfStreams.MAX_DEPTH.get(), UtfStreams.SKIP_UPHILL.get());
        synchronized (CACHE) {
            var cached = CACHE.get(context);
            if (cached != null && cached.containsKey(key)) return cached.get(key);
        }
        // Never generate or retain a tile just to accelerate stream flow.
        // If the already-complete tile is absent, take the exact old path.
        long[] candidates = null;
        if (UtfStreams.FAST_RIVER_SCAN.get() && context.cache != null) {
            var tile = context.cache.provideAtChunkIfPresent(cx, cz);
            if (tile != null) candidates = tile.nativeRiverCandidates(cx, cz);
            if (candidates != null) FAST_CHUNKS.increment();
        }
        long started = System.nanoTime();
        byte[] angles = new byte[256], speeds = new byte[256];
        short[] lo = new short[256], hi = new short[256];
        Arrays.fill(angles, (byte)-1);
        int count = 0;
        try {
            for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
                int i = x + z * 16;
                if (candidates != null && (candidates[i >>> 6] & (1L << (i & 63))) == 0) {
                    FAST_SKIPS.increment();
                    continue;
                }
                int wx = (cx << 4) + x, wz = (cz << 4) + z;
                SAMPLES.increment();
                NativeCapture.Sample s = NativeCapture.sample(context, wx, wz);
                if (s == null) continue;
                boolean calm = UtfStreams.SKIP_UPHILL.get() && s.uphill();
                if (calm) UPHILL.increment();
                // The original Streams layer stays first and retains its own tributary flow.
                // Do not sample engine.plan here: provisional builds may not have a ready watershed.
                int bottom = Math.max(s.bedY() + 1, s.surfaceY() - UtfStreams.MAX_DEPTH.get() + 1);
                int top = s.surfaceY();
                if (bottom > top) continue;
                angles[i] = StreamFlowGrid.encode(s.dx(), s.dz());
                speeds[i] = StreamFlowGrid.packSpeed(calm ? 0.0 : UtfStreams.SPEED.get());
                lo[i] = (short)bottom; hi[i] = (short)top;
                count++;
            }
            COLUMNS.add(count);
            List<StreamFlowGrid.Layer> result = count == 0 ? List.of() : List.of(new StreamFlowGrid.Layer(angles, speeds, lo, hi));
            synchronized (CACHE) {
                var cached = CACHE.computeIfAbsent(context, ignored -> new LinkedHashMap<>(256, 0.75f, true));
                cached.put(key, result);
                if (cached.size() > 2048) cached.remove(cached.keySet().iterator().next());
            }
            return result;
        } finally { BUILDS.increment(); NANOS.add(System.nanoTime() - started); }
    }
}
