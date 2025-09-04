package dev.upcraft.datasync.api.util;

import com.mojang.authlib.GameProfile;
import dev.upcraft.datasync.client.DataSyncModClient;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
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

    @Environment(EnvType.CLIENT)
    public static GameProfile getClientProfile() {
        return DataSyncModClient.getCurrentPlayerProfile();
    }

    @Environment(EnvType.CLIENT)
    public static boolean isOfflineClientPlayer() {
        return isOfflineProfile(getClientProfile());
    }
}
