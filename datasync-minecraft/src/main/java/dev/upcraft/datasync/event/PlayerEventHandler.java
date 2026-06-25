package dev.upcraft.datasync.event;

import com.mojang.authlib.GameProfile;
import dev.upcraft.datasync.DataSyncMod;
import dev.upcraft.datasync.content.DataStore;
import net.minecraft.server.level.ServerPlayer;

public class PlayerEventHandler {

    public static void onPlayerJoin(ServerPlayer player) {
        if (DataSyncMod.LOGIN_AUTOFETCH) {
            GameProfile profile = player.getGameProfile();
            //? >=1.21.9 {
                /*var profileId = profile.id();
                var profileName = profile.name();
                *///?} else {
            var profileId = profile.getId();
            var profileName = profile.getName();
            //?}
            DataStore.refresh(profileId, DataSyncMod.LOGIN_FORCE_REFRESH).thenRunAsync(() -> DataSyncMod.LOGGER.debug("loaded player data for '{}' ({})", profileName, profileId));
        }
    }
}
