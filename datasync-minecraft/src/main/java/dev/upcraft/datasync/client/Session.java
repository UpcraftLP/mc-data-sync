package dev.upcraft.datasync.client;

import dev.upcraft.datasync.api.util.GameProfileHelper;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public record Session(UUID userId, String accessToken, Instant expiresAt) {

    public boolean isValid() {
        return GameProfileHelper.getClientProfile().getId().equals(userId()) && expiresAt().isAfter(Instant.now().plus(10, ChronoUnit.SECONDS));
    }
}
