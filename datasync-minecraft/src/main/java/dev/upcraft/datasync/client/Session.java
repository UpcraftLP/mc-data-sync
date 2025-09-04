package dev.upcraft.datasync.client;

import dev.upcraft.datasync.api.util.GameProfileHelper;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public record Session(UUID userId, String accessToken, Instant expiresAt) {

    public boolean isValid() {
        var currentProfile = GameProfileHelper.getClientProfile();
        //? >=1.21.9 {
        var profileId = currentProfile.id();
        //?} else {
        /*var profileId = currentProfile.getId();
        *///?}
        return userId().equals(profileId) && expiresAt().isAfter(Instant.now());
    }
}
