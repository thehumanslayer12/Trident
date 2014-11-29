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
package net.tridentsdk.packets.play.out;

import io.netty.buffer.ByteBuf;
import net.tridentsdk.api.Location;
import net.tridentsdk.data.Position;
import net.tridentsdk.server.netty.Codec;
import net.tridentsdk.server.netty.packet.OutPacket;

public class PacketPlayOutBlockAction extends OutPacket {

    protected Location location;
    protected byte byte1;
    protected byte byte2;
    protected int blockId;

    @Override
    public int getId() {
        return 0x24;
    }

    public Location getLocation() {
        return this.location;
    }

    public byte getByte1() {
        return this.byte1;
    }

    public byte getByte2() {
        return this.byte2;
    }

    @Override
    public void encode(ByteBuf buf) {
        new Position(this.location).write(buf);

        buf.writeByte((int) this.byte1);
        buf.writeByte((int) this.byte2);

        Codec.writeVarInt32(buf, this.blockId);
    }
}