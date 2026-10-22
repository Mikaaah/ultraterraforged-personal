package com.pandaismyname1.ultraterraforged.test;

import com.pandaismyname1.ultraterraforged.data.PresetPacks;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PresetPackIdentityTest {
    @Test void newPacksUseTnkNamesAndExistingWorldPacksRemainRecognized() {
        assertEquals("tnkterraforged-preset.zip", PresetPacks.WORLD_PACK_NAME);
        assertTrue(PresetPacks.isPresetPack("file/tnkterraforged-preset.zip"));
        assertTrue(PresetPacks.isPresetPack("file/tnkterraforged-preset-123abc.zip"));
        assertTrue(PresetPacks.isPresetPack("file/ultraterraforged-preset.zip"));
        assertTrue(PresetPacks.isPresetPack("file/ultraterraforged-preset-123abc.zip"));
        assertFalse(PresetPacks.isPresetPack("file/other-preset.zip"));
        assertFalse(PresetPacks.isPresetPack("file/tnkterraforged-preset.txt"));
    }
}
