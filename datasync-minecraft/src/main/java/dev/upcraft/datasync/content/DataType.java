package dev.upcraft.datasync.content;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import dev.upcraft.datasync.DataSyncMod;
import dev.upcraft.datasync.api.SyncToken;
import dev.upcraft.datasync.api.util.GameProfileHelper;
import dev.upcraft.datasync.client.DataSyncModClient;
import dev.upcraft.datasync.web.HttpUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpRequest;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public record DataType<T>(Class<T> type, ResourceLocation id, Codec<T> codec) implements SyncToken<T> {

    private static final Logger log = LoggerFactory.getLogger(DataType.class);

    @Override
    public CompletableFuture<Optional<T>> get(UUID playerId) {
        return DataStore.lookup(playerId, this, false);
    }

    @Override
    public Optional<T> getCached(UUID playerId) {
        return DataStore.getCached(playerId, this);
    }

    @Override
    public CompletableFuture<Void> setData(@Nullable T data) {
        if (FabricLoader.getInstance().getEnvironmentType() != EnvType.CLIENT) {
            throw new UnsupportedOperationException("Attempted to set player data for %s on a server".formatted(this.id()));
        }

        var profile = GameProfileHelper.getClientProfile();
        var playerId = profile.getId();

        // manually store data so the client gets an immediate update
        var lookup = DataStore.getPlayerLookup(playerId);
        //noinspection unchecked
        @Nullable T previous = (T) lookup.getIfPresent(this.id());
        lookup.put(this.id(), Optional.ofNullable(data));

        if (GameProfileHelper.isOfflineProfile(profile)) {
            DataSyncMod.LOGGER.debug("Client is using offline mode, cannot persist data!");
            return CompletableFuture.completedFuture(null);
        }

        DataSyncMod.LOGGER.debug("Sending update for {}", this.id());

        // convert to json early so the input object does not need to be thread-safe
        var json = this.codec().encodeStart(JsonOps.INSTANCE, data).resultOrPartial(errMsg -> DataSyncMod.LOGGER.error("Unable to encode data update for {}: {}", this.id(), errMsg)).orElseThrow();

        var uri = URI.create(String.format("%s/v0/data/%s/%s", DataSyncMod.API_URL, playerId, HttpUtil.urlEncode(id())));
        return CompletableFuture.runAsync(() -> HttpUtil.postJsonRequest(uri, json, builder -> {
            var session = DataSyncModClient.SESSION_STORE.getSession();
            if (session == null || !session.isValid()) {
                var loginAttempt = DataSyncModClient.SESSION_STORE.login();
                if (loginAttempt.right().isPresent()) {
                    throw new RuntimeException("Unable to log in: " + loginAttempt.right().orElseThrow());
                }

                session = loginAttempt.orThrow();
            }
            return builder.header("Authorization", "Bearer %s".formatted(session.accessToken()));
        })).exceptionally(t -> {
            DataSyncMod.LOGGER.error("Unable to send data update for {}, restoring previous state", this.id(), t);
            lookup.put(this.id(), Optional.ofNullable(previous));
            return null;
        });
        // TODO if installed on server, send packet to cause immediate sync
    }

    @Nullable
    public T fetch(UUID playerId) {
        if (!DataSyncMod.HAS_INTERNET) {
            return null;
        }

        var uri = URI.create(String.format("%s/v0/data/%s/%s", DataSyncMod.API_URL, playerId, HttpUtil.urlEncode(id())));
        var request = HttpRequest.newBuilder(uri);
        var json = HttpUtil.makeJsonRequest(request);

        if(json == null) {
            return null;
        }

        // TODO better error handling
        return codec().decode(JsonOps.INSTANCE, json).resultOrPartial(errMsg -> DataSyncMod.LOGGER.error("Unable to decode response from {}: {}", uri, errMsg)).map(Pair::getFirst).orElse(null);
    }
}
