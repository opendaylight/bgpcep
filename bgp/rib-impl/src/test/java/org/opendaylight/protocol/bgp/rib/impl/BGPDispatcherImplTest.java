/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.opendaylight.protocol.bgp.rib.impl.CheckUtil.checkIdleState;

import com.google.common.collect.Sets;
import io.netty.channel.Channel;
import io.netty.util.concurrent.Future;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;
import org.junit.Test;
import org.opendaylight.protocol.bgp.rib.spi.State;
import org.opendaylight.protocol.concepts.KeyMapping;
import org.opendaylight.protocol.util.InetSocketAddressUtil;

public class BGPDispatcherImplTest extends AbstractBGPDispatcherTest {
    @Test(timeout = 20000)
    public void testCreateClient() throws InterruptedException, ExecutionException {
        final InetSocketAddress serverAddress = InetSocketAddressUtil.getRandomLoopbackInetSocketAddress();
        final Channel serverChannel = createServer(serverAddress);
        final Future<BGPSessionImpl> futureClient = clientDispatcher.createClient(clientAddress, serverAddress, 2,
            true);
        futureClient.sync();
        final BGPSessionImpl session = futureClient.get();
        assertEquals(State.UP, clientListener.getState());
        assertEquals(State.UP, serverListener.getState());
        assertEquals(AS_NUMBER, session.getAsNumber());
        assertEquals(Sets.newHashSet(IPV_4_TT), session.getAdvertisedTableTypes());
        assertTrue(serverChannel.isWritable());
        session.close();
        serverListener.releaseConnection();
        checkIdleState(clientListener);
        checkIdleState(serverListener);
    }

    @Test
    public void testCreateReconnectingClient() throws Exception {
        final InetSocketAddress serverAddress = InetSocketAddressUtil.getRandomLoopbackInetSocketAddress();
        final Future<Void> future = clientDispatcher.createReconnectingClient(serverAddress, RETRY_TIMER,
            KeyMapping.of(), clientAddress, true);
        final Channel serverChannel = createServer(serverAddress);
        assertEquals(State.UP, serverListener.getState());
        assertTrue(serverChannel.isWritable());
        future.cancel(true);
        serverListener.releaseConnection();
        checkIdleState(serverListener);
    }
}
