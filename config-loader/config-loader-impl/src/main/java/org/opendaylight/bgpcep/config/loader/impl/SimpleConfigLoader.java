/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.config.loader.impl;

import com.google.common.annotations.Beta;
import java.io.File;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.binding.runtime.api.BindingRuntimeContext;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;

@Beta
@Singleton
public final class SimpleConfigLoader extends AbstractWatchingConfigLoader implements AutoCloseable {
    private final @NonNull EffectiveModelContext modelContext;
    private final @NonNull WatchService watchService;
    private final @NonNull File directory;

    @Inject
    public SimpleConfigLoader(final FileWatcher fileWatcher, final BindingRuntimeContext runtimeContext) {
        this.modelContext = runtimeContext.getEffectiveModelContext();
        this.watchService = fileWatcher.getWatchService();
        this.directory = new File(fileWatcher.getPathFile());
    }

    @PostConstruct
    public void init() {
        start();
    }

    @Override
    @PreDestroy
    public void close() {
        stop();
    }

    @Override
    EffectiveModelContext modelContext() {
        return modelContext;
    }

    @Override
    File directory() {
        return directory;
    }

    @Override
    WatchKey takeEvent() throws InterruptedException {
        return watchService.take();
    }
}
