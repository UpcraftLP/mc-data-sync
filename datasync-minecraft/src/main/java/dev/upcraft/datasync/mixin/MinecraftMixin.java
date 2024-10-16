package dev.upcraft.datasync.mixin;

import dev.upcraft.datasync.client.DataSyncModClient;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {

    @Inject(method = "onGameLoadFinished", at = @At(value = "RETURN"))
    private void onFinishLoading(CallbackInfo ci) {
        DataSyncModClient.preloadPlayerData();
    }
}
