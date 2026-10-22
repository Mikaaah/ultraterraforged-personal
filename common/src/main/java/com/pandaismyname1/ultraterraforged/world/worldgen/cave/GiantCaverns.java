package com.pandaismyname1.ultraterraforged.world.worldgen.cave;

import com.pandaismyname1.ultraterraforged.world.worldgen.landform.SiteCache;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.NoiseUtil;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Noise;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Noises;

/**
 * Vast, predominantly deep underground caverns. High terrain increases the
 * CHANCE of a chamber, not its elevation: cavern roofs stay at Y30 or below,
 * with at least 30 blocks of terrain above the sampled footprint.
 *
 * Every chamber is based only on seed and grid position, so adjacent chunks
 * carve the same shape regardless of chunk generation order.
 */
final class GiantCaverns implements CaveFeatures.Feature {
    private static final float GRID = 640.0F;
    private static final int MIN_ROOF_COVER = 30;
    private static final int MAX_ROOF_Y = 30;
    private static final Cavern NONE = new Cavern(0, 0, 0, 0, 0, 0, Integer.MIN_VALUE, 0);

    private final int seed;
    private final float frequency;
    private final Noise walls;
    private final SiteCache<Cavern> caverns = SiteCache.make();

    record Cavern(float x, float y, float z, float radiusX, float radiusY,
                  float radiusZ, int roof, int terrainHeight) {}

    GiantCaverns(int seed, float frequency) {
        this.seed = seed;
        this.frequency = NoiseUtil.clamp(frequency, 0.0F, 1.0F);
        this.walls = Noises.perlin(seed + 1, 24, 2);
    }

    /** Return a stable active site, independent of the requesting chunk. */
    private Cavern active(int gx, int gz, CaveCarving carving) {
        float draw = CaveFeatures.random(this.seed, gx, gz, 0);
        if (draw >= this.frequency) return NONE;
        Cavern c = this.caverns.get(gx, gz, (x, z) -> this.find(x, z, carving));
        if (c == NONE) return NONE;
        float highland = NoiseUtil.clamp(
            (c.terrainHeight() - carving.levels.waterY - 18.0F) / 120.0F, 0.0F, 1.0F);
        return draw < this.frequency * (0.42F + 0.58F * highland) ? c : NONE;
    }

    @Override
    public void carve(CaveCarving carving) {
        int gridX = NoiseUtil.floor(carving.minX / GRID);
        int gridZ = NoiseUtil.floor(carving.minZ / GRID);
        for (int gx = gridX - 1; gx <= gridX + 1; gx++) {
            for (int gz = gridZ - 1; gz <= gridZ + 1; gz++) {
                Cavern cavern = this.active(gx, gz, carving);
                if (cavern == NONE) continue;
                float reach = Math.max(cavern.radiusX(), cavern.radiusZ()) * 1.22F;
                if (carving.reaches(cavern.x() - reach, cavern.z() - reach,
                                    cavern.x() + reach, cavern.z() + reach)) {
                    this.hollow(cavern, carving);
                }
            }
        }
        // Protect every chamber's supporting rock before any connecting passage
        // is cut. Later cave features must not sever the bases of these pillars.
        for (int gx = gridX - 1; gx <= gridX + 1; gx++) {
            for (int gz = gridZ - 1; gz <= gridZ + 1; gz++) {
                Cavern cavern = this.active(gx,gz,carving);
                if (cavern == NONE) continue;
                this.connect(cavern, this.active(gx + 1, gz, carving), carving);
                this.connect(cavern, this.active(gx, gz + 1, carving), carving);
            }
        }
    }

    void prepareDecorations(CaveCarving carving) {
        int gx=NoiseUtil.floor(carving.minX/GRID),gz=NoiseUtil.floor(carving.minZ/GRID);
        for (int x=gx-1;x<=gx+1;x++) for (int z=gz-1;z<=gz+1;z++) {
            Cavern c=this.active(x,z,carving);
            if (c==NONE) continue;
            float reach=Math.max(c.radiusX(),c.radiusZ())*1.22F;
            if (carving.reaches(c.x()-reach,c.z()-reach,c.x()+reach,c.z()+reach))
                carving.addCavern(c,this.style(c));
        }
    }

    /**
     * Wide corridors between adjacent giant chambers. Position and geometry
     * are pure functions of world seed and grid coordinates: no chunk ordering.
     * Protect the surface with the normal hollow() ground-depth constraint.
     */
    private void connect(Cavern from, Cavern to, CaveCarving carving) {
        if (to == NONE) return;
        float dx = to.x() - from.x(), dz = to.z() - from.z();
        float length = (float)Math.sqrt(dx * dx + dz * dz);
        if (length < 1.0F) return;
        float radius = 17.0F;
        if (!carving.reaches(Math.min(from.x(), to.x()) - radius,
                            Math.min(from.z(), to.z()) - radius,
                            Math.max(from.x(), to.x()) + radius,
                            Math.max(from.z(), to.z()) + radius)) return;
        int steps = Math.max(1, (int)Math.ceil(length / 12.0F));
        float sideX = -dz / length, sideZ = dx / length;
        float deviation = (CaveFeatures.random(this.seed,
            (int)Math.floor(from.x() / GRID), (int)Math.floor(from.z() / GRID), 12) - 0.5F) * 38.0F;
        // Sine sway is perpendicular to the chord. Projection onto the
        // chord gives a conservative exact segment window for this chunk,
        // without visiting every point of every ~640-block corridor.
        long window = corridorStepWindow(from.x(), from.z(), dx, dz,
                carving.minX, carving.minZ, steps, radius + 3.0F);
        int first = (int)(window >>> 32), last = (int)window;
        for (int i = first; i <= last; i++) {
            float t = i / (float)steps;
            float sway = (float)Math.sin(t * Math.PI) * deviation;
            float x = from.x() + dx * t + sideX * sway;
            float z = from.z() + dz * t + sideZ * sway;
            if (!carving.reaches(x - radius - 3, z - radius - 3,
                                 x + radius + 3, z + radius + 3)) continue;
            float y = NoiseUtil.lerp(from.y(), to.y(), t) +
                      (float)Math.sin(t * Math.PI * 3.0F) * 3.0F;
            // The tunnel must not puncture Y30 or the actual surface above.
            y = Math.min(y, MAX_ROOF_Y - 13.0F);
            y = Math.max(y, carving.bottom + 17.0F);
            if (y + 12.0F > MAX_ROOF_Y) continue;
            float width = radius * (0.78F + 0.22F * this.walls.compute(x, z, 0));
            float height = 9.0F + 3.0F * this.walls.compute(x + 718, z - 931, 0);
            carving.hollow(x, y, z, width, height, width,
                           CaveCarving.DRY, MIN_ROOF_COVER, this.walls);
        }
    }

    /**
     * Inclusive conservative segment window intersecting a padded chunk.
     * Curving perpendicular to the segment never changes this projection.
     * Extra one-step margin avoids float-rounding edge losses.
     */
    static long corridorStepWindow(float x0, float z0, float dx, float dz,
                                   int minX, int minZ, int steps, float pad) {
        double len2 = (double)dx * dx + (double)dz * dz;
        if (len2 < 1.0 || steps <= 0) return 0L;
        double left = minX - pad, right = minX + 15.0 + pad;
        double near = minZ - pad, far = minZ + 15.0 + pad;
        double xLow = (dx >= 0 ? left : right) - x0;
        double xHigh = (dx >= 0 ? right : left) - x0;
        double zLow = (dz >= 0 ? near : far) - z0;
        double zHigh = (dz >= 0 ? far : near) - z0;
        int first = Math.max(0, (int)Math.floor((xLow * dx + zLow * dz) / len2 * steps) - 1);
        int last = Math.min(steps, (int)Math.ceil((xHigh * dx + zHigh * dz) / len2 * steps) + 1);
        return ((long)first << 32) | (last & 0xFFFFFFFFL);
    }

    // Package-visible for the waterline/bedrock pillar regression test.
    void hollow(Cavern cavern, CaveCarving carving) {
        var pillars = this.pillarsFor(carving);
        int lake = (int) (cavern.y() - cavern.radiusY() * 0.53F);
        int y0 = Math.max(carving.bottom + 3, (int) (cavern.y() - cavern.radiusY() * 1.12F));
        int y1 = Math.min(cavern.roof(), (int) (cavern.y() + cavern.radiusY() * 1.12F));
        Section[][] sections = new Section[y1-y0+1][];
        for (int y=y0;y<=y1;y++) {
            sections[y-y0]=new Section[pillars.size()];
            for (int i=0;i<pillars.size();i++) sections[y-y0][i]=section(pillars.get(i),y,cavern);
        }
        for (int dz = 0; dz < 16; dz++) {
            for (int dx = 0; dx < 16; dx++) {
                int x = carving.minX + dx;
                int z = carving.minZ + dz;
                float nx = (x - cavern.x()) / cavern.radiusX();
                float nz = (z - cavern.z()) / cavern.radiusZ();
                float flat = nx * nx + nz * nz;
                if (flat > 1.35F) continue;
                // Terrain above the cavern is protected even at the edge of a
                // steep valley that falls between the 9 site-height probes.
                int top = Math.min(y1, carving.ground(x, z) - MIN_ROOF_COVER);
                for (int y = y0; y <= top; y++) {
                    float ny = (y - cavern.y()) / cavern.radiusY();
                    float shape = flat + ny * ny;
                    if (shape >= 1.35F) continue; // no expensive wall noise needed
                    float ragged = (this.walls.compute(x + y * 0.7F, z - y * 0.7F, 0) - 0.5F) * 0.7F;
                    if (shape + ragged >= 1.0F) continue;
                    boolean pillar=false;
                    for (Section section : sections[y-y0]) if (section.contains(x,z,ragged)) {pillar=true;break;}
                    if (pillar) {carving.protectPillar(x,y,z);continue;}
                    carving.clear(x, y, z, lake);
                }
            }
        }
    }

    record Pillar(float x, float z, float waist, float aspect, float phase) {}

    java.util.List<Pillar> pillarsFor(CaveCarving carving) {
        var result = new java.util.ArrayList<Pillar>();
        int gx = Math.floorDiv(carving.minX, 64), gz = Math.floorDiv(carving.minZ, 64);
        for (int x = gx - 1; x <= gx + 1; x++) for (int z = gz - 1; z <= gz + 1; z++) {
            if (CaveFeatures.random(this.seed, x, z, 30) > 0.72F) continue;
            float px = x * 64 + 16 + CaveFeatures.random(this.seed,x,z,31) * 32;
            float pz = z * 64 + 16 + CaveFeatures.random(this.seed,x,z,32) * 32;
            if (!carving.reaches(px-48,pz-48,px+48,pz+48)) continue;
            result.add(new Pillar(px,pz,4 + CaveFeatures.random(this.seed,x,z,33)*5,
                    0.7F + CaveFeatures.random(this.seed,x,z,34)*0.7F,
                    CaveFeatures.random(this.seed,x,z,35)*NoiseUtil.PI2));
        }
        return result;
    }

    static boolean isPillar(java.util.List<Pillar> pillars, int x, int y, int z, Cavern cavern) {
        for (Pillar pillar : pillars) if (pillarAt(pillar,x,y,z,cavern)) return true;
        return false;
    }

    record Section(float x,float z,float aspect,float radiusSquared) {
        boolean contains(int x,int z,float roughness) {
            float dx=(x-this.x)/this.aspect, dz=(z-this.z)*this.aspect;
            return dx*dx+dz*dz < this.radiusSquared*(1+roughness*0.3F);
        }
    }

    static Section section(Pillar p,int y,Cavern c) {
        float ny=Math.abs((y-c.y())/c.radiusY());
        float radius=p.waist()+13*ny*ny*(0.7F+0.3F*ny)
                +1.2F*(float)Math.sin(y*0.13F+p.phase());
        return new Section(p.x()+2*(float)Math.sin(y*0.055F+p.phase()),
                p.z()+2*(float)Math.cos(y*0.047F+p.phase()),
                p.aspect()*(1+0.16F*(float)Math.sin(y*0.075F+p.phase())), radius*radius);
    }

    static boolean pillarAt(Pillar p,int x,int y,int z,Cavern c) {
        return section(p,y,c).contains(x,z,0);
    }

    CavernDecorations.Style style(Cavern cavern) {
        float draw = CaveFeatures.random(this.seed, NoiseUtil.floor(cavern.x()/GRID),
                NoiseUtil.floor(cavern.z()/GRID), 20);
        return draw < 0.45F ? CavernDecorations.Style.LUSH
                : draw < 0.8F ? CavernDecorations.Style.DRIPSTONE : CavernDecorations.Style.LANTERN;
    }

    Cavern find(int gridX, int gridZ, CaveCarving carving) {
        float margin = 185.0F;
        float x = gridX * GRID + margin + CaveFeatures.random(this.seed, gridX, gridZ, 1) * (GRID - 2 * margin);
        float z = gridZ * GRID + margin + CaveFeatures.random(this.seed, gridX, gridZ, 2) * (GRID - 2 * margin);
        float radiusX = NoiseUtil.lerp(115.0F, 175.0F, CaveFeatures.random(this.seed, gridX, gridZ, 3));
        float radiusZ = NoiseUtil.lerp(105.0F, 165.0F, CaveFeatures.random(this.seed, gridX, gridZ, 4));
        float desiredY = NoiseUtil.lerp(42.0F, 66.0F, CaveFeatures.random(this.seed, gridX, gridZ, 5));

        int lowest = Integer.MAX_VALUE;
        for (int i = 0; i < 9; i++) {
            float angle = i * NoiseUtil.PI2 / 8.0F;
            float r = i == 8 ? 0.0F : 1.15F;
            lowest = Math.min(lowest, carving.terrainHeight(x + NoiseUtil.cos(angle) * radiusX * r,
                                                               z + NoiseUtil.sin(angle) * radiusZ * r));
        }
        int roof = Math.min(MAX_ROOF_Y, lowest - MIN_ROOF_COVER);
        // On a vanilla -64 bottom, Y30 allows roughly 80 blocks of cavern;
        // deeper presets naturally support taller caverns up to ~130 blocks.
        float maxRadiusY = (roof - carving.bottom - 9.0F) * 0.49F;
        float radiusY = Math.min(desiredY, maxRadiusY);
        if (radiusY < 18.0F) return NONE;
        float y = roof - radiusY;
        if (y - radiusY < carving.bottom + 5) return NONE;
        return new Cavern(x, y, z, radiusX, radiusY, radiusZ, roof, lowest);
    }
}
