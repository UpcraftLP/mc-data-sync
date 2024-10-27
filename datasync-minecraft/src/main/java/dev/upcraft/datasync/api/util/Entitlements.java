package dev.upcraft.datasync.api.util;

import dev.upcraft.datasync.DataSyncMod;
import dev.upcraft.datasync.api.SyncToken;
import dev.upcraft.datasync.util.EntitlementsImpl;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.UUID;

public interface Entitlements {

    static SyncToken<Entitlements> token() {
        return DataSyncMod.ENTITLEMENTS_TOKEN;
    }

    static Entitlements getOrEmpty(UUID playerId) {
        return EntitlementsImpl.getOrEmpty(playerId);
    }

    List<ResourceLocation> keys();
}
