/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.framework;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractProtocolSession<M> extends SimpleChannelInboundHandler<Object> implements ProtocolSession<M> {
	private final static Logger logger = LoggerFactory.getLogger(AbstractProtocolSession.class);

	/**
	 * Handles incoming message (parsing, reacting if necessary).
	 * 
	 * @param msg incoming message
	 */
	protected abstract void handleMessage(final M msg);

	/**
	 * Called when reached the end of input stream while reading.
	 */
	protected abstract void endOfInput();

	/**
	 * Called when the session is added to the pipeline.
	 */
	protected abstract void sessionUp();

	@Override
	final public void channelInactive(final ChannelHandlerContext ctx) throws Exception {
		logger.debug("Channel inactive.");
		endOfInput();
	}

	@Override
	final protected void channelRead0(final ChannelHandlerContext ctx, final Object msg) throws Exception {
		logger.debug("Message was received: {}", msg);
		handleMessage((M)msg);
	}

	@Override
	final public void handlerAdded(final ChannelHandlerContext ctx) {
		sessionUp();
	}
}
