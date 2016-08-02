/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class InetSocketAddressUtilTest {
    private static String ADDRESS1 = "1.1.1.1";
    private static String ADDRESS2 = "2.2.2.2";
    private static int PORT1 = 123;
    private static int PORT2 = 321;
    private static String ADDRESSES_WO_PORT = ADDRESS1 + "," + ADDRESS2;
    private static String ADDRESSES = ADDRESS1 + ":" + PORT1 + "," + ADDRESS2 + ":" + PORT2;
    private static int DEFAULT_PORT = 179;

    @Test
    public void parseAddresses() throws Exception {
        final List<InetSocketAddress> actualResult = InetSocketAddressUtil.parseAddresses(ADDRESSES, DEFAULT_PORT);
        Assert.assertEquals(Arrays.asList(new InetSocketAddress(ADDRESS1, PORT1), new InetSocketAddress(ADDRESS2, PORT2)), actualResult);
    }

    @Test
    public void parseAddressesDefaultPort() throws Exception {
        final List<InetSocketAddress> actualResult = InetSocketAddressUtil.parseAddresses(ADDRESSES_WO_PORT, DEFAULT_PORT);
        final List<InetSocketAddress> expected = Arrays.asList(new InetSocketAddress(ADDRESS1, DEFAULT_PORT), new InetSocketAddress(ADDRESS2, DEFAULT_PORT));
        Assert.assertEquals(expected, actualResult);
    }

    @Test
    public void parseAddressesWithoutPort() throws Exception {
        final List<InetSocketAddress> actualResult = InetSocketAddressUtil.parseAddresses(ADDRESSES);
        Assert.assertEquals(Arrays.asList(new InetSocketAddress(ADDRESS1, PORT1), new InetSocketAddress(ADDRESS2, PORT2)), actualResult);
    }

    @Test
    public void getInetSocketAddress() throws Exception {
        assertEquals(new InetSocketAddress(ADDRESS1, PORT1), InetSocketAddressUtil.getInetSocketAddress(ADDRESS1 + ":" + PORT1, DEFAULT_PORT));
        assertEquals(new InetSocketAddress(ADDRESS1, DEFAULT_PORT), InetSocketAddressUtil.getInetSocketAddress(ADDRESS1, DEFAULT_PORT));
    }

    @Test
    public void getRandomLoopbackInetSocketAddressTest() throws Exception {
        final InetSocketAddress addr1 = InetSocketAddressUtil.getRandomLoopbackInetSocketAddress();
        final InetSocketAddress addr2 = InetSocketAddressUtil.getRandomLoopbackInetSocketAddress();
        assertNotNull(addr1);
        assertNotNull(addr2);
        assertNotEquals(addr1, addr2);
        assertNotEquals(addr1.getHostString(), addr2.getHostString());
        assertNotEquals(addr1.getPort(), addr2.getPort());
    }
}