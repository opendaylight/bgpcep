/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.testtool;

import com.google.common.base.Preconditions;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.GlobalEventExecutor;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.spi.pojo.ServiceLoaderBGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.rib.impl.BGPDispatcherImpl;
import org.opendaylight.protocol.bgp.rib.impl.BGPHandlerFactory;
import org.opendaylight.protocol.bgp.rib.impl.BGPServerSessionNegotiatorFactory;
import org.opendaylight.protocol.bgp.rib.impl.BGPSessionImpl;
import org.opendaylight.protocol.bgp.rib.impl.BGPSessionProposalImpl;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPPeerRegistry;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionPreferences;
import org.opendaylight.protocol.bgp.rib.impl.spi.ReusableBGPPeer;
import org.opendaylight.protocol.bgp.rib.spi.BGPSessionListener;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.LinkstateAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.LinkstateSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.SubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;

public class BGPSpeakerMock {

    private final BGPServerSessionNegotiatorFactory negotiatorFactory;
    private final BGPHandlerFactory factory;
    private final BGPDispatcherImpl disp;
    private final BGPPeerRegistry peerRegistry;
    private final Map<Class<? extends AddressFamily>, Class<? extends SubsequentAddressFamily>> tables;

    private BGPSpeakerMock(final BGPServerSessionNegotiatorFactory negotiatorFactory, final BGPHandlerFactory factory,
                           final DefaultPromise<BGPSessionImpl> defaultPromise) {
        this.disp = new BGPDispatcherImpl(null, new NioEventLoopGroup(), new NioEventLoopGroup());
        this.negotiatorFactory = Preconditions.checkNotNull(negotiatorFactory);
        this.factory = Preconditions.checkNotNull(factory);


        this.peerRegistry = new BGPPeerRegistry() {
            @Override
            public void addPeer(final IpAddress ip, final ReusableBGPPeer peer, final BGPSessionPreferences prefs) {
            }

            @Override
            public void removePeer(final IpAddress ip) {
            }

            @Override
            public boolean isPeerConfigured(final IpAddress ip) {
                return true;
            }

            @Override
            public BGPSessionListener getPeer(final IpAddress ip, final Ipv4Address sourceId, final Ipv4Address remoteId, final Open open) throws BGPDocumentedException {
                return new SpeakerSessionListener();
            }

            @Override
            public BGPSessionPreferences getPeerPreferences(final IpAddress ip) {
                return new BGPSessionProposalImpl((short) 90, new AsNumber(72L), new Ipv4Address("127.0.0.2"), BGPSpeakerMock.this.tables, new AsNumber(72L)).getProposal();
            }

            @Override
            public void close() throws Exception {

            }

            @Override
            public void removePeerSession(final IpAddress ip) {
            }
        };

        this.tables = new HashMap<>();
        this.tables.put(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class);
        this.tables.put(LinkstateAddressFamily.class, LinkstateSubsequentAddressFamily.class);
    }

    public void main(final String[] args) {

        final BGPServerSessionNegotiatorFactory snf = new BGPServerSessionNegotiatorFactory(this.peerRegistry);

        final BGPSpeakerMock mock = new BGPSpeakerMock(snf, new BGPHandlerFactory(ServiceLoaderBGPExtensionProviderContext.getSingletonInstance().getMessageRegistry()), new DefaultPromise<BGPSessionImpl>(GlobalEventExecutor.INSTANCE));

        mock.createServer(new InetSocketAddress("127.0.0.2", 12345));
    }

    private void createServer(final InetSocketAddress address) {
        this.disp.createServer(this.peerRegistry,address);
    }
}
