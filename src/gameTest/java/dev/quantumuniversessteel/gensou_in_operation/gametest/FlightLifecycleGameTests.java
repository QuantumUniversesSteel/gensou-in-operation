package dev.quantumuniversessteel.gensou_in_operation.gametest;

import com.mojang.authlib.GameProfile;
import dev.quantumuniversessteel.gensou_in_operation.GensouInOperation;
import dev.quantumuniversessteel.gensou_in_operation.flight.ReimuTestFlight;
import dev.quantumuniversessteel.gensou_in_operation.flight.FlightAttachments;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestListener;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundChunkBatchFinishedPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundChunkBatchReceivedPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

@Mod("gensou_in_operation_tests")
@GameTestHolder(GensouInOperation.MODID)
@PrefixGameTestTemplate(false)
public final class FlightLifecycleGameTests {
    private static final ResourceLocation OWN_SOURCE =
            ResourceLocation.fromNamespaceAndPath(GensouInOperation.MODID, "reimu_test_flight");
    private static final ResourceLocation OTHER_SOURCE =
            ResourceLocation.fromNamespaceAndPath("flight_test_other", "permission");

    @GameTest(template = "empty")
    public static void grantAndRevokeSynchronize(GameTestHelper helper) {
        Fixture owner = new Fixture(helper);
        Fixture observer = new Fixture(helper);
        owner.drainPackets();
        observer.drainPackets();

        helper.assertTrue(ReimuTestFlight.grant(owner.player), "Grant must succeed");
        helper.assertTrue(ReimuTestFlight.grant(owner.player), "Repeated grant must be harmless");
        helper.assertTrue(owner.player.mayFly() && !owner.player.getAbilities().flying,
                "Permission must not force takeoff");
        helper.startSequence().thenWaitUntil(() -> {
            assertAttributePacket(helper, owner.receivedPackets(), owner.player, true, "owner");
            assertAttributePacket(helper, observer.receivedPackets(), owner.player, true, "observer");
        }).thenExecute(() -> {
            owner.drainPackets();
            observer.drainPackets();
            owner.player.getAbilities().flying = true;
            ReimuTestFlight.revoke(owner.player);
            ReimuTestFlight.revoke(owner.player);
            helper.assertTrue(!owner.player.mayFly() && !owner.player.getAbilities().flying,
                    "Revoking the last source must stop flight");
        }).thenWaitUntil(() -> {
            helper.assertTrue(owner.receivedPackets().stream().anyMatch(packet ->
                            packet instanceof ClientboundPlayerAbilitiesPacket abilities && !abilities.isFlying()),
                    "Owner must receive a stop-flying abilities packet");
            assertAttributePacket(helper, owner.receivedPackets(), owner.player, false, "owner");
            assertAttributePacket(helper, observer.receivedPackets(), owner.player, false, "observer");
        }).thenSucceed();
    }

    @GameTest(template = "empty")
    public static void ordinaryModesPreservePermissionAndFlight(GameTestHelper helper) {
        for (boolean flying : new boolean[]{false, true}) {
            Fixture fixture = new Fixture(helper);
            ReimuTestFlight.grant(fixture.player);
            fixture.player.getAbilities().flying = flying;
            for (GameType mode : new GameType[]{GameType.ADVENTURE, GameType.CREATIVE, GameType.SURVIVAL}) {
                fixture.player.setGameMode(mode);
                assertState(helper, fixture.player, true, flying, "mode " + mode);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void cancelledAndNoOpModeChangesKeepSource(GameTestHelper helper) {
        Fixture fixture = new Fixture(helper);
        Consumer<PlayerEvent.PlayerChangeGameModeEvent> cancel = event -> {
            if (event.getEntity() == fixture.player) {
                event.setCanceled(true);
            }
        };
        NeoForge.EVENT_BUS.addListener(cancel);
        fixture.listeners.add(cancel);
        ReimuTestFlight.grant(fixture.player);
        helper.assertTrue(!fixture.player.setGameMode(GameType.CREATIVE), "Mode change must be cancelled");
        fixture.player.setGameMode(GameType.SURVIVAL);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(fixture.player.gameMode.getGameModeForPlayer() == GameType.SURVIVAL
                            && ReimuTestFlight.hasOwnSource(fixture.player) && fixture.player.mayFly(),
                    "Cancelled or no-op requests must not revoke permission");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void sameTickSpectatorRoundTripsRestoreOriginalFlight(GameTestHelper helper) {
        Fixture fixture = new Fixture(helper);
        ReimuTestFlight.grant(fixture.player);
        for (boolean flying : new boolean[]{false, true}) {
            fixture.player.getAbilities().flying = flying;
            fixture.player.setGameMode(GameType.SPECTATOR);
            assertState(helper, fixture.player, true, true, "spectator forces flight");
            helper.assertTrue(fixture.player.isInvisible(), "Spectator entry must refresh invisibility immediately");
            ReimuTestFlight.grant(fixture.player);
            fixture.player.setGameMode(GameType.CREATIVE);
            assertState(helper, fixture.player, true, flying, "exit to creative restores prior state");
            helper.assertTrue(!fixture.player.isInvisible(), "Spectator exit must refresh invisibility immediately");
            fixture.player.setGameMode(GameType.SPECTATOR);
            fixture.player.setGameMode(GameType.SURVIVAL);
            assertState(helper, fixture.player, true, flying, "same-tick second roundtrip");
            helper.assertTrue(!fixture.player.isInvisible(), "Same-tick roundtrip must not leave spectator invisibility");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void externalSourcesAndBaseValueSurviveCleanup(GameTestHelper helper) {
        Fixture fixture = new Fixture(helper);
        var attribute = fixture.player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
        attribute.addTransientModifier(otherSource());
        ReimuTestFlight.grant(fixture.player);
        fixture.player.getAbilities().flying = true;
        ReimuTestFlight.revoke(fixture.player);
        helper.assertTrue(attribute.hasModifier(OTHER_SOURCE) && fixture.player.mayFly()
                        && fixture.player.getAbilities().flying,
                "An external modifier and its ongoing flight must survive cleanup");

        attribute.removeModifier(OTHER_SOURCE);
        attribute.setBaseValue(1);
        ReimuTestFlight.grant(fixture.player);
        ReimuTestFlight.revoke(fixture.player);
        helper.assertTrue(attribute.getBaseValue() == 1 && fixture.player.mayFly()
                        && fixture.player.getAbilities().flying,
                "Cleanup must not reset an external base value");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void activeFlightAndExternalSourceSurviveReconnect(GameTestHelper helper) {
        Fixture fixture = new Fixture(helper);
        fixture.player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT).addPermanentModifier(otherSource());
        ReimuTestFlight.grant(fixture.player);
        fixture.player.getAbilities().flying = true;
        CompoundTag save = fixture.player.saveWithoutId(new CompoundTag());
        String attributes = save.getList("attributes", Tag.TAG_COMPOUND).toString();
        helper.assertTrue(!attributes.contains(OWN_SOURCE.toString()) && attributes.contains(OTHER_SOURCE.toString()),
                "Only the external modifier is saved as an attribute; our attachment restores our source");
        fixture.reconnect();
        assertState(helper, fixture.player, true, true, "immediate reconnect");
        helper.assertTrue(fixture.player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT).hasModifier(OTHER_SOURCE),
                "External permanent source survives the same load");
        helper.startSequence().thenWaitUntil(() -> {
            assertRestoreBundle(helper, fixture.bundles, fixture.player, true);
            assertState(helper, fixture.player, true, true, "after connection ticks");
        }).thenSucceed();
    }

    @GameTest(template = "empty")
    public static void deathAndRespawnClearSource(GameTestHelper helper) {
        Fixture fixture = new Fixture(helper);
        ReimuTestFlight.grant(fixture.player);
        fixture.player.hurt(fixture.player.damageSources().genericKill(), Float.MAX_VALUE);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(!fixture.player.isAlive() && !ReimuTestFlight.hasOwnSource(fixture.player),
                    "Real death must clear the old player source");
            helper.assertTrue(!ReimuTestFlight.grant(fixture.player), "A dead player cannot receive a new source");
            fixture.respawn();
            helper.assertTrue(!ReimuTestFlight.hasOwnSource(fixture.player) && !fixture.player.mayFly(),
                    "Respawn must start without our permission");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void cancelledDeathWithRecoveryKeepsSource(GameTestHelper helper) {
        Fixture fixture = new Fixture(helper);
        Consumer<LivingDeathEvent> cancel = event -> {
            if (event.getEntity() == fixture.player) {
                event.getEntity().setHealth(1);
                event.setCanceled(true);
            }
        };
        NeoForge.EVENT_BUS.addListener(cancel);
        fixture.listeners.add(cancel);
        ReimuTestFlight.grant(fixture.player);
        fixture.player.hurt(fixture.player.damageSources().genericKill(), Float.MAX_VALUE);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(fixture.player.isAlive() && ReimuTestFlight.hasOwnSource(fixture.player),
                    "A cancelled death that restores health must not clear permission");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void dimensionRoundTripsPreservePermissionAndFlight(GameTestHelper helper) {
        ServerLevel nether = helper.getLevel().getServer().getLevel(Level.NETHER);
        helper.assertTrue(nether != null, "Nether must exist");
        List<Fixture> fixtures = new ArrayList<>();
        for (int state = 0; state < 3; state++) {
            Fixture fixture = new Fixture(helper);
            fixtures.add(fixture);
            boolean enabled = state > 0;
            boolean flying = state == 2;
            if (enabled) ReimuTestFlight.grant(fixture.player);
            fixture.player.getAbilities().flying = flying;
            fixture.player.changeDimension(new DimensionTransition(nether, new Vec3(0, 100, 0), Vec3.ZERO,
                    0, 0, DimensionTransition.DO_NOTHING));
            assertState(helper, fixture.player, enabled, flying, "to Nether");
        }
        // Give destination chunks and teleport acknowledgements time to settle before returning.
        // Repeated same-tick cold-dimension roundtrips can strand chunk-unload work during shutdown.
        helper.startSequence().thenIdle(5).thenWaitUntil(() ->
                helper.assertTrue(nether.hasChunkAt(fixtures.getFirst().player.blockPosition()), "Nether destination loaded"))
                .thenExecute(() -> {
                    for (int state = 0; state < fixtures.size(); state++) {
                        Fixture fixture = fixtures.get(state);
                        assertState(helper, fixture.player, state > 0, state == 2, "settled in Nether");
                        fixture.player.changeDimension(new DimensionTransition(fixture.level, fixture.position, Vec3.ZERO,
                                0, 0, DimensionTransition.DO_NOTHING));
                        assertState(helper, fixture.player, state > 0, state == 2, "to Overworld");
                    }
                }).thenIdle(5).thenSucceed();
    }


    @GameTest(template = "empty")
    public static void disabledAndStoppedStatesSurviveReconnect(GameTestHelper helper) {
        for (int state = 0; state < 3; state++) {
            Fixture fixture = new Fixture(helper);
            if (state > 0) ReimuTestFlight.grant(fixture.player);
            if (state == 2) ReimuTestFlight.revoke(fixture.player);
            fixture.reconnect();
            assertState(helper, fixture.player, state == 1, false, "disabled/stopped reconnect " + state);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void spectatorMemorySurvivesReconnect(GameTestHelper helper) {
        for (boolean flying : new boolean[]{false, true}) {
            Fixture fixture = new Fixture(helper);
            ReimuTestFlight.grant(fixture.player);
            fixture.player.getAbilities().flying = flying;
            fixture.player.setGameMode(GameType.SPECTATOR);
            fixture.reconnect();
            helper.assertTrue(fixture.player.isSpectator(), "Reconnect must preserve saved spectator mode");
            assertState(helper, fixture.player, true, true, "spectator reconnect");
            fixture.player.setGameMode(GameType.SURVIVAL);
            assertState(helper, fixture.player, true, flying, "spectator memory after reconnect");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void spectatorExitSendsRestoredAbilities(GameTestHelper helper) {
        Fixture fixture = new Fixture(helper);
        ReimuTestFlight.grant(fixture.player);
        fixture.player.setGameMode(GameType.SPECTATOR);
        helper.startSequence().thenIdle(2).thenExecute(() -> {
            fixture.drainPackets();
            fixture.player.setGameMode(GameType.SURVIVAL);
        }).thenWaitUntil(() -> {
            var abilities = fixture.receivedPackets().stream()
                    .filter(ClientboundPlayerAbilitiesPacket.class::isInstance)
                    .map(ClientboundPlayerAbilitiesPacket.class::cast).toList();
            helper.assertTrue(!abilities.isEmpty(), "Mode change must send abilities");
            helper.assertTrue(abilities.stream().noneMatch(ClientboundPlayerAbilitiesPacket::isFlying),
                    "Even the first exit abilities packet must restore stopped flight");
            assertState(helper, fixture.player, true, false, "spectator exit after ticks");
            boolean atomic = fixture.bundles.stream().anyMatch(bundle -> {
                List<Object> parts = new ArrayList<>();
                bundle.subPackets().forEach(parts::add);
                return parts.size() == 3
                        && parts.getFirst() instanceof ClientboundGameEventPacket mode
                        && mode.getEvent() == ClientboundGameEventPacket.CHANGE_GAME_MODE
                        && mode.getParam() == GameType.SURVIVAL.getId()
                        && parts.get(1) instanceof ClientboundUpdateAttributesPacket attributes
                        && attributes.getEntityId() == fixture.player.getId()
                        && parts.getLast() instanceof ClientboundPlayerAbilitiesPacket flight && !flight.isFlying();
            });
            helper.assertTrue(atomic, "Mode, complete attribute and restored flight must share one bundle");
        }).thenSucceed();
    }

    @GameTest(template = "empty")
    public static void rewrittenModeRequestUsesActualMode(GameTestHelper helper) {
        Fixture fixture = new Fixture(helper);
        ReimuTestFlight.grant(fixture.player);
        Consumer<PlayerEvent.PlayerChangeGameModeEvent> rewrite = event -> {
            if (event.getEntity() == fixture.player && event.getNewGameMode() == GameType.SPECTATOR) {
                event.setNewGameMode(GameType.CREATIVE);
            }
        };
        NeoForge.EVENT_BUS.addListener(rewrite);
        fixture.listeners.add(rewrite);
        fixture.player.setGameMode(GameType.SPECTATOR);
        helper.assertTrue(fixture.player.isCreative(), "Listener must rewrite the requested mode");
        assertState(helper, fixture.player, true, false, "rewritten mode must not create a spectator memory");
        fixture.player.setGameMode(GameType.SURVIVAL);
        assertState(helper, fixture.player, true, false, "after rewritten request");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void vanillaAndExternalOnlyFlightAreNotCaptured(GameTestHelper helper) {
        Fixture fixture = new Fixture(helper);
        fixture.player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT).addPermanentModifier(otherSource());
        fixture.player.getAbilities().flying = true;
        fixture.player.setGameMode(GameType.SPECTATOR);
        fixture.player.setGameMode(GameType.CREATIVE);
        fixture.reconnect();
        helper.assertTrue(fixture.player.isCreative() && fixture.player.getAbilities().flying,
                "Native mode and external-only flight must not be overwritten");
        helper.assertTrue(!fixture.player.hasData(FlightAttachments.REIMU_TEST_FLIGHT)
                        && !ReimuTestFlight.hasOwnSource(fixture.player), "Must not create our permission from another source");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void activeSavesLoadIntoFreshPlayers(GameTestHelper helper) {
        for (boolean flying : new boolean[]{false, true}) {
            Fixture fixture = new Fixture(helper);
            ReimuTestFlight.grant(fixture.player);
            fixture.player.getAbilities().flying = flying;
            fixture.level.getServer().getPlayerList().saveAll();
            ServerPlayer loaded = fixture.loadDetachedPlayer();
            assertState(helper, loaded, true, flying, "load active save without logout or runtime state");
            fixture.player.setGameMode(GameType.SPECTATOR);
            fixture.level.getServer().getPlayerList().saveAll();
            loaded = fixture.loadDetachedPlayer();
            helper.assertTrue(loaded.isSpectator() && loaded.getAbilities().flying,
                    "Saved spectator mode must force flight on load");
            helper.assertTrue(loaded.getData(FlightAttachments.REIMU_TEST_FLIGHT)
                    .flyingBeforeSpectator().orElseThrow() == flying, "Spectator memory must survive active disk save");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void immediateDeathDisconnectAndSavedDeathCannotRestoreFlight(GameTestHelper helper) {
        Fixture fixture = new Fixture(helper);
        ReimuTestFlight.grant(fixture.player);
        fixture.player.getAbilities().flying = true;
        fixture.player.hurt(fixture.player.damageSources().genericKill(), Float.MAX_VALUE);
        fixture.level.getServer().getPlayerList().saveAll();
        ServerPlayer loaded = fixture.loadDetachedPlayer();
        assertState(helper, loaded, false, false, "dead snapshot loaded before a cleanup tick");
        fixture.reconnect();
        assertState(helper, fixture.player, false, false, "immediate death-screen reconnect");
        fixture.respawn();
        assertState(helper, fixture.player, false, false, "respawn after death-screen reconnect");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void endReturnRebuildsPlayerAndRestoresFlight(GameTestHelper helper) {
        Fixture fixture = new Fixture(helper);
        ReimuTestFlight.grant(fixture.player);
        fixture.player.getAbilities().flying = true;
        ServerLevel end = fixture.level.getServer().getLevel(Level.END);
        fixture.player.changeDimension(new DimensionTransition(end, new Vec3(0, 100, 0), Vec3.ZERO,
                0, 0, DimensionTransition.DO_NOTHING));
        ServerPlayer original = fixture.player;
        helper.startSequence().thenIdle(5).thenWaitUntil(() ->
                helper.assertTrue(end.hasChunkAt(original.blockPosition()), "End destination loaded"))
                .thenExecute(() -> fixture.player.showEndCredits()).thenIdle(3).thenExecute(() -> {
            helper.assertTrue(original.getHealth() > 0 && ReimuTestFlight.hasOwnSource(original),
                    "Removal while watching End credits is not death");
            fixture.drainPackets();
            fixture.respawn();
            helper.assertTrue(original != fixture.player, "End return must rebuild the server player");
            assertState(helper, fixture.player, true, true, "End non-death clone");
        }).thenWaitUntil(() -> {
            List<Object> packets = fixture.receivedPackets();
            int respawn = -1;
            for (int i = 0; i < packets.size(); i++) {
                if (packets.get(i) instanceof ClientboundRespawnPacket) respawn = i;
            }
            helper.assertTrue(respawn >= 0, "End return sends Respawn");
            assertRestorePackets(helper, packets.subList(respawn + 1, packets.size()), fixture.player, true);
            assertRestoreBundle(helper, fixture.bundles, fixture.player, true);
        }).thenSucceed();
    }

    @GameTest(template = "empty")
    public static void endReturnPreservesStoppedAndSpectatorMemory(GameTestHelper helper) {
        for (boolean spectator : new boolean[]{false, true}) {
            Fixture fixture = new Fixture(helper);
            ReimuTestFlight.grant(fixture.player);
            if (spectator) fixture.player.setGameMode(GameType.SPECTATOR);
            fixture.player.showEndCredits();
            fixture.respawn();
            assertState(helper, fixture.player, true, spectator, "End return stopped/spectator state");
            if (spectator) fixture.player.setGameMode(GameType.SURVIVAL);
            assertState(helper, fixture.player, true, false, "End clone must preserve stopped spectator memory");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void forcedLoginModesPreserveSpectatorMemory(GameTestHelper helper) {
        for (boolean flying : new boolean[]{false, true}) {
            Fixture fixture = new Fixture(helper);
            ReimuTestFlight.grant(fixture.player);
            fixture.player.getAbilities().flying = flying;
            fixture.forcedLoginMode = GameType.SPECTATOR;
            fixture.reconnect();
            helper.assertTrue(fixture.player.isSpectator(), "Login mode override must take effect");
            fixture.player.setGameMode(GameType.SURVIVAL);
            assertState(helper, fixture.player, true, flying, "forced spectator exit");
            fixture.player.setGameMode(GameType.SPECTATOR);
            fixture.forcedLoginMode = GameType.SURVIVAL;
            fixture.reconnect();
            assertState(helper, fixture.player, true, flying, "forced survival restores pre-spectator state");
        }
        helper.succeed();
    }

    private static void assertRestoreBundle(GameTestHelper helper, List<ClientboundBundlePacket> bundles,
                                           ServerPlayer player, boolean flying) {
        for (ClientboundBundlePacket bundle : bundles) {
            List<Object> parts = new ArrayList<>();
            bundle.subPackets().forEach(parts::add);
            if (parts.size() == 2 && parts.getFirst() instanceof ClientboundUpdateAttributesPacket attributes
                    && attributes.getEntityId() == player.getId()
                    && attributes.getValues().stream().anyMatch(snapshot -> snapshot.attribute().equals(NeoForgeMod.CREATIVE_FLIGHT)
                    && snapshot.modifiers().stream().anyMatch(modifier -> modifier.id().equals(OWN_SOURCE)))
                    && parts.getLast() instanceof ClientboundPlayerAbilitiesPacket abilities && abilities.isFlying() == flying) {
                return;
            }
        }
        helper.fail("Expected a single atomic attribute/abilities bundle for the restored player");
    }

    private static void assertState(GameTestHelper helper, ServerPlayer player, boolean enabled,
                                    boolean flying, String context) {
        helper.assertTrue(ReimuTestFlight.hasOwnSource(player) == enabled, context + ": source mismatch");
        boolean saved = player.hasData(FlightAttachments.REIMU_TEST_FLIGHT)
                && player.getData(FlightAttachments.REIMU_TEST_FLIGHT).enabled();
        helper.assertTrue(saved == enabled, context + ": saved permission mismatch");
        helper.assertTrue(player.getAbilities().flying == flying, context + ": flying mismatch");
    }

    private static void assertRestorePackets(GameTestHelper helper, List<Object> packets,
                                             ServerPlayer player, boolean flying) {
        assertAttributePacket(helper, packets, player, true, "restored owner");
        for (int i = 0; i + 1 < packets.size(); i++) {
            if (packets.get(i) instanceof ClientboundUpdateAttributesPacket attributes
                    && attributes.getEntityId() == player.getId()
                    && attributes.getValues().stream().anyMatch(snapshot -> snapshot.attribute().equals(NeoForgeMod.CREATIVE_FLIGHT)
                    && snapshot.modifiers().stream().anyMatch(modifier -> modifier.id().equals(OWN_SOURCE)))
                    && packets.get(i + 1) instanceof ClientboundPlayerAbilitiesPacket abilities
                    && abilities.isFlying() == flying) {
                return;
            }
        }
        helper.fail("Expected an ordered own-attribute / restored-abilities pair");
    }

    private static AttributeModifier otherSource() {
        return new AttributeModifier(OTHER_SOURCE, 1, AttributeModifier.Operation.ADD_VALUE);
    }

    private static void assertAttributePacket(GameTestHelper helper, List<Object> packets,
                                              ServerPlayer player, boolean containsOwnSource, String recipient) {
        boolean found = packets.stream().anyMatch(packet ->
                packet instanceof ClientboundUpdateAttributesPacket attributes
                        && attributes.getEntityId() == player.getId()
                        && attributes.getValues().stream().anyMatch(snapshot ->
                        snapshot.attribute().equals(NeoForgeMod.CREATIVE_FLIGHT)
                                && snapshot.modifiers().stream().anyMatch(modifier -> modifier.id().equals(OWN_SOURCE))
                                == containsOwnSource));
        helper.assertTrue(found, recipient + ": expected flight attribute packet for entity " + player.getId()
                + " with source present="
                + containsOwnSource + "; received "
                + packets.stream().map(packet -> packet.getClass().getSimpleName()).distinct().toList());
    }

    // Real server players and lifecycle entry points, with in-memory connections instead of clients.
    private static final class Fixture implements GameTestListener {
        final ServerLevel level;
        final Vec3 position;
        final GameProfile profile;
        final List<Object> listeners = new ArrayList<>();
        final List<Object> received = new ArrayList<>();
        final List<ClientboundBundlePacket> bundles = new ArrayList<>();
        GameType forcedLoginMode;
        ServerPlayer player;
        Connection connection;
        EmbeddedChannel channel;
        boolean closed;

        Fixture(GameTestHelper helper) {
            level = helper.getLevel();
            position = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 1, 1)));
            UUID id = UUID.randomUUID();
            profile = new GameProfile(id, "flight-" + id.toString().substring(0, 8));
            helper.testInfo.addListener(this);
            helper.setBlock(new BlockPos(1, 0, 1), Blocks.STONE);
            connect();
            player.setGameMode(GameType.SURVIVAL);
            player.teleportTo(level, position.x, position.y, position.z, 0, 0);
            helper.onEachTick(this::receivePackets);
        }

        void connect() {
            CommonListenerCookie cookie = CommonListenerCookie.createInitial(profile, false);
            player = new ServerPlayer(level.getServer(), level, profile, cookie.clientInformation()) {
                @Override
                public void loadGameTypes(CompoundTag saved) {
                    // Emulate the final mode override at the real login stage, without changing the shared server.
                    if (forcedLoginMode != null) {
                        CompoundTag overridden = saved == null ? new CompoundTag() : saved.copy();
                        overridden.putInt("playerGameType", forcedLoginMode.getId());
                        super.loadGameTypes(overridden);
                    } else {
                        super.loadGameTypes(saved);
                    }
                }
            };
            connection = new Connection(PacketFlow.SERVERBOUND);
            channel = new EmbeddedChannel(connection);
            // Start a new profile at its test structure; saved positions still override this on reconnect.
            player.moveTo(position.x, position.y, position.z, 0, 0);
            level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
            // Normal connections are ticked by the server; an embedded channel is not auto-registered.
            level.getServer().getConnection().getConnections().add(connection);
        }

        void reconnect() {
            level.getServer().getPlayerList().remove(player);
            closeConnection();
            received.clear();
            bundles.clear();
            connect();
        }

        ServerPlayer loadDetachedPlayer() {
            var cookie = CommonListenerCookie.createInitial(profile, false);
            ServerPlayer loaded = new ServerPlayer(level.getServer(), level, profile, cookie.clientInformation());
            var saved = level.getServer().getPlayerList().load(loaded);
            loaded.loadGameTypes(saved.orElseThrow());
            return loaded;
        }

        void respawn() {
            player.connection.handleClientCommand(new ServerboundClientCommandPacket(
                    ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
            player = level.getServer().getPlayerList().getPlayer(profile.getId());
        }

        List<Object> drainPackets() {
            receivePackets();
            List<Object> packets = List.copyOf(received);
            received.clear();
            bundles.clear();
            return packets;
        }

        List<Object> receivedPackets() {
            receivePackets();
            return List.copyOf(received);
        }

        private void receivePackets() {
            if (closed) {
                return;
            }
            channel.runPendingTasks();
            Object packet;
            while ((packet = channel.readOutbound()) != null) {
                if (packet instanceof ClientboundBundlePacket bundle) {
                    bundles.add(bundle);
                    bundle.subPackets().forEach(received::add);
                } else {
                    received.add(packet);
                }
                // A minimal client stub acknowledges transport progress without applying game state.
                if (packet instanceof ClientboundPlayerPositionPacket positionPacket) {
                    player.connection.handleAcceptTeleportPacket(new ServerboundAcceptTeleportationPacket(positionPacket.getId()));
                } else if (packet instanceof ClientboundChunkBatchFinishedPacket) {
                    player.connection.handleChunkBatchReceived(new ServerboundChunkBatchReceivedPacket(64));
                }
            }
            if (player.isAlive()) {
                // Even a stationary real client reports movement state, refreshing entity/chunk tracking.
                player.connection.handleMovePlayer(new ServerboundMovePlayerPacket.StatusOnly(player.onGround()));
            }
        }

        private void close() {
            if (closed) {
                return;
            }
            closed = true;
            try {
                listeners.forEach(NeoForge.EVENT_BUS::unregister);
                ServerPlayer current = level.getServer().getPlayerList().getPlayer(profile.getId());
                if (current != null) {
                    level.getServer().getPlayerList().remove(current);
                }
            } finally {
                closeConnection();
            }
        }

        private void closeConnection() {
            if (connection != null) {
                level.getServer().getConnection().getConnections().remove(connection);
            }
            if (channel != null) {
                channel.finishAndReleaseAll();
            }
        }

        @Override
        public void testStructureLoaded(GameTestInfo info) {
        }

        @Override
        public void testPassed(GameTestInfo info, GameTestRunner runner) {
            close();
        }

        @Override
        public void testFailed(GameTestInfo info, GameTestRunner runner) {
            close();
        }

        @Override
        public void testAddedForRerun(GameTestInfo oldInfo, GameTestInfo newInfo, GameTestRunner runner) {
            close();
        }
    }
}
