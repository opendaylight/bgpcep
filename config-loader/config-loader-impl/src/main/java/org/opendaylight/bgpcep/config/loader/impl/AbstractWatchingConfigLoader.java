/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.config.loader.impl;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An {@link AbstractConfigLoader} which additionally talks to a {@link WatchService}.
 */
abstract class AbstractWatchingConfigLoader extends AbstractConfigLoader {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractWatchingConfigLoader.class);

    private final AtomicBoolean closed = new AtomicBoolean();
    private final Thread watcherThread;

    @SuppressFBWarnings(value = "MC_OVERRIDABLE_METHOD_CALL_IN_CONSTRUCTOR",
        justification = "https://github.com/spotbugs/spotbugs/issues/1867")
    AbstractWatchingConfigLoader() {
        watcherThread = new Thread(this::dispatchEvents, "Config Loader Watcher Thread");
        watcherThread.setDaemon(true);
    }

    final void start() {
        watcherThread.start();
        LOG.info("Config Loader service started");
    }

    final void stop() {
        LOG.info("Config Loader service stopping");

        closed.set(true);
        watcherThread.interrupt();

        try {
            watcherThread.join();
        } catch (InterruptedException e) {
            LOG.warn("Interrupted while waiting for watcher thread to terminate", e);
        }
        LOG.info("Config Loader service stopped");
    }

    abstract WatchKey takeEvent() throws InterruptedException;

    private void dispatchEvents() {
        while (!closed.get()) {
            final WatchKey key;
            try {
                key = takeEvent();
            } catch (final InterruptedException | ClosedWatchServiceException e) {
                if (!closed.get()) {
                    LOG.warn("Exception while waiting for events, exiting", e);
                    Thread.currentThread().interrupt();
                }
                return;
            }

            if (key != null) {
                key.pollEvents().stream().map(event -> event.context().toString()).forEach(this::handleEvent);
                if (!key.reset()) {
                    LOG.warn("Could not reset the watch key.");
                }
            }
        }
    }
}
