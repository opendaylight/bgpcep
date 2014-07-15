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
import io.netty.util.Timer;
import io.netty.util.concurrent.Promise;

import org.opendaylight.protocol.bgp.parser.BGPSessionListener;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPPeerRegistry;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionValidator;
import org.opendaylight.protocol.framework.SessionListenerFactory;
import org.opendaylight.protocol.framework.SessionNegotiator;
import org.opendaylight.protocol.framework.SessionNegotiatorFactory;
import org.opendaylight.yangtools.yang.binding.Notification;

public final class BGPServerSessionNegotiatorFactory implements SessionNegotiatorFactory<Notification, BGPSessionImpl, BGPSessionListener> {
    private final Timer timer;
    private final BGPSessionValidator validator;
    private final BGPPeerRegistry registry;

    public BGPServerSessionNegotiatorFactory(final Timer timer, final BGPSessionValidator sessionValidator, final BGPPeerRegistry registry) {
        this.registry = registry;
        this.timer = Preconditions.checkNotNull(timer);
        this.validator = Preconditions.checkNotNull(sessionValidator);
    }

    @Override
    public SessionNegotiator<BGPSessionImpl> getSessionNegotiator(final SessionListenerFactory<BGPSessionListener> factory,
            final Channel channel, final Promise<BGPSessionImpl> promise) {
        return new BGPServerSessionNegotiator(this.timer, promise, channel, registry, validator);
    }
}
