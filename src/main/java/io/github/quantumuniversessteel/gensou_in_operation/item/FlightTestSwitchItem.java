package io.github.quantumuniversessteel.gensou_in_operation.item;

import io.github.quantumuniversessteel.gensou_in_operation.flight.ReimuTestFlight;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class FlightTestSwitchItem extends Item {
    public FlightTestSwitchItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);

        if (player instanceof ServerPlayer serverPlayer) {
            if (serverPlayer.isPassenger()
                    || !serverPlayer.onGround()
                    || serverPlayer.mainSupportingBlockPos.isEmpty()) {
                serverPlayer.displayClientMessage(
                        Component.translatable(
                                "message.gensou_in_operation.flight_test_switch.blocked"
                        ),
                        false
                );
                return InteractionResultHolder.fail(stack);
            }

            String messageKey;

            if (ReimuTestFlight.hasOwnSource(serverPlayer)) {
                ReimuTestFlight.revoke(serverPlayer);
                messageKey =
                        "message.gensou_in_operation.flight_test_switch.disabled";
            } else if (ReimuTestFlight.grant(serverPlayer)) {
                messageKey =
                        "message.gensou_in_operation.flight_test_switch.enabled";
            } else {
                serverPlayer.displayClientMessage(
                        Component.translatable(
                                "message.gensou_in_operation.flight_test_switch.unavailable"
                        ),
                        false
                );
                return InteractionResultHolder.fail(stack);
            }

            serverPlayer.displayClientMessage(
                    Component.translatable(messageKey),
                    false
            );
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
