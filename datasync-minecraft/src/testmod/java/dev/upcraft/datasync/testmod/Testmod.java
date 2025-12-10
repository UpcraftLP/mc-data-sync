package dev.upcraft.datasync.testmod;

import dev.upcraft.datasync.api.DataSyncAPI;
import dev.upcraft.datasync.api.SyncToken;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
            if (!player.isSpectator() && stack.is(Items.NETHERITE_AXE)) {
                if (!world.isClientSide()) {

                    SUPPORTER_DATA_SYNC_TOKEN.fetch(player.getUUID());

                    // get the data for the player
                    Optional<SupporterData> optional = player.datasync$get(SUPPORTER_DATA_SYNC_TOKEN);

                    optional.ifPresentOrElse(data -> {
                        var messageComponent = Component.literal(data.message()).withStyle(s -> s.withColor(data.color()));
                        sendMessage(player, Component.literal("Your message is: ").append(messageComponent));
                    }, () -> sendMessage(player, Component.literal("You do not have any data stored!")));
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

    private static void sendMessage(Player player, Component message) {
        player.sendSystemMessage(message);
    }
}
