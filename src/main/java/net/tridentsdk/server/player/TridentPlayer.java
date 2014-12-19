/*
 * Trident - A Multithreaded Server Alternative
 * Copyright 2014 The TridentSDK Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.tridentsdk.server.player;

import net.tridentsdk.base.Substance;
import net.tridentsdk.entity.Entity;
import net.tridentsdk.entity.living.Player;
import net.tridentsdk.factory.Factories;
import net.tridentsdk.meta.nbt.CompoundTag;
import net.tridentsdk.server.TridentServer;
import net.tridentsdk.server.data.Slot;
import net.tridentsdk.server.entity.EntityBuilder;
import net.tridentsdk.server.entity.ParameterValue;
import net.tridentsdk.server.netty.ClientConnection;
import net.tridentsdk.server.netty.packet.Packet;
import net.tridentsdk.server.packets.play.out.*;
import net.tridentsdk.server.threads.ThreadsManager;
import net.tridentsdk.server.world.TridentChunk;
import net.tridentsdk.server.world.TridentWorld;
import net.tridentsdk.util.TridentLogger;
import net.tridentsdk.window.inventory.Item;
import net.tridentsdk.world.LevelType;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Collection;
import java.util.Locale;
import java.util.UUID;

@ThreadSafe
public class TridentPlayer extends OfflinePlayer {
    private final PlayerConnection connection;
    private volatile Locale locale;

    public TridentPlayer(CompoundTag tag, TridentWorld world, ClientConnection connection) {
        super(tag, world);

        this.connection = PlayerConnection.createPlayerConnection(connection, this);
    }

    public static void sendAll(Packet packet) {
        for (Player p : getPlayers()) {
            ((TridentPlayer) p).connection.sendPacket(packet);
        }
    }

    public static Player spawnPlayer(ClientConnection connection, UUID id) {
        CompoundTag offlinePlayer = (OfflinePlayer.getOfflinePlayer(id) == null) ? null :
                OfflinePlayer.getOfflinePlayer(id).toNbt();

        if (offlinePlayer == null) {
            offlinePlayer = OfflinePlayer.generatePlayer(id);
        }

        final TridentPlayer p = EntityBuilder.create()
                .uuid(id)
                .spawnLocation(TridentServer.WORLD.getSpawn())
                .executor(ThreadsManager.playerExecutor())
                .build(TridentPlayer.class, ParameterValue.from(CompoundTag.class, offlinePlayer),
                        ParameterValue.from(TridentWorld.class, TridentServer.WORLD),
                        ParameterValue.from(ClientConnection.class, connection));

        p.executor.addTask(new Runnable() {
            @Override
            public void run() {
                p.connection.sendPacket(new PacketPlayOutJoinGame()
                        .set("entityId", p.getId())
                        .set("gamemode", p.getGameMode())
                        .set("dimension", ((TridentWorld) p.getWorld()).getDimesion())
                        .set("difficulty", p.getWorld().getDifficulty())
                        .set("maxPlayers", (short) 10)
                        .set("levelType", LevelType.DEFAULT));

                p.connection.sendPacket(new PacketPlayOutSpawnPosition().set("location", p.getSpawnLocation()));
                p.connection.sendPacket(p.abilities.toPacket());
                p.connection.sendPacket(new PacketPlayOutPlayerCompleteMove().set("location", p.getSpawnLocation())
                        .set("flags", (byte) 0));

                // Wait for response
                Slot[] slots = new Slot[44];
                slots[43] = new Slot(new Item(Substance.APPLE));
                // p.connection.sendPacket(new PacketPlayOutWindowItems().set("windowId", 0).set("slots", slots));
                p.sendChunks(3);
                for (Entity entity : p.getWorld().getEntities()) {
                    // Register mob, packet sent to new player
                }
            }
        });

        return p;
    }

    public static Player getPlayer(UUID id) {
        for (Player player : getPlayers()) {
            if (player.getUniqueId().equals(id)) {
                return player;
            }
        }

        return null;
    }

    public static Collection<Player> getPlayers() {
        return Factories.threads().players();
    }

    @Override
    public void tick() {
        this.executor.addTask(new Runnable() {
            @Override
            public void run() {
                TridentPlayer.super.tick();
                long keepAlive = ticksExisted.get();
                boolean keepAliveSent = connection.hasSentKeepAlive();

                if (keepAlive >= 300L && !keepAliveSent) {
                    // send Keep Alive packet if not sent already
                    connection.sendPacket(new PacketPlayOutKeepAlive());
                } else if (keepAlive >= 600L) {
                    // kick the player for not responding to the keep alive within 30 seconds/600 ticks
                    kickPlayer("Timed out!");
                }

                ticksExisted.incrementAndGet();
                connection.markSentKeepAlive(true);
            }
        });
    }

    /*
     * @NotJavaDoc
     * TODO: Create Message API and utilize it
     */
    public void kickPlayer(final String reason) {
        this.executor.addTask(new Runnable() {
            @Override
            public void run() {
                TridentPlayer.this.connection.sendPacket(new PacketPlayOutDisconnect().set("reason", reason));
            }
        });
    }

    public PlayerConnection getConnection() {
        return this.connection;
    }

    public void setSlot(final short slot) {
        this.executor.addTask(new Runnable() {
            @Override
            public void run() {
                if ((int) slot > 8 || (int) slot < 0) {
                    TridentLogger.error(new IllegalArgumentException("Slot must be within the ranges of 0-8"));
                }

                TridentPlayer.super.selectedSlot = slot;
            }
        });
    }

    @Override
    public void sendRaw(final String... messages) {
        // TODO: Verify proper implementation
        this.executor.addTask(new Runnable() {
            @Override
            public void run() {
                for (String message : messages) {
                    if (message != null) {
                        TridentPlayer.this.connection.sendPacket(new PacketPlayOutChatMessage().set("jsonMessage",
                                message)
                                .set("position", PacketPlayOutChatMessage.ChatPosition.CHAT));
                    }
                }
            }
        });
    }

    public void sendChunks(int viewDistance) {
        int centX = ((int) Math.floor(loc.getX())) >> 4;
        int centZ = ((int) Math.floor(loc.getZ())) >> 4;

        for (int x = (centX - viewDistance); x <= (centX + viewDistance); x += 1) {
            for (int z = (centZ - viewDistance); z <= (centZ + viewDistance); z += 1) {
                connection.sendPacket(((TridentChunk) getWorld().getChunkAt(x, z, true)).toPacket());
            }
        }
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }
}
