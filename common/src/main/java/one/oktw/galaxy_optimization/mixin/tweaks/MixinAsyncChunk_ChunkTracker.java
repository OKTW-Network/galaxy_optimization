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
