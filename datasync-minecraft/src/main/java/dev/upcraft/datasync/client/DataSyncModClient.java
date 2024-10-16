package dev.upcraft.datasync.client;

import com.mojang.authlib.GameProfile;
import dev.upcraft.datasync.DataSyncMod;
import dev.upcraft.datasync.api.DataSyncAPI;
import dev.upcraft.datasync.api.util.GameProfileHelper;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;

@Environment(EnvType.CLIENT)
public class DataSyncModClient implements ClientModInitializer {

    public static void preloadPlayerData() {
        var profile = Minecraft.getInstance().getUser().getGameProfile();

        // if offline dont try to retrieve data
        if (GameProfileHelper.isOfflineProfile(profile)) {
            DataSyncMod.LOGGER.debug("Offline player detected, not preloading player data");
            return;
        }

        DataSyncAPI.refreshAllPlayerData(profile.getId()).exceptionally(t -> {
            DataSyncMod.LOGGER.error("Unable to preload player data", t);
            return null;
        });
    }

    public static GameProfile getCurrentPlayerProfile() {
        return Minecraft.getInstance().getUser().getGameProfile();
    }

    @Override
    public void onInitializeClient() {

    }
}
