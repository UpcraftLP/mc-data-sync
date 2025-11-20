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
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public record S2CUpdatePlayerDataPacket(UUID targetId, @Nullable ResourceLocation dataTypeId) implements CustomPacketPayload {

    public static final ResourceLocation ID = DataSyncMod.id("s2c_update_player_data");
    public static final Type<S2CUpdatePlayerDataPacket> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CUpdatePlayerDataPacket> CODEC = StreamCodec.ofMember(S2CUpdatePlayerDataPacket::write, S2CUpdatePlayerDataPacket::fromNetwork);

    public boolean refreshAll() {
        return this.dataTypeId() == null;
    }

    public static void send(ServerPlayer p, UUID origin, @Nullable ResourceLocation id) {
        if (ServerPlayNetworking.canSend(p, ID)) {
            ServerPlayNetworking.send(p, new S2CUpdatePlayerDataPacket(origin, id));
        }
    }

    @Environment(EnvType.CLIENT)
    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(TYPE, S2CUpdatePlayerDataPacket::handle);
    }

    public static void registerServer() {
        PayloadTypeRegistry.playS2C().register(TYPE, CODEC);
    }

    @Environment(EnvType.CLIENT)
    public void handle(ClientPlayNetworking.Context context) {
        if (this.refreshAll()) {
            DataSyncAPI.refreshAllPlayerData(this.targetId());
            return;
        }

        DataType<?> type = DataRegistry.getById(this.dataTypeId());
        if (type == null) {
            DataSyncMod.LOGGER.debug("Ignoring incoming sync packet for unknown data type: '{}' from player {}", this.dataTypeId(), this.targetId());
            return;
        }

        DataStore.getPlayerLookup(this.targetId, type).reload();
    }

    private static S2CUpdatePlayerDataPacket fromNetwork(RegistryFriendlyByteBuf friendlyByteBuf) {

        UUID targetId = friendlyByteBuf.readUUID();

        boolean single = friendlyByteBuf.readBoolean();
        ResourceLocation dataType = null;
        if (single) {
            dataType = friendlyByteBuf.readResourceLocation();
        }

        return new S2CUpdatePlayerDataPacket(targetId, dataType);
    }

    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(this.targetId());

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
