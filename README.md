# TNKTerraForged

**Massive terrain, expansive underground caves, and dungeon-aware world generation for Towns & Kingdoms 3.**

An unofficial Minecraft **1.21.1** terrain-generator fork by **Mikaaah**, based on **UltraTerraForged** (pandaismyname1), **ReTerraForged** (raccoonman and contributors), and **TerraForged** (dags, Won-Ton and contributors). Their code, history, notices and credits are retained.

> **TNK3-specific preview:** This is *not* an official UltraTerraForged build, nor an unrelated replacement written from scratch. It replaces the original JAR in your modpack and changes world generation in newly generated chunks.

## What makes this fork different?

- **Massive caves:** enormous, noisy caverns roughly 230–350 blocks across, with interconnected passages and roofs typically no higher than Y=30.
- **Better for modded dungeons:** don't globally reject Integrated, YUNG or Cataclysm structure sites because of a village-only terrain restriction. Carving tries to preserve already-known structure footprints.
- **More exploration, fewer tiny caves:** shipped terrain presets favor large underground spaces, without forcing hard progression stages.
- **River and memory improvements:** water transitions, cave-water repair, tile failure recovery, cache fixes and a capped worldgen worker pool inherited from the source audit.
- **Optional Alex's Caves support (NeoForge):** compatibility from Mikaaah's own Alex's Caves fix is integrated without requiring the port for normal play.

## Editions

| Edition | Minecraft | Details |
| --- | --- | --- |
| [NeoForge 1.21.1](https://github.com/Mikaaah/ultraterraforged-personal/tree/main) | Java 21 | Primary TNK3 build; optional Alex's Caves source integration |
| [Fabric 1.21.1](https://github.com/Mikaaah/ultraterraforged-personal/tree/main) | Java 21 | Shared terrain core; no NeoForge Alex's Caves integration |

### Technical compatibility: legacy mod ID

To preserve existing TNK3 world presets, datapacks, registry names and dependencies, this preview intentionally retains **`ultraterraforged` as the internal mod ID**. The project and build files use **TNKTerraForged** branding; the legacy ID remains a compatibility detail. **Do not install the original UltraTerraForged JAR alongside TNKTerraForged.**

The `ultraterraforged` registry namespace and upstream Java API packages remain stable for existing worlds and compatible addons. Active configuration lives in **`config/tnkterraforged/`**. Missing files are imported from the old UltraTerraForged/ReTerraForged configuration directories without overwriting current settings. Those original files are retained as backups. Project, build resources and the integrated current adapter use TNKTerraForged naming. This build is **1.0.9+1.21.1**.

## Dungeon and biome compatibility

The first integration uses **vanilla structure starts** to protect generated dungeon footprints; no dependency on an individual dungeon mod is introduced. The old globally applied village-only structure placement veto is disabled to prevent rejecting modded dungeon candidates.

The [TNK3 compatibility matrix](docs/TNK3_COMPATIBILITY.md) covers **Integrated Dungeons & Structures, Integrated Dungeons Arise, Integrated Stronghold, Integrated Cataclysm, YUNG's Better Dungeons, Cataclysm, Alex's Caves, Biomes O' Plenty, Oh The Biomes We've Gone, Twilight Forest and the Nether/End overhauls**. Mod-list presence is based on pack planning documents, not an automatically verified running instance.

**This fork does not duplicate dungeon NBT structures, bypass LootJS/FTB Quests progression or override dungeon loot.** Dungeon-frequency balancing should be handled with TNK3 structure-set datapacks once the actual mod versions and registry keys are checked.

### Built-in Streams Reflowing compatibility (NeoForge)

The native river-current adapter from Mikaaah's [UltraTerraForged × Streams Reflowing 0.2.1](https://github.com/Mikaaah/UltraTerraForged-Streams-Currents) is integrated into TNKTerraForged's **NeoForge 1.21.1** edition. When optional **Streams Reflowing 2.15.2** is installed, TNKTerraForged provides height-aware native river currents and corrects current directions using nearby loaded water—including boats, water particles and baked stream blocks—while respecting explicit manual Flow Control overrides.

**Install Streams Reflowing separately. Remove the old standalone `utfstreams` compatibility JAR** to avoid duplicate Mixins. Fabric does not yet include this adapter. See [Streams integration instructions](docs/TNK3_STREAMS_REFLOWING.md).

See the [1.0.3 water and performance notes](docs/TNK3_WATER_PERFORMANCE_FIX.md) for the fixes, configuration migration and test scope.

## Installation and validation

Use one matching loader JAR, select the TNKTerraForged world type in Create World, and generate **new chunks** for changes to apply. Preserve world backups. For optional Alex's Caves 2.0.10 support on NeoForge, keep its Citadel port installed but **remove the separate tfacfix JAR** to prevent double Mixins.

Build on Java 21:

```sh
./gradlew :neoforge:build -Putf.perfMods=false
./gradlew :fabric:build -Putf.perfMods=false
./gradlew :common:test -Putf.perfMods=false
```

Read the [technical changelog](CHANGELOG-PERSONAL.md) and [23-finding audit matrix](AUDIT-STATUS.md). Successful automated builds do **not** prove modpack stability: test generation on multiple seeds, very high terrain, river junctions and all dungeon types with TNK3's full mod list.

## Authors, origins and licensing

- **TNKTerraForged-specific work / Alex's Caves compatibility:** Mikaaah / Towns & Kingdoms
- **UltraTerraForged:** [pandaismyname1](https://github.com/Pandaismyname1/ultraterraforged)
- **ReTerraForged:** [raccoonman](https://github.com/racoonman2/ReTerraForged) and contributors
- **TerraForged:** [dags](https://github.com/TerraForged/TerraForged) and contributors

Original MIT copyright notices are retained in [LICENSE](LICENSE). The NeoForge distribution includes your GPL-3.0-licensed Alex's Caves integration; see [LICENSE-ALEX-COMPAT-GPL-3.0](LICENSE-ALEX-COMPAT-GPL-3.0). Branding does not remove upstream attribution or existing obligations.

### TNK3 biome exclusions

The [24 excluded biomes](docs/TNK3_BIOME_EXCLUSIONS.md) are replaced during natural generation in TNKTerraForged worlds. BOP Grassland remains enabled; biome registrations, blocks, items and existing chunks are preserved. Version 1.0.9 carries this filter into chunk-local climate samplers and makes Cherry Groves more common in Meadow regions of TNK3 terrain.

### Structure compatibility (1.0.8)

TNK cave carving and cavern decorations now honor structure starts referenced from neighboring chunks. This protects large Integrated, YUNG, ATi and vanilla jigsaws beyond their start chunk, while YUNG's Cave Biomes retain their own cave blocks and biome identity. Structure mods remain separate installations. See [compatibility details](docs/TNK_STRUCTURE_COMPAT_1.0.8.md).

### Caverns and biome surfaces (1.0.7)

Giant chambers now have lush, dripstone and lantern variants, with curved rock supports. Rare large coves open onto the sea. Artificial underground water tunnels are removed, and surface rivers stop at submerged ocean floors. Nature's Spirit uses its native surface materials with terrain-aware height checks and soil defaults for ordinary fields. The TNK3 exclusions now include RU Dry Bushland and BOP Wasteland. See [installation and scope](docs/TNK_CAVERNS_BIOMES_1.0.7.md).

### Terrain and water patch (1.0.6)

See the [terrain/water patch notes](docs/TNK_TERRAIN_WATER_1.0.6.md) for submerged coastal relief, connected river-bank repairs, disabled water springs, isolated-pool and wall-pock cleanup, and revised [TNK3 presets](presets/tnk3). These changes affect new chunks; use a new world when selecting a revised preset.
