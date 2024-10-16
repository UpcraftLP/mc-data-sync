package dev.upcraft.datasync.util;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModMetadata;

import java.util.NoSuchElementException;

public class ModHelper {

    private static final String FABRIC_LOADER_ID = "fabricloader";
    private static final String QUILT_LOADER_ID = "quilt_loader";
    private static final String FORGE_LOADER_ID = "forge"; // TODO switch to neoforge when updating to 1.21

    public static ModMetadata getMeta(String modid) {
        return FabricLoader.getInstance().getModContainer(modid).orElseThrow(() -> new NoSuchElementException("Unable to find mod container for ID '%s'".formatted(modid))).getMetadata();
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
        return getMeta("minecraft");
    }

    public static boolean isLoaded(String modid) {
        return FabricLoader.getInstance().isModLoaded(modid);
    }
}
