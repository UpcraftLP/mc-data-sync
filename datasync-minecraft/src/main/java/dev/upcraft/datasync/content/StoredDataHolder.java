package dev.upcraft.datasync.content;

import dev.upcraft.datasync.DataSyncMod;
import dev.upcraft.datasync.api.DataSyncAPI;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class StoredDataHolder<T> {

    private final Object loadingLock = new Object();
    private final DataType<T> type;
    private final UUID playerId;
    private Instant lastUpdated = Instant.MIN;

    @Nullable
    private T value;

    private CompletableFuture<T> loaderFuture;

    private StoredDataHolder(DataType<T> type, UUID playerId) {
        this.type = type;
        this.playerId = playerId;
    }

    private StoredDataHolder(DataType<T> type, UUID playerId, T value) {
        this(type, playerId);
        this.setValue(value);
    }

    public void reload() {
        DataSyncMod.LOGGER.error("BEEEP", new RuntimeException("stacktrace!!!"));
        T previous = this.value;
        if (this.loaderFuture != null) {
            this.loaderFuture.cancel(true);
        }
        this.loaderFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return type.fetchRemote(playerId);
            } catch (Exception e) {
                // unwrap ExecutionException to get the underlying error
                throw new RuntimeException(e.getCause());
            }
        }).exceptionally(t -> {
            DataSyncMod.LOGGER.error("Unable to fetch data for {}[{}]", type.id(), playerId, t);
            return previous;
        }).thenApplyAsync(this::onLoadComplete);
    }

    private T onLoadComplete(T value) {
        synchronized (loadingLock) {
            if (this.isLoading()) {
                this.value = value;
                this.lastUpdated = Instant.now();
                this.loaderFuture = null;
            } else {
                return this.value;
            }
        }
        return value;
    }

    public boolean isLoading() {
        return this.loaderFuture != null;
    }

    public T value() {
        checkExpiry();
        return this.value;
    }

    public void setValue(@Nullable T value) {
        synchronized (this.loadingLock) {
            this.value = value;
            this.lastUpdated = Instant.now();
        }
    }

    public static <T> StoredDataHolder<T> ofValue(DataType<T> type, UUID playerId, T value) {
        return new StoredDataHolder<>(type, playerId, value);
    }

    public static <T> StoredDataHolder<T> load(DataType<T> type, UUID playerId) {
        var value = new StoredDataHolder<>(type, playerId);
        value.reload();
        return value;
    }

    public T or(T defaultValue) {
        if (this.isLoading() || this.value == null) {
            return defaultValue;
        }

        return this.value;
    }

    private void checkExpiry() {
        synchronized (this.loadingLock) {
            if(this.isLoading()) {
                return;
            }
        }

        if (lastUpdated.isBefore(Instant.now().minus(DataSyncAPI.CACHE_DURATION))) {
            this.reload();
        }
    }

    public CompletableFuture<Optional<T>> asFuture() {
        checkExpiry();
        var loader = this.loaderFuture;
        if (loader != null) {
            return loader.thenApply(Optional::ofNullable);
        }

        return CompletableFuture.completedFuture(Optional.ofNullable(this.value));
    }
}
