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

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchService;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Singleton;
import org.opendaylight.yangtools.concepts.AbstractRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Component(immediate = true, service = FileWatcher.class)
public final class DefaultFileWatcher extends AbstractRegistration implements FileWatcher {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultFileWatcher.class);
    //BGPCEP config folder OS agnostic path
    private static final Path PATH = Paths.get("etc","opendaylight","bgpcep");

    private final WatchService watchService;

    public DefaultFileWatcher() throws IOException {
        watchService = FileSystems.getDefault().newWatchService();
        Runtime.getRuntime().addShutdownHook(new Thread(this::close));
    }

    @Override
    public String getPathFile() {
        return PATH.toString();
    }

    @Override
    public WatchService getWatchService() {
        return watchService;
    }

    @Activate
    @PostConstruct
    public void activate() throws IOException {
        final File file = new File(PATH.toString());
        if (!file.exists()) {
            if (!file.mkdirs()) {
                LOG.warn("Failed to create config directory {}", PATH);
                return;
            }
        }

        PATH.register(this.watchService, OVERFLOW, ENTRY_CREATE);
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
        try {
            watchService.close();
        } catch (IOException e) {
            LOG.warn("Failed to close watch service", e);
        }
        LOG.info("File Watcher service stopped");
    }
}
