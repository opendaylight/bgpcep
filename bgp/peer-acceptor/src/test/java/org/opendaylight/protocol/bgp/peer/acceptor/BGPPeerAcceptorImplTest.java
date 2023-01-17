/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.peer.acceptor;

import static org.junit.Assert.assertEquals;
import static org.opendaylight.protocol.bgp.rib.impl.CheckUtil.checkIdleState;
import static org.opendaylight.protocol.util.CheckUtil.waitFutureSuccess;

import java.net.InetSocketAddress;
import java.util.Set;
import org.junit.Test;
import org.opendaylight.protocol.bgp.rib.impl.AbstractBGPDispatcherTest;
import org.opendaylight.protocol.bgp.rib.impl.BGPSessionImpl;
import org.opendaylight.protocol.bgp.rib.spi.State;
import org.opendaylight.protocol.util.InetSocketAddressUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yangtools.yang.common.Uint16;

public class BGPPeerAcceptorImplTest extends AbstractBGPDispatcherTest {
    @Test
    public void testBGPPeerAcceptorImpl() throws Exception {
        final InetSocketAddress inetServerAddress = InetSocketAddressUtil.getRandomLoopbackInetSocketAddress();
        final IpAddressNoZone serverIpAddress = new IpAddressNoZone(new Ipv4AddressNoZone(InetSocketAddressUtil
                .toHostAndPort(inetServerAddress).getHost()));
        final PortNumber portNumber = new PortNumber(Uint16.valueOf(
            InetSocketAddressUtil.toHostAndPort(inetServerAddress).getPort()));
        registry.addPeer(serverIpAddress, serverListener, createPreferences(inetServerAddress));

        try (var bgpPeerAcceptor = new BGPPeerAcceptorImpl(serverIpAddress, portNumber, serverDispatcher)) {
            final var futureClient = clientDispatcher.createClient(clientAddress, inetServerAddress, 2, true);
            waitFutureSuccess(futureClient);
            final BGPSessionImpl session = futureClient.get();
            assertEquals(State.UP, clientListener.getState());
            assertEquals(AS_NUMBER, session.getAsNumber());
            assertEquals(Set.of(IPV_4_TT), session.getAdvertisedTableTypes());
            session.close();
            checkIdleState(clientListener);
        }
    }
}
