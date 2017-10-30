/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.config.loader.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import org.junit.Test;

public class BGPFileWatcherTest {
    private static final String PATH = "etc" + File.separator + "opendaylight" + File.separator
            + "bgpcep" + File.separator;

    @Test
    public void bgpFileWatcherTest() throws Exception {
        final BGPFileWatcher bgpFileWatcher = new BGPFileWatcher();
        assertEquals(PATH, bgpFileWatcher.getPathFile());
        assertNotNull(bgpFileWatcher.getWatchService());
        bgpFileWatcher.close();
    }
}