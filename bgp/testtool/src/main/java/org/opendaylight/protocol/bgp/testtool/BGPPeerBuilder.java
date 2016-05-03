/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.testtool;

import com.google.common.base.Preconditions;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import org.opendaylight.protocol.bgp.rib.impl.BGPDispatcherImpl;
import org.opendaylight.protocol.bgp.rib.impl.StrictBGPPeerRegistry;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPDispatcher;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionPreferences;
import org.opendaylight.protocol.bgp.rib.spi.BGPSessionListener;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.BgpParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class BGPPeerBuilder implements AutoCloseable {
    private static final int RETRY_TIMER = 10;
    private static final Logger LOG = LoggerFactory.getLogger(BGPPeerBuilder.class);

    static void createPeer(final BGPDispatcher dispatcher, final Arguments arguments, final InetSocketAddress localAddress,
        final BGPSessionListener sessionListener, final BgpParameters bgpParameters) {
        final AsNumber as = arguments.getAs();
        final BGPSessionPreferences proposal = new BGPSessionPreferences(as, arguments.getHoldtimer(), new Ipv4Address(localAddress.getHostName()),
            as, Collections.singletonList(bgpParameters));

        final InetSocketAddress remoteAddress = arguments.getRemoteAddress();
        final StrictBGPPeerRegistry strictBGPPeerRegistry = new StrictBGPPeerRegistry();
        strictBGPPeerRegistry.addPeer(StrictBGPPeerRegistry.getIpAddress(remoteAddress), sessionListener, proposal);

        if (arguments.getInitiateConnection()) {
            addFutureListener(localAddress, ((BGPDispatcherImpl) dispatcher).createClient(localAddress, remoteAddress, strictBGPPeerRegistry, RETRY_TIMER));
        } else {
            addFutureListener(localAddress, dispatcher.createServer(strictBGPPeerRegistry, localAddress));
        }
        LOG.debug("{} {} {}", remoteAddress, sessionListener, proposal);
    }

    private static <T> void addFutureListener(final InetSocketAddress localAddress, final Future<T> future) {
        future.addListener(new GenericFutureListener<Future<T>>() {
            @Override
            public void operationComplete(final Future<T> future) throws ExecutionException, InterruptedException {
                Preconditions.checkArgument(future.isSuccess(), "Unable to start bgp session on %s", localAddress, future.cause());
            }
        });
    }

    @Override
    public void close() throws Exception {

    }
}
