/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.config.loader.impl;

import static com.google.common.base.Verify.verifyNotNull;

import com.google.common.annotations.Beta;
import java.nio.file.Path;
import java.nio.file.WatchKey;
import org.opendaylight.bgpcep.config.loader.spi.ConfigLoader;
import org.opendaylight.mdsal.binding.runtime.api.BindingRuntimeContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;

@Beta
@Component(immediate = true, service = ConfigLoader.class)
public final class OSGiConfigLoader extends AbstractWatchingConfigLoader {
    @Reference
    FileWatcher watcher;

    private Path directory;

    @Reference(policy = ReferencePolicy.DYNAMIC, updated = "setRuntimeContext", unbind = "setRuntimeContext")
    void setRuntimeContext(final BindingRuntimeContext runtimeContext) {
        updateModelContext(runtimeContext.getEffectiveModelContext());
    }

    @Activate
    void activate() {
        directory = watcher.getPathFile();
        start();
    }

    @Deactivate
    void deactivate() {
        try {
            stop();
        } finally {
            directory = null;
        }
    }

    @Override
    Path directory() {
        return verifyNotNull(directory);
    }

    @Override
    WatchKey takeEvent() throws InterruptedException {
        return watcher.getWatchService().take();
    }
}
