package dev.upcraft.datasync.api.ext;

import dev.upcraft.datasync.api.SyncToken;
import dev.upcraft.datasync.api.util.Entitlements;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface DataSyncPlayerExt {

    CompletableFuture<Entitlements> datasync$getEntitlements();

    <T> CompletableFuture<Optional<T>> datasync$get(SyncToken<T> token);
}
