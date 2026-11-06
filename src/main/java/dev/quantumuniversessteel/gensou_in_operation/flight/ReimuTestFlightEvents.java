package dev.quantumuniversessteel.gensou_in_operation.flight;

import dev.quantumuniversessteel.gensou_in_operation.GensouInOperation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = GensouInOperation.MODID)
public final class ReimuTestFlightEvents {
    private ReimuTestFlightEvents() {
    }

    @SubscribeEvent
    public static void onLoad(PlayerEvent.LoadFromFile event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ReimuTestFlight.restoreLoadedPlayer(player);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ReimuTestFlight.finishLogin(player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Death-screen disconnect may happen before the next player tick.
            ReimuTestFlight.clearIfDead(player);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onClone(PlayerEvent.Clone event) {
        if (event.getOriginal() instanceof ServerPlayer original
                && event.getEntity() instanceof ServerPlayer replacement) {
            ReimuTestFlight.restoreClone(original, replacement, event.isWasDeath());
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Respawn has been sent; the old client player no longer owns this state.
            ReimuTestFlight.sync(player);
        }
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ReimuTestFlight.sync(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ReimuTestFlight.clearIfDead(player);
        }
    }
}
