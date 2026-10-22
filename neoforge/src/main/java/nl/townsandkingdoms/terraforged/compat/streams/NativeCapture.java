package nl.townsandkingdoms.terraforged.compat.streams;

import com.pandaismyname1.ultraterraforged.world.worldgen.GeneratorContext;
import com.pandaismyname1.ultraterraforged.world.worldgen.cell.Cell;
import com.pandaismyname1.ultraterraforged.world.worldgen.rivermap.RiverRoutes;
import com.pandaismyname1.ultraterraforged.world.worldgen.terrain.populator.RiverPopulator;
import com.pandaismyname1.ultraterraforged.world.worldgen.util.PosUtil;

/** Reads cached native river identity, with a non-blocking terrain fallback for provisional queries. */
public final class NativeCapture {
    private static final ThreadLocal<Cell> PROBES = ThreadLocal.withInitial(Cell::new);
    private static final ThreadLocal<Cell> CELLS = ThreadLocal.withInitial(Cell::new);
    private NativeCapture() {}

    public static Sample sample(GeneratorContext context, int x, int z) {
        Cell cell = CELLS.get().reset();
        // Use a completed tile when available. Provisional current queries never wait for a tile
        // or start generation; WorldLookup falls back to a pure query only on a cache miss.
        if (!context.riverData.apply(cell, x, z)) context.lookup.apply(cell, x, z);
        RiverPopulator carver = cell.riverCarver;
        if (!cell.terrain.isRiver() || carver == null || carver.frame == null) return null;
        float t = Math.max(0, Math.min(1, cell.riverT));
        float dt = Math.min(0.05f, 8.0f / Math.max(1, carver.river.length));
        float before = Math.max(0, t - dt), after = Math.min(1, t + dt);
        if (after <= before) return null;
        int scale = context.levels.worldHeight;
        float level = cell.waterLevel > 0 ? cell.waterLevel : context.levels.water;
        int surface = FlowRules.waterY(level, scale);
        if (!(cell.height < level) || surface < context.levels.waterY) return null;
        long a = RiverRoutes.channelInWorld(carver, before);
        long b = RiverRoutes.channelInWorld(carver, after);
        double dx = PosUtil.unpackLeftf(b) - PosUtil.unpackLeftf(a);
        double dz = PosUtil.unpackRightf(b) - PosUtil.unpackRightf(a);
        if (!FlowRules.usableVector(dx, dz)) return null;
        double length = Math.hypot(dx, dz);
        double ux = dx / length, uz = dz / length;
        // Sample final native water, including confluences, rather than only this carver's profile.
        double ahead = surfaceAt(context, x + (int)Math.round(ux * 8), z + (int)Math.round(uz * 8),
                carver.waterLevelAt(after) * scale);
        double behind = surfaceAt(context, x - (int)Math.round(ux * 8), z - (int)Math.round(uz * 8),
                carver.waterLevelAt(before) * scale);
        int direction = FlowRules.downhill(behind, surface, ahead);
        boolean uphill = direction == 0;
        if (direction < 0) { dx = -dx; dz = -dz; }
        return new Sample(dx, dz, surface, FlowRules.waterY(cell.height, scale), uphill,
                carver.main, x, z);
    }

    private static double surfaceAt(GeneratorContext context, int x, int z, double fallback) {
        Cell probe = PROBES.get().reset();
        if (!context.riverData.apply(probe, x, z)) context.lookup.apply(probe, x, z);
        float water = probe.waterLevel > 0 ? probe.waterLevel : context.levels.water;
        return probe.terrain.isRiver() && probe.height < water
                ? FlowRules.waterY(water, context.levels.worldHeight) : fallback;
    }

    public record Sample(double dx, double dz, int surfaceY, int bedY,
                         boolean uphill, boolean main, int x, int z) {}
}
