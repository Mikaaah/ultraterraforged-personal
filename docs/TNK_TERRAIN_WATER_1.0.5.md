# TNKTerraForged 1.0.5 terrain and water patch

## Confirmed defects

- CaveCarving.reconcileWater used a chunk-local breadth-first flood over *any* connected air, rather than just the intended cave volume. A hillside spring could fill outdoor air with permanent source water up to the spring elevation. Different chunk-local seeds also allowed different synthetic water ceilings in connected caves.
- RiverPopulator and WaterRim could raise arbitrarily low ground to an elevated river level, producing narrow artificial dams. Water levels were selected before later landforms and tile filters changed the surrounding banks.
- The shallow-ocean base had a constant seven-block depth. Broad shelves in the Expanse presets further suppressed seabed variation.
- Giant cavern connectors used the same smooth ellipsoid radius along their paths, producing very regular tubes.

## Changes

Cave reconciliation now schedules exposed existing water for normal fluid processing and does not synthesize new sources. Explicit cavern lakes, sea caves and underground rivers retain the water authored by their individual carvers. Isolated dry caves remain dry. It reads only the chunk being generated.

River embankment and rim repairs may add at most two blocks. A finite-neighborhood, double-buffered check lowers unsupported raised water against the finished surrounding ground before rim repair. Properly contained mountain rivers keep their original elevations. This check is a local safety correction, not a new global river routing solver; the existing longitudinal profiles and junction logic are retained.

Shallow-ocean terrain uses seed-dependent relief in block units. Shelf depths include small-scale variation and a broader drop-off transition. Large cavern connections vary in cross-section and use world-coordinate wall noise while retaining the existing roof protections and corridor clipping bounds.

The included Balanced and Grand Expanse presets retain raised rivers and giant caverns. They use narrower shelves, smaller sand waves, fewer blue holes and less aggressive entrances. Balanced keeps the lighter vertical range and cave frequencies.

## Install

1. Replace the old TNKTerraForged JAR with the supplied NeoForge 1.21.1 JAR. Keep only one copy.
2. Copy the two JSON files to config/tnkterraforged/presets/ if you want the revised presets, replacing the earlier copies.
3. Select the desired preset when creating a new world. Terrain, cave and preset changes do not rebuild existing chunks. A new world provides a clean comparison without old terrain boundaries.
4. Keep Streams Reflowing installed separately. The existing integrated adapter remains included; do not add the older standalone utfstreams compatibility JAR.

## Verification scope

Regression coverage includes conservation of water source blocks beside an exposed spring, fluid update scheduling, bounds on bank fill, support checks against low final ground, enclosed mountain rivers, shallow-ocean relief at several world heights, existing ocean features, preset datapack generation, river placement, and cavern bounds/pillars.

The screenshots were reviewed, but their exact world seed/coordinates and the complete running modpack were not available for reproduction. This is a targeted test build, not a claim that every cave opening, waterfall or elevated river in every preset is resolved. Full pack exploration and multi-chunk fluid ticking remain runtime checks.
