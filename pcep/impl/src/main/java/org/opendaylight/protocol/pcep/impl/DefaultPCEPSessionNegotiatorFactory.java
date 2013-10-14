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

public final class DefaultPCEPSessionNegotiatorFactory extends AbstractPCEPSessionNegotiatorFactory {
	private final PCEPOpenObject localPrefs;
	private final int maxUnknownMessages;
	private final Timer timer;

	public DefaultPCEPSessionNegotiatorFactory(final Timer timer, final PCEPOpenObject localPrefs,
			final int maxUnknownMessages) {
		this.timer = Preconditions.checkNotNull(timer);
		this.localPrefs = Preconditions.checkNotNull(localPrefs);
		this.maxUnknownMessages = maxUnknownMessages;
	}

	@Override
	protected AbstractPCEPSessionNegotiator createNegotiator(final Promise<PCEPSessionImpl> promise,
			final PCEPSessionListener listener , final Channel channel, final short sessionId) {
		return new DefaultPCEPSessionNegotiator(timer, promise, channel, listener, sessionId, maxUnknownMessages, localPrefs);
	}
}
