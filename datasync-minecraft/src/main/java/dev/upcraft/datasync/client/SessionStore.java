package dev.upcraft.datasync.client;

import com.google.gson.JsonObject;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.upcraft.datasync.DataSyncMod;
import dev.upcraft.datasync.web.HttpUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.core.UUIDUtil;
import net.minecraft.util.ExtraCodecs;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public class SessionStore {

    private static final Codec<Pair<String, Long>> CHALLENGE_TOKEN_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("token").forGetter(Pair::getFirst),
            Codec.LONG.fieldOf("expires_in").forGetter(Pair::getSecond)
    ).apply(instance, Pair::of));

    private static final Codec<Session> SESSION_TOKEN_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.STRING_CODEC.fieldOf("id").forGetter(Session::userId),
            Codec.STRING.fieldOf("token").forGetter(Session::accessToken),
            ExtraCodecs.INSTANT_ISO8601.fieldOf("expires_at").forGetter(Session::expiresAt)
    ).apply(instance, Session::new));

    @Nullable
    private Instant cooldownUntil;

    @Nullable
    private Session session;

    @Nullable
    public UUID getUserId() {
        if(session == null) {
            return null;
        }
        return session.userId();
    }

    public @Nullable Session getSession() {
        return session;
    }

    public Either<Session, String> login() {
        if(cooldownUntil != null && cooldownUntil.isAfter(Instant.now())) {
            return Either.right("Rate limited");
        }

        var mcSession = Minecraft.getInstance().getUser();
        var profile = mcSession.getGameProfile();
        var profileId = profile.getId().toString();
        var challengeReqData = new JsonObject();
        challengeReqData.addProperty("id", profileId);
        var challengeUri = URI.create("%s/auth/mojang/challenge".formatted(DataSyncMod.API_URL));
        var response = HttpUtil.postJson(challengeUri, challengeReqData);

        var opt = CHALLENGE_TOKEN_CODEC.decode(JsonOps.INSTANCE, response).resultOrPartial(err -> DataSyncMod.LOGGER.error("received malformed response for login challenge: {}", err)).map(Pair::getFirst);
        if(opt.isEmpty()) {
            cooldownUntil = Instant.now().plus(30, ChronoUnit.SECONDS);
            return Either.right("Received malformed login challenge");
        }

        var challenge = opt.get().getFirst();
        try {
            Minecraft.getInstance().getMinecraftSessionService().joinServer(profile, mcSession.getAccessToken(), challenge);
        } catch (AuthenticationException e) {
            DataSyncMod.LOGGER.error("Unable to authenticate", e);
            cooldownUntil = Instant.now().plus(30, ChronoUnit.SECONDS);
            return Either.right("Unable to authenticate with Mojang session servers");
        }

        var authSuccessData = new JsonObject();
        authSuccessData.addProperty("id", profileId);
        authSuccessData.addProperty("username", mcSession.getGameProfile().getName());
        authSuccessData.addProperty("token", challenge);
        var authSuccessUri = URI.create("%s/auth/mojang".formatted(DataSyncMod.API_URL));
        response = HttpUtil.postJson(authSuccessUri, authSuccessData);

        var dsSessionOpt = SESSION_TOKEN_CODEC.decode(JsonOps.INSTANCE, response).resultOrPartial(err -> DataSyncMod.LOGGER.error("received malformed response for login success: {}", err)).map(Pair::getFirst);

        if(dsSessionOpt.isEmpty()) {
            return Either.right("Received malformed session response");
        }

        session = dsSessionOpt.get();
        DataSyncMod.LOGGER.debug("Successfully authenticated, session valid until {}", dsSessionOpt.get().expiresAt());
        return Either.left(dsSessionOpt.get());
    }
}
