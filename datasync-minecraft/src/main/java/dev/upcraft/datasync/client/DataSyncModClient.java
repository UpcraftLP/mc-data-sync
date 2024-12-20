package dev.upcraft.datasync.client;

import com.mojang.authlib.GameProfile;
import dev.upcraft.datasync.DataSyncMod;
import dev.upcraft.datasync.api.util.GameProfileHelper;
import dev.upcraft.datasync.content.DataStore;
import dev.upcraft.datasync.net.S2CUpdatePlayerDataPacket;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;

@Environment(EnvType.CLIENT)
public class DataSyncModClient implements ClientModInitializer {

    public static final SessionStore SESSION_STORE = new SessionStore();

    public static void preloadPlayerData() {
        if (!DataSyncMod.LOGIN_AUTOFETCH) {
            return;
        }

        var profile = getCurrentPlayerProfile();

        // if offline dont try to retrieve data
        if (GameProfileHelper.isOfflineProfile(profile)) {
            DataSyncMod.LOGGER.debug("Offline player detected, not preloading player data");
            return;
        }

        DataStore.refresh(profile.getId(), DataSyncMod.LOGIN_FORCE_REFRESH).exceptionally(t -> {
            DataSyncMod.LOGGER.error("Unable to preload player data", t);
            return null;
        });
    }

    public static GameProfile getCurrentPlayerProfile() {
        //? <1.21 {
        /*return Minecraft.getInstance().getUser().getGameProfile();
        *///?} else {
        return Minecraft.getInstance().getGameProfile();
        //?}
    }

    @Override
    public void onInitializeClient() {
        S2CUpdatePlayerDataPacket.register();
    }
}
