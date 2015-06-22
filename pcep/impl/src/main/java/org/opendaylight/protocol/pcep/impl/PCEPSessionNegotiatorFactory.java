/*
 *
 *  * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.protocol.pcep.impl;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInboundHandler;
import io.netty.util.concurrent.Promise;
import org.opendaylight.protocol.pcep.PCEPSessionListenerFactory;

/**
 * Created by cgasparini on 22.6.2015.
 */
public interface PCEPSessionNegotiatorFactory {
    /**
     * Create a new negotiator attached to a channel, which will notify
     * a promise once the negotiation completes.
     *
     * @param channel Underlying channel
     * @param promise Promise to be notified
     * @return new negotiator instance
     */
    <T extends ChannelInboundHandler> T getSessionNegotiator(PCEPSessionListenerFactory factory, Channel channel, Promise<PCEPSessionImpl> promise);
}
