/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import io.netty.channel.Channel;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import io.netty.util.concurrent.Promise;

import java.util.concurrent.TimeUnit;

import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPError;
import org.opendaylight.protocol.bgp.parser.BGPMessage;
import org.opendaylight.protocol.bgp.parser.BGPSessionListener;
import org.opendaylight.protocol.bgp.parser.message.BGPKeepAliveMessage;
import org.opendaylight.protocol.bgp.parser.message.BGPNotificationMessage;
import org.opendaylight.protocol.bgp.parser.message.BGPOpenMessage;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionPreferences;
import org.opendaylight.protocol.framework.AbstractSessionNegotiator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

final class BGPSessionNegotiator extends AbstractSessionNegotiator<BGPMessage, BGPSessionImpl> {
	// 4 minutes recommended in http://tools.ietf.org/html/rfc4271#section-8.2.2
	private static final int INITIAL_HOLDTIMER = 4;

	private enum State {
		/**
		 * Negotiation has not started yet.
		 */
		Idle,
		/**
		 * We have sent our Open message, and are waiting for the peer's Open
		 * message.
		 */
		OpenSent,
		/**
		 * We have received the peer's Open message, which is acceptable, and
		 * we're waiting the acknowledgement of our Open message.
		 */
		OpenConfirm,
		/**
		 * The negotiation finished.
		 */
		Finished,
	}

	private static final Logger logger = LoggerFactory.getLogger(BGPSessionNegotiator.class);
	private final BGPSessionListener listener;
	private final Timer timer;
	private final BGPSessionPreferences localPref;
	private BGPOpenMessage remotePref;
	private State state = State.Idle;
	private final short keepAlive = 15;

	BGPSessionNegotiator(final Timer timer, final Promise<BGPSessionImpl> promise, final Channel channel,
			final BGPSessionPreferences initialPrefs, final BGPSessionListener listener) {
		super(promise, channel);
		this.listener = Preconditions.checkNotNull(listener);
		this.localPref = Preconditions.checkNotNull(initialPrefs);
		this.timer = Preconditions.checkNotNull(timer);
	}

	@Override
	protected void startNegotiation() {
		Preconditions.checkState(state == State.Idle);
		channel.writeAndFlush(new BGPOpenMessage(localPref.getMyAs(), (short) localPref.getHoldTime(), localPref.getBgpId(), localPref.getParams()));
		state = State.OpenSent;

		final Object lock = this;
		timer.newTimeout(new TimerTask() {
			@Override
			public void run(final Timeout timeout) throws Exception {
				synchronized (lock) {
					if (state != State.Finished) {
						negotiationFailed(new BGPDocumentedException("HoldTimer expired", BGPError.FSM_ERROR));
						state = State.Finished;
					}
				}
			}
		}, INITIAL_HOLDTIMER, TimeUnit.MINUTES);
	}

	@Override
	protected synchronized void handleMessage(final BGPMessage msg) {
		logger.debug("Channel {} handling message in state {}", channel, state);

		switch (state) {
		case Finished:
		case Idle:
			throw new IllegalStateException("Unexpected state " + state);
		case OpenConfirm:
			if (msg instanceof BGPKeepAliveMessage) {
				final BGPKeepAliveMessage ka = (BGPKeepAliveMessage) msg;

				// FIXME: we miss some stuff over here

				negotiationSuccessful(new BGPSessionImpl(timer, listener, channel, keepAlive, remotePref));
				state = State.Finished;
				return;
			} else if (msg instanceof BGPNotificationMessage) {
				final BGPNotificationMessage ntf = (BGPNotificationMessage) msg;
				negotiationFailed(new BGPDocumentedException("Peer refusal", ntf.getError()));
				state = State.Finished;
				return;
			}

			break;
		case OpenSent:
			if (msg instanceof BGPOpenMessage) {
				final BGPOpenMessage open = (BGPOpenMessage) msg;

				// TODO: validate the open message

				remotePref = open;
				channel.writeAndFlush(new BGPKeepAliveMessage());
				state = State.OpenConfirm;
				logger.debug("Channel {} moved to OpenConfirm state with remote proposal {}", channel, remotePref);
				return;
			}
			break;
		}

		// Catch-all for unexpected message
		logger.warn("Channel {} state {} unexpected message {}", channel, state, msg);
		channel.writeAndFlush(new BGPNotificationMessage(BGPError.FSM_ERROR));
		negotiationFailed(new BGPDocumentedException("Unexpected message", BGPError.FSM_ERROR));
		state = State.Finished;
	}
}
