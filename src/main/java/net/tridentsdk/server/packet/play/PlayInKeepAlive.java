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
import net.tridentsdk.server.net.NetClient;
import net.tridentsdk.server.packet.PacketIn;
import net.tridentsdk.util.Cache;

import javax.annotation.concurrent.Immutable;
import java.util.concurrent.ThreadLocalRandom;

import static net.tridentsdk.server.net.NetData.rvint;

/**
 * Sent by the client in order to enusre that the
 * connection
 * remains active.
 */
@Immutable
public final class PlayInKeepAlive extends PacketIn {
    /**
     * The keep alive time cache
     */
    private static final Cache<NetClient, Integer> TICK_IDS =
            new Cache<>(NetClient.KEEP_ALIVE_KICK_NANOS / 1000000, (client, id) -> client.disconnect("No KeepAlive response"));

    /**
     * Obtains the next keep alive ID for the given net
     * client
     *
     * @param client the client
     * @return the next teleport ID
     */
    public static int query(NetClient client) {
        // retarded int limit on VarInt, idk
        int value = ThreadLocalRandom.current().nextInt(0xFFFFFFF);
        TICK_IDS.put(client, value);

        return value;
    }

    public PlayInKeepAlive() {
        super(PlayInKeepAlive.class);
    }

    @Override
    public void read(ByteBuf buf, NetClient client) {
        int id = rvint(buf);
        Integer localId = TICK_IDS.get(client);

        if (localId != null && id != localId) {
            client.disconnect("Keep alive ID mismatch, actual:" + localId + " rcvd:" + id);
        }

        if ((System.nanoTime() - client.lastKeepAlive()) > NetClient.KEEP_ALIVE_KICK_NANOS) {
            client.disconnect("Timed out");
        }
    }
}