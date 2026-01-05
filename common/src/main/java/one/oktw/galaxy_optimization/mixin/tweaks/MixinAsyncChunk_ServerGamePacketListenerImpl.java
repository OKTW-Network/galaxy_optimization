package one.oktw.galaxy_optimization.mixin.tweaks;

import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class MixinAsyncChunk_ServerGamePacketListenerImpl {
    @Shadow
    public ServerPlayer player;

    @Shadow
    private static double clampHorizontal(double d) {
        return 0;
    }

    @Shadow
    public abstract void teleport(double x, double y, double z, float yaw, float pitch);

    @Inject(method = "handleMovePlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V"), cancellable = true)
    private void noBlockingMove(ServerboundMovePlayerPacket packet, CallbackInfo ci) {
        if (!packet.hasPosition()) return;

        int x = SectionPos.posToSectionCoord(clampHorizontal(packet.getX(this.player.getX())));
        int z = SectionPos.posToSectionCoord(clampHorizontal(packet.getZ(this.player.getZ())));
        if (!player.serverLevel().getChunkSource().isPositionTicking(ChunkPos.asLong(x, z))) {
            player.setDeltaMovement(Vec3.ZERO);
            teleport(this.player.getX(), this.player.getY(), this.player.getZ(), this.player.getYRot(), this.player.getXRot());
            ci.cancel();
        }
    }

    @Inject(method = "teleport(DDDFFLjava/util/Set;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;absMoveTo(DDDFF)V", shift = At.Shift.AFTER))
    private void onTeleport(double x, double y, double z, float yaw, float pitch, Set<RelativeMovement> set, CallbackInfo ci) {
        ServerLevel world = player.serverLevel();
        if (!world.players().contains(player)) return;
        world.getChunkSource().move(this.player);
    }
}
