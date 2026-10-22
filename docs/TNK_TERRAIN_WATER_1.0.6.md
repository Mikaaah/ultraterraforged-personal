# TNKTerraForged 1.0.6 terrain patch

For Minecraft 1.21.1 / NeoForge 21.1.x. Replace the previous TNKTerraForged JAR; keep only one version installed. The mod ID and upstream registry/API namespace remain `ultraterraforged` for compatibility. The embedded Streams integration remains included.

Copy the supplied Balanced/Grand presets to `config/tnkterraforged/presets/`, replacing the older copies, then select the intended preset while creating a **new world**. Generated chunks and existing saved world datapacks are not repaired by replacing a preset on disk. Use a new world for a clean comparison; generating new chunks beside old terrain can expose generation boundaries.

## Changes

The old coast used a single height equal to the sea's top solid block. Broad coastal blends could emerge as dry, perfectly level ocean floors. Coastal beds now have shallow seeded relief below the sea. Final biome continentalness follows whether the finished terrain is dry or submerged, including coastal islands. Intentionally submerged seamounts and guyots remain part of ocean terrain.

Canyon cuts are included in river routing and leveling. Where channels interact, the carver retains original bank ground rather than allowing a second valley to cut it away. Bank repairs remain bounded by the original terrain plus two blocks; water-to-water joins remain open. The 1.0.5 column suppression is removed so it cannot erase a section of a connected river. Mouth transitions remain interpolated and bounded; steep natural banks can use rapids of up to six blocks per sixteen-block survey reach instead of forcing an excessive approach trench. Terrain-following profiles are retained; this is not a replacement watershed simulator.

The final biome pass processes interior and border cells exactly once. Previously it reused masked interior chunk indices and omitted border chunks.

Vanilla water springs and custom hillside Springs are disabled in the Overworld, including older presets that enable them. Isolated one-cell pools are filled to their water surface; diagonal water connections are preserved. Other mods' independent water features are not globally intercepted. Lava springs keep their preset setting.

Custom cave carving closes only isolated single-block rock pocks surrounded by at least five natural-rock neighbours. Larger openings and authored cave lakes are retained. Repairs stay inside the current chunk and only touch blocks carved by this mod; they do not repair every possible vanilla or modded cave opening.

## Verification scope

Regression coverage includes marine/coastal heights, biome classification, negative-coordinate tile borders, real terrain river banks and tributary joins, gorge depth, bounded transitions, spring-policy datapack serialization, isolated pools, cave-water safety, and preservation of larger cave openings. The optional NeoForge server smoke also checks water in physically generated coastal and elevated-river columns alongside current-layer physics and packet handling.

The supplied screenshots were inspected, but no world seed accompanied them. These checks exercise deterministic test seeds; they do not reproduce every screenshot or validate the complete TNK3 modpack. If a specific cliff hole or river defect remains, retain the seed, chosen preset, coordinates and installed mod versions for an exact reproduction.
