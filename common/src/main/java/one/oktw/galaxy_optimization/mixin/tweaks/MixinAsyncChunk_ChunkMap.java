package one.oktw.galaxy_optimization.mixin.tweaks;

import com.mojang.datafixers.DataFixer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiSection;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.status.ChunkType;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import net.minecraft.world.level.chunk.storage.ChunkStorage;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import one.oktw.galaxy_optimization.mixin.accessor.SectionStorageAccessor;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.*;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Mixin(ChunkMap.class)
public abstract class MixinAsyncChunk_ChunkMap extends ChunkStorage {
    @Unique
    private final HashMap<ChunkPos, CompletableFuture<Void>> galaxy_optimization$poiFutures = new HashMap<>();

    @Shadow
    @Final
    private static Logger LOGGER;
    @Shadow
    @Final
    ServerLevel level;
    @Shadow
    @Final
    private PoiManager poiManager;
    @Shadow
    @Final
    private BlockableEventLoop<Runnable> mainThreadExecutor;

    public MixinAsyncChunk_ChunkMap(RegionStorageInfo storageKey, Path directory, DataFixer dataFixer, boolean dsync) {
        super(storageKey, directory, dataFixer, dsync);
    }

    @Shadow
    private static boolean isChunkDataValid(CompoundTag nbt) {
        return false;
    }

    @Shadow
    protected abstract CompletableFuture<Optional<CompoundTag>> readChunk(ChunkPos chunkPos);

    @Shadow
    protected abstract ChunkAccess createEmptyChunk(ChunkPos chunkPos);

    @Shadow
    protected abstract byte markPosition(ChunkPos pos, ChunkType type);

    @Shadow
    protected abstract ChunkAccess handleChunkLoadFailure(Throwable throwable, ChunkPos chunkPos);

    /**
     * @author James58899
     * @reason Async POI loading
     */
    @Overwrite
    private CompletableFuture<ChunkAccess> scheduleChunkLoad(ChunkPos pos) {
        CompletableFuture<Optional<CompoundTag>> chunkNbtFuture = this.readChunk(pos).thenApply(nbt -> nbt.filter(nbt2 -> {
            boolean bl = isChunkDataValid(nbt2);
            if (!bl) {
                LOGGER.error("Chunk file at {} is missing level data, skipping", pos);
            }
            return bl;
        }));
        SectionStorageAccessor poiStorage = ((SectionStorageAccessor) poiManager);
        Optional<PoiSection> poiData = poiStorage.callGet(pos.toLong());
        var poiFuture = CompletableFuture.<Void>completedFuture(null);
        //noinspection OptionalAssignedToNull
        if (poiData == null || poiData.isEmpty()) {
            if (galaxy_optimization$poiFutures.containsKey(pos)) {
                poiFuture = galaxy_optimization$poiFutures.get(pos);
            } else {
                poiFuture = ((SectionStorageAccessor) poiManager).callTryRead(pos).thenAcceptAsync(nbt -> {
                    RegistryOps<Tag> registryOps = RegistryOps.create(NbtOps.INSTANCE, level.registryAccess());
                    poiStorage.callReadColumn(pos, registryOps, nbt.orElse(null));
                }, this.mainThreadExecutor);
                galaxy_optimization$poiFutures.put(pos, poiFuture);
            }
        }
        return CompletableFuture.allOf(chunkNbtFuture, poiFuture).thenApplyAsync(unused -> {
            galaxy_optimization$poiFutures.remove(pos);
            var nbt = chunkNbtFuture.join();
            this.level.getProfiler().incrementCounter("chunkLoad");
            if (nbt.isPresent()) {
                ProtoChunk chunk = ChunkSerializer.read(this.level, this.poiManager, this.storageInfo(), pos, nbt.get());
                this.markPosition(pos, ((ChunkAccess) chunk).getPersistedStatus().getChunkType());
                return chunk;
            }
            return this.createEmptyChunk(pos);
        }, this.mainThreadExecutor).exceptionallyAsync(throwable -> this.handleChunkLoadFailure(throwable, pos), this.mainThreadExecutor);
    }
}
