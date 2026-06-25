package dev.upcraft.datasync.testmod;

import com.mojang.logging.LogUtils;
import dev.upcraft.datasync.api.DataSyncAPI;
import dev.upcraft.datasync.api.SyncToken;
//? if fabric {
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
//?} elif neoforge {
/*import net.minecraft.world.InteractionResult;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
*///?}
//? >=1.21.4 && fabric {
/*import net.minecraft.world.InteractionResult;
 *///?}
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;

import java.util.Optional;

//? if neoforge {
/*@EventBusSubscriber
@Mod(Testmod.MOD_ID)
*///?}
public class Testmod /*? if fabric {*/ implements ModInitializer/*?}*/ {

    public static final String MOD_ID = "testmod";
    public static final SyncToken<SupporterData> SUPPORTER_DATA_SYNC_TOKEN = DataSyncAPI.register(SupporterData.class, Testmod.id("test_data"), SupporterData.CODEC);
    private static final Logger LOGGER = LogUtils.getLogger();

    private static /*? >=1.21.4 { */ /*InteractionResult *//*?} else { */ net.minecraft.world.InteractionResultHolder<ItemStack> /*?} */ onInteract(Player player, Level world, InteractionHand hand, ItemStack stack) {

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

    public static ResourceLocation id(String path) {
        //? <1.21 {
        /*return new ResourceLocation(MOD_ID, path);
         *///?} else {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
        //?}
    }

    private static void sendMessage(Player player, Component message) {
        //? >=26.1 {
        /*LOGGER.info("[{}] {}", player.getPlainTextName(), message.getString());
         *///?} else {
        LOGGER.info("[{}] {}", player.getScoreboardName(), message.getString());
        //?}

        //? >=26.1 || <=1.21.1 {
        player.sendSystemMessage(message);
        //?} else {
        /*((net.minecraft.server.level.ServerPlayer) player).sendSystemMessage(message);
         *///?}
    }

    //? if fabric {
    @Override
    public void onInitialize() {
        UseItemCallback.EVENT.register((player, level, hand) -> onInteract(player, level, hand, player.getItemInHand(hand)));
    }
    //?} elif neoforge {
    /*@SubscribeEvent
    public static void interactPlayer(PlayerInteractEvent.RightClickItem event) {
        var result = onInteract(event.getEntity(), event.getLevel(), event.getHand(), event.getItemStack())/^? <1.21.4 {^/ /^.getResult() ^//^?}^/;
        if(result != InteractionResult.PASS) {
            event.setCancellationResult(result);
            event.setCanceled(true);
        }
    }
    *///?}
}
