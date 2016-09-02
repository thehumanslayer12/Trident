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
package net.tridentsdk.server.packet.play;

import io.netty.buffer.ByteBuf;
import net.tridentsdk.base.BlockDirection;
import net.tridentsdk.base.Position;
import net.tridentsdk.server.TridentServer;
import net.tridentsdk.server.net.NetClient;
import net.tridentsdk.server.packet.PacketIn;
import net.tridentsdk.server.player.TridentPlayer;

import javax.annotation.concurrent.Immutable;

/**
 * Client confirmation of the player's current position and
 * look direction prior to spawning.
 */
@Immutable
public final class PlayInPosLook extends PacketIn {
    public PlayInPosLook() {
        super(PlayInPosLook.class);
    }

    @Override
    public void read(ByteBuf buf, NetClient client) {
        TridentPlayer player = client.player();
        Position pos = player.position();
        Position oldPos = pos.clone();

        pos.setX(buf.readDouble());
        pos.setY(buf.readDouble());
        pos.setZ(buf.readDouble());
        pos.setYaw(buf.readFloat());
        pos.setPitch(buf.readFloat());
        player.setOnGround(buf.readBoolean());

        Position delta = pos.clone().subtract(oldPos);

        PlayOutEntityLookAndRelativeMove lookAndRelativeMove = new PlayOutEntityLookAndRelativeMove(player, delta);
        PlayOutEntityHeadLook headLook = new PlayOutEntityHeadLook(player);
        TridentServer.instance().players().stream().filter(p -> !p.equals(player)).forEach(p -> {
            ((TridentPlayer) p).net().sendPacket(lookAndRelativeMove);
            ((TridentPlayer) p).net().sendPacket(headLook);
        });

        if(pos.getChunkX() != oldPos.getChunkX()){
            player.updateChunks(delta.x() > 0 ? BlockDirection.EAST : BlockDirection.WEST);
        }else if(pos.getChunkZ() != oldPos.getChunkZ()){
            player.updateChunks(delta.z() > 0 ? BlockDirection.SOUTH : BlockDirection.NORTH);
        }
    }
}