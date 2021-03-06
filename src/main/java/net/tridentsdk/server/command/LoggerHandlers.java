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
package net.tridentsdk.server.command;

import net.tridentsdk.command.logger.LogHandler;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class contains the handlers that plugins may use to
 * change the output of the logger or their loggers.
 */
@ThreadSafe
public class LoggerHandlers extends PipelinedLogger {
    /**
     * The set of handlers that intercept all output
     */
    private final Set<LogHandler> handlers = Collections.newSetFromMap(new ConcurrentHashMap<>());

    /**
     * Creates a new handler class for the all messages log
     * interceptors.
     *
     * @param next the next logger in the pipeline
     */
    public LoggerHandlers(PipelinedLogger next) {
        super(next);
    }

    @Override
    public LogMessageImpl handle(LogMessageImpl msg) {
        boolean doLog = true;
        for (LogHandler handler : this.handlers) {
            if (!handler.handle(msg)) {
                doLog = false;
            }
        }
        return doLog ? msg : null;
    }

    /**
     * Obtains the all the handlers that are attached to
     * the
     * output.
     *
     * @return the logger handlers
     */
    public Set<LogHandler> handlers() {
        return this.handlers;
    }
}