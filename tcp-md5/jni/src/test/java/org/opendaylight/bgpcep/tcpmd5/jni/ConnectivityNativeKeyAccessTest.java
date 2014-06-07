/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.tcpmd5.jni;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.bgpcep.tcpmd5.KeyAccess;
import org.opendaylight.bgpcep.tcpmd5.KeyAccessFactory;
import org.opendaylight.bgpcep.tcpmd5.KeyMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnectivityNativeKeyAccessTest {
    private static final Logger LOG = LoggerFactory.getLogger(ConnectivityNativeKeyAccessTest.class);
    private static final byte[] KEY1 = new byte[] { 1 };
    private static final byte[] KEY2 = new byte[] { 2, 3 };

    private KeyAccessFactory factory;
    private Thread serverThread;
    private InetAddress address;
    private int port;
    private AtomicReference<SocketChannel> serverClient;

    @Before
    public void setup() throws IOException {
        factory = NativeKeyAccessFactory.getInstance();
        address = InetAddress.getLoopbackAddress();
        serverClient = new AtomicReference<>();

        final ServerSocketChannel sc = ServerSocketChannel.open();
        LOG.debug("Instatiated server {}", sc);

        final KeyAccess ka = factory.getKeyAccess(sc);
        final KeyMapping map = new KeyMapping();
        map.put(address, KEY1);
        ka.setKeys(map);
        LOG.debug("Server got key {}", KEY1);

        sc.bind(new InetSocketAddress(address, 0));
        port = ((InetSocketAddress) sc.getLocalAddress()).getPort();
        LOG.debug("Server bound to port {}", port);

        serverThread = new Thread(new Runnable() {
            @Override
            public void run() {
                LOG.debug("Server thread started");
                try {
                    SocketChannel s = sc.accept();
                    LOG.debug("Accepted channel {}", s);
                    serverClient.set(s);
                } catch (IOException e) {
                    LOG.debug("Failed to accept connection", e);
                }
            }
        });
        serverThread.start();
    }

    @After
    public void teardown() throws InterruptedException {
        serverThread.interrupt();
        serverThread.join();
    }

    @Test(timeout = 2000)
    public void testMatchingKey() throws IOException {
        try (final SocketChannel c = SocketChannel.open()) {
            final KeyAccess ka = factory.getKeyAccess(c);
            final KeyMapping map = new KeyMapping();
            map.put(address, KEY1);
            ka.setKeys(map);

            c.connect(new InetSocketAddress(address, port));
            assertTrue(c.isConnected());
        }
    }

    @Test(timeout = 5000, expected = SocketTimeoutException.class)
    public void testMisatchedKey() throws IOException {
        try (final SocketChannel c = SocketChannel.open()) {
            final KeyAccess ka = factory.getKeyAccess(c);
            final KeyMapping map = new KeyMapping();
            map.put(address, KEY2);
            ka.setKeys(map);

            c.socket().connect(new InetSocketAddress(address, port), 2000);
            assertFalse(c.isConnected());
        }
    }
}
