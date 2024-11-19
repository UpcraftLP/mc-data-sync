package dev.upcraft.datasync.testmod;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.Items;

public class TestmodClient implements ClientModInitializer {
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
                    Testmod.SUPPORTER_DATA_SYNC_TOKEN.setData(newData);
                }
                return InteractionResultHolder.success(stack);
            }
            return InteractionResultHolder.pass(stack);
        });
    }
}
