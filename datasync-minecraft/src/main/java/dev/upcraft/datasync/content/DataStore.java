package dev.upcraft.datasync.content;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import dev.upcraft.datasync.DataSyncMod;
import dev.upcraft.datasync.api.DataSyncAPI;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class DataStore {

    private static final Map<UUID, Cache<ResourceLocation, Optional<?>>> globalStore = new HashMap<>();

    public static <T> CompletableFuture<Optional<T>> lookup(UUID playerId, DataType<T> type, boolean forceRefresh) {
        var lookup = getPlayerLookup(playerId);
        if(forceRefresh) {
            lookup.invalidate(type.id());
        }
        return CompletableFuture.supplyAsync(() -> {
            try {
                //noinspection unchecked
                return (Optional<T>) lookup.get(type.id(), () -> Optional.ofNullable(type.fetch(playerId)));
            } catch (ExecutionException e) {
                // unwrap ExecutionException to get the underlying error
                throw new RuntimeException(e.getCause());
            }
        }).exceptionally(t -> {
            DataSyncMod.LOGGER.error("Unable to fetch data for {}[{}]", type.id(), playerId, t);
            return Optional.empty();
        });
    }

    public static CompletableFuture<Void> refresh(UUID uuid) {
        var startTime = Instant.now();
        return CompletableFuture.allOf(DataRegistry.values().parallelStream().map(dataType -> lookup(uuid, dataType, true)).toArray(CompletableFuture[]::new)).thenRun(() -> {
            var stopTime = Instant.now();
            var duration = Duration.between(startTime, stopTime);
            DataSyncMod.LOGGER.info("Loaded {} player data objects (took {}s {}ms)", DataRegistry.size(), duration.toSeconds(), duration.toMillisPart());
        });
    }

    public static <T> Optional<T> getCached(UUID playerId, DataType<T> type) {
        var lookup = getPlayerLookup(playerId);
        //noinspection unchecked
        @Nullable T value = (T) lookup.getIfPresent(type.id());
        return Optional.ofNullable(value);
    }

    public static Cache<ResourceLocation, Optional<?>> getPlayerLookup(UUID playerId) {
        return (globalStore.computeIfAbsent(playerId, key -> CacheBuilder.newBuilder().maximumSize(200).expireAfterWrite(DataSyncAPI.CACHE_DURATION).build()));
    }
}
