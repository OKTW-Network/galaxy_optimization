package one.oktw.galaxy_optimization.mixin.accessor;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.entity.ai.village.poi.PoiSection;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.SectionStorage;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Mixin(SectionStorage.class)
public interface SectionStorageAccessor {
    @Invoker
    @Nullable
    Optional<PoiSection> callGet(long pos);

    @Invoker
    CompletableFuture<Optional<CompoundTag>> callTryRead(ChunkPos pos);

    @Invoker
    void callReadColumn(ChunkPos pos, RegistryOps<Tag> ops, @Nullable CompoundTag nbt);
}
