package dev.upcraft.datasync.web;

import com.google.common.base.Suppliers;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import dev.upcraft.datasync.DataSyncMod;
import dev.upcraft.datasync.util.ModHelper;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import oshi.SystemInfo;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

public class HttpUtil {

    private static final Gson GSON = new Gson();
    private static final ExecutorService BACKGROUND_EXECUTOR = Executors.newCachedThreadPool(r -> {
        var t = new Thread(r, "[DataSync] Background Data Fetcher");
        t.setDaemon(true);
        return t;
    });
    private static final Supplier<HttpClient> httpClient = Suppliers.memoize(() -> HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).executor(BACKGROUND_EXECUTOR).build());

    public static HttpClient getClient() {
        return httpClient.get();
    }

    public static String getUserAgentString() {
        var meta = ModHelper.getMeta(DataSyncMod.MOD_ID);
        var gameMeta = ModHelper.getGameMeta();
        var loader = buildLoaderUAString();
        var system = buildSystemUAString();

        return String.format("%s/%s (%s) %s/%s (%s)", meta.getName(), meta.getVersion(), loader, gameMeta.getName(), gameMeta.getVersion(), system);
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

        var bitness = switch (os.getBitness()) {
            case 32 -> "x86";
            case 64 -> "x64";
            default -> throw new IllegalStateException("Unsupported operating system bitness: " + os.getBitness());
        };

        return String.format("%s %s; %s", osFamily, osVersion, bitness);
    }

    public static HttpRequest.Builder acceptsJson(HttpRequest.Builder builder) {
        return builder.header("Accept", "application/json;charset=UTF-8");
    }

    public static HttpRequest.Builder sendsJson(HttpRequest.Builder builder) {
        return builder.header("Content-Type", "application/json;charset=UTF-8");
    }

    public static HttpRequest.Builder addUserAgentHeader(HttpRequest.Builder requestBuilder) {
        return requestBuilder.header("User-Agent", getUserAgentString())
                //FIXME move elsewhere and make 6 seconds default
                .timeout(Duration.ofSeconds(2));
    }

    public static void postJsonRequest(URI uri, JsonElement json) {
        var requestBuilder = HttpRequest.newBuilder(uri).POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(json)));
        sendsJson(requestBuilder);
        addUserAgentHeader(requestBuilder);
        var request = requestBuilder.build();
        try {
            var response = getClient().send(request, HttpResponse.BodyHandlers.discarding());
            DataSyncMod.LOGGER.trace("HTTP {}:{} - {}", request.method(), response.statusCode(), request.uri());
            if (response.statusCode() != 204) {
                // TODO log exception message
                throw new IOException("Unable to send data: received HTTP " + response.statusCode());
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
        var request = requestBuilder.build();

        try {
            HttpResponse<InputStream> response = getClient().send(request, HttpResponse.BodyHandlers.ofInputStream());
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
}