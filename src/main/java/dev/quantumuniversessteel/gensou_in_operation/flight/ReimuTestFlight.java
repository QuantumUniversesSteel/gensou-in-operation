package dev.quantumuniversessteel.gensou_in_operation.flight;

import dev.quantumuniversessteel.gensou_in_operation.GensouInOperation;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.NeoForgeMod;

import java.util.List;
import java.util.Optional;

public final class ReimuTestFlight {
    private static final ResourceLocation SOURCE_ID =
            ResourceLocation.fromNamespaceAndPath(GensouInOperation.MODID, "reimu_test_flight");
    private static final AttributeModifier SOURCE =
            new AttributeModifier(SOURCE_ID, 1.0, AttributeModifier.Operation.ADD_VALUE);

    private ReimuTestFlight() {
    }

    private static ReimuTestFlightState state(ServerPlayer player) {
        return player.hasData(FlightAttachments.REIMU_TEST_FLIGHT)
                ? player.getData(FlightAttachments.REIMU_TEST_FLIGHT) : ReimuTestFlightState.DISABLED;
    }

    public static boolean hasOwnSource(ServerPlayer player) {
        AttributeInstance attribute = player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
        return attribute != null && attribute.hasModifier(SOURCE_ID);
    }

    public static boolean grant(ServerPlayer player) {
        AttributeInstance attribute = player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
        if (!player.isAlive() || attribute == null) {
            return false;
        }
        if (!state(player).enabled()) {
            // Enabling in spectator must not capture its forced flight as a takeoff.
            player.setData(FlightAttachments.REIMU_TEST_FLIGHT,
                    new ReimuTestFlightState(true, player.isSpectator() ? Optional.of(false) : Optional.empty()));
        }
        if (!attribute.hasModifier(SOURCE_ID)) {
            attribute.addTransientModifier(SOURCE);
        }
        return true;
    }

    public static void revoke(ServerPlayer player) {
        boolean owned = state(player).enabled() || hasOwnSource(player);
        player.removeData(FlightAttachments.REIMU_TEST_FLIGHT);
        AttributeInstance attribute = player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
        if (attribute != null) {
            attribute.removeModifier(SOURCE_ID);
        }
        if (owned && player.getAbilities().flying && !player.mayFly()) {
            player.getAbilities().flying = false;
            if (player.connection != null) {
                player.onUpdateAbilities();
            }
        }
    }

    /** After NBT load, before game-mode setup, connection setup, and the initial abilities packet. */
    static void restoreLoadedPlayer(ServerPlayer player) {
        ReimuTestFlightState saved = state(player);
        if (player.getHealth() <= 0.0F) {
            revoke(player);
            return;
        }
        if (saved.enabled()) {
            grant(player);
            // Capture before loadGameTypes: the server may force a different login mode.
            boolean resumeFlying = saved.flyingBeforeSpectator().orElse(player.getAbilities().flying);
            player.setData(FlightAttachments.REIMU_TEST_FLIGHT,
                    new ReimuTestFlightState(true, Optional.of(resumeFlying)));
            player.getAbilities().flying = resumeFlying;
        }
    }

    static void finishLogin(ServerPlayer player) {
        if (!state(player).enabled()) {
            return;
        }
        if (!player.isSpectator()) {
            player.setData(FlightAttachments.REIMU_TEST_FLIGHT, new ReimuTestFlightState(true, Optional.empty()));
        }
        sync(player);
    }

    /** After NeoForge's attachment copy; only an actual death discards this mod's state. */
    static void restoreClone(ServerPlayer original, ServerPlayer replacement, boolean wasDeath) {
        if (wasDeath) {
            revoke(original);
            revoke(replacement);
        } else if (state(replacement).enabled()) {
            grant(replacement);
            replacement.getAbilities().flying = original.getAbilities().flying;
        }
    }

    /** Only called around a mode update that passed the cancellable event and actually executes. */
    public static void afterGameModeChange(ServerPlayer player, GameType oldMode, boolean wasFlying) {
        ReimuTestFlightState saved = state(player);
        GameType newMode = player.gameMode.getGameModeForPlayer();
        if (!saved.enabled() || oldMode == newMode) {
            return;
        }
        if (oldMode != GameType.SPECTATOR && newMode == GameType.SPECTATOR) {
            player.setData(FlightAttachments.REIMU_TEST_FLIGHT,
                    new ReimuTestFlightState(true, Optional.of(wasFlying)));
        } else if (oldMode == GameType.SPECTATOR && newMode != GameType.SPECTATOR) {
            player.getAbilities().flying = saved.flyingBeforeSpectator().orElse(false) && player.mayFly();
            player.setData(FlightAttachments.REIMU_TEST_FLIGHT, new ReimuTestFlightState(true, Optional.empty()));
        }
    }

    static void clearIfDead(ServerPlayer player) {
        // Removal during End credits is not death: isAlive() would also reject that player.
        if (player.getHealth() <= 0.0F && (state(player).enabled() || hasOwnSource(player))) {
            revoke(player);
        }
    }

    /** Mode and flight must be applied in one client task, without a tick in the old mode in between. */
    public static void syncGameMode(ServerPlayer player, ClientboundGameEventPacket mode) {
        AttributeInstance attribute = player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
        player.connection.send(new ClientboundBundlePacket(attribute == null
                ? List.of(mode, new ClientboundPlayerAbilitiesPacket(player.getAbilities()))
                : List.of(mode, new ClientboundUpdateAttributesPacket(player.getId(), List.of(attribute)),
                        new ClientboundPlayerAbilitiesPacket(player.getAbilities()))));
    }

    /** Restore the new client player with vanilla packets, not a custom protocol. */
    static void sync(ServerPlayer player) {
        if (state(player).enabled() && player.connection != null) {
            AttributeInstance attribute = player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
            if (attribute != null) {
                player.connection.send(new ClientboundBundlePacket(List.of(
                        new ClientboundUpdateAttributesPacket(player.getId(), List.of(attribute)),
                        new ClientboundPlayerAbilitiesPacket(player.getAbilities()))));
            }
        }
    }
}
