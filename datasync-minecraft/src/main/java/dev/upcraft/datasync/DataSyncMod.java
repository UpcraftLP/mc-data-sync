package dev.upcraft.datasync;

import com.mojang.logging.LogUtils;
import dev.upcraft.datasync.api.DataSyncAPI;
import dev.upcraft.datasync.api.SyncToken;
import dev.upcraft.datasync.api.util.Entitlements;
import dev.upcraft.datasync.event.PlayerEventHandler;
import dev.upcraft.datasync.net.S2CUpdatePlayerDataPacket;
import dev.upcraft.datasync.util.EntitlementsImpl;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.time.Duration;
//?if fabric {
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
//?} elif neoforge {
/*import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
*///?}

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

//? if neoforge {
/*@Mod(DataSyncMod.MOD_ID)
@EventBusSubscriber
*///?}
public class DataSyncMod /*? if fabric {*/ implements ModInitializer/*?}*/ {

    public static final String MOD_ID = "datasync_minecraft";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final boolean HAS_INTERNET = checkInternetAccess();

    public static final String API_URL = "https://datasync-api.uuid.gg/api";
    public static final ResourceLocation ENTITLEMENTS_ID = dataId("entitlements");
    public static final SyncToken<Entitlements> ENTITLEMENTS_TOKEN = DataSyncAPI.register(Entitlements.class, DataSyncMod.ENTITLEMENTS_ID, EntitlementsImpl.CODEC);

    public static final Duration REQUEST_TIMEOUT = Duration.ofMillis(Long.getLong("datasync.request.timeout", 6000));
    public static final boolean LOGIN_AUTOFETCH = !Boolean.getBoolean("datasync.login_autofetch.disable");
    public static final boolean LOGIN_FORCE_REFRESH = Boolean.getBoolean("datasync.login_force_refresh");

    public static ResourceLocation dataId(String path) {
        //? <1.21 {
        /*return new ResourceLocation("datasync", path);
        *///?} else {
        return ResourceLocation.fromNamespaceAndPath("datasync", path);
        //?}
    }

    public static ResourceLocation id(String path) {
        //? <1.21 {
        /*return new ResourceLocation(MOD_ID, path);
         *///?} else {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
        //?}
    }

    //?if fabric {
    @Override
    public void onInitialize() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            PlayerEventHandler.onPlayerJoin(handler.getPlayer());
        });
        S2CUpdatePlayerDataPacket.registerServer();
    }
    //?} elif neoforge {
    /*@SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if(event.getEntity() instanceof ServerPlayer serverPlayer) {
            PlayerEventHandler.onPlayerJoin(serverPlayer);
        }
    }
    *///?}

    private static boolean checkInternetAccess() {
        //? java: >=21 {
        try (var client = HttpClient.newHttpClient()) {
        //?} else {
        /*try {
            var client = HttpClient.newHttpClient();
         *///?}
            var request = HttpRequest.newBuilder().uri(URI.create("https://sessionserver.mojang.com")).build();
            client.send(request, HttpResponse.BodyHandlers.discarding());
            return true;
        } catch (InterruptedException | IOException e) {
            LOGGER.error("failed to connect to mojang session server, disabling online features!");
        }

        return false;
    }
}
