/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.netconf.util;

import java.util.concurrent.TimeUnit;

import org.opendaylight.netconf.api.NetconfMessage;
import org.opendaylight.netconf.api.NetconfSession;
import org.opendaylight.netconf.api.NetconfSessionPreferences;
import org.opendaylight.netconf.util.xml.XMLUtil;
import org.opendaylight.netconf.util.xml.Xml;
import org.opendaylight.netconf.util.xml.XmlElement;
import org.opendaylight.protocol.framework.AbstractSessionNegotiator;
import org.opendaylight.protocol.framework.SessionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

import io.netty.channel.Channel;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.Promise;

public abstract class NetconfSessionNegotiator<P extends NetconfSessionPreferences, S extends NetconfSession> extends
		AbstractSessionNegotiator<NetconfMessage, S> {

	// TODO what time ?
	private static final long INITIAL_HOLDTIMER = 1;
	private static final Logger logger = LoggerFactory.getLogger(NetconfSessionNegotiator.class);

	protected final P sessionPreferences;

	private final SessionListener sessionListener;

	/**
	 * Possible states for Finite State Machine
	 */
	private enum State {
		IDLE, OPEN_WAIT, FAILED, ESTABLISHED
	}

	private State state = State.IDLE;
	private final Timer timer;

	protected NetconfSessionNegotiator(P sessionPreferences, Promise<S> promise, Channel channel, Timer timer,
			SessionListener sessionListener) {
		super(promise, channel);
		this.sessionPreferences = sessionPreferences;
		this.timer = timer;
		this.sessionListener = sessionListener;
	}

	@Override
	protected void startNegotiation() throws Exception {
		final Optional<SslHandler> sslHandler = getSslHandler(channel);
		if (sslHandler.isPresent()) {
			Future<Channel> future = sslHandler.get().handshakeFuture();
			future.addListener(new GenericFutureListener<Future<? super Channel>>() {
				@Override
				public void operationComplete(Future<? super Channel> future) throws Exception {
					Preconditions.checkState(future.isSuccess(), "Ssl handshake was not successful");
					logger.debug("Ssl handshake complete");
					start();
				}
			});
		} else
			start();
	}

	private static Optional<SslHandler> getSslHandler(Channel channel) {
		final SslHandler sslHandler = channel.pipeline().get(SslHandler.class);
		return sslHandler == null ? Optional.<SslHandler> absent() : Optional.of(sslHandler);
	}

	private void start() {
		final NetconfMessage helloMessage = this.sessionPreferences.getHelloMessage();
		logger.debug("Session negotiation started with hello message {}", Xml.toString(helloMessage.getDocument()));

		sendMessage(helloMessage);
		changeState(State.OPEN_WAIT);

		this.timer.newTimeout(new TimerTask() {
			@Override
			public void run(final Timeout timeout) throws Exception {
				synchronized (this) {
					if (state != State.ESTABLISHED) {
						final IllegalStateException cause = new IllegalStateException("Session was not established after " + timeout);
						negotiationFailed(cause);
						changeState(State.FAILED);
					}
				}
			}
		}, INITIAL_HOLDTIMER, TimeUnit.MINUTES);
	}

	private void sendMessage(NetconfMessage message) {
		this.channel.writeAndFlush(message);
	}

	@Override
	protected void handleMessage(NetconfMessage netconfMessage) {
		final Document doc = netconfMessage.getDocument();

		if (isHelloMessage(doc)) {
			changeState(State.ESTABLISHED);
			S session = getSession(sessionListener, channel, doc);
			negotiationSuccessful(session);
		} else {
			final IllegalStateException cause = new IllegalStateException("Received message was not hello as expected, but was "
					+ Xml.toString(doc));
			negotiationFailed(cause);
		}
	}

	protected abstract S getSession(SessionListener sessionListener, Channel channel, Document doc);

	private boolean isHelloMessage(Document doc) {
		try {
			XmlElement.fromDomElementWithExpected(doc.getDocumentElement(), "hello", XMLUtil.URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0);

		} catch (IllegalArgumentException | IllegalStateException e) {
			return false;
		}
		return true;
	}

	private void changeState(final State newState) {
		logger.debug("Changing state from : {} to : {}", state, newState);
		Preconditions.checkState(isStateChangePermitted(state, newState), "Cannot change state from %s to %s", state, newState);
		this.state = newState;
	}

	private static boolean isStateChangePermitted(State state, State newState) {
		if (state == State.IDLE && newState == State.OPEN_WAIT)
			return true;
		if (state == State.OPEN_WAIT && newState == State.ESTABLISHED)
			return true;
		if (state == State.OPEN_WAIT && newState == State.FAILED)
			return true;

		return false;
	}
}
