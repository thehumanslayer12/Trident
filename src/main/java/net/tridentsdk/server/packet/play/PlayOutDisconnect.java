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
import net.tridentsdk.chat.ChatComponent;
import net.tridentsdk.server.packet.PacketOut;

import javax.annotation.concurrent.Immutable;

import static net.tridentsdk.server.net.NetData.wstr;

/**
 * Disconnect packet for PLAY.
 */
@Immutable
public final class PlayOutDisconnect extends PacketOut {
    /**
     * The reason why the player is disconnected
     */
    private final ChatComponent reason;

    public PlayOutDisconnect(ChatComponent reason) {
        super(PlayOutDisconnect.class);
        this.reason = reason;
    }

    @Override
    public void write(ByteBuf buf) {
        wstr(buf, this.reason.toString());
    }
}
