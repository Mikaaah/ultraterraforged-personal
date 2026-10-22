package com.pandaismyname1.ultraterraforged.world.worldgen.cave;

import org.jetbrains.annotations.Nullable;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.StructureManager;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.ResourceLocation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import com.pandaismyname1.ultraterraforged.world.worldgen.GeneratorContext;
import com.pandaismyname1.ultraterraforged.world.worldgen.cell.Cell;
import com.pandaismyname1.ultraterraforged.world.worldgen.heightmap.Levels;
import com.pandaismyname1.ultraterraforged.world.worldgen.tile.Tile;

/**
 * Carving caves into one chunk, once its surface is built: hollowing out ellipsoids, lining them with other rock, and
 * reading the height of the ground inside and around the chunk.
 */
public final class CaveCarving {
	// no water in what's hollowed out
	public static final int DRY = Integer.MIN_VALUE;
	// the hollow may break through the surface
	public static final int OPEN = Integer.MIN_VALUE;
	private static final BlockState AIR = Blocks.AIR.defaultBlockState();
	private static final BlockState WATER = Blocks.WATER.defaultBlockState();

	public final ChunkAccess chunk;
	public final GeneratorContext context;
	public final Levels levels;
	public final int chunkX;
	public final int chunkZ;
	public final int minX;
	public final int minZ;
	public final int bottom;
	private final int[] ground = new int[256];
	private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
	private final Cell cell = new Cell();
    private final BitSet carvedAir = new BitSet();
    private final BitSet authoredWater = new BitSet();
    private final BitSet protectedPillars = new BitSet();
    // Cached biome quart samples avoid repeatedly decoding holders during huge caves.
    private final Map<Long, Boolean> protectedBiomeCache = new HashMap<>();

    // Starts in this chunk and in neighboring chunks referenced by it. Protect
    // individual pieces: a mineshaft's aggregate bounding box includes vast
    // amounts of empty rock between corridors and must not block cave carving.
    private final List<BoundingBox> protectedStructures;
    private static final boolean PROTECT_STRUCTURES =
        !"false".equalsIgnoreCase(System.getProperty("tnkterraforged.structureProtection", "true"));

    private boolean insideStructure(int x, int y, int z) {
        if (this.protectedStructures.isEmpty()) return false;
        for (BoundingBox bounds : this.protectedStructures) {
            if (x >= bounds.minX() - 4 && x <= bounds.maxX() + 4
                    && y >= bounds.minY() - 4 && y <= bounds.maxY() + 4
                    && z >= bounds.minZ() - 4 && z <= bounds.maxZ() + 4) return true;
        }
        return false;
    }

	// the chunk around this one that ground last read from its tile, or null if its tile wasn't there
	@Nullable
	private Tile.Chunk near;
	private int nearX = Integer.MIN_VALUE;
	private int nearZ = Integer.MIN_VALUE;

    private final net.minecraft.core.RegistryAccess registries;
    private final java.util.Set<net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome>> possibleBiomes;
    private final Map<GiantCaverns.Cavern, CavernDecorations.Style> caverns = new java.util.LinkedHashMap<>();
    private final Map<Long, net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome>> cavernBiomes = new HashMap<>();

    CaveCarving(ChunkAccess chunk, GeneratorContext context) {
        this(chunk, context, null, null, null);
    }

    CaveCarving(ChunkAccess chunk, GeneratorContext context, net.minecraft.core.RegistryAccess registries,
            java.util.Set<net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome>> possibleBiomes) {
        this(chunk, context, registries, possibleBiomes, null);
    }

    CaveCarving(ChunkAccess chunk, GeneratorContext context, net.minecraft.core.RegistryAccess registries,
            java.util.Set<net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome>> possibleBiomes,
            StructureManager structures) {
        this.possibleBiomes = possibleBiomes;
        this.registries = registries;
		this.chunk = chunk;
		this.context = context;
		this.levels = context.levels;
		ChunkPos chunkPos = chunk.getPos();
		this.chunkX = chunkPos.x;
		this.chunkZ = chunkPos.z;
		this.minX = chunkPos.getMinBlockX();
		this.minZ = chunkPos.getMinBlockZ();
		this.bottom = chunk.getMinBuildHeight();
        // Structure references are complete by SURFACE. Resolve neighboring
        // starts through the worldgen region so a jigsaw that began next door
        // protects its pieces here as well. Avoid the lookup for empty refs.
        java.util.stream.Stream<StructureStart> starts = chunk.getAllStarts().values().stream();
        if (PROTECT_STRUCTURES && structures != null && !chunk.getAllReferences().isEmpty()) {
            starts = java.util.stream.Stream.concat(starts,
                    structures.startsForStructure(chunkPos, structure -> true).stream());
        }
        this.protectedStructures = PROTECT_STRUCTURES ? starts
            .filter(StructureStart::isValid)
            .flatMap(start -> start.getPieces().stream().map(StructurePiece::getBoundingBox))
            .filter(b -> b.maxX() >= this.minX - 4 && b.minX() <= this.minX + 19
                      && b.maxZ() >= this.minZ - 4 && b.minZ() <= this.minZ + 19)
            .distinct()
            .toList() : List.of();
		for (int dz = 0; dz < 16; dz++) {
			for (int dx = 0; dx < 16; dx++) {
				this.ground[dz << 4 | dx] = chunk.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, dx, dz);
			}
		}
	}

	/**
	 * The top block of the ground: from the chunk inside it, and from the terrain around it. For carving only: what's
	 * laid out across chunks must use terrainHeight, which is the same from every chunk.
	 */
	public int ground(float x, float z) {
		int bx = (int) Math.floor(x);
		int bz = (int) Math.floor(z);
		int dx = bx - this.minX;
		int dz = bz - this.minZ;
		if (dx >= 0 && dz >= 0 && dx < 16 && dz < 16) {
			return this.ground[dz << 4 | dx];
		}
		// the chunks next to this one are nearly always in tiles already generated: reading those is far cheaper than
		// working out the terrain afresh, which rock shelters would otherwise do over a thousand times a chunk. A chunk
		// can't finish its features, which lets its tile go, while one next to it is still being carved.
		int cx = bx >> 4;
		int cz = bz >> 4;
		if (this.near == null || cx != this.nearX || cz != this.nearZ) {
			Tile tile = this.context.cache != null ? this.context.cache.provideAtChunkIfPresent(cx, cz) : null;
			this.near = tile != null ? tile.getChunkReader(cx, cz) : null;
			this.nearX = cx;
			this.nearZ = cz;
		}
		return this.levels.scale(this.near != null ? this.near.getCell(bx & 15, bz & 15).height : this.cell(bx, bz).height);
	}

	/**
	 * The terrain at a place, straight from the heightmap, before erosion: the same whichever chunk asks, and without
	 * generating the tiles around it, so caves that reach across chunks are laid out the same in each. The cell is
	 * reused by the next call.
	 */
	public Cell cell(float x, float z) {
		this.context.lookup.compute(this.cell.reset(), (int) Math.floor(x), (int) Math.floor(z));
		return this.cell;
	}

	// the top block of the ground there, as cell gives it
	public int terrainHeight(float x, float z) {
		return this.levels.scale(this.cell(x, z).height);
	}

	// whether a box reaches into this chunk
	public boolean reaches(float minX, float minZ, float maxX, float maxZ) {
		return maxX >= this.minX && minX < this.minX + 16 && maxZ >= this.minZ && minZ < this.minZ + 16;
	}

	/**
	 * Hollows out an ellipsoid: water up to the given level, air above it. Bedrock and fluids are left alone, and so
	 * is the ground within roof blocks of the surface, unless roof is OPEN.
	 */
	public void hollow(float cx, float cy, float cz, float rx, float ry, float rz, int water, int roof) {
        this.hollow(cx, cy, cz, rx, ry, rz, water, roof, null);
    }

    // Large connections use the same world-coordinate wall noise in every chunk.
    void hollow(float cx, float cy, float cz, float rx, float ry, float rz, int water, int roof,
            com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Noise walls) {
		int x0 = Math.max(this.minX, (int) Math.floor(cx - rx));
		int x1 = Math.min(this.minX + 15, (int) Math.ceil(cx + rx));
		int z0 = Math.max(this.minZ, (int) Math.floor(cz - rz));
		int z1 = Math.min(this.minZ + 15, (int) Math.ceil(cz + rz));
		if (x0 > x1 || z0 > z1) {
			return;
		}
		int y0 = Math.max(this.bottom + 1, (int) Math.floor(cy - ry));
		int y1 = (int) Math.ceil(cy + ry);
		for (int x = x0; x <= x1; x++) {
			float nx = (x + 0.5F - cx) / rx;
			for (int z = z0; z <= z1; z++) {
				float nz = (z + 0.5F - cz) / rz;
				float flat = nx * nx + nz * nz;
				if (flat >= 1.0F) {
					continue;
				}
				int top = roof == OPEN ? y1 : Math.min(y1, this.ground[(z - this.minZ) << 4 | (x - this.minX)] - roof);
				for (int y = y0; y <= top; y++) {
					float ny = (y + 0.5F - cy) / ry;
                    float shape = flat + ny * ny;
                    // Only erode into the nominal cross-section, preserving the
                    // conservative corridor bounds and the surface roof guard.
                    float roughness = walls == null ? 0.0F
                            : 0.28F * walls.compute(x + y * 0.7F, z - y * 0.7F, 0);
					if (shape + roughness >= 1.0F) {
						continue;
					}
					this.clear(x, y, z, water);
				}
			}
		}
	}

	/**
	 * Hollows out a single block, as hollow does.
	 */
    void protectPillar(int x,int y,int z) {
        this.protectedPillars.set(((y-this.bottom)<<8)|((z-this.minZ)<<4)|(x-this.minX));
    }

	public void clear(int x, int y, int z, int water) {
        if (y >= this.bottom && this.contains(x,z) && this.protectedPillars.get(
                ((y-this.bottom)<<8)|((z-this.minZ)<<4)|(x-this.minX))) return;
        if (this.insideStructure(x, y, z) || this.isProtectedCaveBiome(x, y, z)) return;
		this.pos.set(x, y, z);
		BlockState state = this.chunk.getBlockState(this.pos);
		int bit = ((y - this.bottom) << 8) | ((z - this.minZ) << 4) | (x - this.minX);
		if (state.is(Blocks.BEDROCK) || (!state.getFluidState().isEmpty()
                && !(water == DRY && this.authoredWater.get(bit)))) {
			return;
		}
		BlockState fill = y <= water ? WATER : AIR;
		// Another cave may already be open here. Filling that air makes a
		// free-standing ellipsoid of water in the middle of a large chamber.
		if (fill == WATER && state.isAir()) return;
		if (state != fill) {
            if (fill == WATER) this.authoredWater.set(bit);
            else this.authoredWater.clear(bit);
            if (fill == AIR && !state.isAir() && y >= this.bottom && y < this.chunk.getMaxBuildHeight()) {
                this.carvedAir.set(bit);
            }
            this.chunk.setBlockState(this.pos, fill, false);
        }
	}

	/**
	 * Replaces the solid rock in a shell around an ellipsoid, out to the given thickness in blocks, with a lining,
	 * leaving the surface itself alone. Hollow the ellipsoid after lining it.
	 */
	public void line(float cx, float cy, float cz, float rx, float ry, float rz, float thickness, Lining lining) {
		float outerX = rx + thickness;
		float outerY = ry + thickness;
		float outerZ = rz + thickness;
		int x0 = Math.max(this.minX, (int) Math.floor(cx - outerX));
		int x1 = Math.min(this.minX + 15, (int) Math.ceil(cx + outerX));
		int z0 = Math.max(this.minZ, (int) Math.floor(cz - outerZ));
		int z1 = Math.min(this.minZ + 15, (int) Math.ceil(cz + outerZ));
		if (x0 > x1 || z0 > z1) {
			return;
		}
		int y0 = Math.max(this.bottom + 1, (int) Math.floor(cy - outerY));
		int y1 = (int) Math.ceil(cy + outerY);
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				int top = Math.min(y1, this.ground[(z - this.minZ) << 4 | (x - this.minX)] - 1);
				for (int y = y0; y <= top; y++) {
					float ox = (x + 0.5F - cx) / outerX;
					float oy = (y + 0.5F - cy) / outerY;
					float oz = (z + 0.5F - cz) / outerZ;
					if (ox * ox + oy * oy + oz * oz >= 1.0F) {
						continue;
					}
					this.pos.set(x, y, z);
					BlockState state = this.chunk.getBlockState(this.pos);
					if (state.isAir() || state.is(Blocks.BEDROCK) || !state.getFluidState().isEmpty()) {
						continue;
					}
					if (!this.insideStructure(x, y, z) && !this.isProtectedCaveBiome(x, y, z)) this.chunk.setBlockState(this.pos, lining.at(x, y, z), false);
				}
			}
		}
	}

	/**
	 * The cell of a column of this chunk, as the world was generated from it.
	 */
	public Cell tileCell(int dx, int dz) {
		return this.context.cache.provideAtChunk(this.chunkX, this.chunkZ).getChunkReader(this.chunkX, this.chunkZ).getCell(dx, dz);
	}

	public BlockState get(int x, int y, int z) {
		return this.chunk.getBlockState(this.pos.set(x, y, z));
	}

	public void set(int x, int y, int z, BlockState state) {
        if (this.insideStructure(x, y, z) || this.isProtectedCaveBiome(x, y, z)) return;
		this.chunk.setBlockState(this.pos.set(x, y, z), state, false);
	}

	// whether a block is in this chunk
	public boolean contains(int x, int z) {
		return x >= this.minX && z >= this.minZ && x < this.minX + 16 && z < this.minZ + 16;
	}


    /**
     * Preserve native cave volumes and their own worldgen features without
     * linking either optional mod into TNK's runtime.
     */
    private boolean isProtectedCaveBiome(int x, int y, int z) {
        int qx=QuartPos.fromBlock(x), qy=QuartPos.fromBlock(y), qz=QuartPos.fromBlock(z);
        long key = net.minecraft.core.BlockPos.asLong(qx, qy, qz);
        Boolean cached = this.protectedBiomeCache.get(key);
        if (cached != null) return cached;
        var biomeHolder = this.chunk.getNoiseBiome(qx, qy, qz);
        // Synthetic test chunks and unfinished worldgen may return no holder.
        boolean protectedBiome = biomeHolder != null && biomeHolder.unwrapKey()
            .map(biome -> {
                String namespace = biome.location().getNamespace();
                return "alexscaves".equals(namespace) || "yungscavebiomes".equals(namespace);
            }).orElse(false);
        this.protectedBiomeCache.put(key, protectedBiome);
        return protectedBiome;
    }

    boolean canDecorate(int x,int y,int z) {
        return !this.insideStructure(x,y,z) && !this.isProtectedCaveBiome(x,y,z);
    }

    void addCavern(GiantCaverns.Cavern cavern, CavernDecorations.Style style) {
        this.caverns.put(cavern, style);
    }

    void paintCavernBiome(int x, int y, int z, net.minecraft.resources.ResourceKey<net.minecraft.world.level.biome.Biome> key) {
        if (this.registries == null || this.insideStructure(x,y,z) || this.isProtectedCaveBiome(x,y,z)) return;
        var holder = this.registries.registryOrThrow(net.minecraft.core.registries.Registries.BIOME).getHolder(key);
        holder.filter(value -> this.possibleBiomes == null || this.possibleBiomes.contains(value))
                .ifPresent(value -> this.cavernBiomes.put(BlockPos.asLong(x>>2,y>>2,z>>2),value));
    }

    void decorateCaverns() {
        this.caverns.forEach((cavern, style) -> CavernDecorations.decorate(this,cavern,style));
        if (!this.cavernBiomes.isEmpty()) {
            this.chunk.fillBiomesFromNoise((qx,qy,qz,sampler) -> this.cavernBiomes.getOrDefault(
                    BlockPos.asLong(qx,qy,qz), this.chunk.getNoiseBiome(qx,qy,qz)), null);
        }
    }

    /** Close only isolated one-block pocks made by our carvers, retaining larger cave openings. */
    public void repairSingleBlockHoles() {
        java.util.Map<BlockPos, BlockState> repairs = new java.util.HashMap<>();
        for (int bit = this.carvedAir.nextSetBit(0); bit >= 0; bit = this.carvedAir.nextSetBit(bit + 1)) {
            int x = this.minX + (bit & 15), z = this.minZ + ((bit >>> 4) & 15);
            int y = this.bottom + (bit >>> 8);
            if (x <= this.minX || x >= this.minX + 15 || z <= this.minZ || z >= this.minZ + 15
                    || y <= this.bottom || y >= this.chunk.getMaxBuildHeight() - 1) continue;
            if (!this.chunk.getBlockState(this.pos.set(x,y,z)).isAir()) continue;
            int rock = 0, air = 0;
            BlockState replacement = null;
            for (net.minecraft.core.Direction direction : net.minecraft.core.Direction.values()) {
                BlockState next = this.chunk.getBlockState(this.pos.set(x + direction.getStepX(), y + direction.getStepY(), z + direction.getStepZ()));
                if (next.is(Blocks.STONE) || next.is(net.minecraft.tags.BlockTags.BASE_STONE_OVERWORLD)) {
                    rock++;
                    replacement = next;
                } else if (next.isAir()) air++;
            }
            if (rock >= 5 && rock + air == 6) repairs.put(new BlockPos(x,y,z), replacement);
        }
        repairs.forEach((position, state) -> this.chunk.setBlockState(position, state, false));
    }

    /**
     * Wake existing water exposed by carving. Never manufacture source blocks:
     * a spring opening onto a slope must flow down it, not flood every connected
     * air block up to its height (including open sky within this chunk).
     * Authored sea caves and cavern lakes already supply their own waterline.
     */
    public void reconcileWater() {
        BitSet scheduled = new BitSet();
        for (int bit = this.carvedAir.nextSetBit(0); bit >= 0; bit = this.carvedAir.nextSetBit(bit + 1)) {
            int x = this.minX + (bit & 15);
            int z = this.minZ + ((bit >>> 4) & 15);
            int y = this.bottom + (bit >>> 8);
            this.scheduleWater(x, y + 1, z, scheduled);
            this.scheduleWater(x - 1, y, z, scheduled);
            this.scheduleWater(x + 1, y, z, scheduled);
            this.scheduleWater(x, y, z - 1, scheduled);
            this.scheduleWater(x, y, z + 1, scheduled);
        }
    }

    private void scheduleWater(int x, int y, int z, BitSet scheduled) {
        // Never read or mutate a neighboring generation chunk.
        if (!this.contains(x, z) || y < this.bottom || y >= this.chunk.getMaxBuildHeight()) return;
        int bit = ((y - this.bottom) << 8) | ((z - this.minZ) << 4) | (x - this.minX);
        if (scheduled.get(bit)) return;
        BlockState state = this.chunk.getBlockState(this.pos.set(x, y, z));
        if (state.is(Blocks.WATER) || state.getFluidState().is(net.minecraft.tags.FluidTags.WATER)) {
            scheduled.set(bit);
            this.chunk.markPosForPostprocessing(this.pos);
        }
    }

	/**
	 * What lines a cave, which may vary from block to block.
	 */
	public interface Lining {
		BlockState at(int x, int y, int z);
	}
}
