package one.oktw.galaxy_optimization.mixin.tweaks;

import net.minecraft.server.level.ChunkTracker;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ChunkTracker.class)
public abstract class MixinAsyncChunk_ChunkTracker {
    @Redirect(method = "checkNeighborsAfterUpdate", at = @At(value = "NEW", target = "(J)Lnet/minecraft/world/level/ChunkPos;"))
    private ChunkPos skipCreateChunkPos(long pos) {
        return ChunkPos.ZERO;
    }

    @Redirect(method = "checkNeighborsAfterUpdate", at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/ChunkPos;x:I"))
    private int getX(ChunkPos instance, long pos, int level, boolean decrease) {
        return (int) pos;
    }

    @Redirect(method = "checkNeighborsAfterUpdate", at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/ChunkPos;z:I"))
    private int getZ(ChunkPos instance, long pos, int level, boolean decrease) {
        return (int) (pos >> 32);
    }

    @Redirect(method = "getComputedLevel", at = @At(value = "NEW", target = "(J)Lnet/minecraft/world/level/ChunkPos;"))
    private ChunkPos skipCreateChunkPos2(long pos) {
        return ChunkPos.ZERO;
    }

    @Redirect(method = "getComputedLevel", at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/ChunkPos;x:I"))
    private int getX2(ChunkPos instance, long pos, long excludedId, int maxLevel) {
        return (int) pos;
    }

    @Redirect(method = "getComputedLevel", at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/ChunkPos;z:I"))
    private int getZ2(ChunkPos instance, long pos, long excludedId, int maxLevel) {
        return (int) (pos >> 32);
    }
}
