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

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.concurrent.GuardedBy;

import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPError;
import org.opendaylight.protocol.bgp.parser.BGPSessionListener;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionPreferences;
import org.opendaylight.protocol.framework.AbstractSessionNegotiator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.LinkstateAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.LinkstateSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.Keepalive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.KeepaliveBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.Notify;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.NotifyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.open.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.open.bgp.parameters.CParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.open.bgp.parameters.c.parameters.CMultiprotocol;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

public final class BGPSessionNegotiator extends AbstractSessionNegotiator<Notification, BGPSessionImpl> {
	// 4 minutes recommended in http://tools.ietf.org/html/rfc4271#section-8.2.2
	// FIXME to actual value
	protected static final int INITIAL_HOLDTIMER = 1;

	@VisibleForTesting
	public enum State {
		/**
		 * Negotiation has not started yet.
		 */
		Idle,
		/**
		 * We have sent our Open message, and are waiting for the peer's Open message.
		 */
		OpenSent,
		/**
		 * We have received the peer's Open message, which is acceptable, and we're waiting the acknowledgement of our
		 * Open message.
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

	@GuardedBy("this")
	private Open remotePref;

	@GuardedBy("this")
	private State state = State.Idle;

	@GuardedBy("this")
	private BGPSessionImpl session;

	public BGPSessionNegotiator(final Timer timer, final Promise<BGPSessionImpl> promise, final Channel channel,
			final BGPSessionPreferences initialPrefs, final BGPSessionListener listener) {
		super(promise, channel);
		this.listener = Preconditions.checkNotNull(listener);
		this.localPref = Preconditions.checkNotNull(initialPrefs);
		this.timer = Preconditions.checkNotNull(timer);
	}

	@Override
	protected void startNegotiation() {
		Preconditions.checkState(this.state == State.Idle);
		this.channel.writeAndFlush(new OpenBuilder().setMyAsNumber(this.localPref.getMyAs()).setHoldTimer(this.localPref.getHoldTime()).setBgpIdentifier(
				this.localPref.getBgpId()).setBgpParameters(this.localPref.getParams()).build());
		this.state = State.OpenSent;

		final Object lock = this;
		this.timer.newTimeout(new TimerTask() {
			@Override
			public void run(final Timeout timeout) throws Exception {
				synchronized (lock) {
					if (BGPSessionNegotiator.this.state != State.Finished) {
						negotiationFailed(new BGPDocumentedException("HoldTimer expired", BGPError.FSM_ERROR));
						BGPSessionNegotiator.this.channel.writeAndFlush(new NotifyBuilder().setErrorCode(
								BGPError.HOLD_TIMER_EXPIRED.getCode()).setErrorSubcode(BGPError.HOLD_TIMER_EXPIRED.getSubcode()).build());
						BGPSessionNegotiator.this.state = State.Finished;
					}
				}
			}
		}, INITIAL_HOLDTIMER, TimeUnit.MINUTES);
	}

	@Override
	protected synchronized void handleMessage(final Notification msg) {
		logger.debug("Channel {} handling message in state {}", this.channel, this.state);

		switch (this.state) {
		case Finished:
		case Idle:
			final Notify fsmError = new NotifyBuilder().setErrorCode(BGPError.FSM_ERROR.getCode()).setErrorSubcode(
					BGPError.FSM_ERROR.getSubcode()).build();
			this.channel.writeAndFlush(fsmError);
		case OpenConfirm:
			if (msg instanceof Keepalive) {
				negotiationSuccessful(this.session);
			} else if (msg instanceof Notify) {
				final Notify ntf = (Notify) msg;
				negotiationFailed(new BGPDocumentedException("Peer refusal", BGPError.forValue(ntf.getErrorCode(), ntf.getErrorSubcode())));
			}
			this.state = State.Finished;
			return;
		case OpenSent:
			if (msg instanceof Open) {
				final Open openObj = (Open) msg;

				final List<BgpParameters> prefs = openObj.getBgpParameters();
				if (prefs != null && !prefs.isEmpty()) {
					for (final BgpParameters param : openObj.getBgpParameters()) {
						final CParameters cap = param.getCParameters();
						if (cap instanceof CMultiprotocol) {
							if (((CMultiprotocol) cap).getMultiprotocolCapability().getAfi() == LinkstateAddressFamily.class
									&& ((CMultiprotocol) cap).getMultiprotocolCapability().getSafi() == LinkstateSubsequentAddressFamily.class) {
								this.remotePref = openObj;
								this.channel.writeAndFlush(new KeepaliveBuilder().build());
								this.session = new BGPSessionImpl(this.timer, this.listener, this.channel, this.remotePref);
								this.state = State.OpenConfirm;
								logger.debug("Channel {} moved to OpenConfirm state with remote proposal {}", this.channel, this.remotePref);
								return;
							}
						}
					}
				}
				final Notify ntf = new NotifyBuilder().setErrorCode(BGPError.UNSPECIFIC_OPEN_ERROR.getCode()).setErrorSubcode(
						BGPError.UNSPECIFIC_OPEN_ERROR.getSubcode()).build();
				this.channel.writeAndFlush(ntf);
				negotiationFailed(new BGPDocumentedException("Linkstate capability is not configured on router. Check the configuration of BGP speaker.", BGPError.forValue(
						ntf.getErrorCode(), ntf.getErrorSubcode())));
				this.state = State.Finished;
				return;
			}
			break;
		}

		// Catch-all for unexpected message
		logger.warn("Channel {} state {} unexpected message {}", this.channel, this.state, msg);
		this.channel.writeAndFlush(new NotifyBuilder().setErrorCode(BGPError.FSM_ERROR.getCode()).setErrorSubcode(
				BGPError.FSM_ERROR.getSubcode()).build());
		negotiationFailed(new BGPDocumentedException("Unexpected message", BGPError.FSM_ERROR));
		this.state = State.Finished;
	}

	public synchronized State getState() {
		return this.state;
	}
}
