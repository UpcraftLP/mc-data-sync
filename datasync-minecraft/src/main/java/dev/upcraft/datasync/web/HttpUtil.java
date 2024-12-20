package dev.upcraft.datasync.web;

import com.google.common.base.Suppliers;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Lifecycle;
import com.sun.jna.Platform;
import dev.upcraft.datasync.DataSyncMod;
import dev.upcraft.datasync.util.ModHelper;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import oshi.SystemInfo;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public class HttpUtil {

    private static final Gson GSON = new Gson();
    private static final ExecutorService BACKGROUND_EXECUTOR = Executors.newCachedThreadPool(r -> {
        var t = new Thread(r, "[DataSync] Background Data Fetcher");
        t.setDaemon(true);
        return t;
    });
    private static final Supplier<HttpClient> httpClient = Suppliers.memoize(() -> HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).executor(BACKGROUND_EXECUTOR).build());
    private static final Supplier<String> USER_AGENT = Suppliers.memoize(() -> {
        var meta = ModHelper.getMeta(DataSyncMod.MOD_ID);
        var gameMeta = ModHelper.getGameMeta();
        var loader = buildLoaderUAString();
        String system;
        try {
            system = buildSystemUAString();
        } catch (Exception e) {
            DataSyncMod.LOGGER.error("Error during gathering system info", e);

            system = String.format("%s;%s", System.getProperty("os.name").replace(' ', '_'), Platform.ARCH);
        }

        return String.format("%s/%s (%s) %s/%s (%s)", meta.getName(), meta.getVersion(), loader, gameMeta.getName(), gameMeta.getVersion(), system);
    });

    public static HttpClient getClient() {
        return httpClient.get();
    }

    public static String getUserAgentString() {
        return USER_AGENT.get();
    }

    public static String buildLoaderUAString() {
        var meta = ModHelper.getLoaderMeta();

        var sb = new StringBuilder();
        sb.append(meta.getName()).append('/').append(meta.getVersion());
        if(ModHelper.isLoaded("connectormod")) {
            sb.append("; ");
            var connectorMeta = ModHelper.getMeta("connectormod");
            sb.append(connectorMeta.getName()).append('/').append(connectorMeta.getVersion());
        }

        return sb.toString();
    }

    public static String buildSystemUAString() {
        var system = new SystemInfo();
        var os = system.getOperatingSystem();

        var osFamily = os.getFamily();
        var osVersion = os.getVersionInfo().getVersion();

        return String.format("%s_%s; %s", osFamily, osVersion, Platform.ARCH);
    }

    public static HttpRequest.Builder acceptsJson(HttpRequest.Builder builder) {
        return builder.header("Accept", "application/json;charset=UTF-8");
    }

    public static HttpRequest.Builder sendsJson(HttpRequest.Builder builder) {
        return builder.header("Content-Type", "application/json;charset=UTF-8");
    }

    public static HttpRequest.Builder addUserAgentHeader(HttpRequest.Builder requestBuilder) {
        return requestBuilder.header("User-Agent", getUserAgentString());
    }

    public static void postJsonRequest(URI uri, JsonElement json, UnaryOperator<HttpRequest.Builder> extraProperties) {
        var requestBuilder = HttpRequest.newBuilder(uri).POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(json)));
        sendsJson(requestBuilder);
        addUserAgentHeader(requestBuilder);
        var request = extraProperties.apply(requestBuilder.timeout(DataSyncMod.REQUEST_TIMEOUT)).build();
        try {
            var response = getClient().send(request, HttpResponse.BodyHandlers.discarding());
            DataSyncMod.LOGGER.trace("HTTP {}:{} - {}", request.method(), response.statusCode(), request.uri());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                // TODO log exception message
                throw new IOException("Unable to send data: received HTTP " + response.statusCode());
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("unable to send request", e);
        } catch (IOException e) {
            throw new UncheckedIOException("IO error when sending request", e);
        }
    }

    public static JsonElement postJson(URI uri, JsonElement json) {
        var requestBuilder = HttpRequest.newBuilder(uri).POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(json)));
        sendsJson(requestBuilder);
        acceptsJson(requestBuilder);
        addUserAgentHeader(requestBuilder);
        var request = requestBuilder.timeout(DataSyncMod.REQUEST_TIMEOUT).build();
        try {
            var response = getClient().send(request, HttpResponse.BodyHandlers.ofInputStream());
            DataSyncMod.LOGGER.trace("HTTP {}:{} - {}", request.method(), response.statusCode(), request.uri());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IOException("Unable to send data: received HTTP " + response.statusCode());
            }

            try (var reader = new InputStreamReader(response.body(), StandardCharsets.UTF_8)) {
                return GSON.fromJson(reader, JsonElement.class);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("unable to send request", e);
        } catch (IOException e) {
            throw new UncheckedIOException("IO error when sending request", e);
        }
    }

    @Nullable
    public static JsonElement makeJsonRequest(HttpRequest.Builder requestBuilder) {
        acceptsJson(requestBuilder);
        addUserAgentHeader(requestBuilder);
        var request = requestBuilder.timeout(DataSyncMod.REQUEST_TIMEOUT).build();

        try {
            var response = getClient().send(request, HttpResponse.BodyHandlers.ofInputStream());
            DataSyncMod.LOGGER.trace("HTTP {}:{} - {}", request.method(), response.statusCode(), request.uri());
            if(response.statusCode() < 200 || response.statusCode() >= 300) {
                if(response.statusCode() == 404) {
                    // user not found
                    return null;
                }

                // TODO exception with better message?
                return null;
            }

            try (var reader = new InputStreamReader(response.body(), StandardCharsets.UTF_8)) {
                return GSON.fromJson(reader, JsonElement.class);
            }
        } catch (ConnectException e) {
            if(e.getMessage() != null) {
                DataSyncMod.LOGGER.error("HTTP {} {} - {}", request.method(), request.uri(), e.getMessage());
            }
            else {
                DataSyncMod.LOGGER.error("HTTP {} {} - UNKNOWN ERROR", request.method(), request.uri(), e);
            }
            return null;
        }
        catch (InterruptedException e) {
            throw new RuntimeException("unable to handle request", e);
        } catch (IOException e) {
            throw new RuntimeException("IO error when handling request", e);
        }

    }

    public static String urlEncode(ResourceLocation id) {
        return id.getNamespace() + '/' + id.getPath();
    }

    public static final Codec<UUID> UUID_CODEC = Codec.STRING.comapFlatMap(string -> {
        try {
            return DataResult.success(UUID.fromString(string), Lifecycle.stable());
        } catch (IllegalArgumentException var2) {
            //? >=1.20.1 {
            return DataResult.error(() -> "Invalid UUID " + string + ": " + var2.getMessage());
            //?} else {
            /*return DataResult.error("Invalid UUID " + string + ": " + var2.getMessage());
            *///?}
        }
    }, UUID::toString);
}
