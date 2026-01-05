/*
 * OKTW Galaxy Optimization
 * Copyright (C) 2026-2026
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package one.oktw.galaxy_optimization.mixin.tweaks;

import net.minecraft.server.level.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import one.oktw.galaxy_optimization.mixin.accessor.ChunkMapAccessor;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Mixin(ServerChunkCache.class)
public abstract class MixinAsyncChunk_ServerChunkCache {
    @Shadow
    @Final
    ServerLevel level;

    @Shadow
    @Nullable
    protected abstract ChunkHolder getVisibleChunkIfPresent(long pos);

    @Shadow
    protected abstract boolean chunkAbsent(@Nullable ChunkHolder holder, int maxLevel);

    /**
     * @author James58899
     * @reason Use static ChunkPos.toLong
     */
    @Overwrite
    public boolean hasChunk(int x, int z) {
        return !this.chunkAbsent(this.getVisibleChunkIfPresent(ChunkPos.asLong(x, z)), ChunkLevel.byStatus(ChunkStatus.FULL));
    }

    @Redirect(method = "tickChunks", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ChunkMap;getChunks()Ljava/lang/Iterable;"))
    private Iterable<ChunkHolder> earlyCheckChunkShouldTick(ChunkMap instance) {
        ChunkMapAccessor accessor = (ChunkMapAccessor) instance;
        var stream = StreamSupport.stream(accessor.callGetChunks().spliterator(), false);
        return stream.filter(chunkHolder -> {
            LevelChunk chunk = chunkHolder.getTickingChunk();
            if (chunk == null) return false;
            ChunkPos pos = chunk.getPos();
            return level.isNaturalSpawningAllowed(pos) && accessor.callAnyPlayerCloseEnoughForSpawning(pos);
        }).collect(Collectors.toList());
    }

    @Redirect(method = "tickChunks", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;isNaturalSpawningAllowed(Lnet/minecraft/world/level/ChunkPos;)Z"))
    private boolean skipDupTickCheck(ServerLevel instance, ChunkPos pos) {
        return true;
    }

    @Redirect(method = "tickChunks", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ChunkMap;anyPlayerCloseEnoughForSpawning(Lnet/minecraft/world/level/ChunkPos;)Z"))
    private boolean skipDupTickCheck(ChunkMap instance, ChunkPos pos) {
        return true;
    }
}
