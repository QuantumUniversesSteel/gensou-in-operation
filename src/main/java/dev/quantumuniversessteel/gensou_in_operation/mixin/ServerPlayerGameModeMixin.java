package dev.quantumuniversessteel.gensou_in_operation.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.quantumuniversessteel.gensou_in_operation.flight.ReimuTestFlight;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeMixin {
    @Shadow @Final protected ServerPlayer player;

    @WrapOperation(method = "changeGameModeForPlayer", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayerGameMode;setGameModeForPlayer(Lnet/minecraft/world/level/GameType;Lnet/minecraft/world/level/GameType;)V"))
    private void gensou_in_operation$preserveFlight(ServerPlayerGameMode instance, GameType nextMode,
                                                   GameType previousMode, Operation<Void> original) {
        GameType oldMode = instance.getGameModeForPlayer();
        boolean wasFlying = player.getAbilities().flying;
        original.call(instance, nextMode, previousMode);
        // This is after a real mode update, but before its first abilities packet.
        ReimuTestFlight.afterGameModeChange(player, oldMode, wasFlying);
    }

    @WrapOperation(method = "changeGameModeForPlayer", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayer;onUpdateAbilities()V"))
    private void gensou_in_operation$deferEarlyAbilities(ServerPlayer instance, Operation<Void> original) {
        // The enclosing ServerPlayer#setGameMode sends the complete update after mode side effects.
        if (!ReimuTestFlight.hasOwnSource(instance)) {
            original.call(instance);
        }
    }
}
