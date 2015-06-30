/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.impl;

import io.netty.channel.Channel;
import io.netty.util.concurrent.Promise;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPPeerRegistry;

/**
 * Server negotiator. Validates established connections using BGPServerSessionValidator
 */
public final class BGPSessionNegotiator extends AbstractBGPSessionNegotiator {

    public BGPSessionNegotiator(final Promise<BGPSessionImpl> promise, final Channel channel,
            final BGPPeerRegistry registry, final boolean inbound) {
        super(promise, channel, registry, inbound);
    }
}
