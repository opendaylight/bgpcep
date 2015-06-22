/*
 *
 *  * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.controller.config.yang.bgp.rib.impl;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInboundHandler;
import org.opendaylight.protocol.bgp.rib.impl.BGPSessionImpl;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPPeerRegistry;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionPreferences;
import org.opendaylight.protocol.bgp.rib.spi.BGPSessionListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Open;

/**
 * Created by cgasparini on 22.6.2015.
 */
public abstract class BGPSessionNegotiator extends BGPSessionImpl implements ChannelInboundHandler {
    public BGPSessionNegotiator(final BGPSessionListener listener, final Channel channel, final Open remoteOpen, final BGPSessionPreferences localPreferences, final BGPPeerRegistry peerRegistry) {
        super(listener, channel, remoteOpen, localPreferences, peerRegistry);
    }

    public BGPSessionNegotiator(final BGPSessionListener listener, final Channel channel, final Open remoteOpen, final int localHoldTimer, final BGPPeerRegistry peerRegistry) {
        super(listener, channel, remoteOpen, localHoldTimer, peerRegistry);
    }
}
