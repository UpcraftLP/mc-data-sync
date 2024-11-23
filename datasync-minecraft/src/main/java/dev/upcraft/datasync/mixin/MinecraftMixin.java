package dev.upcraft.datasync.mixin;

import dev.upcraft.datasync.client.DataSyncModClient;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {

    //? >=1.20.1 {
    @Inject(method = "onGameLoadFinished", at = @At(value = "RETURN"))
    //?} else {
    /*// lambda method that runs when the game finishes loading
    @Inject(method = "method_29338", at = @At(value = "TAIL"))
    *///?}
    private void onFinishLoading(CallbackInfo ci) {
        DataSyncModClient.preloadPlayerData();
    }
}
