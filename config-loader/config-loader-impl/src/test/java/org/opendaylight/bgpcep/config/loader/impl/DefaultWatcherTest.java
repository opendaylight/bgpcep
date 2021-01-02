/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.config.loader.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.nio.file.Path;
import org.junit.Test;

public class DefaultWatcherTest {
    @Test
    public void bgpFileWatcherTest() throws Exception {
        try (DefaultFileWatcher bgpFileWatcher = new DefaultFileWatcher()) {
            bgpFileWatcher.activate();

            assertEquals(Path.of("etc", "opendaylight", "bgpcep"), bgpFileWatcher.getPathFile());
            assertNotNull(bgpFileWatcher.getWatchService());
        }
    }
}
