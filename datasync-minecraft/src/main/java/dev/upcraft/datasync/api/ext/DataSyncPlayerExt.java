package dev.upcraft.datasync.api.ext;

import dev.upcraft.datasync.api.SyncToken;
import dev.upcraft.datasync.api.util.Entitlements;

import java.util.Optional;

public interface DataSyncPlayerExt {

    Entitlements datasync$getEntitlements();

    <T> Optional<T> datasync$get(SyncToken<T> token);

    <T> T datasync$getOrDefault(SyncToken<T> token, T defaultValue);
}
