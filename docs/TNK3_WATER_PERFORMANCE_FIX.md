# TNKTerraForged 1.0.3: raised water and generation overhead

This release fixes raised-water placement and removes repeated terrain calculations from the integrated NeoForge Streams adapter. Terrain changes apply to newly generated chunks.

## Water placement

- A raised-water column now needs a real supporting bed within its 24-block fill range. The previous top-down fill could leave a floating slab of water when no bed was reached.
- Every exposed source block on a water face is scheduled for fluid postprocessing, including the lower blocks. Enclosed pools avoid unnecessary updates. Placement never reads neighboring chunks.
- Downstream water profiles spread abrupt drops across upstream reaches, including steep river junctions. The existing rising terrain profile is preserved to avoid creating unnecessary trenches. River-bank and gorge-depth regressions remain enforced.

## Current-query performance

Native river identity and route position travel with terrain cells. When Streams is installed, a bounded cache retains compact river-column snapshots for up to 2,048 chunks after pooled terrain tiles are released. Dry chunks are remembered too. The old observer Mixin and per-query observer allocations have been removed.

The adapter first reads these snapshots, then completed terrain tiles. Missing data still uses the existing non-blocking pure-terrain fallback. Loaded-water checks, boats, particles, manual Flow Control overrides and separate current height bands are retained.

A local benchmark of 32,768 terrain queries measured 144.65 ms for the previous uncached query path and 4.06 ms for compact cached reads (about 36 times faster). This measures the adapter's terrain-query work, not end-to-end chunk-generation throughput or full-modpack performance.

## Paths and compatibility

Active files are consolidated under `config/tnkterraforged/`, including `streams.toml` and `alex-caves.toml` when those integrations are installed. Existing presets and settings are imported from the previous directories and root-level compatibility files. Current settings take priority; originals are preserved.

New world preset packs are named `tnkterraforged-preset.zip` (or a hashed TNK filename in Create World). Older selected pack names remain readable. The project name, Maven group, build-resource filenames, logger, generated language-provider labels and integrated Streams source package use TNKTerraForged naming. The `ultraterraforged` mod/registry namespace and upstream Java API packages remain intentionally stable for existing worlds, datapacks and external integrations. Historical upstream documentation and license notices retain their original names.

## Validation

- 49 targeted JUnit regressions passed: water placement/continuity, river banks/gorges, cached data and pooling, configuration migration, cave foundations, tile retry/lifecycle, Mixin annotations and preset loading.
- 18 pure flow-rule checks and 7 connected-water search checks passed.
- Four water regressions fail against the previous implementation and pass against the corrected implementation.
- Both loader builds and the NeoForge Streams runtime check are recorded with the release's verification results.

The screenshot's exact seed and coordinates were not supplied. No graphical full-modpack comparison was performed; the included runtime check and terrain regressions cover the isolated generator/integration.
