/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import io.netty.channel.Channel;
import io.netty.util.concurrent.Promise;

import org.opendaylight.protocol.bgp.rib.impl.spi.BGPPeerRegistry;
import org.opendaylight.protocol.bgp.rib.spi.BGPSessionListener;
import org.opendaylight.protocol.framework.SessionListenerFactory;
import org.opendaylight.protocol.framework.SessionNegotiator;
import org.opendaylight.protocol.framework.SessionNegotiatorFactory;
import org.opendaylight.yangtools.yang.binding.Notification;

public final class BGPSessionNegotiatorFactory implements SessionNegotiatorFactory<Notification, BGPSessionImpl, BGPSessionListener> {
    private final BGPPeerRegistry peerRegistry;
    private final boolean inbound;

    public BGPSessionNegotiatorFactory(final BGPPeerRegistry peerRegistry, final boolean inbound) {
        this.peerRegistry = peerRegistry;
        this.inbound = inbound;
    }

    @Override
    public SessionNegotiator<BGPSessionImpl> getSessionNegotiator(final SessionListenerFactory<BGPSessionListener> factory,
            final Channel channel, final Promise<BGPSessionImpl> promise) {
        return new BGPSessionNegotiator(promise, channel, peerRegistry, inbound);
    }
}
