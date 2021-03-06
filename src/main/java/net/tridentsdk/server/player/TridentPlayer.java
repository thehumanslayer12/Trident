/*
 * Trident - A Multithreaded Server Alternative
 * Copyright 2017 The TridentSDK Team
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

import lombok.Getter;
import lombok.Setter;
import net.tridentsdk.base.BlockDirection;
import net.tridentsdk.base.Position;
import net.tridentsdk.chat.ChatColor;
import net.tridentsdk.chat.ChatComponent;
import net.tridentsdk.chat.ChatType;
import net.tridentsdk.chat.ClientChatMode;
import net.tridentsdk.entity.living.Player;
import net.tridentsdk.event.player.PlayerJoinEvent;
import net.tridentsdk.server.TridentServer;
import net.tridentsdk.server.concurrent.PoolSpec;
import net.tridentsdk.server.entity.TridentEntity;
import net.tridentsdk.server.entity.meta.EntityMetaType;
import net.tridentsdk.server.net.NetClient;
import net.tridentsdk.server.packet.login.Login;
import net.tridentsdk.server.packet.play.*;
import net.tridentsdk.server.ui.bossbar.AbstractBossBar;
import net.tridentsdk.server.ui.tablist.TabListElement;
import net.tridentsdk.server.ui.tablist.TridentGlobalTabList;
import net.tridentsdk.server.ui.tablist.TridentTabList;
import net.tridentsdk.server.world.TridentWorld;
import net.tridentsdk.ui.bossbar.BossBar;
import net.tridentsdk.ui.tablist.TabList;
import net.tridentsdk.ui.title.Title;
import net.tridentsdk.world.IntPair;
import net.tridentsdk.world.World;
import net.tridentsdk.world.opt.GameMode;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This class is the implementation of a Minecraft client
 * that is represented by a physical entity in a world.
 */
@ThreadSafe
@EntityMetaType(TridentPlayerMeta.class)
public class TridentPlayer extends TridentEntity implements Player {
    /**
     * The players on the server
     */
    @Getter
    private static final Map<UUID, TridentPlayer> players = new ConcurrentHashMap<>();
    /**
     * The cache time of a chunk
     */
    private static final int CHUNK_CACHE_MILLIS = 1000 * 30; // 30 Seconds

    /**
     * A map of chunk -> time, storing the last time
     * the chunk was sent to the client
     */
    private final Map<IntPair, Long> chunkSentTime = new ConcurrentHashMap<>();

    /**
     * The net connection that this player has to the
     * server
     */
    private final NetClient client;
    /**
     * The player's name
     */
    @Getter
    private final String name;
    /**
     * The player's UUID
     */
    @Getter
    private final UUID uuid;
    /**
     * The player's display name
     */
    @Getter
    private volatile ChatComponent displayName;
    /**
     * The player's current game mode
     */
    @Getter
    private volatile GameMode gameMode;
    /**
     * The player's skin textures.
     */
    @Getter
    private volatile TabListElement.PlayerProperty skinTextures;
    /**
     * The player's render distance
     */
    @Getter
    @Setter
    private volatile int renderDistance = 7;

    @Getter
    @Setter
    private volatile String locale;
    @Setter
    private volatile boolean chatColors;
    @Setter
    private volatile ClientChatMode chatMode;

    /**
     * Whether the player has finished logging in
     */
    private final AtomicBoolean finishedLogin = new AtomicBoolean(false);

    /**
     * The player's meta data
     */
    @Getter
    private final TridentPlayerMeta metadata;

    /**
     * The player's current tablist
     */
    @Getter
    private volatile TabList tabList;
    /**
     * The boss bars that are being displayed to this
     * player.
     */
    private final List<BossBar> bossBars = new CopyOnWriteArrayList<>();

    /**
     * Whether the player is in god mode
     */
    @Getter
    private volatile boolean godMode;

    /**
     * Whether the player can fly
     */
    private volatile boolean canFly;

    /**
     * Whether the player is flying
     */
    @Getter
    private volatile boolean flying;

    /**
     * The player's flying speed
     */
    @Getter
    private volatile float flyingSpeed = Player.DEFAULT_FLYING_SPEED;

    /**
     * The player's walking speed
     */
    @Getter
    private volatile float walkingSpeed = Player.DEFAULT_WALKING_SPEED;

    /**
     * Constructs a new player.
     */
    private TridentPlayer(NetClient client, World world, String name, UUID uuid, TabListElement.PlayerProperty skinTextures) {
        super(world, PoolSpec.PLAYERS);
        this.metadata = (TridentPlayerMeta) super.getMetadata();

        this.client = client;
        this.name = name;
        this.uuid = uuid;
        this.displayName = ChatComponent.text(name);
        this.gameMode = world.getWorldOptions().getGameMode();
        this.canFly = this.gameMode == GameMode.CREATIVE || this.gameMode == GameMode.SPECTATOR;
        this.skinTextures = skinTextures;
    }

    /**
     * Spawns a new player.
     *
     * @param client the client representing the player
     * @param name the player name
     * @param uuid the player UUID
     * @param skinTextures the player textures
     */
    public static TridentPlayer spawn(NetClient client, String name, UUID uuid, TabListElement.PlayerProperty skinTextures) {
        TridentWorld world = TridentServer.getInstance().getWorldLoader().getDefaultWorld();
        TridentPlayer player = new TridentPlayer(client, world, name, uuid, skinTextures);
        TridentPlayer.players.put(uuid, player);
        client.setPlayer(player);
        Login.finish();

        TridentServer.getInstance().getEventController().dispatch(new PlayerJoinEvent(player));

        Position playerPosition = player.getPosition();
        playerPosition.setY(4);

        client.sendPacket(new PlayOutJoinGame(player, world));
        client.sendPacket(PlayOutPluginMsg.BRAND);
        client.sendPacket(new PlayOutDifficulty(world));
        client.sendPacket(new PlayOutSpawnPos());
        client.sendPacket(new PlayOutPosLook(player));
        client.sendPacket(new PlayOutPlayerAbilities(player));

        return player;
    }

    /**
     * Resumes the joining process after the player has
     * confirmed the client spawn position.
     */
    public void resumeLogin() {
        if (!this.finishedLogin.compareAndSet(false, true)) {
            return;
        }

        this.setTabList(TridentGlobalTabList.getInstance());
        TridentGlobalTabList.getInstance().update();

        PlayOutSpawnPlayer newPlayerPacket = new PlayOutSpawnPlayer(this);
        ChatComponent chat = ChatComponent.create()
                .setColor(ChatColor.YELLOW)
                .setTranslate("multiplayer.player.joined")
                .addWith(this.name);
        this.sendMessage(chat, ChatType.CHAT);

        TridentPlayer.players.values()
                .stream()
                .filter(p -> !p.equals(this))
                .forEach(p -> {
                    p.sendMessage(chat, ChatType.CHAT);

                    p.net().sendPacket(newPlayerPacket);

                    PlayOutSpawnPlayer oldPlayerPacket = new PlayOutSpawnPlayer(p);
                    this.client.sendPacket(oldPlayerPacket);
                });

        Position pos = this.getPosition();
        int initialChunkRadius = 3;
        for (int x = pos.getChunkX() - initialChunkRadius; x <= pos.getChunkX() + initialChunkRadius; x++) {
            for (int z = pos.getChunkZ() - initialChunkRadius; z <= pos.getChunkZ() + initialChunkRadius; z++) {
                int finalX = x;
                int finalZ = z;
                CompletableFuture
                        .supplyAsync(() -> this.getWorld().getChunkAt(finalX, finalZ), this.pool)
                        .thenAcceptAsync(chunk -> this.client.sendPacket(new PlayOutChunk(chunk)), this.pool);
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
        // If the player isn't in the list, they haven't
        // finished logging in yet; cleanup
        if (TridentPlayer.players.remove(this.uuid) == null) {
            Login.finish();
        }

        this.setTabList(null);
        TridentGlobalTabList.getInstance().update();

        ChatComponent chat = ChatComponent.create()
                .setColor(ChatColor.YELLOW)
                .setTranslate("multiplayer.player.left")
                .addWith(this.name);
        TridentPlayer.players.values().forEach(e -> e.sendMessage(chat, ChatType.CHAT));
    }

    @Override
    public void setDisplayName(ChatComponent displayName) {
        this.displayName = displayName != null ? displayName : ChatComponent.text(this.name);
        // TODO update
    }

    @Override
    public void sendMessage(ChatComponent chat, ChatType type) {
        ClientChatMode chatMode = this.chatMode;
        if (ClientChatMode.COMMANDS_ONLY.equals(chatMode) && ChatType.SYSTEM.equals(type)
                || ClientChatMode.CHAT_AND_COMMANDS.equals(chatMode)) {
            this.net().sendPacket(new PlayOutChat(chat, type, this.chatColors));
        }
    }

    @Override
    public void kick(ChatComponent reason) {
        this.client.disconnect(reason);
    }

    @Override
    public void setTabList(TabList tabList) {
        TabList old = this.tabList;
        if (old != null) {
            old.unsubscribe(this);
        }

        if (tabList != null) {
            this.tabList = tabList;
            tabList.subscribe(this);
            ((TridentTabList) tabList).forceSend(this);
        }
    }

    @Override
    public List<BossBar> getBossBars() {
        return Collections.unmodifiableList(this.bossBars);
    }

    @Override
    public void addBossBar(BossBar bossBar) {
        if(bossBar == null){
            throw new NullPointerException();
        }
        
        if (this.bossBars.add(bossBar)) {
            this.net().sendPacket(new PlayOutBossBar.Add(bossBar));
        }
    }

    @Override
    public void removeBossBar(BossBar bossBar) {
        if(bossBar == null){
            throw new NullPointerException();
        }
        
        if (this.bossBars.remove(bossBar)) {
            this.net().sendPacket(new PlayOutBossBar.Remove(bossBar));
        }
    }

    @Override
    public void updateBossBars() {
        this.updateBossBars(false);
    }

    private void updateBossBars(boolean force) {
        for (BossBar bar : this.bossBars) {
            AbstractBossBar bossBar = (AbstractBossBar) bar;
            if (force) {
                this.net().sendPacket(new PlayOutBossBar.Add(bossBar));
                continue;
            }

            int changed = AbstractBossBar.STATE.get(bossBar);
            do {
                boolean health, title, style, flags, sky;
                sky = (changed >>> 4 & 1) == 1;
                title = (changed >>> 3 & 1) == 1;
                health = (changed >>> 2 & 1) == 1;
                style = (changed >>> 1 & 1) == 1;
                flags = (changed & 1) == 1;

                if (sky) {
                    this.net().sendPacket(new PlayOutBossBar.Add(bossBar));
                } else {
                    if (health) {
                        this.net().sendPacket(new PlayOutBossBar.UpdateHealth(bossBar));
                    }
                    if (title) {
                        this.net().sendPacket(new PlayOutBossBar.UpdateTitle(bossBar));
                    }
                    if (style) {
                        this.net().sendPacket(new PlayOutBossBar.UpdateStyle(bossBar));
                    }
                    if (flags) {
                        this.net().sendPacket(new PlayOutBossBar.UpdateFlags(bossBar));
                    }
                }
            } while (!bossBar.unsetChanged(changed));
        }
    }

    @Override
    public void sendTitle(Title title) {
        if (!title.isDefaultFadeTimes()) {
            this.net().sendPacket(new PlayOutTitle.SetTiming(title));
        }

        ChatComponent mainTitle = title.getHeader();
        ChatComponent subtitle = title.getSubtitle();

        this.net().sendPacket(new PlayOutTitle.SetTitle(mainTitle));
        this.net().sendPacket(new PlayOutTitle.SetSubtitle(subtitle));
    }

    @Override
    public void resetTitle() {
        this.net().sendPacket(new PlayOutTitle.SetTitle((ChatComponent) null));
        this.net().sendPacket(new PlayOutTitle.SetSubtitle((ChatComponent) null));
        this.net().sendPacket(new PlayOutTitle.SetTiming());
    }

    @Override
    public void setPosition(Position position) {
        Position pos = this.getPosition();
        // TODO this is dumb
        if (position.getChunkX() != pos.getChunkX()) {
            this.updateChunks(position.getChunkX() > pos.getChunkX() ? BlockDirection.EAST : BlockDirection.WEST);
        } else if (position.getChunkZ() != pos.getChunkZ()) {
            this.updateChunks(position.getChunkZ() > pos.getChunkZ() ? BlockDirection.SOUTH : BlockDirection.NORTH);
        }
        super.setPosition(position);
    }

    /**
     * Sets the texture of this player to a different skin
     * data.
     *
     * @param skinTextures the skin textures
     */
    public void setTextures(TabListElement.PlayerProperty skinTextures) {
        this.skinTextures = skinTextures;
        // TODO Push update to tablist and other players
    }

    @Override
    public void setGameMode(GameMode gameMode) {
        this.gameMode = gameMode;
        this.canFly = gameMode == GameMode.CREATIVE || gameMode == GameMode.SPECTATOR;
        this.client.sendPacket(new PlayOutPlayerAbilities(this));
    }

    @Override
    public void setGodMode(boolean godMode) {
        this.setGodMode(godMode, true);
    }

    public void setGodMode(boolean godMode, boolean sendPacket) {
        this.godMode = godMode;
        if (sendPacket) {
            this.client.sendPacket(new PlayOutPlayerAbilities(this));
        }
    }

    @Override
    public boolean canFly() {
        return this.canFly;
    }

    @Override
    public void setCanFly(boolean canFly) {
        this.setCanFly(canFly, true);
    }

    public void setCanFly(boolean canFly, boolean sendPacket) {
        this.canFly = canFly;
        if (sendPacket) {
            this.client.sendPacket(new PlayOutPlayerAbilities(this));
        }
    }

    @Override
    public void setFlying(boolean flying) {
        this.setFlying(flying, true);
    }

    public void setFlying(boolean flying, boolean sendPacket) {
        this.flying = flying;
        if (sendPacket) {
            this.client.sendPacket(new PlayOutPlayerAbilities(this));
        }
    }

    @Override
    public void setFlyingSpeed(float flyingSpeed) {
        this.setFlyingSpeed(flyingSpeed, true);
    }

    public void setFlyingSpeed(float flyingSpeed, boolean sendPacket) {
        this.flyingSpeed = flyingSpeed;
        if (sendPacket) {
            this.client.sendPacket(new PlayOutPlayerAbilities(this));
        }
    }

    @Override
    public void setWalkingSpeed(float walkingSpeed) {
        this.setWalkingSpeed(walkingSpeed, true);
    }

    public void setWalkingSpeed(float walkingSpeed, boolean sendPacket) {
        this.walkingSpeed = walkingSpeed;
        if (sendPacket) {
            this.client.sendPacket(new PlayOutPlayerAbilities(this));
        }
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
        int centerX = this.getPosition().getChunkX();
        int centerZ = this.getPosition().getChunkZ();

        int renderDistance = this.renderDistance;
        int radius = renderDistance / 2;

        if (direction != null) {
            centerX += direction.getXDiff() * radius;
            centerZ += direction.getZDiff() * radius;
        }

        /* Should be 16, but renderDistance has to be divided by 2 */
        this.pool.execute(() ->
                this.chunkSentTime.keySet().iterator().forEachRemaining(chunk -> {
                    if(Math.abs(chunk.getX() - this.position.getChunkX()) > radius
                            || Math.abs(chunk.getZ()) - this.position.getChunkZ() > radius){
                        this.chunkSentTime.remove(chunk);
                    }
                }));

        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                IntPair position = IntPair.make(x, z);
                if (System.currentTimeMillis() - this.chunkSentTime.getOrDefault(position, 0L) > TridentPlayer.CHUNK_CACHE_MILLIS) {
                    CompletableFuture
                            .supplyAsync(() -> this.getWorld().chunkAt(position), this.pool)
                            .thenAcceptAsync(chunk -> {
                                this.client.sendPacket(new PlayOutChunk(chunk));
                                this.chunkSentTime.put(position, System.currentTimeMillis());
    
                                TridentPlayer.players.values().stream()
                                        .filter(player -> !player.equals(this))
                                        .filter(player -> player.getPosition().getChunkX() == position.getX() && player.getPosition().getChunkZ() == position.getZ())
                                        .forEach(player -> this.client.sendPacket(new PlayOutSpawnPlayer(player)));
                            }, this.pool);
                }
            }
        }
    }
}