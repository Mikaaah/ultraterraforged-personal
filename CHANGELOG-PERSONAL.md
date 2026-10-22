# TNKTerraForged — development changelog

## 1.0.10+1.21.1 — Cave structures, water and island profile

- Protect individual underground structure pieces instead of the encompassing start bounding box, so mineshafts do not leave giant rectangular areas of solid terrain inside caverns.
- Avoid filling already open cave air with water from later cave pools. Allow later dry caves to remove water placed by an earlier TNK cave within the same chunk while retaining naturally generated fluids.
- Lower mushroom islands and blend a long shore slope from the ocean floor without a high flat top.
- Spread closely placed third-party random-spread structure sets in TNK worlds while respecting explicit preset overrides and preserving vanilla and already rare sets. Custom placement types retain their own placement logic.

## 1.0.9+1.21.1 — Biome selection and Cherry Groves

- Copy the TNK3 exclusion policy into cached chunk climate samplers, closing the path that could generate RU Gravel Beach or BOP Wasteland in new chunks despite the global biome-source filter.
- Promote part of suitable vanilla Meadow regions to Cherry Grove, using stable per-region values from the TNK climate map. Keep the remaining meadows and modded biomes.
- Survey both TNK3 Expanse presets across a 40,000-block square and check the promoted groves and retained meadows.

## 1.0.8+1.21.1 — Structure and cave-biome compatibility

- Protect structure pieces referenced from neighboring chunks during custom cave carving, lining and decorations, including large jigsaw structures from the supplied Integrated, YUNG and ATi families.
- Preserve YUNG's Frosted and Lost Cave biomes and their blocks when custom caverns overlap them, alongside existing Alex's Caves protection.
- Increase the protection margin around structure bounding boxes to four blocks. Feature-based bridges remain owned by YUNG's own placement stage.

## 1.0.7+1.21.1 — Biome-aware caverns and mod surfaces

- Restore rare Mushroom Fields islands in deep ocean with terrain above sea level and the matching vanilla biome, and give Stony Shore actual elevated rocky terrain rather than a flat sea-level strip.
- Stop river channels from cutting/tagging existing submerged ocean floors. Remove artificial underground water tunnels, including saved presets, and omit their retired UI controls.
- Replace extruded cavern columns with seeded, curved and tapered rock supports. Protect their foundations from later custom connections; reuse per-height cross-sections during carving.
- Give huge chambers coherent lush, dripstone and lantern styles. Apply matching vanilla cave biomes only when available in the biome source; preserve Alex's Caves and known structures. Place anchored fittings after vanilla carvers.
- Add rare giant coastal coves with flooded sea entrances, navigable headroom and guarded cliff roofs. Controlled by the existing sea-cave setting.
- Import optional native mod surface rules while preserving TNK fallback surfaces. Adapt Nature's Spirit's preliminary-height tests to actual terrain, retaining native materials and giving ordinary fields (including Cypress Fields) grass/dirt ground cover.
- Exclude `regions_unexplored:dry_bushland` and `biomesoplenty:wasteland`; keep BOP Grassland. Total exclusions: 24.
- Update both Expanse presets and add targeted geometry, attachment, biome-palette, native surface and cove regression tests plus an optional exact-JAR Nature's Spirit server smoke test.

## 1.0.6+1.21.1 — Coastal beds and connected rivers

- Replace the constant sea-surface coastal bed with shallow, seeded relief. Classify dry shoreline as coast/inland and submerged land tags as marine climate.
- Survey canyon cuts before choosing river routes and water levels. Retain original ground at intersecting river banks, within the existing two-block fill budget; preserve real water joins.
- Remove the per-column water suppression that could interrupt connected rivers. Allow bounded rapids on steep natural banks instead of excessively flattening their approaches.
- Process every tile cell once in the final biome pass, including borders at negative coordinates.
- Disable vanilla and custom water springs even for older enabled presets. Fill isolated one-cell surface pools while retaining diagonal stream connections.
- Close isolated single-block rock pocks made by custom cave carvers; preserve larger openings, structures and other mods' protected caves.
- Update both Expanse presets. See `docs/TNK_TERRAIN_WATER_1.0.6.md` for installation and verification scope.

## 1.0.5+1.21.1 — Terrain and water safety

- Stop cave reconciliation from flooding outdoor air with synthetic source blocks; schedule exposed water to flow instead.
- Cap artificial river embankments and rim repairs at two blocks, and constrain raised water against finished terrain.
- Replace the flat shallow-ocean base with seed-dependent relief; add shelf texture and soften shelf drop-offs.
- Give giant cavern connections varying, rough cross-sections while retaining roof guards and clipping bounds.
- Add revised Balanced/Grand Expanse presets and regression coverage. See `docs/TNK_TERRAIN_WATER_1.0.5.md` for installation and verification limits.

## 1.0.4+1.21.1 — TNK3 biome exclusions

- Exclude the agreed 16 Biomes O' Plenty and 6 Regions Unexplored biomes from newly generated TNKTerraForged terrain. Keep BOP Grassland.
- Replace excluded selections with an allowed vanilla biome at the same climate point, using a per-world search tree built once. No neighboring-chunk searches or extra terrain generation.
- Keep biome registrations, blocks, items and existing chunks. Other world generators and dimensions without TNK terrain cells remain unaffected.
- Include both the 1.0.3 water/cache/path fixes and the newer main-branch cavern/current performance changes.
- See `docs/TNK3_BIOME_EXCLUSIONS.md` for exact IDs and scope.

## 1.0.3+1.21.1 — River water, current-query performance and configuration cleanup

- Bound steep downstream mouth transitions even when the preferred gorge depth cannot meet the parent river. Preserve existing rising terrain profiles so smoothing does not introduce unnecessarily deep trenches.
- Require a real riverbed within the raised-water fill depth before writing source blocks; avoid unsupported floating slabs.
- Postprocess every exposed water block on a column face instead of only the top; skip enclosed pool faces and never read neighboring chunks during this pass.
- Keep the selecting river and route position in terrain cells, including cell copies and resets. Current layers reuse completed terrain tiles for their center and slope probes, falling back to pure queries without waiting for missing tiles. Retain compact river snapshots for up to 2,048 chunks so tile-pool retirement does not restore the repeated queries; dry chunks are cached too. Remove the old observer Mixin and its per-query allocations.
- Consolidate active configuration under `config/tnkterraforged/`, including optional Streams and Alex's Caves settings. Import missing legacy files without overwriting current settings or deleting originals.
- Rename project, access widener and Mixin resource files; move integrated current code to `nl.townsandkingdoms.terraforged.compat.streams`. Use TNK names for generated preset packs and language-provider labels, while continuing to recognize existing world packs. Retain upstream API packages and world registry IDs deliberately for compatibility.
- Add block-placement, steep-transition, config-import and cached-river identity regressions, plus an opt-in raw/cached query benchmark.

## 2026-10-20 — Large-cavern worldgen performance follow-up

- Optimize giant-cavern corridor iteration by projecting each chunk onto the path and visiting only potentially intersecting corridor segments. Path positions, curve sway, radius, roof clearance and generated block shapes are unchanged.
- Add deterministic corridor-range regression coverage alongside existing caverns and raised-river tests.
- Add conservative Streams fast path: reuse a still-live generated terrain tile to skip expensive native river recomputation outside valleys/banks, retaining exact fallback for unavailable tiles. `fastRiverScan` can be switched off for comparison; diagnostic counters report avoided samples.
- An experimental carve-mask water-reconciliation shortcut **failed a new flood regression**; it was reverted rather than changing how cave lakes flood. Original water logic remains intact.
- This addresses source-level hotspots but is **not** a benchmark of full TNK3 performance. The 640-block-height Grand Expanse preset and 100% giant-cavern frequency remain expensive.


## 2026-10-20 — Streams Reflowing native integration

- Integrate the complete NeoForge source of Mikaaah's **UltraTerraForged Streams Currents 0.2.1-beta** into TNKTerraForged, without registering a second mod.
- Preserve native raised/winding river flow sampling, vertical flow bands, downhill route correction, nearby loaded-water verification, particle/boat behavior and manual **Streams Flow Control** overrides.
- Automatically disable the integration when Streams Reflowing is absent. Compile against Streams Reflowing **2.15.2** without redistributing its JAR.
- Reuse the optional runtime config, networking, server commands (`/utfstreams status` and `/utfstreams inspect`) and developer integration smoke checks.
- Add conditional Mixin activation and reject the separate `utfstreams` addon when the integrated implementation is present.
- Run the original pure downhill-routing and water-height tests in an isolated NeoForge test source set; no GPL adapter code is included in Fabric.
- NeoForge-only integration; Fabric remains on the existing terrain-generation implementation.


## 2026-10-20 — Underwater pillar foundation fix

- Fix giant-cavern support pillars ending abruptly at the lake surface: preserve their rock columns above **and below** the waterline, through the lakebed.
- Lake-water filling and open cavern space remain unchanged; carving is skipped only for actual pillar columns.
- Move pillar rejection before the costly per-height wall-noise loop, reducing unnecessary noise sampling in pillar columns.
- Add `GiantCavernPillarTest` verifying a pillar remains solid above, at and below the waterline while a nearby open column is water-filled below the lake and air above it.
- Affects **newly generated chunks only**; existing underwater pillars are not retroactively reconstructed.


## 0.1.0 preview — TNK3-specific derivative (2026-10-20)
- Rebranded loaders' display names, creation screen and build archives to **TNKTerraForged**.
- Retained the original `ultraterraforged` mod ID, registry/data namespaces, version base and preset storage intentionally for world and integration compatibility; this fork **replaces**, never co-installs with, UltraTerraForged.
- Scoped unsafe static global structure terrain veto behind `-Dtnkterraforged.legacyGlobalStructureRules=true` (off by default), because this old village rule could reject **all** modded structure generation sites.
- Added generic structure-start bounding-box preservation during UTF cave carving, lining and local water repairs (configurable with `-Dtnkterraforged.structureProtection=false`).
- Compatible in design with IDAS, Integrated Dungeons Arise, Integrated Stronghold, Integrated Cataclysm, YUNG's Better Dungeons and Cataclysm; their actual JAR versions, placement rules, and multi-chunk overlaps still need in-game verification.
- Existing optional Alex's Caves 2.0.10 source integration retained for NeoForge; Fabric makes no such claim.
- Kept MIT upstream notices and GPL-3.0 integrated NeoForge compatibility obligations.
- No new dungeon structures, dungeon loot or progression bypasses are introduced.

## 2026-10-20 — Cave networks, failed-tile recovery and repository cleanup

### World generation
- **Giant cavern connectivity:** add deterministic, winding passages between neighboring active giant caverns, without dependency on the order in which chunks generate.
- **Cavern altitude weighting:** use block-space sea level for mountain weighting rather than comparing block elevation with a normalized water level.
- **Built-in caves:** reduce the spaghetti, noodle, regular/deep carver and ravine densities across six shipped presets; user-defined presets are not modified.
- **Cross-chunk fluid work:** use a primitive long flood queue and 2-byte bottom-relative ceilings, and only allocate the flood-height table when a water-connected cave is actually discovered. Dry cave chunks avoid this allocation entirely. True cross-chunk aquifer connectivity still requires live tests.

### Memory and reliability
- **Normal tile generation:** release pooled tile arrays when any async worker or filter fails; wait until all workers finish before closing.
- **Failed tile caching:** remove failed tile entries so later requests can retry generation rather than reusing a completed exceptional future indefinitely.
- **Idle scheduler:** disable scheduled polling for caches configured with infinite lifetime (such as active tile caches).
- **Regression:** add an asynchronous tile-failure retry JUnit test.

### Presentation and repository
- Rewrite README to describe actual fork features, supported loaders, credit and compatibility.
- Update NeoForge and Fabric mod metadata, project homepages, issue links and original/fork contributor credits; retain correct MIT and GPL license declarations.
- Consolidate development branches to NeoForge/Fabric 1.21.1 editions, retaining only necessary repository administration branch(es) if GitHub permissions prevent changing the default.

**Validation:** source compilation and targeted tests are required; geometry, fluid seams, Alex's Caves coexistence and user input still require actual Minecraft-world checks. Build results do not constitute a performance benchmark.

---

## 2026-10-20 — Audit completion implementation

### High-priority world generation
- **A04:** Stop resetting every active world's caches when another world initializes. Cache registration and scheduler polling now use weak references to prevent abandoned worlds being kept alive.
- **A05/C05:** Use smaller one-block water terraces and interpolated reach samples. Smooth forced sea/tributary junction drops backwards through the upstream profile, without raising water or exceeding feasible gorge depth.
- **A06:** Detect newly opened cave air touching filled water, propagate that water through connected air up to the source-water elevation, and mark chunk edges for postprocessing. Disconnected caves remain dry.
- **A09:** Reenable the serialized terrain-type exclusion in structure placement rules, including their existing village rule.
- **B03:** Prevent pooled tiles from being retired by timer/capacity while in-flight chunks may be using them; retire on the final chunk drop.

### Compatibility and stability
- **C01 (NeoForge source integrated):** Adapted the existing GPL-3.0 standalone compatibility implementation into the UTF NeoForge module: rare-cave biome routing, structure placement validation, verified `/locateacbiome`, world sampler binding and native cavern protection. Conditional Mixin plugin and compile-only Alex's Caves 2.0.10 dependency keep the port optional. **Remove the standalone tfacfix JAR** when testing this build. Runtime compatibility still requires validation.
- **C03:** Replace HEAD/TAIL surface region context with a scoped MixinExtras method wrapper and `try/finally`, so exceptions cannot leave a stale thread-local region.
- **C04/C05:** Add river mouth/gorge/terrace unit tests and a dual-loader Java 21 release-mode regression workflow.

**Licensing:** The embedded NeoForge Alex's Caves compat sources retain the GPL-3.0 obligations of the separately published fix; original UltraTerraForged MIT credits remain. See `LICENSE-ALEX-COMPAT-GPL-3.0`.

**Validation policy:** See `AUDIT-STATUS.md` for every one of the original 23 audit findings and outstanding runtime/integration tests. Static checks and builds alone do not validate the TNK3 modpack or resolve historic chunks.

---

## Unreleased (Minecraft 1.21.1; fork changes)

### Cave generation
- **Massive caverns:** increased chamber diameters to approximately 230–350 blocks from 80–150, with noise-ragged ceilings, stone pillars and deep lakes preserved.
- **Terrain-aware placement:** giant cavern attempts are more common below elevated terrain; the probability rises with sampled surface elevation instead of moving caverns higher.
- **Deep, protected roofs:** cavern roof limited to Y=30 or lower, with a 30-block overburden from measured terrain. Actual local column height also clips the carve, protecting valleys between samples.
- **Adaptive vertical fit:** cavern heights shrink to available world depth instead of breaking bedrock or poking through terrain; deeper world presets allow taller caverns.
- **Density:** grid reduced from 900 to 640 blocks. Increased default giant-cavern frequency 0.30→0.65; default small horizontal layer-cave feature disabled (players can re-enable).
- **Carver performance:** reject distant columns and vertically excluded blocks before expensive per-block wall-noise evaluation. Deterministic seed/grid placement, chunk independent.

### UI / preset editor
- **Slider mouse interaction:** explicitly process clicks and dragging inside nested scrolling panels instead of depending on indirect value forwarding. Apply accepted normalized values and update labels immediately.
- **Slider focus:** correctly associate clicked scrolling-panel child and release its focus; clear focus when rebuilding lists.
- **Precision:** avoid needlessly casting normalized slider positions to float when rendering value text. Protect stored slider values against NaN or outside-range callback values.

### Stability and correctness
- **Spawn safety:** only override climate spawn search for UltraTerraForged climate samplers, and guard a missing spawn center. Other dimensions use the original Minecraft finder.
- **World-state isolation:** invalidates fallback density cache when the world/generator lookup changes, not merely coordinates; also resets cache before a failed recomputation.
- **Density-function bounds:** advertise conservative numeric bounds because continentalness and weirdness can be negative.
- **Noise restoration:** always restore thread-local sample-cell state using `try/finally` if heightmap evaluation throws.
- **Concurrent cache insertion:** recheck under the write lock before evicting an old entry; avoids needless eviction on racing same-key inserts.
- **Object pooling:** pool resources remain marked released even when the array pool is full.
- **River-noise performance:** skip two lake-domain warp noises for columns outside every river network, retaining the same algorithm for intersecting networks.
- **Raised-water postprocessing:** count pre-existing water as filled when identifying river columns that may spill.

### Additional stability, memory and performance corrections
- **LazyCallable:** distinguish computed-null from not-yet-computed values; Runnable-based adapters now complete once without repeatedly executing or throwing after returning null.
- **Landform cache:** evict only one old site at capacity instead of clearing the entire 4096-entry per-thread cache, retaining hot locations and stable deterministic site geometry.
- **Tile generation:** on worker or filter failure, release allocated tile and pooled arrays after all workers have completed, for both regular terrain and zoomed previews.
- **Performance configuration:** `performance_internal.conf` now actually reads and validates `tileSize`, `batchCount` and `threadCount` rather than always ignoring the file.
- **Generation workers:** cap default worker threads at 8 (or fewer depending on the CPU) to reduce competition with C2ME and JVM memory pressure; `threadCount` is applied after generator initialization, avoiding platform-config access during static executor bootstrap.
- **Release safety:** disable automated publishing to the original project's CurseForge and Modrinth pages; remove the upstream project IDs.

### Validation and scope
- These modifications affect newly generated chunks, **not existing terrain**.
- Initial regression coverage: existing cave/river/option tests and new targeted cavern roof tests; real-world visual checking and mod compatibility tests remain necessary.
- **Integrated in later audit work:** optional NeoForge Alex's Caves source compatibility and cache lifecycle fixes. Cross-chunk aquifer behavior still requires testing in full Minecraft worlds.
- The existing `1.0.2+1.21.1` mod version is intentionally preserved in the test branch for dependencies that pin the version.
