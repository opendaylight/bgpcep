/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.peer.acceptor;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import io.netty.util.concurrent.Future;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.protocol.bgp.rib.impl.AbstractBGPDispatcherTest;
import org.opendaylight.protocol.bgp.rib.impl.BGPSessionImpl;
import org.opendaylight.protocol.util.InetSocketAddressUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.peer.acceptor.config.rev161003.BgpPeerAcceptorConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.peer.acceptor.config.rev161003.BgpPeerAcceptorConfigBuilder;


public class BGPPeerAcceptorImplTest extends AbstractBGPDispatcherTest {
    @Test
    public void testBGPPeerAcceptorImpl() throws InterruptedException, ExecutionException {
        final InetSocketAddress inetServerAddress = InetSocketAddressUtil.getRandomLoopbackInetSocketAddress();
        final IpAddress serverIpAddress = new IpAddress(new Ipv4Address(InetSocketAddressUtil.toHostAndPort(inetServerAddress).getHostText()));
        final BgpPeerAcceptorConfig config = new BgpPeerAcceptorConfigBuilder().setBindingAddress(serverIpAddress)
            .setBindingPort(new PortNumber(InetSocketAddressUtil.toHostAndPort(inetServerAddress).getPort())).build();
        this.registry.addPeer(serverIpAddress, this.serverListener, createPreferences(inetServerAddress));

        final BGPPeerAcceptorImpl bgpPeerAcceptor = new BGPPeerAcceptorImpl(config, this.registry, this.serverDispatcher);
        final Future<BGPSessionImpl> futureClient = this.clientDispatcher.createClient(inetServerAddress, this.registry, 2, Optional.absent());

        waitFutureSuccess(futureClient);
        final BGPSessionImpl session = futureClient.get();
        Assert.assertEquals(BGPSessionImpl.State.UP, this.clientListener.getState());
        Assert.assertEquals(AS_NUMBER, session.getAsNumber());
        Assert.assertEquals(Sets.newHashSet(IPV_4_TT), session.getAdvertisedTableTypes());
        session.close();
        checkIdleState(this.clientListener);
    }
}