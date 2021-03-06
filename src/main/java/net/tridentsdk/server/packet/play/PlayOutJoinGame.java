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
package net.tridentsdk.server.packet.play;

import io.netty.buffer.ByteBuf;
import net.tridentsdk.entity.living.Player;
import net.tridentsdk.server.packet.PacketOut;
import net.tridentsdk.world.World;
import net.tridentsdk.world.opt.LevelType;
import net.tridentsdk.world.opt.WorldOpts;

import javax.annotation.concurrent.Immutable;

import static net.tridentsdk.server.net.NetData.wstr;

/**
 * Sent directly after login success, this provides the
 * player with information about the world they are
 * joining.
 */
@Immutable
public final class PlayOutJoinGame extends PacketOut {
    /**
     * World data that is sent to the player
     */
    private final WorldOpts opts;
    /**
     * The level type
     */
    private final LevelType type;
    /**
     * The player that is joining the game
     */
    private final Player player;

    public PlayOutJoinGame(Player player, World world) {
        super(PlayOutJoinGame.class);
        this.player = player;
        this.opts = world.getWorldOptions();
        this.type = world.getGeneratorOptions().getLevelType();
    }

    @Override
    public void write(ByteBuf buf) {
        buf.writeInt(this.player.getId());
        buf.writeByte(this.opts.getGameMode().asByte());
        buf.writeInt(this.opts.getDimension().asByte());
        buf.writeByte(this.opts.getDifficulty().asByte());
        buf.writeByte(0); // ignored by client
        wstr(buf, this.type.toString());
        buf.writeBoolean(false); // disable reduce debug
    }
}
