package io.github.quantumuniversessteel.gensou_in_operation.gametest;

import com.mojang.authlib.GameProfile;
import io.github.quantumuniversessteel.gensou_in_operation.GensouInOperation;
import io.github.quantumuniversessteel.gensou_in_operation.flight.ReimuTestFlight;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestListener;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundChunkBatchFinishedPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundChunkBatchReceivedPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.entity.Entity;
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
    public static void modeChangesClearSourceAndPreserveVanillaFlight(GameTestHelper helper) {
        Fixture fixture = new Fixture(helper);
        ReimuTestFlight.grant(fixture.player);
        fixture.player.setGameMode(GameType.ADVENTURE);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(!ReimuTestFlight.hasOwnSource(fixture.player) && !fixture.player.mayFly(),
                    "Adventure transition must clear the test source");
            ReimuTestFlight.grant(fixture.player);
            fixture.player.setGameMode(GameType.CREATIVE);
            fixture.player.getAbilities().flying = true;
            helper.runAfterDelay(2, () -> {
                helper.assertTrue(!ReimuTestFlight.hasOwnSource(fixture.player)
                                && fixture.player.mayFly() && fixture.player.getAbilities().flying,
                        "Cleanup must preserve creative flight");
                ReimuTestFlight.grant(fixture.player);
                fixture.player.setGameMode(GameType.SPECTATOR);
                helper.runAfterDelay(2, () -> {
                    helper.assertTrue(!ReimuTestFlight.hasOwnSource(fixture.player)
                                    && fixture.player.mayFly() && fixture.player.getAbilities().flying,
                            "Cleanup must preserve spectator flight");
                    fixture.player.setGameMode(GameType.SURVIVAL);
                    helper.runAfterDelay(2, () -> {
                        helper.assertTrue(!ReimuTestFlight.hasOwnSource(fixture.player) && !fixture.player.mayFly(),
                                "Returning to survival must not restore the test permission");
                        helper.succeed();
                    });
                });
            });
        });
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
    public static void sameTickModeRoundTripClearsSource(GameTestHelper helper) {
        Fixture fixture = new Fixture(helper);
        ReimuTestFlight.grant(fixture.player);
        fixture.player.setGameMode(GameType.CREATIVE);
        fixture.player.setGameMode(GameType.SURVIVAL);
        helper.assertTrue(!ReimuTestFlight.hasOwnSource(fixture.player) && !fixture.player.mayFly(),
                "A completed mode change must not be hidden by a same-tick return");
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
    public static void logoutAndReconnectDoNotRestoreSource(GameTestHelper helper) {
        Fixture fixture = new Fixture(helper);
        fixture.player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT).addPermanentModifier(otherSource());
        ReimuTestFlight.grant(fixture.player);
        fixture.player.getAbilities().flying = true;
        String activeSave = fixture.player.saveWithoutId(new CompoundTag()).toString();
        helper.assertTrue(!activeSave.contains(OWN_SOURCE.toString()) && activeSave.contains(OTHER_SOURCE.toString()),
                "An active save must omit our transient source and keep the external permanent source");
        ServerPlayer previous = fixture.player;
        fixture.level.getServer().getPlayerList().remove(previous);
        helper.assertTrue(!ReimuTestFlight.hasOwnSource(previous), "Logout must clear the old player before saving");
        fixture.closeConnection();
        fixture.connect();
        helper.assertTrue(!ReimuTestFlight.hasOwnSource(fixture.player), "Reconnect must not restore our source");
        helper.assertTrue(fixture.player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT).hasModifier(OTHER_SOURCE)
                        && fixture.player.mayFly(),
                "Another source must survive the real player save/load path");
        helper.succeed();
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
            fixture.player = fixture.level.getServer().getPlayerList()
                    .respawn(fixture.player, false, Entity.RemovalReason.KILLED);
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
    public static void dimensionRoundTripClearsOnlyOurSource(GameTestHelper helper) {
        Fixture fixture = new Fixture(helper);
        var attribute = fixture.player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
        attribute.addTransientModifier(otherSource());
        ReimuTestFlight.grant(fixture.player);
        ServerLevel nether = fixture.level.getServer().getLevel(Level.NETHER);
        helper.assertTrue(nether != null, "Nether must exist for dimension integration test");
        fixture.player.changeDimension(new DimensionTransition(nether, new Vec3(0, 100, 0), Vec3.ZERO,
                0, 0, DimensionTransition.DO_NOTHING));
        helper.assertTrue(fixture.player.serverLevel() == nether && !ReimuTestFlight.hasOwnSource(fixture.player),
                "Completed dimension travel must clear our source");
        helper.assertTrue(attribute.hasModifier(OTHER_SOURCE) && fixture.player.mayFly(),
                "Dimension cleanup must preserve an external source");
        ReimuTestFlight.grant(fixture.player);
        fixture.player.changeDimension(new DimensionTransition(fixture.level, fixture.position, Vec3.ZERO,
                0, 0, DimensionTransition.DO_NOTHING));
        helper.assertTrue(fixture.player.serverLevel() == fixture.level && !ReimuTestFlight.hasOwnSource(fixture.player),
                "Returning dimension travel must also clear our source");
        helper.succeed();
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
            helper.onEachTick(this::receivePackets);
        }

        void connect() {
            CommonListenerCookie cookie = CommonListenerCookie.createInitial(profile, false);
            player = new ServerPlayer(level.getServer(), level, profile, cookie.clientInformation());
            connection = new Connection(PacketFlow.SERVERBOUND);
            channel = new EmbeddedChannel(connection);
            level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
            // Normal connections are ticked by the server; an embedded channel is not auto-registered.
            level.getServer().getConnection().getConnections().add(connection);
            player.setGameMode(GameType.SURVIVAL);
            player.teleportTo(level, position.x, position.y, position.z, 0, 0);
        }

        List<Object> drainPackets() {
            receivePackets();
            List<Object> packets = List.copyOf(received);
            received.clear();
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
