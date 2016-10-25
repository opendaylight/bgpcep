/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.config.loader.impl;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.function.Function;
import javax.annotation.concurrent.GuardedBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BGPFileWatcher implements FileWatcher, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(BGPFileWatcher.class);
    private static final String INTERRUPTED = "InterruptedException";
    private static final String DEFAULT_APP_CONFIG_FILE_PATH = "etc" + File.separator + "opendaylight" + File.separator + "bgp";
    private static final Path PATH = Paths.get(DEFAULT_APP_CONFIG_FILE_PATH);
    @GuardedBy("this")
    private WatchService watchService;

    public BGPFileWatcher() throws IOException {
        try {
            this.watchService = FileSystems.getDefault().newWatchService();
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    try {
                        BGPFileWatcher.this.watchService.close();
                    } catch (final IOException e) {
                        LOG.warn(INTERRUPTED, e);
                    }
                }
            });
            PATH.register(this.watchService, OVERFLOW, ENTRY_CREATE);
        } catch (final IOException e) {
            this.watchService.close();
            this.watchService = null;
            LOG.warn(INTERRUPTED, e);
        }
    }

    @Override
    public String getPathFile() {
        return DEFAULT_APP_CONFIG_FILE_PATH;
    }

    @Override
    public synchronized void handleEvents(final Function<String, Void> handleFile) {
        if (this.watchService != null) {
            try {
                final WatchKey key = this.watchService.take();
                if (key != null) {
                    for (final WatchEvent event : key.pollEvents()) {
                        handleFile.apply(event.context().toString());
                    }
                    final boolean reset = key.reset();
                    if (!reset) {
                        LOG.warn("Could not reset the watch key.");
                        return;
                    }
                }
            } catch (final InterruptedException e) {
                LOG.warn(INTERRUPTED, e);
            }
        }
        return;
    }

    @Override
    public synchronized void close() throws Exception {
        if (this.watchService != null) {
            this.watchService.close();
        }
    }
}
