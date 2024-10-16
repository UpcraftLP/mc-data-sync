package dev.upcraft.datasync.api;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface SyncToken<T> {

    CompletableFuture<Optional<T>> get(UUID playerId);

    Optional<T> getCached(UUID playerId);

    CompletableFuture<Void> setData(T data);
}
