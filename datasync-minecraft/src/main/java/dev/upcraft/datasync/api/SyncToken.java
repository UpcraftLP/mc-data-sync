package dev.upcraft.datasync.api;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface SyncToken<T> {

    /**
     * same as {@link #get(UUID)} but returns a future that will complete once the value has been retrieved from the remote server successfully.
     */
    CompletableFuture<Optional<T>> fetch(UUID playerId);

    /**
     * Retrieves the cached data value, causing it to be loaded if necessary.
     * <br>
     * This operation is <strong>non-blocking</strong> and will always return immediately.
     *
     * @return the cached value if present, or {@link Optional#empty()} otherwise
     */
    Optional<T> get(UUID playerId);

    /**
     * Retrieves the cached data value, causing a refresh if necessary.
     * <br>
     * This operation is non-blocking and will always return immediately.
     *
     * @return the cached value if present, or {@code defaultValue} otherwise
     */
    T getOrDefault(UUID playerId, T defaultValue);

    /**
     * Retrieves the cached data value <strong>without</strong> causing a refresh.
     * @return the cached value
     */
    Optional<T> getCached(UUID playerId);

    /**
     * Updates the remote server with the new data.
     *
     * @throws UnsupportedOperationException if not called on the client
     */
    CompletableFuture<Void> setData(T data);
}
