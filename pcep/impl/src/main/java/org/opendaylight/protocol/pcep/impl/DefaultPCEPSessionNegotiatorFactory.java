/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import com.google.common.base.Preconditions;
import io.netty.channel.Channel;
import io.netty.util.concurrent.Promise;
import org.opendaylight.controller.config.yang.pcep.impl.Tls;
import org.opendaylight.protocol.pcep.PCEPSessionListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.Open;

public final class DefaultPCEPSessionNegotiatorFactory extends AbstractPCEPSessionNegotiatorFactory {
    private final Open localPrefs;
    private final int maxUnknownMessages;
    private final Tls tlsConfiguration;

    public DefaultPCEPSessionNegotiatorFactory(final Open localPrefs, final int maxUnknownMessages) {
        this(localPrefs, maxUnknownMessages, null);
    }

    public DefaultPCEPSessionNegotiatorFactory(final Open localPrefs, final int maxUnknownMessages, final Tls tlsConfiguration) {
        this.localPrefs = Preconditions.checkNotNull(localPrefs);
        this.maxUnknownMessages = maxUnknownMessages;
        this.tlsConfiguration = tlsConfiguration;
    }

    @Override
    protected AbstractPCEPSessionNegotiator createNegotiator(final Promise<PCEPSessionImpl> promise, final PCEPSessionListener listener,
            final Channel channel, final short sessionId) {
        return new DefaultPCEPSessionNegotiator(promise, channel, listener, sessionId, this.maxUnknownMessages, this.localPrefs, this.tlsConfiguration);
    }
}
