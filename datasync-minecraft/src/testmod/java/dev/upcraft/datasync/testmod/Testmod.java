package dev.upcraft.datasync.testmod;

import dev.upcraft.datasync.api.DataSyncAPI;
import dev.upcraft.datasync.api.SyncToken;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.Items;

import java.util.Optional;

public class Testmod implements ModInitializer, ClientModInitializer {

    public static final String MODID = "testmod";
    public static final SyncToken<SupporterData> SUPPORTER_DATA_SYNC_TOKEN = DataSyncAPI.register(SupporterData.class, Testmod.id("test_data"), SupporterData.CODEC);

    @Override
    public void onInitialize() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            var stack = player.getItemInHand(hand);

            // when right clicking with a Netherite Axe, show a message to the player
            if (stack.is(Items.NETHERITE_AXE)) {
                if (!world.isClientSide()) {
                    // get the data for the player
                    Optional<SupporterData> optional = player.datasync$get(SUPPORTER_DATA_SYNC_TOKEN);

                    optional.ifPresentOrElse(data -> {
                        var messageComponent = Component.literal(data.message()).withStyle(s -> s.withColor(data.color()));
                        player.sendSystemMessage(Component.literal("Your message is: ").append(messageComponent));
                    }, () -> player.sendSystemMessage(Component.literal("You do not have any data stored!")));
                }
                return InteractionResultHolder.sidedSuccess(stack, world.isClientSide());
            }

            return InteractionResultHolder.pass(stack);
        });
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MODID, path);
    }

    @Override
    public void onInitializeClient() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            var stack = player.getItemInHand(hand);

            // when right clicking with a stick, switch to a new color
            // !!IMPORTANT: this is done clientside only!!
            if (stack.is(Items.STICK)) {
                if (world.isClientSide()) {
                    var random = player.getRandom();
                    String message = String.format("You have %d points!", random.nextInt(20000) + 300);
                    int color = Mth.color(random.nextFloat(), random.nextFloat(), random.nextFloat());

                    SupporterData newData = new SupporterData(message, color);

                    // send the new values to the server
                    // (this returns a future so you can react to when the sending is finished)
                    SUPPORTER_DATA_SYNC_TOKEN.setData(newData);
                }
                return InteractionResultHolder.success(stack);
            }
            return InteractionResultHolder.pass(stack);
        });
    }
}
