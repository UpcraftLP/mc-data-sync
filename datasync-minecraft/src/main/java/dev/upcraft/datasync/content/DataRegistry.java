package dev.upcraft.datasync.content;

import com.mojang.serialization.Codec;
import dev.upcraft.datasync.DataSyncMod;
import dev.upcraft.datasync.api.SyncToken;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

public class DataRegistry {

    private static final Map<ResourceLocation, DataType<?>> DATA_TYPES = new HashMap<>();
    private static final Collection<DataType<?>> VALUES_VIEW = Collections.unmodifiableCollection(DATA_TYPES.values());

    public static <T> SyncToken<T> add(Class<T> type, ResourceLocation id, Codec<T> codec) {
        if(DATA_TYPES.containsKey(id)) {
            throw new IllegalStateException("Data Type '%s' already registered! (%s)".formatted(id, DATA_TYPES.get(id).type().getName()));
        }
        DataSyncMod.LOGGER.debug("Registering new sync token: {} ({})", id, type.getName());
        DataType<T> t = new DataType<>(type, id, codec);
        DATA_TYPES.put(id, t);

        return t;
    }

    public static int size() {
        return DATA_TYPES.size();
    }

    public static Collection<DataType<?>> values() {
        return VALUES_VIEW;
    }
}
