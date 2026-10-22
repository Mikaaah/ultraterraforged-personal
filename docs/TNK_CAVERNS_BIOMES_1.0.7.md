# TNKTerraForged 1.0.7 for Minecraft 1.21.1

Replace the previous TNKTerraForged/UltraTerraForged JAR with the matching loader build. Install only one edition. Keep Nature's Spirit installed separately; it is optional and is not bundled.

The ZIP includes the NeoForge JAR and revised Balanced/Grand Expanse JSON presets. Import a preset in the world creation screen. For a dedicated server, place the selected JSON at `config/tnkterraforged/server-preset.json` before creating the world and use `level-type=ultraterraforged:ultraterraforged`.

New chunks receive the code fixes. A new world is needed to adopt a different terrain preset; existing world datapacks retain their saved parameters. Already generated river trenches, stone fields, pillars and caves are not repaired retroactively. Cave-biome boundaries can meet older generation at explored chunk edges.

## Behavior

- Existing submerged seabeds are no longer cut/tagged as surface rivers. Artificial underground river tunnels are also omitted even if an older preset enabled them. Normal inland rivers and cavern lakes remain.
- Giant chambers use curved, widening rock supports and coherent lush, dripstone or lantern styles. Lush variants include moss, clay and glowberry vines; lantern variants use sparse ceiling-anchored chains and lamps. The available vanilla cave biome IDs are used, avoiding a new biome registry dependency. A fixed/restricted biome source keeps its own biome palette when its feature index lacks those cave biomes.
- Known structure starts and Alex's Caves biome volumes remain protected. This is best-effort structure protection at generation time, not a guarantee for every modded placement stage.
- Rare ocean-facing cliff coves have flooded entrances and large inner chambers. They share the sea-cave enable/frequency controls. Coves are conditional on suitable coastal terrain and will not occur at every river mouth or coastline.
- Nature's Spirit's own public surface-rule factory supplies its special materials. TNK height checks also admit the actual ground; ordinary grassy fields receive grass over dirt, while native sand, chalk, kaolin and other special rules take precedence. No Nature's Spirit classes are required when it is absent.
- Exclude `regions_unexplored:dry_bushland` (the upstream registry name, rather than dry_shrubland) and `biomesoplenty:wasteland`. Keep BOP Grassland.
- Rocky coasts now rise into varied stony headlands with submerged shelves between them. Rare deep-ocean Mushroom Fields form actual dry mycelium islands; their climate follows the raised land instead of leaving a biome label over open water.

Tectonic's source was reviewed for general ideas about varying pillars, regional cavern decoration and ceiling-anchored lanterns. This implementation uses TNK's own chunk-local carvers and geometry; Tectonic is not required and no Tectonic assets are bundled.

## Verification

The regression workflow builds both loader releases and checks geometry, water safety, cove access, native surface-rule adaptation, cave-biome selection/protection, legacy configuration, preset export, caches and Streams flow rules. A separate server smoke workflow downloads the exact hash of the supplied Nature's Spirit 2.3.1 NeoForge JAR, installs optional Lithostitched for the test, and forces Cypress Fields over TNK terrain to inspect generated grass/dirt columns. Full TNK3 pack, exact player seed and visual tuning remain separate from these focused checks.
