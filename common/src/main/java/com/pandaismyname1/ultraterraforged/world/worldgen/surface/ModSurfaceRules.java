package com.pandaismyname1.ultraterraforged.world.worldgen.surface;

import java.util.HashMap;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.SurfaceRules;
import com.pandaismyname1.ultraterraforged.UTFCommon;
import com.pandaismyname1.ultraterraforged.compat.terrablender.TBCompat;
import com.pandaismyname1.ultraterraforged.platform.ModLoaderUtil;
import com.pandaismyname1.ultraterraforged.world.worldgen.surface.condition.UTFSurfaceConditions;

/** Runtime bridge: native biome surfaces first, TNK terrain surfaces as fallback. */
public final class ModSurfaceRules {
    private ModSurfaceRules() {}

    @SuppressWarnings("unchecked")
    public static SurfaceRules.RuleSource wrap(SurfaceRules.RuleSource base, RegistryAccess registries) {
        if (TBCompat.isEnabled()) base = terraBlender(base);
        if (!ModLoaderUtil.isLoaded("natures_spirit")) return base;
        ResourceKey<Biome>[] biomes = registries.registryOrThrow(Registries.BIOME).registryKeySet().stream()
                .filter(key -> key.location().getNamespace().equals("natures_spirit")).toArray(ResourceKey[]::new);
        if (biomes.length == 0) return base;
        var nativeRules = natureSpirit();
        if (nativeRules != null) nativeRules = adaptNative(nativeRules);
        // Cypress and other ordinary fields use the vanilla grass/dirt default; the native rule set only
        // describes exceptional surfaces. Actual TNK ground can be below the
        // vanilla preliminary surface estimate, so provide its soil explicitly.
        var cypress = SurfaceRules.ifTrue(SurfaceRules.isBiome(biomes),
                SurfaceRules.ifTrue(UTFSurfaceConditions.nearGround(5), SurfaceRules.ifTrue(SurfaceRules.not(SurfaceRules.steep()), SurfaceRules.sequence(
                    SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SurfaceRules.sequence(
                        SurfaceRules.ifTrue(SurfaceRules.waterBlockCheck(0,0), SurfaceRules.state(Blocks.GRASS_BLOCK.defaultBlockState())),
                        SurfaceRules.state(Blocks.DIRT.defaultBlockState()))),
                    SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, SurfaceRules.state(Blocks.DIRT.defaultBlockState()))))));
        return nativeRules == null ? SurfaceRules.sequence(cypress,base)
                : SurfaceRules.sequence(SurfaceRules.ifTrue(SurfaceRules.isBiome(biomes),nativeRules),cypress,base);
    }

    static SurfaceRules.RuleSource adaptNative(SurfaceRules.RuleSource nativeRules) {
        // Once per rule set, through the public vanilla codecs. Preserve the
        // original biome/noise/material tests, but admit the actual TNK surface
        // where vanilla's preliminary height estimate misses it.
        try {
            var json=SurfaceRules.RuleSource.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,nativeRules).getOrThrow();
            return SurfaceRules.RuleSource.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,adaptConditions(json)).getOrThrow();
        } catch (RuntimeException failure) {
            UTFCommon.LOGGER.warn("Could not adapt native surface height checks; retaining native rules",failure);
            return nativeRules;
        }
    }

    private static com.google.gson.JsonElement adaptConditions(com.google.gson.JsonElement json) {
        if (json.isJsonObject()) {
            var object=json.getAsJsonObject();
            if (object.has("type") && (object.get("type").getAsString().equals("minecraft:above_preliminary_surface")
                    || object.get("type").getAsString().equals("above_preliminary_surface"))) {
                return SurfaceRules.ConditionSource.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,
                        UTFSurfaceConditions.any(SurfaceRules.abovePreliminarySurface(),UTFSurfaceConditions.nearGround(8))).getOrThrow();
            }
            var copy=new com.google.gson.JsonObject();
            object.entrySet().forEach(entry->copy.add(entry.getKey(),adaptConditions(entry.getValue())));
            return copy;
        }
        if (json.isJsonArray()) {
            var copy=new com.google.gson.JsonArray();
            json.getAsJsonArray().forEach(value->copy.add(adaptConditions(value)));
            return copy;
        }
        return json;
    }

    private static SurfaceRules.RuleSource natureSpirit() {
        try {
            // No dependency on Nature's Spirit classes when the mod is absent.
            // Its own rules retain chalk, sandy soil, kaolin and other native materials.
            return (SurfaceRules.RuleSource)Class.forName("net.hibiscus.naturespirit.world.NSSurfaceRules")
                    .getMethod("makeRules").invoke(null);
        } catch (ReflectiveOperationException | LinkageError error) {
            UTFCommon.LOGGER.warn("Could not load Nature's Spirit surface rules; retaining terrain fallback", error);
            return null;
        }
    }

    private static SurfaceRules.RuleSource terraBlender(SurfaceRules.RuleSource base) {
        var existing=new HashMap<String,SurfaceRules.RuleSource>();
        // NoiseGeneratorSettings may already have been wrapped by TerraBlender.
        // Unwrap it so its minecraft default cannot mask the TNK fallback.
        while (base instanceof terrablender.worldgen.surface.NamespacedSurfaceRuleSource wrapped) {
            wrapped.sources().forEach(existing::putIfAbsent);
            base=wrapped.base();
        }
        var all = terrablender.api.SurfaceRuleManager.getNamespacedRules(
                terrablender.api.SurfaceRuleManager.RuleCategory.OVERWORLD,base);
        if (all instanceof terrablender.worldgen.surface.NamespacedSurfaceRuleSource namespaced) {
            var sources = new HashMap<>(namespaced.sources());
            sources.putAll(existing);
            sources.remove("minecraft");
            return new terrablender.worldgen.surface.NamespacedSurfaceRuleSource(base,sources);
        }
        return base;
    }
}
