package com.pandaismyname1.ultraterraforged.test;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.pandaismyname1.ultraterraforged.data.preset.settings.Preset;
import org.junit.jupiter.api.*;
class ExpansePresetTest {
    @BeforeAll static void bootstrap() { TestBootstrap.init(); }
    @Test void bothPackPresetsProduceValidWorldDatapacks() throws Exception {
        for(String name : new String[]{"Balanced", "Grand"}) {
            var path=Path.of("../presets/tnk3/TNKTerraForged_"+name+"_Expanse.json");
            Preset preset=Preset.CODEC.parse(JsonOps.INSTANCE,JsonParser.parseString(Files.readString(path)))
                    .getOrThrow(error -> new AssertionError(name+": "+error));
            var pack=PresetDatapack.generate(preset);
            assertTrue(pack.errors().isEmpty(),name+": "+String.join("\n",pack.errors()));
            assertTrue(preset.rivers().raisedWater,"mountain rivers should remain supported");
            assertTrue(preset.caveFeatures().giantCaverns.enabled,"large caves should remain enabled");
        }
    }
}
