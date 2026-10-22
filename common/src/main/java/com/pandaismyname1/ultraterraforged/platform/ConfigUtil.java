package com.pandaismyname1.ultraterraforged.platform;

import java.io.IOException;
import java.nio.file.Path;

import dev.architectury.injectables.annotations.ExpectPlatform;
import com.pandaismyname1.ultraterraforged.UTFCommon;

public class ConfigUtil {
	public static final Path UTF_CONFIG_PATH = getConfigPath().resolve("tnkterraforged");
	public static final Path LEGACY_CONFIG_PATH = getConfigPath().resolve(UTFCommon.LEGACY_MOD_ID);
	
	public static Path utf(String path) {
		return UTF_CONFIG_PATH.resolve(path);
	}
	
	public static Path legacy(String path) {
		return LEGACY_CONFIG_PATH.resolve(path);
	}
	
	@ExpectPlatform
	public static Path getConfigPath() {
		throw new IllegalStateException();
	}
	
    static {
        try {
            ConfigMigration.migrate(getConfigPath());
        } catch (IOException e) {
            throw new IllegalStateException("Cannot initialize TNKTerraForged configuration", e);
        }
    }
}
