/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.base.Charsets;
import io.netty.util.concurrent.Future;
import java.net.InetSocketAddress;
import org.opendaylight.bgpcep.tcpmd5.KeyMapping;
import org.opendaylight.protocol.bgp.parser.BGPSession;
import org.opendaylight.protocol.bgp.parser.BGPSessionListener;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionPreferences;
import org.opendaylight.protocol.bgp.rib.impl.spi.GlobalBGPSessionRegistry;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIB;
import org.opendaylight.protocol.bgp.rib.spi.Peer;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;

/**
 * Class representing a client peer. We have a single instance for each peer, which provides translation from BGP events into
 * RIB actions.
 */
public final class BGPPeer extends AbstractBGPPeer implements BGPSessionListener, Peer, AutoCloseable {

    private Future<Void> cf;

    public BGPPeer(final String name, final InetSocketAddress address, final String password, final BGPSessionPreferences prefs,
            final AsNumber remoteAs, final RIB rib, final GlobalBGPSessionRegistry sessionRegistry) {
        super(name, rib, sessionRegistry);

        final KeyMapping keys;
        if (password != null) {
            keys = new KeyMapping();
            keys.put(address.getAddress(), password.getBytes(Charsets.US_ASCII));
        } else {
            keys = null;
        }

        // FIXME leaking this from constructor, createClient call should be removed from constructor
        this.cf = rib.getDispatcher().createReconnectingClient(address, prefs, remoteAs, this, rib.getTcpStrategyFactory(),
                rib.getSessionStrategyFactory(), keys);
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

}
