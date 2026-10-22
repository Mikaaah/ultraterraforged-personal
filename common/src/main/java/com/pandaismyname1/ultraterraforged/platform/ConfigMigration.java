package com.pandaismyname1.ultraterraforged.platform;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

/** Imports missing settings into the canonical folder without changing the originals. */
public final class ConfigMigration {
    public static void migrate(Path configRoot) throws IOException {
        Path target = configRoot.resolve("tnkterraforged");
        Files.createDirectories(target);
        importFile(configRoot.resolve("ultraterraforged-alex-caves.toml"), target.resolve("alex-caves.toml"));
        importFile(configRoot.resolve("tnkterraforged-streams.toml"), target.resolve("streams.toml"));
        importFile(configRoot.resolve("utfstreams-common.toml"), target.resolve("streams.toml"));
        // Most recent fork wins; files already in the canonical directory always win.
        for (String name : new String[] {"ultraterraforged", "reterraforged"}) {
            Path previous = configRoot.resolve(name);
            if (!Files.isDirectory(previous, LinkOption.NOFOLLOW_LINKS)) continue;
            try (var paths = Files.walk(previous)) {
                for (Path path : (Iterable<Path>) paths::iterator) {
                    if (Files.isSymbolicLink(path)) continue;
                    Path destination = target.resolve(previous.relativize(path));
                    if (Files.isDirectory(path)) Files.createDirectories(destination);
                    else if (Files.isRegularFile(path) && !Files.exists(destination)) Files.copy(path, destination);
                }
            }
        }
    }

    private static void importFile(Path from, Path to) throws IOException {
        if (Files.isRegularFile(from, LinkOption.NOFOLLOW_LINKS) && !Files.exists(to)) Files.copy(from, to);
    }

    private ConfigMigration() {}
}
