package com.pandaismyname1.ultraterraforged.test;

import com.pandaismyname1.ultraterraforged.platform.ConfigMigration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class ConfigMigrationTest {
    @TempDir Path root;

    @Test void importsNestedPresetsWithCanonicalThenNewestLegacyPriority() throws Exception {
        Files.createDirectories(root.resolve("ultraterraforged/presets"));
        Files.createDirectories(root.resolve("reterraforged/presets"));
        Files.createDirectories(root.resolve("tnkterraforged"));
        Files.writeString(root.resolve("tnkterraforged/client.json"), "current");
        Files.writeString(root.resolve("ultraterraforged/client.json"), "old");
        Files.writeString(root.resolve("ultraterraforged/presets/custom.json"), "fork preset");
        Files.writeString(root.resolve("reterraforged/presets/custom.json"), "older preset");
        Files.writeString(root.resolve("reterraforged/presets/extra.json"), "extra preset");
        ConfigMigration.migrate(root);
        ConfigMigration.migrate(root);
        assertEquals("current", Files.readString(root.resolve("tnkterraforged/client.json")));
        assertEquals("fork preset", Files.readString(root.resolve("tnkterraforged/presets/custom.json")));
        assertEquals("extra preset", Files.readString(root.resolve("tnkterraforged/presets/extra.json")));
        assertEquals("old", Files.readString(root.resolve("ultraterraforged/client.json")));
    }

    @Test void freshInstallCreatesOnlyCanonicalFolder() throws Exception {
        ConfigMigration.migrate(root);
        assertTrue(Files.isDirectory(root.resolve("tnkterraforged")));
        assertFalse(Files.exists(root.resolve("ultraterraforged")));
    }

    @Test void importsOptionalCompatSettingsBeforeNeoForgeRegistersThem() throws Exception {
        Files.writeString(root.resolve("ultraterraforged-alex-caves.toml"), "alex setting");
        Files.writeString(root.resolve("tnkterraforged-streams.toml"), "current streams setting");
        Files.writeString(root.resolve("utfstreams-common.toml"), "older streams setting");
        ConfigMigration.migrate(root);
        assertEquals("alex setting", Files.readString(root.resolve("tnkterraforged/alex-caves.toml")));
        assertEquals("current streams setting", Files.readString(root.resolve("tnkterraforged/streams.toml")));
    }
}
