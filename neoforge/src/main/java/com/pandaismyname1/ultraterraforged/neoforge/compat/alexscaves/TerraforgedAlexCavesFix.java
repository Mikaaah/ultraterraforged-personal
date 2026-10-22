package com.pandaismyname1.ultraterraforged.neoforge.compat.alexscaves;

import com.github.alexmodguy.alexscaves.server.level.biome.ACBiomeMapHolder;
import com.github.alexmodguy.alexscaves.server.level.biome.ACBiomeRegistry;
import com.github.alexmodguy.alexscaves.server.level.biome.BiomeSourceAccessor;
import com.pandaismyname1.ultraterraforged.world.worldgen.UTFRandomState;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.Commands;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.HashSet;

public final class TerraforgedAlexCavesFix {
    public static final String MOD_ID = "ultraterraforged";
    public static final Logger LOG = LogUtils.getLogger();

    public TerraforgedAlexCavesFix(ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, FixConfig.SPEC, "tnkterraforged/alex-caves.toml");
        NeoForge.EVENT_BUS.addListener(this::onLevelLoad);
        NeoForge.EVENT_BUS.addListener(this::onLevelUnload);
        NeoForge.EVENT_BUS.addListener(this::onCommands);
    }

    public static WorldContext context(Climate.Sampler sampler) {
        return ((SamplerAccess) (Object) sampler).tfacfix$getContext();
    }

    private void onLevelLoad(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        var randomState = level.getChunkSource().randomState();
        var access = (SamplerAccess) (Object) randomState.sampler();
        if (!level.dimension().equals(Level.OVERWORLD)
                || !((UTFRandomState) (Object) randomState).usesCells()) {
            access.tfacfix$setContext(null);
            return;
        }
        var terrain = ((UTFRandomState) (Object) randomState).generatorContext();
        if (terrain == null) {
            LOG.warn("TFACFix: active TNKTerraForged world has no initialized terrain context; leaving its sampler untouched.");
            return;
        }
        var registry = level.registryAccess().registryOrThrow(Registries.BIOME);
        var map = new HashMap<net.minecraft.resources.ResourceKey<Biome>, Holder<Biome>>();
        var caveHolders = new HashSet<Holder<Biome>>();
        for (var key : ACBiomeRegistry.ALEXS_CAVES_BIOMES) {
            registry.getHolder(key).ifPresent(holder -> { map.put(key, holder); caveHolders.add(holder); });
        }
        ACBiomeMapHolder.initializeFromRegistry(registry);
        var source = level.getChunkSource().getGenerator().getBiomeSource();
        ((BiomeSourceAccessor) source).expandBiomesWith(caveHolders);
        access.tfacfix$setContext(new WorldContext(level.getSeed(), level.dimension(), terrain, map));
        LOG.info("TFACFix: bound active UltraTerraForged Overworld sampler; {} Alex cave biomes available.", map.size());
    }

    private void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            ((SamplerAccess) (Object) level.getChunkSource().randomState().sampler()).tfacfix$setContext(null);
        }
    }

    private void onCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("tfacfix").requires(source -> source.hasPermission(2))
                .then(Commands.literal("status").executes(command -> {
                    var source = command.getSource();
                    boolean active = context(source.getLevel().getChunkSource().randomState().sampler()) != null;
                    source.sendSuccess(() -> Component.literal("Terraforged Alex's Caves Fix: "
                            + (active ? "active TNKTerraForged world" : "inactive for this generator/dimension")
                            + "; placement=" + FixConfig.ENABLE_PLACEMENT.get()
                            + "; cave protection=" + FixConfig.PROTECT_CAVES.get()), false);
                    return active ? 1 : 0;
                })));
    }
}
