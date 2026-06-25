package dev.upcraft.datasync.testmod;

//? if fabric {

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
//?} elif neoforge {
/*import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
*///?}
//? >=1.21.4 {
/*import net.minecraft.world.InteractionResult;
 *///?}
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

//? if neoforge {
/*@EventBusSubscriber(modid = Testmod.MOD_ID)
 *///?}
public class TestmodClient /*? if fabric { */ implements ClientModInitializer /*?}*/ {

    private static /*? >=1.21.4 { */ /*InteractionResult *//*?} else { */ net.minecraft.world.InteractionResultHolder<ItemStack> /*?} */ onUse(Player player, Level world, InteractionHand hand, ItemStack stack) {
        // when right-clicking with a stick, switch to a new color
        // !!IMPORTANT: this is done clientside only!!
        if (stack.is(Items.STICK)) {
            if (world.isClientSide()) {
                var random = player.getRandom();
                String message = String.format("You have %d points!", random.nextInt(20000) + 300);

                int color = Mth.hsvToRgb(random.nextFloat(), random.nextFloat(), 0.5F + random.nextFloat() * 0.5F);

                SupporterData newData = new SupporterData(message, color);

                // send the new values to the server
                // (this returns a future so you can react to when the sending is finished)
                Testmod.SUPPORTER_DATA_SYNC_TOKEN.setData(newData);
            }

            //? >=1.21.4 {
            /*return InteractionResult.SUCCESS;
             *///?} else {
            return net.minecraft.world.InteractionResultHolder.success(stack);
            //?}
        }
        //? >=1.21.4 {
        /*return InteractionResult.PASS;
         *///?} else {
        return net.minecraft.world.InteractionResultHolder.pass(stack);
        //?}
    }

    //? if fabric {
    @Override
    public void onInitializeClient() {
        UseItemCallback.EVENT.register((player, world, hand) -> onUse(player, world, hand, player.getItemInHand(hand)));
    }
    //?} elif neoforge {
    /*@SubscribeEvent
    public static void onUseItem(PlayerInteractEvent.RightClickItem event) {
        var result = onUse(event.getEntity(), event.getLevel(), event.getHand(), event.getItemStack())/^? <1.21.4 {^/ /^.getResult() ^//^?}^/;
        if(result != InteractionResult.PASS) {
            event.setCancellationResult(result);
            event.setCanceled(true);
        }
    }
    *///?}
}
