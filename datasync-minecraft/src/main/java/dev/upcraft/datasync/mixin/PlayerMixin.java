package dev.upcraft.datasync.mixin;

import com.mojang.authlib.GameProfile;
import dev.upcraft.datasync.api.SyncToken;
import dev.upcraft.datasync.api.ext.DataSyncPlayerExt;
import dev.upcraft.datasync.api.util.Entitlements;
import dev.upcraft.datasync.util.EntitlementsImpl;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Optional;

@Mixin(Player.class)
public abstract class PlayerMixin extends LivingEntity implements DataSyncPlayerExt {

    private PlayerMixin(EntityType<? extends LivingEntity> entityType, Level level) {
        super(entityType, level);
        throw new UnsupportedOperationException();
    }

    @Shadow public abstract GameProfile getGameProfile();

    @Override
    public Entitlements datasync$getEntitlements() {
        //? >=1.21.9 {
        var profileId = this.getGameProfile().id();
        //?} else {
        /*var profileId = this.getGameProfile().getId();
         *///?}
        return Entitlements.token().getOrDefault(profileId, EntitlementsImpl.empty());
    }

    @Override
    public <T> Optional<T> datasync$get(SyncToken<T> token) {
        return token.get(this.getUUID());
    }

    @Override
    public <T> T datasync$getOrDefault(SyncToken<T> token, T defaultValue) {
        return token.getOrDefault(this.getUUID(), defaultValue);
    }
}
