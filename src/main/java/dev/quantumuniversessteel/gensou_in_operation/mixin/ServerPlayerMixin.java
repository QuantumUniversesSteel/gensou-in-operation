package dev.quantumuniversessteel.gensou_in_operation.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import dev.quantumuniversessteel.gensou_in_operation.flight.ReimuTestFlight;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {
    @Shadow protected abstract void updateInvisibilityStatus();

    @WrapOperation(method = "setGameMode", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;send(Lnet/minecraft/network/protocol/Packet;)V"))
    private void gensou_in_operation$deferModePacket(ServerGamePacketListenerImpl connection, Packet<?> packet,
            Operation<Void> original, @Share("gensou_in_operation$mode") LocalRef<ClientboundGameEventPacket> pending) {
        if (ReimuTestFlight.hasOwnSource((ServerPlayer) (Object) this)
                && packet instanceof ClientboundGameEventPacket mode
                && mode.getEvent() == ClientboundGameEventPacket.CHANGE_GAME_MODE) {
            pending.set(mode);
        } else {
            original.call(connection, packet);
        }
    }

    @WrapOperation(method = "setGameMode", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayer;onUpdateAbilities()V"))
    private void gensou_in_operation$sendModeAndFlight(ServerPlayer player, Operation<Void> original,
            @Share("gensou_in_operation$mode") LocalRef<ClientboundGameEventPacket> pending) {
        if (pending.get() != null) {
            ReimuTestFlight.syncGameMode(player, pending.get());
            // Preserve the non-packet side effect of ServerPlayer#onUpdateAbilities.
            updateInvisibilityStatus();
        } else {
            original.call(player);
        }
    }
}
