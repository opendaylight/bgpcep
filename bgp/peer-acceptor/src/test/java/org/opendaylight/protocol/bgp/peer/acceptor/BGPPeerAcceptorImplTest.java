/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.peer.acceptor;

import static org.opendaylight.protocol.bgp.rib.impl.CheckUtil.checkIdleState;
import static org.opendaylight.protocol.util.CheckUtil.waitFutureSuccess;

import com.google.common.collect.Sets;
import io.netty.util.concurrent.Future;
import java.net.InetSocketAddress;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.protocol.bgp.rib.impl.AbstractBGPDispatcherTest;
import org.opendaylight.protocol.bgp.rib.impl.BGPSessionImpl;
import org.opendaylight.protocol.bgp.rib.spi.State;
import org.opendaylight.protocol.util.InetSocketAddressUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;


public class BGPPeerAcceptorImplTest extends AbstractBGPDispatcherTest {
    @Test
    public void testBGPPeerAcceptorImpl() throws Exception {
        final InetSocketAddress inetServerAddress = InetSocketAddressUtil.getRandomLoopbackInetSocketAddress();
        final IpAddress serverIpAddress = new IpAddress(new Ipv4Address(InetSocketAddressUtil
                .toHostAndPort(inetServerAddress).getHost()));
        final PortNumber portNumber = new PortNumber(InetSocketAddressUtil.toHostAndPort(inetServerAddress).getPort());
        this.registry.addPeer(serverIpAddress, this.serverListener, createPreferences(inetServerAddress));

        final BGPPeerAcceptorImpl bgpPeerAcceptor = new BGPPeerAcceptorImpl(serverIpAddress, portNumber,
            this.serverDispatcher);
        bgpPeerAcceptor.start();
        final Future<BGPSessionImpl> futureClient = this.clientDispatcher
            .createClient(this.clientAddress, inetServerAddress, 2, true);
        waitFutureSuccess(futureClient);
        final BGPSessionImpl session = futureClient.get();
        Assert.assertEquals(State.UP, this.clientListener.getState());
        Assert.assertEquals(AS_NUMBER, session.getAsNumber());
        Assert.assertEquals(Sets.newHashSet(IPV_4_TT), session.getAdvertisedTableTypes());
        session.close();
        checkIdleState(this.clientListener);

        bgpPeerAcceptor.close();
    }
}
