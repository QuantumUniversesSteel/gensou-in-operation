package io.github.quantumuniversessteel.gensou_in_operation.flight;

import io.github.quantumuniversessteel.gensou_in_operation.GensouInOperation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.NeoForgeMod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ReimuTestFlight {
    private static final ResourceLocation SOURCE_ID =
            ResourceLocation.fromNamespaceAndPath(
                    GensouInOperation.MODID, "reimu_test_flight"
            );

    private static final AttributeModifier SOURCE =
            new AttributeModifier(
                    SOURCE_ID,
                    1.0,
                    AttributeModifier.Operation.ADD_VALUE
            );

    // Server-thread-only session state; never stored in player data.
    private static final Map<UUID, GameType> GRANTED_GAME_MODES = new HashMap<>();

    private ReimuTestFlight() {
    }

    public static boolean hasOwnSource(ServerPlayer player) {
        AttributeInstance attribute =
                player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);

        return attribute != null && attribute.hasModifier(SOURCE_ID);
    }

    public static boolean grant(ServerPlayer player) {
        if (!player.isAlive()) {
            return false;
        }

        AttributeInstance attribute =
                player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);

        if (attribute == null) {
            return false;
        }

        if (!attribute.hasModifier(SOURCE_ID)) {
            attribute.addTransientModifier(SOURCE);
        }

        GRANTED_GAME_MODES.put(player.getUUID(), player.gameMode.getGameModeForPlayer());
        return true;
    }

    public static void revoke(ServerPlayer player) {
        GRANTED_GAME_MODES.remove(player.getUUID());
        AttributeInstance attribute =
                player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);

        if (attribute != null && attribute.removeModifier(SOURCE_ID)) {
            // Removing our source must not stop creative, spectator, or another source's flight.
            if (player.getAbilities().flying && !player.mayFly()) {
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
            }
        }
    }

    static void checkSession(ServerPlayer player) {
        if (!hasOwnSource(player)) {
            GRANTED_GAME_MODES.remove(player.getUUID());
            return;
        }

        GameType grantedMode = GRANTED_GAME_MODES.get(player.getUUID());
        if (!player.isAlive() || grantedMode != player.gameMode.getGameModeForPlayer()) {
            revoke(player);
        }
    }

    static void clearSessions() {
        GRANTED_GAME_MODES.clear();
    }
}
