package dev.upcraft.datasync.net;

import dev.upcraft.datasync.DataSyncMod;
import dev.upcraft.datasync.api.DataSyncAPI;
import dev.upcraft.datasync.content.DataRegistry;
import dev.upcraft.datasync.content.DataStore;
import dev.upcraft.datasync.content.DataType;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.Nullable;

@EventBusSubscriber
public record C2SUpdatePlayerDataPacket(@Nullable Identifier dataTypeId) implements CustomPacketPayload {

    public static final Identifier ID = DataSyncMod.id("c2s_update_player_data");
    public static final CustomPacketPayload.Type<C2SUpdatePlayerDataPacket> TYPE = new CustomPacketPayload.Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SUpdatePlayerDataPacket> CODEC = StreamCodec.ofMember(C2SUpdatePlayerDataPacket::write, C2SUpdatePlayerDataPacket::fromNetwork);

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1").optional();
        registrar.playToServer(TYPE, CODEC, C2SUpdatePlayerDataPacket::handle);
    }

    public boolean refreshAll() {
        return this.dataTypeId() == null;
    }

    public static void trySend(@Nullable Identifier id) {
        if(Minecraft.getInstance().getConnection().hasChannel(TYPE)) {
            ClientPacketDistributor.sendToServer(new C2SUpdatePlayerDataPacket(id));
        }
    }

    public void handle(IPayloadContext context) {
        if(!(context.player() instanceof ServerPlayer player)) {
           return;
        }
        var originId = player.getGameProfile().id();
        var server = player.level().getServer();

        if(this.refreshAll()) {
            DataSyncAPI.refreshAllPlayerData(originId);
        }
        else {
            DataType<?> type = DataRegistry.getById(this.dataTypeId());
            if (type != null) {
                DataStore.getPlayerLookup(player.getGameProfile().id(), type).reload();
            } else {
                DataSyncMod.LOGGER.trace("Relaying sync packet for unknown data type '{}' for player {} ({})", this.dataTypeId(), player.getGameProfile().name(), originId);
            }
        }

        server.getPlayerList().getPlayers().stream().filter(p -> !p.getUUID().equals(player.getUUID())).forEach(p -> S2CUpdatePlayerDataPacket.send(p, originId, this.dataTypeId()));
    }

    private static C2SUpdatePlayerDataPacket fromNetwork(FriendlyByteBuf friendlyByteBuf) {
        boolean single = friendlyByteBuf.readBoolean();
        Identifier dataType = null;
        if (single) {
            dataType = friendlyByteBuf.readIdentifier();
        }

        return new C2SUpdatePlayerDataPacket(dataType);
    }

    public void write(FriendlyByteBuf buf) {
        if (this.dataTypeId() != null) {
            buf.writeBoolean(true);
            buf.writeIdentifier(this.dataTypeId());
        } else {
            buf.writeBoolean(false);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
