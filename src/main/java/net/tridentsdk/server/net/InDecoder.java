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
package net.tridentsdk.server.net;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import net.tridentsdk.command.logger.Logger;
import net.tridentsdk.server.TridentServer;
import net.tridentsdk.server.packet.Packet;
import net.tridentsdk.server.packet.PacketIn;
import net.tridentsdk.server.packet.PacketRegistry;

import javax.annotation.concurrent.ThreadSafe;
import java.util.List;
import java.util.zip.Inflater;

import static net.tridentsdk.server.net.NetData.arr;
import static net.tridentsdk.server.net.NetData.rvint;

/**
 * This is the first decoder in the pipeline. Incoming
 * packets are read and decompressed through this decoder.
 */
@ThreadSafe
public class InDecoder extends ByteToMessageDecoder {
    /**
     * The logger used for debugging packets
     */
    private static final Logger LOGGER = Logger.get(InDecoder.class);
    /**
     * The packet inflater used for uncompressing packets
     */
    private static final ThreadLocal<Inflater> INFLATER = ThreadLocal.withInitial(Inflater::new);
    /**
     * Obtains the configured compression threshold.
     */
    public static final int COMPRESSION_THRESH = TridentServer.cfg().compressionThresh();

    /**
     * The net client which holds this channel handler
     */
    private NetClient client;

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        this.client = NetClient.get(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        this.client.setState(NetClient.NetState.HANDSHAKE);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> list) throws Exception {
        // Step 1: Decrypt if enabled
        // If not, use the raw buffer
        NetCrypto crypto = this.client.getCryptoModule();
        ByteBuf decrypt = buf;
        if (crypto != null) {
            decrypt = ctx.alloc().buffer();
            crypto.decrypt(buf, decrypt);
        }

        // Step 2: Decompress if enabled
        // If not, compressed, use raw buffer
        // Toss appropriate header fields
        ByteBuf decompressed = decrypt;
        if (this.client.doCompression()) {
            rvint(decrypt); // toss full packet length
            int compressedLen = rvint(decrypt);
            if (compressedLen > COMPRESSION_THRESH) {
                decompressed = ctx.alloc().buffer();
                byte[] in = arr(decrypt);

                Inflater inflater = INFLATER.get();
                inflater.setInput(in);

                byte[] buffer = new byte[NetClient.BUFFER_SIZE];
                while (!inflater.finished()) {
                    int bytes = inflater.inflate(buffer);
                    decompressed.writeBytes(buffer, 0, bytes);
                }

                inflater.reset();
            }
        } else {
            rvint(decompressed); // toss full packet length
        }

        // Step 3: Decode packet
        int id = rvint(decompressed);

        Class<? extends Packet> cls = PacketRegistry.byId(this.client.getState(), Packet.Bound.SERVER, id);
        PacketIn packet = PacketRegistry.make(cls);

        LOGGER.debug("RECV: " + packet.getClass().getSimpleName());
        packet.read(decompressed, this.client);

        // If we created a new buffer, release it here
        if (decompressed != decrypt) {
            decompressed.release();
        }

        if (decrypt != buf) {
            decrypt.release();
        }
    }
}