package dev.upcraft.datasync.net;

import dev.upcraft.datasync.DataSyncMod;
import dev.upcraft.datasync.api.DataSyncAPI;
import dev.upcraft.datasync.content.DataRegistry;
import dev.upcraft.datasync.content.DataStore;
import dev.upcraft.datasync.content.DataType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public record C2SUpdatePlayerDataPacket(@Nullable ResourceLocation dataTypeId) implements CustomPacketPayload {

    public static final ResourceLocation ID = DataSyncMod.id("c2s_update_player_data");
    public static final CustomPacketPayload.Type<C2SUpdatePlayerDataPacket> TYPE = new CustomPacketPayload.Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SUpdatePlayerDataPacket> CODEC = StreamCodec.ofMember(C2SUpdatePlayerDataPacket::write, C2SUpdatePlayerDataPacket::fromNetwork);

    public static void register() {
        PayloadTypeRegistry.playC2S().register(TYPE, CODEC);
        ServerPlayNetworking.registerGlobalReceiver(C2SUpdatePlayerDataPacket.TYPE, C2SUpdatePlayerDataPacket::handle);
    }

    public boolean refreshAll() {
        return this.dataTypeId() == null;
    }

    @Environment(EnvType.CLIENT)
    public static void trySend(@Nullable ResourceLocation id) {
        if (ClientPlayNetworking.canSend(TYPE)) {
            ClientPlayNetworking.send(new C2SUpdatePlayerDataPacket(id));
        }
    }

    public void handle(ServerPlayNetworking.Context context) {
        var serverPlayer = context.player();
        var originId = serverPlayer.getGameProfile().getId();
        var server = context.server();

        if(this.refreshAll()) {
            DataSyncAPI.refreshAllPlayerData(originId);
        }
        else {
            DataType<?> type = DataRegistry.getById(this.dataTypeId());
            if (type != null) {
                DataStore.getPlayerLookup(serverPlayer.getGameProfile().getId(), type).reload();
            } else {
                DataSyncMod.LOGGER.trace("Relaying sync packet for unknown data type '{}' for player {} ({})", this.dataTypeId(), serverPlayer.getGameProfile().getName(), originId);
            }
        }
        PlayerLookup.all(server).stream().filter(p -> p != serverPlayer).forEach(p -> S2CUpdatePlayerDataPacket.send(p, originId, this.dataTypeId()));
    }

    private static C2SUpdatePlayerDataPacket fromNetwork(FriendlyByteBuf friendlyByteBuf) {
        boolean single = friendlyByteBuf.readBoolean();
        ResourceLocation dataType = null;
        if (single) {
            dataType = friendlyByteBuf.readResourceLocation();
        }

        return new C2SUpdatePlayerDataPacket(dataType);
    }

    public void write(FriendlyByteBuf buf) {
        if (this.dataTypeId() != null) {
            buf.writeBoolean(true);
            buf.writeResourceLocation(this.dataTypeId());
        } else {
            buf.writeBoolean(false);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
