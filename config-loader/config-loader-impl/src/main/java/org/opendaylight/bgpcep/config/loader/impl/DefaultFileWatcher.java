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
public final class DefaultFileWatcher extends AbstractRegistration implements FileWatcher, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultFileWatcher.class);
    private static final String BGPCEP_CONFIG_FOLDER = "bgpcep";
    private static final String DEFAULT_APP_CONFIG_FILE_PATH = "etc" + File.separator + "opendaylight"
            + File.separator + BGPCEP_CONFIG_FOLDER + File.separator;
    private static final Path PATH = Paths.get(DEFAULT_APP_CONFIG_FILE_PATH);

    private final WatchService watchService;

    public DefaultFileWatcher() throws IOException {
        watchService = FileSystems.getDefault().newWatchService();
        Runtime.getRuntime().addShutdownHook(new Thread(this::close));
    }

    @Override
    public String getPathFile() {
        return DEFAULT_APP_CONFIG_FILE_PATH;
    }

    @Override
    public WatchService getWatchService() {
        return watchService;
    }

    @Activate
    @PostConstruct
    public void activate() throws IOException {
        final File file = new File(DEFAULT_APP_CONFIG_FILE_PATH);
        if (!file.exists()) {
            if (!file.mkdirs()) {
                LOG.warn("Failed to create config directory {}", DEFAULT_APP_CONFIG_FILE_PATH);
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
