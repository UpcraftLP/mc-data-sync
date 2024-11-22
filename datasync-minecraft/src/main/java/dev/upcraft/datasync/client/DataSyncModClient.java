package dev.upcraft.datasync.client;

import com.mojang.authlib.GameProfile;
import dev.upcraft.datasync.DataSyncMod;
import dev.upcraft.datasync.api.util.GameProfileHelper;
import dev.upcraft.datasync.content.DataStore;
import dev.upcraft.datasync.net.S2CUpdatePlayerDataPacket;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

@Environment(EnvType.CLIENT)
public class DataSyncModClient implements ClientModInitializer {

    public static final SessionStore SESSION_STORE = new SessionStore();

    public static void preloadPlayerData() {
        if (!DataSyncMod.LOGIN_AUTOFETCH) {
            return;
        }

        var profile = Minecraft.getInstance().getUser().getGameProfile();

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
        return Minecraft.getInstance().getUser().getGameProfile();
    }

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(S2CUpdatePlayerDataPacket.TYPE, S2CUpdatePlayerDataPacket::handle);
    }
}
