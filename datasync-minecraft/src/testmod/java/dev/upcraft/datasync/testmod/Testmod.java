package dev.upcraft.datasync.testmod;

import dev.upcraft.datasync.api.DataSyncAPI;
import dev.upcraft.datasync.api.SyncToken;
import dev.upcraft.datasync.api.ext.DataSyncPlayerExt;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;

import java.util.Optional;

public class Testmod implements ModInitializer {

    public static final String MOD_ID = "testmod";
    public static final SyncToken<SupporterData> SUPPORTER_DATA_SYNC_TOKEN = DataSyncAPI.register(SupporterData.class, Testmod.id("test_data"), SupporterData.CODEC);

    @Override
    public void onInitialize() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            var stack = player.getItemInHand(hand);

            // when right clicking with a Netherite Axe, show a message to the player
            if (stack.is(Items.NETHERITE_AXE) && !world.isClientSide()) {
                if (!world.isClientSide()) {

                    SUPPORTER_DATA_SYNC_TOKEN.fetch(player.getUUID());

                    // get the data for the player
                    // FIXME switch back to using injected interfaces once StoneCutter suppports it
                    //  https://github.com/stonecutter-versioning/stonecutter/issues/12
                    // Optional<SupporterData> optional = player.datasync$get(SUPPORTER_DATA_SYNC_TOKEN);
                    //noinspection RedundantCast
                    Optional<SupporterData> optional = ((DataSyncPlayerExt) player).datasync$get(SUPPORTER_DATA_SYNC_TOKEN);

                    optional.ifPresentOrElse(data -> {
                        var messageComponent = Component.literal(data.message()).withStyle(s -> s.withColor(data.color()));
                        sendMessage((ServerPlayer) player, Component.literal("Your message is: ").append(messageComponent));
                    }, () -> sendMessage((ServerPlayer) player, Component.literal("You do not have any data stored!")));
                }

                //? <1.21.4 {
                return net.minecraft.world.InteractionResultHolder.success(stack);
                //?} else {
                /*return InteractionResult.SUCCESS;
                *///?}
            }

            //? <1.21.4 {
            return net.minecraft.world.InteractionResultHolder.pass(stack);
            //?} else {
            /*return net.minecraft.world.InteractionResult.PASS;
             *///?}
        });
    }

    public static ResourceLocation id(String path) {
        //? <1.21 {
        /*return new ResourceLocation(MOD_ID, path);
         *///?} else {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
        //?}
    }

    private static void sendMessage(ServerPlayer player, Component message) {
        player.sendSystemMessage(message);
    }
}
