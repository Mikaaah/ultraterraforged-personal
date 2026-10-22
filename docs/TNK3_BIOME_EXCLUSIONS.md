# TNK3 natural biome exclusions

Version 1.0.9 excludes these 24 biome IDs from natural selection in TNKTerraForged worlds. BOP Grassland (`biomesoplenty:grassland`) is kept.

## Biomes O’ Plenty (17)

- `biomesoplenty:cold_desert`
- `biomesoplenty:field`
- `biomesoplenty:forested_field`
- `biomesoplenty:gravel_beach`
- `biomesoplenty:highland`
- `biomesoplenty:marsh`
- `biomesoplenty:moor`
- `biomesoplenty:muskeg`
- `biomesoplenty:prairie`
- `biomesoplenty:pumpkin_patch`
- `biomesoplenty:rocky_shrubland`
- `biomesoplenty:shrubland`
- `biomesoplenty:snowy_fir_clearing`
- `biomesoplenty:tundra`
- `biomesoplenty:volcanic_plains`
- `biomesoplenty:volcano`
- `biomesoplenty:wasteland`

## Regions Unexplored (7)

- `regions_unexplored:shrubland`
- `regions_unexplored:grassland`
- `regions_unexplored:prairie`
- `regions_unexplored:gravel_beach`
- `regions_unexplored:grassy_beach`
- `regions_unexplored:pumpkin_fields`
- `regions_unexplored:dry_bushland`

## Behavior

The final biome selected by the multi-noise source, including TerraBlender, is checked against the exact registry IDs. An excluded result is replaced using a private, filtered Overworld climate search tree. Allowed results do not resample climate. The fallback is initialized on the global TNK terrain sampler and copied to each cached chunk sampler; worlds and dimensions without TNK terrain cells retain their original selection. No BOP/RU implementation classes are linked and neither mod becomes required.

Registrations, blocks, items, commands and saved chunks remain intact. Existing chunks retain their biomes; explore new chunks to see the change. The biome source still advertises its original candidate set for codec/registry compatibility; this policy controls actual XYZ selection. This removes the named biomes, not TNKTerraForged’s independent volcano terrain landform.

Runtime validation with the full TNK3 mod list is separate from the focused regression tests.
