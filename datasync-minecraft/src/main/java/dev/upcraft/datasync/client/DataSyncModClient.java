package dev.upcraft.datasync.client;

import com.mojang.authlib.GameProfile;
import dev.upcraft.datasync.DataSyncMod;
import dev.upcraft.datasync.api.util.GameProfileHelper;
import dev.upcraft.datasync.content.DataStore;
import net.minecraft.client.Minecraft;
//? if fabric {
import dev.upcraft.datasync.net.S2CUpdatePlayerDataPacket;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
//?} elif neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
*///?}

//? if fabric {
@Environment(EnvType.CLIENT)
public class DataSyncModClient implements ClientModInitializer {
//?} elif neoforge {
/*@Mod(value = DataSyncMod.MOD_ID, dist = Dist.CLIENT)
public class DataSyncModClient {
*///?}

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

        //? >=1.21.9 {
        /*var id = profile.id();
        *///?} else {
        var id = profile.getId();
        //?}

        DataStore.refresh(id, DataSyncMod.LOGIN_FORCE_REFRESH).exceptionally(t -> {
            DataSyncMod.LOGGER.error("Unable to preload player data", t);
            return null;
        });
    }

    public static GameProfile getCurrentPlayerProfile() {
        //? >=1.21 {
        return Minecraft.getInstance().getGameProfile();
        //?} else {
        /*return Minecraft.getInstance().getUser().getGameProfile();
        *///?}
    }

    //? if fabric {
    @Override
    public void onInitializeClient() {
        S2CUpdatePlayerDataPacket.register();
    }
    //?}
}
