/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import io.netty.channel.Channel;
import io.netty.util.concurrent.Promise;
import org.opendaylight.protocol.framework.SessionListenerFactory;
import org.opendaylight.protocol.framework.SessionNegotiator;
import org.opendaylight.protocol.framework.SessionNegotiatorFactory;
import org.opendaylight.protocol.pcep.PCEPSessionListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SessionNegotiator which takes care of making sure sessions between PCEP peers are kept unique. This needs to be
 * further subclassed to provide either a client or server factory.
 */
public abstract class AbstractPCEPSessionNegotiatorFactory implements
        SessionNegotiatorFactory<Message, PCEPSessionImpl, PCEPSessionListener> {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractPCEPSessionNegotiatorFactory.class);

    private PCEPPeerRegistry sessionRegistry = new PCEPPeerRegistry();

    /**
     * Create a new negotiator. This method needs to be implemented by subclasses to actually provide a negotiator.
     *
     * @param promise Session promise to be completed by the negotiator
     * @param channel Associated channel
     * @param sessionId Session ID assigned to the resulting session
     * @return a PCEP session negotiator
     */
    protected abstract AbstractPCEPSessionNegotiator createNegotiator(Promise<PCEPSessionImpl> promise, PCEPSessionListener listener,
            Channel channel, short sessionId);

    @Override
    public final SessionNegotiator<PCEPSessionImpl> getSessionNegotiator(final SessionListenerFactory<PCEPSessionListener> factory,
            final Channel channel, final Promise<PCEPSessionImpl> promise) {

        LOG.debug("Instantiating bootstrap negotiator for channel {}", channel);
        return new PCEPSessionNegotiator(channel, promise, factory, this);
    }

    public PCEPPeerRegistry getSessionRegistry() {
        return this.sessionRegistry;
    }
}
