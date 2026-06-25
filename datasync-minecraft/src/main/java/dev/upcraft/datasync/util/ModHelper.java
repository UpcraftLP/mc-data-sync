package dev.upcraft.datasync.util;

import dev.upcraft.datasync.DataSyncMod;
//?if fabric {
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
//?} elif neoforge {
/*import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.LoadingModList;
*///?}

import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class ModHelper {

    private static final String FABRIC_LOADER_ID = "fabricloader";
    private static final String QUILT_LOADER_ID = "quilt_loader";

    //? >1.20.1 {
    private static final String FORGE_LOADER_ID = "neoforge";
    //?} else {
    /*private static final String FORGE_LOADER_ID = "forge";
     *///?}

    @Nullable
    public static ModMetadata getMeta(String modid) {
        //? if fabric {
        var meta = FabricLoader.getInstance().getModContainer(modid).map(ModContainer::getMetadata).orElse(null);
        return meta != null ? new ModMetadata(meta.getId(), meta.getName(), meta.getVersion().getFriendlyString()) : null;
        //?} elif neoforge {
        /*var modlist = ModList.get();
        if(modlist != null) {
            for (var mod : modlist.getMods()) {
                if(modid.equals(mod.getModId())) {
                    return new ModMetadata(mod.getModId(), mod.getDisplayName(), mod.getVersion().toString());
                }
            }

            return null;
        }

        //? if <26.1 {
        var loadingModList = LoadingModList.get();
        //?} else {
        /^var loadingModList = FMLLoader.getCurrent().getLoadingModList();
        ^///?}
        for (var mod : loadingModList.getMods()) {
            if(modid.equals(mod.getModId())) {
                return new ModMetadata(mod.getModId(), mod.getDisplayName(), mod.getVersion().toString());
            }
        }

        return null;
        *///?}
    }

    public static ModMetadata getSelfMeta() {
        return Objects.requireNonNull(ModHelper.getMeta(DataSyncMod.MOD_ID), "DataSync not found!");
    }

    public static ModMetadata getLoaderMeta() {
        // Forge via Sinytra Connector
        if(isLoaded(FORGE_LOADER_ID)) {
            return getMeta(FORGE_LOADER_ID);
        }

        if(isLoaded(QUILT_LOADER_ID)) {
            return getMeta(QUILT_LOADER_ID);
        }

        // if nothing else, it has to be plain old Fabric
        return getMeta(FABRIC_LOADER_ID);
    }

    public static ModMetadata getGameMeta() {
        return Objects.requireNonNull(getMeta("minecraft"), "Minecraft not found!");
    }

    public static boolean isLoaded(String modid) {
        //? if fabric {
        return FabricLoader.getInstance().isModLoaded(modid);
        //?} elif neoforge {
        /*var modlist = ModList.get();
        if(modlist != null) {
            return modlist.isLoaded(modid);
        }

        //? if <26.1 {
        var loadingModList = LoadingModList.get();
         //?} else {
        /^var loadingModList = FMLLoader.getCurrent().getLoadingModList();
        ^///?}
        for (var mod : loadingModList.getMods()) {
            if (modid.equals(mod.getModId())) {
                return true;
            }
        }

        return false;
        *///?}
    }

    public static boolean isClientEnv() {
        //? if fabric {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
        //?} elif neoforge && <26.1 {
        /*return FMLLoader.getDist().isClient();
        *///?} elif neoforge {
        /*return FMLLoader.getCurrent().getDist().isClient();
        *///?}
    }
}
