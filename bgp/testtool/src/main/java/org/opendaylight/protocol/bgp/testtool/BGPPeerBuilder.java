/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.testtool;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import io.netty.util.concurrent.Future;
import java.net.InetSocketAddress;
import java.util.Collections;
import org.opendaylight.protocol.bgp.rib.impl.BGPDispatcherImpl;
import org.opendaylight.protocol.bgp.rib.impl.StrictBGPPeerRegistry;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPDispatcher;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPPeerRegistry;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionPreferences;
import org.opendaylight.protocol.bgp.rib.spi.BGPSessionListener;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.open.message.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class BGPPeerBuilder {
    private static final int RETRY_TIMER = 10;
    private static final Logger LOG = LoggerFactory.getLogger(BGPPeerBuilder.class);

    private BGPPeerBuilder() {
        throw new UnsupportedOperationException();
    }

    static void createPeer(final BGPDispatcher dispatcher, final Arguments arguments,
            final InetSocketAddress localAddress, final BGPSessionListener sessionListener,
            final BgpParameters bgpParameters) {
        final AsNumber as = arguments.getAs();
        final BGPSessionPreferences proposal = new BGPSessionPreferences(as, arguments.getHoldTimer(),
                new BgpId(localAddress.getAddress().getHostAddress()), as, Collections.singletonList(bgpParameters),
                Optional.absent());
        final BGPPeerRegistry strictBGPPeerRegistry = dispatcher.getBGPPeerRegistry();
        if (arguments.getInitiateConnection()) {
            for (final InetSocketAddress remoteAddress : arguments.getRemoteAddresses()) {
                strictBGPPeerRegistry.addPeer(StrictBGPPeerRegistry.getIpAddress(remoteAddress), sessionListener,
                        proposal);
                addFutureListener(localAddress, ((BGPDispatcherImpl) dispatcher).createClient(localAddress,
                        remoteAddress, RETRY_TIMER, true));
            }
        } else {
            for (final InetSocketAddress remoteAddress : arguments.getRemoteAddresses()) {
                strictBGPPeerRegistry.addPeer(StrictBGPPeerRegistry.getIpAddress(remoteAddress), sessionListener,
                        proposal);
            }
            addFutureListener(localAddress, dispatcher.createServer(localAddress));
        }
        LOG.debug("{} {}", sessionListener, proposal);
    }

    private static <T> void addFutureListener(final InetSocketAddress localAddress, final Future<T> future) {
        future.addListener(future1 -> Preconditions.checkArgument(future1.isSuccess(),
                "Unable to start bgp session on %s", localAddress, future1.cause()));
    }
}
