package dev.upcraft.datasync.content;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import dev.upcraft.datasync.DataSyncMod;
import dev.upcraft.datasync.api.SyncToken;
import dev.upcraft.datasync.api.util.GameProfileHelper;
import dev.upcraft.datasync.client.DataSyncModClient;
import dev.upcraft.datasync.net.C2SUpdatePlayerDataPacket;
import dev.upcraft.datasync.web.HttpUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.http.HttpRequest;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public record DataType<T>(Class<T> type, ResourceLocation id, Codec<T> codec) implements SyncToken<T> {

    @Override
    public CompletableFuture<Optional<T>> fetch(UUID playerId) {
        return DataStore.lookup(playerId, this, false).asFuture();
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
        //? >=1.21.9 {
        var playerId = profile.id();
        //?} else {
        /*var playerId = profile.getId();
        *///?}

        // manually store data so the client gets an immediate update
        var lookup = DataStore.getPlayerLookupEmpty(playerId, this);
        @Nullable T previous = lookup.value();
        lookup.setValue(data);

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
        })).thenRunAsync(() -> {
            C2SUpdatePlayerDataPacket.trySend(this.id());
        }, Minecraft.getInstance()).exceptionally(t -> {
            DataSyncMod.LOGGER.error("Unable to send data update for {}, restoring previous state", this.id(), t);
            lookup.setValue(previous);
            return null;
        });
    }

    @Nullable
    public T fetchRemote(UUID playerId) {
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

    @Override
    public Optional<T> get(UUID playerId) {
        return Optional.ofNullable(DataStore.lookup(playerId, this, false).value());
    }

    @Override
    public T getOrDefault(UUID playerId, T defaultValue) {
        return DataStore.lookup(playerId, this, false).or(defaultValue);
    }
}
