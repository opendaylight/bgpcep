/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.config.loader.impl;

import java.nio.file.Path;
import java.nio.file.WatchService;
import org.eclipse.jdt.annotation.NonNullByDefault;

@NonNullByDefault
public interface FileWatcher {
    /**
     * Path Folder watched.
     *
     * @return Path
     */
    Path getPathFile();

    /**
     * Return WatchService.
     *
     * @return WatchService
     */
    WatchService getWatchService();
}
