/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bmp.mock;

import static org.junit.Assert.assertEquals;

import ch.qos.logback.classic.Level;
import java.net.InetSocketAddress;
import java.util.Collections;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import org.junit.Test;

public class BmpMockArgumentsTest {

    @Test
    public void testDefaultArguments() {
        final BmpMockArguments arguments = BmpMockArguments.parseArguments(new String[]{});
        assertEquals(1, arguments.getRoutersCount());
        assertEquals(0, arguments.getPeersCount());
        assertEquals(0, arguments.getPrePolicyRoutesCount());
        assertEquals(0, arguments.getPostPolicyRoutesCount());
        assertEquals(Level.INFO, arguments.getLogLevel());
        assertEquals(new InetSocketAddress("127.0.0.1", 0), arguments.getLocalAddress());
        assertEquals(Collections.singletonList(new InetSocketAddress("127.0.0.1", 12345)),
                arguments.getRemoteAddress());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWrongArgument() {
        BmpMockArguments.parseArguments(new String[]{"--routers_count", "abcd"});
    }

    @Test
    public void testGetRoutersCount() throws ArgumentParserException {
        final BmpMockArguments arguments = BmpMockArguments.parseArguments(new String[]{"--routers_count", "10"});
        assertEquals(10, arguments.getRoutersCount());
    }

    @Test
    public void testGetPeersCount() {
        final BmpMockArguments arguments = BmpMockArguments.parseArguments(new String[]{"--peers_count", "5"});
        assertEquals(5, arguments.getPeersCount());
    }

    @Test
    public void testGetPrePolicyRoutesCount() {
        final BmpMockArguments arguments = BmpMockArguments.parseArguments(new String[]{"--pre_policy_routes", "20"});
        assertEquals(20, arguments.getPrePolicyRoutesCount());
    }

    @Test
    public void testGetPostPolicyRoutesCount() {
        final BmpMockArguments arguments = BmpMockArguments.parseArguments(new String[]{"--post_policy_routes", "100"});
        assertEquals(100, arguments.getPostPolicyRoutesCount());
    }

    @Test
    public void testGetLocalAddress() {
        final BmpMockArguments arguments = BmpMockArguments.parseArguments(new String[]{"--local_address", "1.2.3.4"});
        assertEquals(new InetSocketAddress("1.2.3.4", 0), arguments.getLocalAddress());
    }

    @Test
    public void testGetRemoteAddress() {
        final BmpMockArguments arguments = BmpMockArguments
                .parseArguments(new String[]{"--remote_address", "4.5.6.7:1025"});
        assertEquals(Collections.singletonList(new InetSocketAddress("4.5.6.7", 1025)),
                arguments.getRemoteAddress());
    }

    @Test
    public void testGetLogLevel() {
        final BmpMockArguments arguments = BmpMockArguments.parseArguments(new String[]{"--log_level", "TRACE"});
        assertEquals(Level.TRACE, arguments.getLogLevel());
    }

}
