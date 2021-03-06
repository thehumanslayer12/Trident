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

import net.tridentsdk.util.Misc;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

/**
 * This class represents the file logger which writes the
 * messages sent by loggers to the log file.
 *
 * <p>In addition, this class also manages the log files,
 * moving them to appropriate directories when they fill to
 * the text editor limits.</p>
 */
@ThreadSafe
public class FileLogger extends PipelinedLogger {
    /**
     * The directory to the log files
     */
    private static final Path DIR = Misc.HOME_PATH.resolve("logs");
    /**
     * The system line separator
     */
    private static final String LINE_SEP = System.getProperty("line.separator");
    /**
     * Max file length, 80 mb
     */
    private static final int MAX_LEN = 83886080;
    /**
     * The index separator for the
     */
    private static final String IDX_SEPARATOR = Pattern.quote(".");

    /**
     * The file writer
     */
    @GuardedBy("lock")
    private Writer out;
    /**
     * Current log file
     */
    @GuardedBy("lock")
    private Path current;
    /**
     * The file writer lock
     */
    private final Object lock = new Object();

    /**
     * Creates a new log file logger, which logs items to
     * a file before being sent to the underlying logger.
     *
     * @param next the next logger in the pipeline
     */
    private FileLogger(PipelinedLogger next) {
        super(next);
    }

    /**
     * Initializes the files and directories, attempts to
     * find the last log file.
     */
    public static FileLogger init(PipelinedLogger next) throws Exception {
        FileLogger logger = new FileLogger(next);

        if (!Files.exists(DIR)) {
            Files.createDirectory(DIR);
        }

        File[] files = DIR.toFile().listFiles();
        if (files != null && files.length > 0) {
            int idx = -1;
            File f = null;
            for (File file : files) {
                String[] split = file.getName().split(IDX_SEPARATOR);
                int i = Integer.parseInt(split[1]);
                if (i > idx) {
                    idx = i;
                    f = file;
                }
            }

            if (f == null) throw new RuntimeException();

            synchronized (logger.lock) {
                logger.makeNewLog(f.toPath());
            }
        } else {
            synchronized (logger.lock) {
                logger.makeNewLog(0);
            }
        }

        return logger;
    }

    @Override
    public LogMessageImpl handle(LogMessageImpl msg) {
        Writer out = this.check();

        try {
            out.write(msg.format(0));
            out.write(LINE_SEP);
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return msg;
    }

    /**
     * Checks if the file needs to be truncated
     *
     * @return the writer, including if it was updated
     */
    private Writer check() {
        if (ThreadLocalRandom.current().nextInt(50) == 1) {
            synchronized (this.lock) {
                try {
                    if (Files.size(this.current) > MAX_LEN) {
                        this.makeNewLog(this.current);
                    }

                    return this.out;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        } else {
            synchronized (this.lock) {
                return this.out;
            }
        }
    }

    /**
     * Creates a new log file
     *
     * @param last the last log file
     * @throws IOException if something dumb went wrong
     */
    @GuardedBy("lock")
    private void makeNewLog(Path last) throws IOException {
        String[] split = last.toFile().getName().split(IDX_SEPARATOR);
        int curIdx = Integer.parseInt(split[1]) + 1;
        this.makeNewLog(curIdx);
    }

    /**
     * Creates a new log file based from the new index
     *
     * @param idx the new index
     * @throws IOException if something dumb went wrong
     */
    @GuardedBy("lock")
    private void makeNewLog(int idx) throws IOException {
        Path path = DIR.resolve("log." + idx + ".log");
        Files.createFile(path);

        this.current = path;
        this.out = new FileWriter(path.toFile());
    }
}