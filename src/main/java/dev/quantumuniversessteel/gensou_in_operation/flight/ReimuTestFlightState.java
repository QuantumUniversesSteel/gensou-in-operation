package dev.quantumuniversessteel.gensou_in_operation.flight;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;

/** Server-owned permission and the flight state hidden by spectator's forced flight. */
public record ReimuTestFlightState(boolean enabled, Optional<Boolean> flyingBeforeSpectator) {
    public static final ReimuTestFlightState DISABLED = new ReimuTestFlightState(false, Optional.empty());
    public static final Codec<ReimuTestFlightState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("enabled", false).forGetter(ReimuTestFlightState::enabled),
            Codec.BOOL.optionalFieldOf("flying_before_spectator").forGetter(ReimuTestFlightState::flyingBeforeSpectator)
    ).apply(instance, ReimuTestFlightState::new));
}
