package com.pandaismyname1.ultraterraforged.world.worldgen.rivermap;

import com.pandaismyname1.ultraterraforged.world.worldgen.cell.Cell;
import com.pandaismyname1.ultraterraforged.world.worldgen.terrain.TerrainType;
import com.pandaismyname1.ultraterraforged.world.worldgen.terrain.populator.RiverPopulator;
import com.pandaismyname1.ultraterraforged.world.worldgen.tile.Tile;
import java.util.LinkedHashMap;
import java.util.Map;

/** Compact, bounded copies of finished river columns survive retirement of pooled terrain tiles. */
public final class RiverDataCache {
    private static final int LIMIT = 2048;
    private static final Column[] DRY = new Column[256];
    private record Column(RiverPopulator carver, float t, float height, float water) {}
    private final Map<Long, Column[]> chunks = new LinkedHashMap<>(256, .75f, true);
    private static long key(int x, int z) { return (long)x << 32 | (z & 0xffffffffL); }

    public void capture(int cx, int cz, Tile.Chunk source) {
        Column[] columns = DRY;
        for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
            Cell cell = source.getCell(x, z);
            if (!cell.terrain.isRiver() || cell.riverCarver == null) continue;
            if (columns == DRY) columns = new Column[256];
            columns[z << 4 | x] = new Column(cell.riverCarver, cell.riverT, cell.height, cell.waterLevel);
        }
        synchronized (chunks) {
            chunks.put(key(cx, cz), columns);
            if (chunks.size() > LIMIT) chunks.remove(chunks.keySet().iterator().next());
        }
    }

    /** True also for a known dry column: absence of a current must not trigger terrain regeneration. */
    public boolean apply(Cell target, int x, int z) {
        Column[] columns;
        synchronized (chunks) { columns = chunks.get(key(x >> 4, z >> 4)); }
        if (columns == null) return false;
        target.reset();
        Column column = columns[(z & 15) << 4 | (x & 15)];
        if (column != null) {
            target.terrain = TerrainType.RIVER;
            target.riverCarver = column.carver;
            target.riverT = column.t;
            target.height = column.height;
            target.waterLevel = column.water;
        }
        return true;
    }
}
