# TNKTerraForged 1.0.8: structure compatibility

Install the NeoForge 1.21.1 TNKTerraForged JAR alongside the desired structure mods and their own required dependencies. TNK does not include or replace those mods. Keep the existing `ultraterraforged` world type and choose the intended TNK3 terrain preset when making a new world.

The supplied JARs include ATi Structures, Epic Structures: Villages, YUNG's Bridges, Cave Biomes, Better Dungeons, Mineshafts, Desert and Jungle Temples, Ocean Monuments, Witch Huts and Nether Fortresses; Integrated API, IDAS, Integrated Dungeons Arise, Integrated Stronghold, Cataclysm, Mowzie's Mobs, Seven Seas and Patches. Most register native or custom jigsaw structures. Epic Structures replaces vanilla village templates, so its pieces continue to use vanilla village placement. YUNG's Bridges uses a placed feature in river biomes rather than a structure start.

TNK now resolves structure references while carving each chunk. A large structure that begins in a neighboring chunk therefore protects its bounding box here, including the four-block margin, from custom caves, lining and cavern decoration. Structures whose starts have not been recorded at this generation stage cannot supply a bounding box yet; later feature placement remains under the owning mod's control. Vanilla caves and structures retain their normal generator order.

YUNG's Frosted and Lost Caves keep their own biome cells and blocks where TNK custom cave carving or cavern styles overlap them. Alex's Caves protection continues to work the same way. No optional mod class is loaded by this compatibility path, so the TNK JAR runs without these mods.

Changes apply to new chunks. Existing terrain and structures do not change when updating a JAR; switch presets in a newly created world. The focused regression checks the neighboring-start bounding box and preservation of an external cave biome. Full TNK3 pack placement frequencies, library dependencies and exact player-seed layouts need an in-game check with the complete pack.
