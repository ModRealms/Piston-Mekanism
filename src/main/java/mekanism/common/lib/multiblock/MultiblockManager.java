package mekanism.common.lib.multiblock;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import mekanism.api.NBTConstants;
import mekanism.common.lib.MekanismSavedData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MultiblockManager<T extends MultiblockData> {

    private static final Set<MultiblockManager<?>> managers = new HashSet<>();

    private final String name;
    private final String nameLower;

    private final Supplier<MultiblockCache<T>> cacheSupplier;
    private final Supplier<IStructureValidator<T>> validatorSupplier;

    /**
     * A map containing references to all multiblock inventory caches per world
     */
    public final Map<ResourceKey<Level>, Map<UUID, MultiblockCache<T>>> caches = new HashMap<>();

    /**
     * Note: This can and will be null on the client side
     */
    private Map<ResourceKey<Level>, MultiblockCacheDataHandler> dataHandler = new HashMap<>();

    public MultiblockManager(String name, Supplier<MultiblockCache<T>> cacheSupplier, Supplier<IStructureValidator<T>> validatorSupplier) {
        this.name = name;
        this.nameLower = name.toLowerCase(Locale.ROOT);
        this.cacheSupplier = cacheSupplier;
        this.validatorSupplier = validatorSupplier;
        managers.add(this);
    }

    /**
     * Note: It is important that callers also call {@link #trackCache(ServerLevel, UUID, MultiblockCache)} after initializing any data the cache might require.
     */
    public MultiblockCache<T> createCache() {
        return cacheSupplier.get();
    }

    /**
     * Adds a cache as tracked and marks the manager as dirty.
     */
    public void trackCache(ServerLevel level, UUID id, MultiblockCache<T> cache) {
        this.getCaches(level).put(id, cache);
        markDirty();
    }

    public Map<UUID, MultiblockCache<T>> getCaches(ServerLevel level) {
        return this.getCaches(level.dimension());
    }

    public Map<UUID, MultiblockCache<T>> getCaches(ResourceKey<Level> level) {
        if(!this.caches.containsKey(level)) {
            this.caches.put(level, new HashMap<>());
        }

        return this.caches.get(level);
    }


    @Nullable
    public MultiblockCache<T> getCache(ServerLevel level, UUID multiblockID) {
        return this.getCaches(level).get(multiblockID);
    }

    public IStructureValidator<T> createValidator() {
        return validatorSupplier.get();
    }

    public String getName() {
        return name;
    }

    public String getNameLower() {
        return nameLower;
    }

    public boolean isCompatible(BlockEntity tile) {
        if (tile instanceof IMultiblock<?> multiblock) {
            return multiblock.getManager() == this;
        }
        return false;
    }

    public static void reset() {
        for (MultiblockManager<?> manager : managers) {
            manager.caches.clear();
            manager.dataHandler.clear();
        }
    }

    /**
     * Replaces and invalidates all the caches with the given ids with a new cache with the given id.
     */
    public void replaceCaches(ServerLevel level, Set<UUID> staleIds, UUID id, MultiblockCache<T> cache) {
        for (UUID staleId : staleIds) {
            getCaches(level).remove(staleId);
            System.out.println("Removing stale/old UUID from cache: " + staleId);
        }
        trackCache(level, id, cache);
    }

    public void handleDirtyMultiblock(T multiblock) {
        //Validate the multiblock is actually dirty and needs saving
        if (multiblock.isDirty()) {
            MultiblockCache<T> cache = getCache((ServerLevel) multiblock.getWorld(), multiblock.inventoryID);
            //Validate the multiblock's cache exists as if it doesn't we want to ignore it
            // in theory this method should only be called if the multiblock is valid and formed
            // but in case something goes wrong, don't let it
            if (cache != null) {
                cache.sync(multiblock);
                //If the multiblock is dirty mark the manager's data handler as dirty to ensure that we save
                markDirty();
                // next we can reset the dirty state of the multiblock
                multiblock.resetDirty();
            }
        }
    }

    /**
     * Grabs a unique inventory ID for a multiblock.
     *
     * @return unique inventory ID
     */
    public UUID getUniqueInventoryID() {
        return UUID.randomUUID();
    }

    private void markDirty() {
        dataHandler.values().forEach(SavedData::setDirty);
        System.out.println("Marked all dimensions as dirty");
    }

    /**
     * Note: This should only be called from the server side
     */
    public static void createOrLoadAll() {
        for (MultiblockManager<?> manager : managers) {
            manager.createOrLoad();
        }
    }

    /**
     * Note: This should only be called from the server side
     */
    public static void createOrLoadAll(ServerLevel level) {
        for (MultiblockManager<?> manager : managers) {
            manager.createOrLoad(level);
        }
    }

    /**
     * Note: This should only be called from the server side
     */
    private void createOrLoad() {
        dataHandler.put(ServerLevel.OVERWORLD, MekanismSavedData.createSavedData(() -> new MultiblockCacheDataHandler(ServerLevel.OVERWORLD), getNameLower()));
    }

    /**
     * Note: This should only be called from the server side
     */
    private void createOrLoad(ServerLevel level) {
        // Clear existing cache from the dimension to ensure there are no lingering structures that shouldn't be here.
        // todo: is there another way we can find this out without needing to add a level 'layer' to the cache?
        this.getCaches(level).clear();

        dataHandler.put(level.dimension(), MekanismSavedData.createSavedData(level.getDataStorage(), () -> new MultiblockCacheDataHandler(level.dimension()), getNameLower()));
    }

    private class MultiblockCacheDataHandler extends MekanismSavedData {

        private final ResourceKey<Level> level;

        public MultiblockCacheDataHandler(ResourceKey<Level> level) {
            this.level = level;
        }

        @Override
        public void load(@NotNull CompoundTag nbt) {
            if (nbt.contains(NBTConstants.CACHE, Tag.TAG_LIST)) {
                ListTag cachesNbt = nbt.getList(NBTConstants.CACHE, Tag.TAG_COMPOUND);
                for (int i = 0; i < cachesNbt.size(); i++) {
                    CompoundTag cacheTags = cachesNbt.getCompound(i);
                    if (cacheTags.hasUUID(NBTConstants.INVENTORY_ID)) {
                        UUID id = cacheTags.getUUID(NBTConstants.INVENTORY_ID);
                        System.out.println("Loading UUID from file cache: " + id);
                        MultiblockCache<T> cachedData = cacheSupplier.get();
                        cachedData.load(cacheTags);
                        getCaches(level).put(id, cachedData);
                    }
                }
            }
        }

        @NotNull
        @Override
        public CompoundTag save(@NotNull CompoundTag nbt) {
            ListTag cachesNbt = new ListTag();
            for (Map.Entry<UUID, MultiblockCache<T>> entry : getCaches(level).entrySet()) {
                CompoundTag cacheTags = new CompoundTag();
                //Note: We can just store the inventory id in the same compound tag as the rest of the cache data
                // as none of the caches save anything to this tag
                cacheTags.putUUID(NBTConstants.INVENTORY_ID, entry.getKey());

                System.out.println("Saving UUID into file cache: " + entry.getKey());
                entry.getValue().save(cacheTags);
                cachesNbt.add(cacheTags);
            }
            nbt.put(NBTConstants.CACHE, cachesNbt);
            return nbt;
        }
    }
}