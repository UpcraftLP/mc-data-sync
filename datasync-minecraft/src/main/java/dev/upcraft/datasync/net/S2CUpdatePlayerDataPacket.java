package dev.upcraft.datasync.net;

import dev.upcraft.datasync.DataSyncMod;
import dev.upcraft.datasync.api.DataSyncAPI;
import dev.upcraft.datasync.content.DataRegistry;
import dev.upcraft.datasync.content.DataStore;
import dev.upcraft.datasync.content.DataType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public record S2CUpdatePlayerDataPacket(UUID targetId, @Nullable ResourceLocation dataTypeId) implements FabricPacket {

    public static final ResourceLocation ID = DataSyncMod.id("s2c_update_player_data");
    public static final PacketType<S2CUpdatePlayerDataPacket> TYPE = PacketType.create(ID, S2CUpdatePlayerDataPacket::fromNetwork);

    public boolean refreshAll() {
        return this.dataTypeId() == null;
    }

    public static void send(ServerPlayer p, UUID origin, @Nullable ResourceLocation id) {
        if (ServerPlayNetworking.canSend(p, TYPE)) {
            ServerPlayNetworking.send(p, new S2CUpdatePlayerDataPacket(origin, id));
        }
    }

    @Environment(EnvType.CLIENT)
    public void handle(LocalPlayer localPlayer, PacketSender packetSender) {
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

    private static S2CUpdatePlayerDataPacket fromNetwork(FriendlyByteBuf friendlyByteBuf) {

        UUID targetId = friendlyByteBuf.readUUID();

        boolean single = friendlyByteBuf.readBoolean();
        ResourceLocation dataType = null;
        if (single) {
            dataType = friendlyByteBuf.readResourceLocation();
        }

        return new S2CUpdatePlayerDataPacket(targetId, dataType);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeUUID(this.targetId());

        if (this.dataTypeId() != null) {
            buf.writeBoolean(true);
            buf.writeResourceLocation(this.dataTypeId());
        } else {
            buf.writeBoolean(false);
        }
    }

    @Override
    public PacketType<?> getType() {
        return TYPE;
    }
}
