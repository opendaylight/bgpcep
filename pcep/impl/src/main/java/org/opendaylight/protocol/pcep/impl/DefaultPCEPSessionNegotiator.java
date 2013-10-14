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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.OpenObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.message.open.message.OpenBuilder;

import com.google.common.base.Preconditions;

public final class DefaultPCEPSessionNegotiator extends AbstractPCEPSessionNegotiator {
	private final PCEPSessionListener listener;
	private final int maxUnknownMessages;

	public DefaultPCEPSessionNegotiator(final Timer timer, final Promise<PCEPSessionImpl> promise, final Channel channel,
			final PCEPSessionListener listener, final short sessionId, final int maxUnknownMessages, final OpenObject localPrefs) {
		super(timer, promise, channel);
		this.maxUnknownMessages = maxUnknownMessages;
		this.myLocalPrefs = new OpenBuilder().setKeepalive(localPrefs.getKeepalive()).setDeadTimer(localPrefs.getDeadTimer()).setSessionId(
				sessionId).setTlvs(localPrefs.getTlvs()).build();
		this.listener = Preconditions.checkNotNull(listener);
	}

	private final OpenObject myLocalPrefs;

	@Override
	protected OpenObject getInitialProposal() {
		return this.myLocalPrefs;
	}

	@Override
	protected PCEPSessionImpl createSession(final Timer timer, final Channel channel, final OpenObject localPrefs,
			final OpenObject remotePrefs) {
		return new PCEPSessionImpl(timer, this.listener, this.maxUnknownMessages, channel, localPrefs, remotePrefs);
	}

	@Override
	protected boolean isProposalAcceptable(final OpenObject open) {
		return true;
	}

	@Override
	protected OpenObject getCounterProposal(final OpenObject open) {
		return null;
	}

	@Override
	protected OpenObject getRevisedProposal(final OpenObject suggestion) {
		return this.myLocalPrefs;
	}
}
