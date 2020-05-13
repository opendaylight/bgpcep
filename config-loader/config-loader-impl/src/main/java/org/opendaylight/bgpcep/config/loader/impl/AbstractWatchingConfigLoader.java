/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.config.loader.impl;

import java.nio.file.ClosedWatchServiceException;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An {@link AbstractConfigLoader} which additionally talks to a {@link WatchService}.
 */
abstract class AbstractWatchingConfigLoader extends AbstractConfigLoader {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractWatchingConfigLoader.class);
    private static final String INTERRUPTED = "InterruptedException";

    private final Thread watcherThread = new Thread(this::dispatchEvents);

    @GuardedBy("this")
    private boolean closed = false;

    final void start() {
        this.watcherThread.start();
        LOG.info("Config Loader service started");
    }

    final void stop() {
        LOG.info("Config Loader service stopping");

        synchronized (this) {
            this.closed = true;
            this.watcherThread.interrupt();
        }

        try {
            this.watcherThread.join();
        } catch (InterruptedException e) {
            LOG.warn("Interrupted while waiting for watcher thread to terminate", e);
        }
        LOG.info("Config Loader service stopped");
    }

    abstract WatchKey takeEvent() throws InterruptedException;

    private void dispatchEvents() {
        while (!Thread.currentThread().isInterrupted()) {
            handleChanges();
        }
    }

    private synchronized void handleChanges() {
        final WatchKey key;
        try {
            key = takeEvent();
        } catch (final InterruptedException | ClosedWatchServiceException e) {
            if (!closed) {
                LOG.warn(INTERRUPTED, e);
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
