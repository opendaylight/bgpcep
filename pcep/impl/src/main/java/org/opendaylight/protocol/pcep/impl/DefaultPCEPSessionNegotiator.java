/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

import io.netty.channel.Channel;
import io.netty.util.Timer;
import io.netty.util.concurrent.Promise;

import org.opendaylight.protocol.pcep.PCEPSessionListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.OpenBuilder;

public final class DefaultPCEPSessionNegotiator extends AbstractPCEPSessionNegotiator {
    private final PCEPSessionListener listener;
    private final int maxUnknownMessages;

    public DefaultPCEPSessionNegotiator(final Timer timer, final Promise<PCEPSessionImpl> promise, final Channel channel,
            final PCEPSessionListener listener, final short sessionId, final int maxUnknownMessages, final Open localPrefs) {
        super(timer, promise, channel);
        this.maxUnknownMessages = maxUnknownMessages;
        this.myLocalPrefs = new OpenBuilder().setKeepalive(localPrefs.getKeepalive()).setDeadTimer(localPrefs.getDeadTimer()).setSessionId(
                sessionId).setTlvs(localPrefs.getTlvs()).build();
        this.listener = Preconditions.checkNotNull(listener);
    }

    private final Open myLocalPrefs;

    @Override
    protected Open getInitialProposal() {
        return this.myLocalPrefs;
    }

    @Override
    @VisibleForTesting
    public PCEPSessionImpl createSession(final Timer timer, final Channel channel, final Open localPrefs, final Open remotePrefs) {
        return new PCEPSessionImpl(timer, this.listener, this.maxUnknownMessages, channel, localPrefs, remotePrefs);
    }

    @Override
    protected boolean isProposalAcceptable(final Open open) {
        return true;
    }

    @Override
    protected Open getCounterProposal(final Open open) {
        return null;
    }

    @Override
    protected Open getRevisedProposal(final Open suggestion) {
        return this.myLocalPrefs;
    }
}
