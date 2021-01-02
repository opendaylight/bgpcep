/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.config.loader.impl;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;
import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.lang.ref.Cleaner;
import java.lang.ref.Cleaner.Cleanable;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.WatchService;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Singleton;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.concepts.AbstractRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Component(immediate = true, service = FileWatcher.class)
public final class DefaultFileWatcher extends AbstractRegistration implements FileWatcher {
    // TODO: this allocates a thread to the cleaning action, which really is a safety net. if we get some centralized
    //       place for low-usage ODL threads we want to use that.
    private static final Cleaner CLEANER = Cleaner.create();

    private static final class State implements Runnable {
        final @NonNull WatchService watchService;

        State(final WatchService watchService) {
            this.watchService = requireNonNull(watchService);
        }

        @Override
        public void run() {
            try {
                watchService.close();
            } catch (IOException e) {
                LOG.warn("Failed to close watch service", e);
            }
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(DefaultFileWatcher.class);
    //BGPCEP config folder OS agnostic path
    private static final @NonNull Path PATH = Path.of("etc","opendaylight","bgpcep");

    private final State state;
    private final Cleanable cleanable;

    public DefaultFileWatcher() throws IOException {
        state = new State(FileSystems.getDefault().newWatchService());
        cleanable = CLEANER.register(this, state);
    }

    @Override
    public Path getPathFile() {
        return PATH;
    }

    @Override
    public WatchService getWatchService() {
        return state.watchService;
    }

    @Activate
    @PostConstruct
    public void activate() throws IOException {
        final File file = new File(PATH.toString());
        if (!file.exists() && !file.mkdirs()) {
            LOG.warn("Failed to create config directory {}", PATH);
            return;
        }

        PATH.register(getWatchService(), OVERFLOW, ENTRY_CREATE);
        LOG.info("File Watcher service started");
    }

    @Deactivate
    @PreDestroy
    public void deactivate() {
        // Just route to close(), it will do the right thing
        close();
    }

    @Override
    protected void removeRegistration() {
        cleanable.clean();
        LOG.info("File Watcher service stopped");
    }
}
