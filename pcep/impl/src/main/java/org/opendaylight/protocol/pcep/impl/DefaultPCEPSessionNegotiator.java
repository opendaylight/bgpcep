/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import io.netty.channel.Channel;
import io.netty.util.Timer;
import io.netty.util.concurrent.Promise;

import org.opendaylight.protocol.pcep.PCEPSessionListener;
import org.opendaylight.protocol.pcep.object.PCEPOpenObject;

import com.google.common.base.Preconditions;

public final class DefaultPCEPSessionNegotiator extends AbstractPCEPSessionNegotiator {
	private final PCEPSessionListener listener;
	private final int maxUnknownMessages;

	public DefaultPCEPSessionNegotiator(final Timer timer, final Promise<PCEPSessionImpl> promise, final Channel channel,
			final PCEPSessionListener listener, final short sessionId, final int maxUnknownMessages, final PCEPOpenObject localPrefs) {
		super(timer, promise, channel);
		this.maxUnknownMessages = maxUnknownMessages;
		this.myLocalPrefs = new PCEPOpenObject(localPrefs.getKeepAliveTimerValue(), localPrefs.getDeadTimerValue(), sessionId, localPrefs.getTlvs());
		this.listener = Preconditions.checkNotNull(listener);
	}

	private final PCEPOpenObject myLocalPrefs;

	@Override
	protected PCEPOpenObject getInitialProposal() {
		return myLocalPrefs;
	}

	@Override
	protected PCEPSessionImpl createSession(final Timer timer, final Channel channel, final PCEPOpenObject localPrefs, final PCEPOpenObject remotePrefs) {
		return new PCEPSessionImpl(timer, listener, maxUnknownMessages, channel, localPrefs, remotePrefs);
	}

	@Override
	protected boolean isProposalAcceptable(final PCEPOpenObject open) {
		return true;
	}

	@Override
	protected PCEPOpenObject getCounterProposal(final PCEPOpenObject open) {
		return null;
	}

	@Override
	protected PCEPOpenObject getRevisedProposal(final PCEPOpenObject suggestion) {
		return myLocalPrefs;
	}
}
