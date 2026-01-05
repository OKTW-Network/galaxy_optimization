package one.oktw.galaxy_optimization.mixin.tweaks;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Tuple;
import net.minecraft.util.thread.ProcessorMailbox;
import net.minecraft.util.thread.StrictQueue;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.IOWorker;
import net.minecraft.world.level.chunk.storage.IOWorker.Priority;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import one.oktw.galaxy_optimization.util.VirtualTaskExecutor;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Mixin(IOWorker.class)
public abstract class MixinAsyncChunk_IOWorker {
    @Unique
    private final AtomicBoolean galaxy_optimization$writeLock = new AtomicBoolean(false);

    @Mutable
    @Shadow
    @Final
    private ProcessorMailbox<StrictQueue.IntRunnable> mailbox;

    @Mutable
    @Shadow
    @Final
    private Map<ChunkPos, IOWorker.PendingStore> pendingWrites;

    @Shadow
    protected abstract void runStore(ChunkPos pos, IOWorker.PendingStore result);

    @Inject(method = "<init>", at = @At("RETURN"))
    private void parallelExecutor(RegionStorageInfo storageKey, Path directory, boolean dsync, CallbackInfo ci) {
        pendingWrites = new ConcurrentHashMap<>();
        mailbox = new VirtualTaskExecutor<>(new StrictQueue.FixedPriorityQueue(4 /* FOREGROUND,BACKGROUND,WRITE_DONE,SHUTDOWN */), "IOWorker-" + storageKey.type());
    }

    /**
     * @author James58899
     * @reason no low priority write & bulk write
     */
    @Overwrite
    private void storePendingChunk() {
        if (!this.pendingWrites.isEmpty() && !galaxy_optimization$writeLock.getAndSet(true)) {
            HashMap<Long, ArrayList<Tuple<ChunkPos, IOWorker.PendingStore>>> map = new HashMap<>();
            pendingWrites.forEach((pos, result) -> map.computeIfAbsent(ChunkPos.asLong(pos.getRegionX(), pos.getRegionZ()), k -> new ArrayList<>()).add(new Tuple<>(pos, result)));
            map.values().forEach(list ->
                mailbox.tell(new StrictQueue.IntRunnable(Priority.FOREGROUND.ordinal(), () -> list.forEach(pair -> runStore(pair.getA(), pair.getB()))))
            );
            this.mailbox.tell(new StrictQueue.IntRunnable(Priority.BACKGROUND.ordinal(), () -> {
                galaxy_optimization$writeLock.set(false);
                storePendingChunk();
            }));
        }
    }

    /**
     * @author James58899
     * @reason no low priority write
     */
    @Overwrite
    private void tellStorePending() {
        storePendingChunk();
    }

    /**
     * @author James58899
     * @reason no delay set result
     */
    @Overwrite
    public CompletableFuture<Void> store(ChunkPos pos, @Nullable CompoundTag nbt) {
        IOWorker.PendingStore result = this.pendingWrites.computeIfAbsent(pos, pos2 -> new IOWorker.PendingStore(nbt));
        result.data = nbt;
        return result.result;
    }

    @Inject(method = "loadAsync", at = @At("HEAD"), cancellable = true)
    private void fastRead(ChunkPos pos, CallbackInfoReturnable<CompletableFuture<Optional<CompoundTag>>> cir) {
        IOWorker.PendingStore result = this.pendingWrites.get(pos);
        if (result != null) {
            cir.setReturnValue(CompletableFuture.completedFuture(Optional.ofNullable(result.data)));
        }
    }

    @Inject(method = "runStore", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/storage/RegionFileStorage;write(Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/nbt/CompoundTag;)V", shift = At.Shift.BEFORE), cancellable = true)
    private void removeResults(ChunkPos pos, IOWorker.PendingStore result, CallbackInfo ci) {
        if (!this.pendingWrites.remove(pos, result)) { // Only write once
            ci.cancel();
        }
    }
}
