/*
 * Trident - A Multithreaded Server Alternative
 * Copyright 2016 The TridentSDK Team
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

import com.google.common.collect.Maps;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.tridentsdk.base.BlockDirection;
import net.tridentsdk.base.Position;
import net.tridentsdk.chat.ChatColor;
import net.tridentsdk.chat.ChatComponent;
import net.tridentsdk.chat.ChatType;
import net.tridentsdk.entity.living.Player;
import net.tridentsdk.server.entity.TridentEntity;
import net.tridentsdk.server.net.EntityMetadata;
import net.tridentsdk.server.net.NetClient;
import net.tridentsdk.server.packet.play.*;
import net.tridentsdk.server.ui.tablist.TridentTabList;
import net.tridentsdk.server.ui.tablist.TridentTabListManager;
import net.tridentsdk.server.world.TridentChunk;
import net.tridentsdk.server.world.TridentWorld;
import net.tridentsdk.ui.tablist.TabList;
import net.tridentsdk.world.World;
import net.tridentsdk.world.WorldLoader;
import net.tridentsdk.world.opt.GameMode;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class is the implementation of a Minecraft client
 * that is represented by a physical entity in a world.
 */
@ThreadSafe
public class TridentPlayer extends TridentEntity implements Player {
    // TODO player abilities
    // TODO client settings
    // TODO chunks
    /**
     * The players on the server
     */
    public static final Map<UUID, TridentPlayer> PLAYERS = Maps.newConcurrentMap();
    /**
     * The cache time of a chunk
     */
    private static final int chunkCacheTime = 1000 * 10; // 10 Seconds

    /**
     * The net connection that this player has to the
     * server
     */
    private final NetClient client;
    /**
     * The player's name
     */
    private final String name;
    /**
     * The player's UUID
     */
    private final UUID uuid;
    /**
     * The player's current tablist
     */
    @Getter
    private volatile TabList tabList;
    /**
     * The player's skin value
     */
    @Getter
    private volatile String textures;
    /**
     * The player's current game mode
     */
    @Getter
    private volatile GameMode gameMode;
    /**
     * The player's render distance
     */
    @Getter
    private volatile int renderDistance;
    /**
     * The player's meta data
     */
    @Getter
    private final TridentPlayerMeta metadata = new TridentPlayerMeta(new EntityMetadata());

    /**
     * A map of chunk -> time, storing the last time
     * the chunk was sent to the client
     */
    private final Map<HashedChunkPosition, Long> chunkSentTime = new ConcurrentHashMap<>();

    /**
     * Constructs a new player.
     */
    private TridentPlayer(NetClient client, World world, String name, UUID uuid, String textures) {
        super(world);
        this.client = client;
        this.name = name;
        this.uuid = uuid;
        this.gameMode = world.opts().gameMode();
        this.textures = textures;
        this.renderDistance = 7; // TODO Infer from settings packet
    }

    /**
     * Spawns a new player.
     *
     * @param client the client representing the player
     * @param name the player name
     * @param uuid the player UUID
     * @param textures the player textures
     */
    public static TridentPlayer spawn(NetClient client, String name, UUID uuid, String textures) {
        TridentWorld world = (TridentWorld) WorldLoader.instance().getDefault();
        TridentPlayer player = new TridentPlayer(client, world, name, uuid, textures);
        PLAYERS.put(uuid, player);
        client.setPlayer(player);

        Position playerPosition = player.position();
        playerPosition.setY(4);

        client.sendPacket(new PlayOutJoinGame(player, world));
        client.sendPacket(PlayOutPluginMsg.BRAND);
        client.sendPacket(new PlayOutDifficulty(world));
        client.sendPacket(new PlayOutSpawnPos());
        client.sendPacket(new PlayOutPosLook(player));
        client.sendPacket(new PlayOutAbilities(false, false, player.getGameMode()));

        player.resumeLogin(); // TODO Remove after fixed Teleport Confirmation

        return player;
    }

    /**
     * Resumes the joining process after the player has
     * confirmed the client spawn position.
     */
    public void resumeLogin() {
        TridentTabListManager tabList = TridentTabListManager.getInstance();
        this.setTabList(tabList.getGlobalTabList());
        tabList.getGlobalTabList().addPlayer(this);

        PlayOutSpawnPlayer newPlayerPacket = new PlayOutSpawnPlayer(this);
        ChatComponent chat = ChatComponent.create()
                .setColor(ChatColor.YELLOW)
                .setTranslate("multiplayer.player.joined")
                .addWith(this.name);
        this.sendMessage(chat, ChatType.CHAT);

        TridentPlayer.PLAYERS.values()
                .stream()
                .filter(p -> !p.equals(this))
                .forEach(p -> {
                    p.sendMessage(chat, ChatType.CHAT);

                    p.net().sendPacket(newPlayerPacket);

                    PlayOutSpawnPlayer oldPlayerPacket = new PlayOutSpawnPlayer(p);
                    this.client.sendPacket(oldPlayerPacket);
                });

        Position pos = this.position();
        int initialChunkRadius = 3;
        for (int x = pos.getChunkX() - initialChunkRadius; x <= pos.getChunkX() + initialChunkRadius; x++) {
            for (int z = pos.getChunkZ() - initialChunkRadius; z <= pos.getChunkZ() + initialChunkRadius; z++) {
                TridentChunk chunk = this.world().chunkAt(x, z);
                this.client.sendPacket(new PlayOutChunk(chunk));
            }
        }
    }

    /**
     * Obtains the network connection of this player.
     *
     * @return the net connection
     */
    public NetClient net() {
        return this.client;
    }

    @Override
    public void doTick() {
        this.client.tick();
    }

    @Override
    public void doRemove() {
        PLAYERS.remove(this.uuid);
        TridentTabListManager.getInstance().getGlobalTabList().removePlayer(this);

        ChatComponent chat = ChatComponent.create()
                .setColor(ChatColor.YELLOW)
                .setTranslate("multiplayer.player.left")
                .addWith(this.name);
        PLAYERS.values().forEach(e -> e.sendMessage(chat, ChatType.CHAT));
    }

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public UUID uuid() {
        return this.uuid;
    }

    @Override
    public void sendMessage(ChatComponent chat, ChatType type) {
        this.net().sendPacket(new PlayOutChat(chat, type));
    }

    @Override
    public void kick(ChatComponent reason) {
        this.client.disconnect(reason);
    }

    @Override
    public void setGameMode(GameMode gameMode) {
        this.gameMode = gameMode;
        this.client.sendPacket(new PlayOutAbilities(false, false, gameMode));
    }

    @Override
    public void setTabList(TabList tabList) {
        TridentTabListManager.getInstance().setTabList(this, this.tabList, tabList);
        this.tabList = tabList;
        ((TridentTabList) tabList).sendToPlayer(this);
    }

    /**
     * Sets the texture of this player to a different skin
     * data.
     *
     * @param textures the skin data
     */
    public void setTextures(String textures) {
        this.textures = textures;
        // TODO Push update to tablist and other players
    }

    /**
     * Send an update to the client with the chunks
     * If direction is null, chunks around the player will be sent
     *
     * @param direction the direction the player moved or null
     */
    public void updateChunks(BlockDirection direction) {
        // TODO Improve this algorithm
        // For example, send chunks closer to the player first

        int centerX = position().getChunkX();
        int centerZ = position().getChunkZ();

        int radius = renderDistance / 2;

        if(direction != null) {
            centerX += (direction.getXDiff() * radius);
            centerZ += (direction.getZDiff() * radius);
        }

        chunkSentTime.keySet().iterator().forEachRemaining(chunk -> {
            /* Should be 16, but renderDistance has to be divided by 2 */
            if(chunk.distanceTo(position()) > renderDistance * 8 /* == (renderDistance / 2) * 16 */){
                chunkSentTime.remove(chunk);
            }
        });

        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                HashedChunkPosition position = new HashedChunkPosition(x, z);
                if(System.currentTimeMillis() - chunkSentTime.getOrDefault(position, 0L) > chunkCacheTime){
                    TridentChunk chunk = this.world().chunkAt(x, z);
                    this.client.sendPacket(new PlayOutChunk(chunk));
                    chunkSentTime.put(position, System.currentTimeMillis());
                }
            }
        }
    }

    @Data
    @RequiredArgsConstructor
    private class HashedChunkPosition {

        private final int x;
        private final int z;

        public double distanceTo(Position position) {
            return Math.abs((this.x - position.getChunkX()) + (this.z - position.getChunkZ()));
        }

    }

}
