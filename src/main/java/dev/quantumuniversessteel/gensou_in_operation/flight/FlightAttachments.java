package dev.quantumuniversessteel.gensou_in_operation.flight;

import dev.quantumuniversessteel.gensou_in_operation.GensouInOperation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public final class FlightAttachments {
    private static final DeferredRegister<AttachmentType<?>> TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, GensouInOperation.MODID);

    // Serializable attachments survive non-death clones by default, but not death.
    public static final Supplier<AttachmentType<ReimuTestFlightState>> REIMU_TEST_FLIGHT = TYPES.register(
            "reimu_test_flight", () -> AttachmentType.builder(() -> ReimuTestFlightState.DISABLED)
                    .serialize(ReimuTestFlightState.CODEC).build());

    private FlightAttachments() {
    }

    public static void register(IEventBus modBus) {
        TYPES.register(modBus);
    }
}
