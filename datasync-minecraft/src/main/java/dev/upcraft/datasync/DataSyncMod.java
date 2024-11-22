package dev.upcraft.datasync;

import com.mojang.authlib.GameProfile;
import dev.upcraft.datasync.api.DataSyncAPI;
import dev.upcraft.datasync.api.SyncToken;
import dev.upcraft.datasync.api.util.Entitlements;
import dev.upcraft.datasync.content.DataStore;
import dev.upcraft.datasync.net.C2SUpdatePlayerDataPacket;
import dev.upcraft.datasync.util.EntitlementsImpl;
import dev.upcraft.datasync.util.ModHelper;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class DataSyncMod implements ModInitializer {

    public static final String MOD_ID = "datasync_minecraft";
    public static final Logger LOGGER = LoggerFactory.getLogger(ModHelper.getMeta(MOD_ID).getName());
    public static final boolean HAS_INTERNET = checkInternetAccess();

    public static final String API_URL = "https://datasync-api.uuid.gg/api";
    public static final ResourceLocation ENTITLEMENTS_ID = dataId("entitlements");
    public static final SyncToken<Entitlements> ENTITLEMENTS_TOKEN = DataSyncAPI.register(Entitlements.class, DataSyncMod.ENTITLEMENTS_ID, EntitlementsImpl.CODEC);

    public static final Duration REQUEST_TIMEOUT = Duration.ofMillis(Long.getLong("datasync.request.timeout", 6000));
    public static final boolean LOGIN_AUTOFETCH = !Boolean.getBoolean("datasync.login_autofetch.disable");
    public static final boolean LOGIN_FORCE_REFRESH = Boolean.getBoolean("datasync.login_force_refresh");

    public static ResourceLocation dataId(String path) {
        return new ResourceLocation("datasync", path);
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (LOGIN_AUTOFETCH) {
                GameProfile profile = handler.getPlayer().getGameProfile();
                DataStore.refresh(profile.getId(), LOGIN_FORCE_REFRESH).thenRunAsync(() -> DataSyncMod.LOGGER.debug("loaded player data for '{}' ({})", profile.getName(), profile.getId()));
            }
        });
        ServerPlayNetworking.registerGlobalReceiver(C2SUpdatePlayerDataPacket.TYPE, C2SUpdatePlayerDataPacket::handle);
    }

    private static boolean checkInternetAccess() {
        try {
            var client = HttpClient.newHttpClient();
            var request = HttpRequest.newBuilder().uri(URI.create("https://sessionserver.mojang.com")).build();
            client.send(request, HttpResponse.BodyHandlers.discarding());
            return true;
        } catch (InterruptedException | IOException e) {
            LOGGER.error("failed to connect to mojang session server, disabling online features!");
        }

        return false;
    }
}
