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
package net.tridentsdk.server;

import net.tridentsdk.Server;
import net.tridentsdk.command.Console;
import net.tridentsdk.config.Config;
import net.tridentsdk.server.config.ServerConfig;

import java.net.InetSocketAddress;

public class TridentServer implements Server {
    private final Config config;
    private final Console console;
    private final InetSocketAddress address;

    public TridentServer(ServerConfig config, Console console) {
        this.config = config;
        this.console = console;

        this.address = new InetSocketAddress(config.address(), config.port());
    }

    @Override
    public InetSocketAddress address() {
        return this.address;
    }

    @Override
    public Console console() {
        return this.console;
    }

    @Override
    public String version() {
        return "0.5-alpha";
    }

    @Override
    public void reload() {
    }

    @Override
    public void shutdown() {
    }
}