package io.github.quantumuniversessteel.gensou_in_operation.flight;

import io.github.quantumuniversessteel.gensou_in_operation.GensouInOperation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = GensouInOperation.MODID)
public final class ReimuTestFlightEvents {
    private ReimuTestFlightEvents() {
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        clearSource(event);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        clearSource(event);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        clearSource(event);
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        clearSource(event);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ReimuTestFlight.checkSession(player);
        }
    }

    @SubscribeEvent
    public static void onChangingGameMode(PlayerEvent.PlayerChangeGameModeEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Inspect only the actual current mode, never the still-cancellable requested mode.
            // This also catches a completed change before a second change in the same tick.
            ReimuTestFlight.checkSession(player);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        ReimuTestFlight.clearSessions();
    }

    private static void clearSource(PlayerEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ReimuTestFlight.revoke(player);
        }
    }
}
