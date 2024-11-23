package dev.upcraft.datasync.client;

import net.minecraft.client.Minecraft;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public record Session(UUID userId, String accessToken, Instant expiresAt) {

    public boolean isValid() {
        return Minecraft.getInstance().getGameProfile().getId().equals(userId()) && expiresAt().isAfter(Instant.now().plus(10, ChronoUnit.SECONDS));
    }
}
