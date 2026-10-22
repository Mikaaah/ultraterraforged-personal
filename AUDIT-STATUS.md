# UltraTerraForged 1.21.1 audit completion matrix

The pinned original audit identified **23 findings**. This file tracks the live source changes, not merely the ability to compile. The original audit is `UltraTerraForged_1.21.1_Source_Audit.md` (first-pass static analysis).

| Audit | Implementation | Testing/remaining risk |
| --- | --- | --- |
| A01 | Scope spawn finder to UTF samplers | Compiled; vanilla sampler in-game test needed |
| A02 | Isolate density cache by WorldLookup identity | Compiled |
| A03 | Conservative density function bounds | Compiled |
| A04 | Remove global cache reset at RandomState construction; use weak cache registration and weak scheduler references | Needs repeated create/unload profiling |
| A05 | Redistribute forced river-mouth height discontinuities backwards respecting gorge limits | WaterContinuityTest; real seed checking still necessary |
| A06 | Wake exposed existing fluids after carving; removed chunk-local source-water flood that could fill outdoor air | CaveWaterSafetyTest covers exposed hillside/source conservation; in-game multi-chunk aquifer tests still needed |
| A07 | Count preexisting source water in postprocessing | Compiled |
| A08 | Restore cell context in finally | Compiled |
| A09 | Honor terrain blacklist in structure rule | Need dedicated village/ancient-city seed validation |
| B01 | Parse performance_internal.conf; honor threadCount | Unit-tested on earlier branch |
| B02 | Check cache entry before capacity eviction under writer lock | Compiled |
| B03 | Never age/capacity evict pooled tiles still in use; explicit removal at final chunk drop | Failure/reconnect stress tests needed |
| B04 | Incremental site eviction | Unit-tested earlier |
| B05 | Compute lake warp only for intersecting river networks | Compiled |
| B06 | Bound/configure UTF worker pool | No comparative JFR benchmark |
| B07 | Close failed tile allocations after workers complete | Compiled |
| B08 | Keep pooled resources released after rejected restore | Compiled |
| B09 | Fix LazyCallable Runnable-null completion | Unit-tested earlier |
| C01 | Integrated conditional NeoForge Alex's Caves biome selection, native chamber placement, locate command, context binding and native UTF carve protection | Compile-only 2.0.10 dependency; in-game port tests still required |
| C02 | Fork workflow no longer targets upstream releases | Upstream CurseForge/Modrinth publishing disabled |
| C03 | Wrap full buildSurface in exception-safe SurfaceRegion scope | Mixins on modded server need runtime check |
| C04 | Dedicated water regression tests and dual-loader CI | Full multi-mod NeoForge pack GameTests still needed |
| C05 | One-block water terraces and continuous reach interpolation with gorge-limited receiver smoothing | Must verify nonleaking rivers across seeds |

## Compatibility note

The Alex's Caves integration now lives in **the NeoForge module of UTF itself**, compiled against the optional v2.0.10 port using a compile-only dependency. It does not bundle the port or require it at runtime. When using this integrated build, **REMOVE the older standalone tfacfix JAR** to avoid duplicate Mixin injection. The Alex's Caves 2.0.10 JAR and required Citadel port remain separate. The imported compatibility source is GPL-3.0; see `docs/alex-caves-native-compat.md` and `LICENSE-ALEX-COMPAT-GPL-3.0`.

## Never claim runtime verification from CI compilation

In-game world generation on the actual Minecraft 1.21.1 NeoForge mod list, at known seeds/presets and newly generated chunks, is required before calling this fully validated.


## Post-audit source review (2026-10-20)

The original 23 audit entries are not a complete test or performance certification. Additional checks identified these corrections:

| New finding | Resolution | Verification |
| --- | --- | --- |
| P01: normal terrain failure left allocated pooled arrays alive | `TileGenerator.generate` now closes tiles only after all workers finish unsuccessfully | Both loaders built; failure-injection unit test for retention still desirable |
| P02: failed tile futures remain cached | `TileCache.provide` and `provideIfPresent` discard failed entries on access to permit retry | `FailedTileRetryTest` |
| P03: non-expiring tile cache still polls every 20 ms | Skip scheduler creation for effectively non-expiring caches | Source check; scheduling benchmark recommended |
| P04: large locally flooded cave allocates boxed queue elements and 4-byte per-voxel ceilings | Primitive long FIFO and 2-byte bottom-relative water ceiling storage | Build and existing water tests |
| P05: giant chambers can be isolated | Seed/grid-based corridor linking adjacent active chambers with protected roofs | Geometry tests pass; real-world distribution must be mapped |
| P06: vanilla small-cave density remains high | Reduce spaghetti/noodle/small-carver values in built-in presets, keep cheese caves and custom preset settings | Preset tests pass; new chunks only |
| P07: mountain-weighted cave selection mixes normalized and world-block units | Compare terrain elevation with `levels.waterY` rather than normalized `waterLevel` | Geometry tests pass |

**Remaining integration limitation:** A per-chunk cave flood pass cannot inspect adjacent in-flight chunks. It marks fluid border blocks for vanilla postprocessing but is not a complete cross-chunk hydrodynamic solver. Exact multi-chunk seams require in-game verification and, if reproducible, a separate chunk-safe worldgen-stage reconciliation strategy. No claim is made that the river/bubble defect is wholly eliminated without a seed/preset reproduction.

**Alex's Caves:** compatibility code was authored as Mikaaah's standalone mod before being included here. The original third-party mod dependencies and licenses still apply.
