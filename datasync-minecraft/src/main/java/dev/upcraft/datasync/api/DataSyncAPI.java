package dev.upcraft.datasync.api;

import com.mojang.serialization.Codec;
import dev.upcraft.datasync.content.DataRegistry;
import dev.upcraft.datasync.content.DataStore;
import net.minecraft.resources.ResourceLocation;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class DataSyncAPI {

    public static final Duration CACHE_DURATION = Duration.ofHours(1);

    public static <T> SyncToken<T> register(Class<T> type, ResourceLocation id, Codec<T> codec) {
        return DataRegistry.add(type, id, codec);
    }

    public static CompletableFuture<Void> refreshAllPlayerData(UUID playerId) {
        return DataStore.refresh(playerId, true);
    }
}
