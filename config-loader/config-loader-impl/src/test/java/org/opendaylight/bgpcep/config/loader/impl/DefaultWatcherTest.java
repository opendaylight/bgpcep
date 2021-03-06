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

import java.io.File;
import org.junit.Test;

public class DefaultWatcherTest {
    private static final String PATH = String.join(File.separator, "etc", "opendaylight", "bgpcep");

    @Test
    public void bgpFileWatcherTest() throws Exception {
        try (DefaultFileWatcher bgpFileWatcher = new DefaultFileWatcher()) {
            bgpFileWatcher.activate();

            assertEquals(PATH, bgpFileWatcher.getPathFile());
            assertNotNull(bgpFileWatcher.getWatchService());
        }
    }
}
