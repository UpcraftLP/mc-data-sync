package dev.upcraft.datasync.content;

import dev.upcraft.datasync.DataSyncMod;
import net.minecraft.resources.ResourceLocation;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class DataStore {

    private static final Map<UUID, Map<ResourceLocation, StoredDataHolder<?>>> globalStore = new HashMap<>();

    @SuppressWarnings("unchecked")
    public static <T> StoredDataHolder<T> getPlayerLookup(UUID playerId, DataType<T> type) {
        return (StoredDataHolder<T>) (globalStore.computeIfAbsent(playerId, key -> new HashMap<>()).computeIfAbsent(type.id(), id -> StoredDataHolder.load(type, playerId)));
    }

    @SuppressWarnings("unchecked")
    public static <T> StoredDataHolder<T> getPlayerLookupEmpty(UUID playerId, DataType<T> type) {
        return (StoredDataHolder<T>) (globalStore.computeIfAbsent(playerId, key -> new HashMap<>()).computeIfAbsent(type.id(), id -> StoredDataHolder.ofValue(type, playerId, null)));
    }

    public static <T> StoredDataHolder<T> lookup(UUID playerId, DataType<T> type, boolean forceRefresh) {
        var value = getPlayerLookup(playerId, type);
        if (forceRefresh) {
            value.reload();
        }
        return value;
    }

    public static CompletableFuture<Void> refresh(UUID uuid, boolean force) {
        var startTime = Instant.now();
        return CompletableFuture.allOf(DataRegistry.values().parallelStream().map(dataType -> lookup(uuid, dataType, force).asFuture()).toArray(CompletableFuture[]::new)).thenRun(() -> {
            var stopTime = Instant.now();
            var duration = Duration.between(startTime, stopTime);
            DataSyncMod.LOGGER.info("Loaded {} player data objects (took {}s {}ms)", DataRegistry.size(), duration.toSeconds(), duration.toMillisPart());
        });
    }

    public static <T> Optional<T> getCached(UUID playerId, DataType<T> type) {
        return Optional.ofNullable(getPlayerLookupEmpty(playerId, type).value());
    }
}
