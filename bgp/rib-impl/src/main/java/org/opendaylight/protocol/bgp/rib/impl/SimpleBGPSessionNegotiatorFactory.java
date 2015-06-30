/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import io.netty.channel.Channel;
import io.netty.util.concurrent.Promise;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPPeerRegistry;
import org.opendaylight.protocol.bgp.rib.spi.BGPSessionNegotiatorFactory;

public class SimpleBGPSessionNegotiatorFactory implements BGPSessionNegotiatorFactory<BGPSessionImpl> {
    private final BGPPeerRegistry peerRegistry;

    public SimpleBGPSessionNegotiatorFactory(final BGPPeerRegistry peerRegistry) {
        this.peerRegistry = peerRegistry;
    }

    @Override
    public BGPSessionNegotiator getSessionNegotiator(final Channel channel, final Promise<BGPSessionImpl> promise) {
        return new BGPSessionNegotiator(promise, channel, this.peerRegistry);
    }
}
