/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl.client;

import com.google.common.base.Charsets;
import io.netty.util.concurrent.Future;
import java.net.InetSocketAddress;
import org.opendaylight.bgpcep.tcpmd5.KeyMapping;
import org.opendaylight.protocol.bgp.parser.BGPSession;
import org.opendaylight.protocol.bgp.rib.impl.AbstractBGPPeer;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPPeerRegistry;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionPreferences;
import org.opendaylight.protocol.bgp.rib.impl.spi.ConfiguredPeer;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIB;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;

/**
 * Class representing a client peer. Client peer connection orientation: (local BGP id) -> (remote BGP id)
 */
public final class BGPClientPeer extends AbstractBGPPeer implements ConfiguredPeer {

    private final IpAddress host;
    private Future<Void> cf;

    public BGPClientPeer(final IpAddress host, final RIB rib, final BGPPeerRegistry sessionRegistry) {
        super(peerName(host), rib, sessionRegistry);
        this.host = host;
    }

    @Override
    public RIB getRib() {
        return super.getRib();
    }

    private static String peerName(final IpAddress host) {
        if (host.getIpv4Address() != null) {
            return host.getIpv4Address().getValue();
        }
        if (host.getIpv6Address() != null) {
            return host.getIpv6Address().getValue();
        }

        return null;
    }

    public void initiateConnection(final InetSocketAddress address, final String password,
            final BGPSessionPreferences prefs, final AsNumber remoteAs) {
        final KeyMapping keys;
        if (password != null) {
            keys = new KeyMapping();
            keys.put(address.getAddress(), password.getBytes(Charsets.US_ASCII));
        } else {
            keys = null;
        }

        cf = getRib().getDispatcher().createReconnectingClient(address, prefs, remoteAs, this, getRib().getTcpStrategyFactory(),
                getRib().getSessionStrategyFactory(), keys);
    }

    @Override
    public synchronized void close() {
        if (this.cf != null) {
            this.cf.cancel(true);
            super.close();
            this.cf = null;
        }
    }

    @Override
    protected Ipv4Address getSourceBgpId(final BGPSession session, final RIB rib) {
        return rib.getBgpIdentifier();
    }

    @Override
    protected Ipv4Address getDestinationBgpId(final BGPSession session, final RIB rib) {
        return session.getBgpId();
    }

    @Override
    public IpAddress getRemoteAddress() {
        return host;
    }
}
