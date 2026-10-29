package io.github.quantumuniversessteel.gensou_in_operation.flight;

import io.github.quantumuniversessteel.gensou_in_operation.GensouInOperation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.neoforged.neoforge.common.NeoForgeMod;

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

    private ReimuTestFlight() {
    }

    public static boolean hasOwnSource(ServerPlayer player) {
        AttributeInstance attribute =
                player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);

        return attribute != null && attribute.hasModifier(SOURCE_ID);
    }

    public static boolean grant(ServerPlayer player) {
        AttributeInstance attribute =
                player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);

        if (attribute == null) {
            return false;
        }

        if (!attribute.hasModifier(SOURCE_ID)) {
            attribute.addTransientModifier(SOURCE);
        }

        return true;
    }

    public static void revoke(ServerPlayer player) {
        AttributeInstance attribute =
                player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);

        if (attribute != null) {
            attribute.removeModifier(SOURCE_ID);
        }
    }
}