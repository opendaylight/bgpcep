/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.base.Preconditions;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInboundHandler;
import io.netty.util.concurrent.Promise;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPPeerRegistry;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionValidator;
import org.opendaylight.protocol.bgp.rib.spi.BGPSessionListenerFactory;

public final class BGPServerSessionNegotiatorFactory {
    private final BGPSessionValidator validator;
    private final BGPPeerRegistry registry;

    public BGPServerSessionNegotiatorFactory(final BGPSessionValidator sessionValidator, final BGPPeerRegistry registry) {
        this.registry = registry;
        this.validator = Preconditions.checkNotNull(sessionValidator);
    }

    public <T extends ChannelInboundHandler> T getSessionNegotiator(final BGPSessionListenerFactory factory,
            final Channel channel, final Promise<BGPSessionImpl> promise) {
        return (T) new BGPServerSessionNegotiator(promise, channel, registry, validator);
    }
}
