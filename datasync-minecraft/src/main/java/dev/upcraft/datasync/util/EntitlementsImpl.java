package dev.upcraft.datasync.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.upcraft.datasync.api.util.Entitlements;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.function.UnaryOperator;

public record EntitlementsImpl(List<ResourceLocation> keys) implements Entitlements {

    public static final Codec<EntitlementsImpl> RAW_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.listOf().fieldOf("values").forGetter(EntitlementsImpl::keys)
    ).apply(instance, EntitlementsImpl::new));

    public static final Codec<Entitlements> CODEC = RAW_CODEC.xmap(UnaryOperator.identity(), entitlements -> new EntitlementsImpl(entitlements.keys()));

    public static EntitlementsImpl empty() {
        return new EntitlementsImpl(List.of());
    }
}
