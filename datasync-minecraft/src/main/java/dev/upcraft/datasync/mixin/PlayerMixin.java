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
import java.util.concurrent.CompletableFuture;

@Mixin(Player.class)
public abstract class PlayerMixin extends LivingEntity implements DataSyncPlayerExt {

    private PlayerMixin(EntityType<? extends LivingEntity> entityType, Level level) {
        super(entityType, level);
        throw new UnsupportedOperationException();
    }

    @Shadow public abstract GameProfile getGameProfile();

    @Override
    public CompletableFuture<Entitlements> datasync$getEntitlements() {
        return Entitlements.token().get(this.getGameProfile().getId()).thenApply(opt -> opt.orElseGet(EntitlementsImpl::empty));
    }

    @Override
    public <T> CompletableFuture<Optional<T>> datasync$get(SyncToken<T> token) {
        return token.get(this.getUUID());
    }
}
