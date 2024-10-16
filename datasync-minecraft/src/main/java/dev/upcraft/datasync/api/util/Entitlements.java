package dev.upcraft.datasync.api.util;

import dev.upcraft.datasync.DataSyncMod;
import dev.upcraft.datasync.api.SyncToken;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public interface Entitlements {

    static SyncToken<Entitlements> token() {
        return DataSyncMod.ENTITLEMENTS_TOKEN;
    }

    List<ResourceLocation> keys();
}
