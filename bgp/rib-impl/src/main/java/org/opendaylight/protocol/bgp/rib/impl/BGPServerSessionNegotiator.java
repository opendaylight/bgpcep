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
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionPreferences;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionValidator;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Open;

/**
 * Server negotiator. Validates established connections using BGPServerSessionValidator
 */
public final class BGPServerSessionNegotiator extends AbstractBGPSessionNegotiator {

    public BGPServerSessionNegotiator(final Promise<BGPSessionImpl> promise, final Channel channel,
            final BGPPeerRegistry registry, final BGPSessionValidator sessionValidator) {
        super(promise, channel, registry, sessionValidator);
    }

    @Override
    protected Ipv4Address getSourceId(final Open openMsg, final BGPSessionPreferences preferences) {
        return preferences.getBgpId();
    }

    @Override
    protected Ipv4Address getDestinationId(final Open openMsg, final BGPSessionPreferences preferences) {
        return openMsg.getBgpIdentifier();
    }
}
