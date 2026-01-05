package one.oktw.galaxy_optimization.mixin.tweaks;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.RegionFile;
import net.minecraft.world.level.chunk.storage.RegionFileStorage;
import one.oktw.galaxy_optimization.interfaces.RegionFileInputStream;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.concurrent.locks.ReentrantLock;

@Mixin(RegionFileStorage.class)
public abstract class MixinAsyncChunk_RegionFileStorage {
    @Unique
    private final ReentrantLock lock = new ReentrantLock();

    @Inject(method = "getRegionFile", at = @At("HEAD"))
    private void readLock(ChunkPos pos, CallbackInfoReturnable<RegionFile> cir) {
        lock.lock();
    }

    @Inject(method = "getRegionFile", at = @At("RETURN"))
    private void readUnlock(ChunkPos pos, CallbackInfoReturnable<RegionFile> cir) {
        lock.unlock();
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void closeLock(CallbackInfo ci) {
        lock.lock();
    }

    @Inject(method = "close", at = @At("RETURN"))
    private void closeUnlock(CallbackInfo ci) {
        lock.unlock();
    }

    @Inject(method = "flush", at = @At("HEAD"))
    private void syncLock(CallbackInfo ci) {
        lock.lock();
    }

    @Inject(method = "flush", at = @At("RETURN"))
    private void syncUnlock(CallbackInfo ci) {
        lock.unlock();
    }

    @Redirect(method = "read", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/storage/RegionFile;getChunkDataInputStream(Lnet/minecraft/world/level/ChunkPos;)Ljava/io/DataInputStream;"))
    private DataInputStream overwriteGetIntputStream(RegionFile regionFile, ChunkPos pos) throws IOException {
        return ((RegionFileInputStream) regionFile).getChunkInputStreamNoSync(pos);
    }
}
