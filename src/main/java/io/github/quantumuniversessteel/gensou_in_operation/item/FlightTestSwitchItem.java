package io.github.quantumuniversessteel.gensou_in_operation.item;

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

        if (!level.isClientSide()) {
            player.displayClientMessage(
                    Component.translatable(
                            "message.gensou_in_operation.flight_test_switch.used"
                    ),
                    false
            );
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
