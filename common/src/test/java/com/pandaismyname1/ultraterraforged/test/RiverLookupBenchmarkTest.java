package com.pandaismyname1.ultraterraforged.test;

import com.pandaismyname1.ultraterraforged.world.worldgen.GeneratorContext;
import com.pandaismyname1.ultraterraforged.world.worldgen.cell.Cell;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Compares the previous current adapter's raw lookup with the replacement cached lookup.
 * This measures current-layer terrain queries, not full chunk-generation throughput. */
class RiverLookupBenchmarkTest {
    @Test void compareRawAndCachedTerrainQueries() throws Exception {
        assumeTrue(Boolean.getBoolean("utf.bench"));
        TestBootstrap.init();
        var preset = BuiltinPresetRenderTest.presets().get("default").get();
        GeneratorContext context;
        boolean previous = GeneratorContext.retainRiverData;
        try {
            GeneratorContext.retainRiverData = true;
            context = GeneratorContext.makeCached(preset, 2031102026, 2, 4, false);
            context.cache.provideAtChunk(0, 0);
        } finally {
            GeneratorContext.retainRiverData = previous;
        }
        Cell cell = new Cell();
        for (int i = 0; i < 2048; i++) {
            context.lookup.compute(cell.reset(), i & 63, (i >> 6) & 63);
            assertTrue(context.riverData.apply(cell.reset(), i & 63, (i >> 6) & 63));
        }
        long raw = 0, cached = 0;
        double checksum = 0;
        int count = 64 * 64 * 8;
        for (int round = 0; round < 8; round++) {
            long start = System.nanoTime();
            for (int z = 0; z < 64; z++) for (int x = 0; x < 64; x++) {
                context.lookup.compute(cell.reset(), x, z);
                checksum += cell.height;
            }
            raw += System.nanoTime() - start;
            start = System.nanoTime();
            for (int z = 0; z < 64; z++) for (int x = 0; x < 64; x++) {
                assertTrue(context.riverData.apply(cell.reset(), x, z));
                checksum += cell.height;
            }
            cached += System.nanoTime() - start;
        }
        assertTrue(Double.isFinite(checksum));
        String report = "{\"queries\":" + count + ",\"rawMs\":" + raw / 1e6
            + ",\"cachedMs\":" + cached / 1e6 + ",\"lookupSpeedup\":" + raw / (double)cached + "}";
        Path output = Path.of("build/reports/river-lookup-benchmark.json");
        Files.createDirectories(output.getParent());
        Files.writeString(output, report);
        System.out.println("River terrain-query benchmark: " + report);
    }
}
