package dev.upcraft.datasync.api.util;

import com.mojang.authlib.GameProfile;
import dev.upcraft.datasync.client.DataSyncModClient;
import dev.upcraft.datasync.util.ModHelper;
import net.minecraft.core.UUIDUtil;

public class GameProfileHelper {

    public static boolean isOfflineProfile(GameProfile profile) {
        //? <1.21 {
        /*if(!profile.isComplete()) {
            return true;
        }
        *///?}

        //? >=1.21.9 {
        /*var name = profile.name();
        var expectedId = profile.id();
        *///?} else {
        var name = profile.getName();
        var expectedId = profile.getId();
        //?}

        return UUIDUtil.createOfflinePlayerUUID(name).equals(expectedId);
    }

    public static GameProfile getClientProfile() {
        if(!ModHelper.isClientEnv()) {
            throw new UnsupportedOperationException("Attempted to call client-only method on server!");
        }
        return DataSyncModClient.getCurrentPlayerProfile();
    }

    public static boolean isOfflineClientPlayer() {
        return isOfflineProfile(getClientProfile());
    }
}
