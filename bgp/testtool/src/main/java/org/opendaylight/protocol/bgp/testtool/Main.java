/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.testtool;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.opendaylight.protocol.util.LoggerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Starter class for testing.
 */
public final class Main {
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    private Main() {
        throw new UnsupportedOperationException();
    }

    public static void main(final String[] args) throws IOException {
        final Arguments arguments = Arguments.parseArguments(args);
        LoggerUtil.initiateLogger(arguments);

        final BGPTestTool bgpTestTool = new BGPTestTool();
        final InputStreamReader isr = new InputStreamReader(System.in, StandardCharsets.UTF_8);
        final BufferedReader br = new BufferedReader(isr);
        bgpTestTool.start(arguments);
        for (;;) {
            LOG.info("Insert local address:");
            bgpTestTool.printCount(br.readLine());
        }
    }
}
