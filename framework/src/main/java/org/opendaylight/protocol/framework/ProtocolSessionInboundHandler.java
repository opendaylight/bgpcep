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

final class ProtocolSessionInboundHandler extends SimpleChannelInboundHandler<ProtocolMessage> {

	private final static Logger logger = LoggerFactory.getLogger(ProtocolSessionInboundHandler.class);

	private final ProtocolSession session;

	public ProtocolSessionInboundHandler(final ProtocolSession session) {
		this.session = session;
	}

	@Override
	public void channelActive(final ChannelHandlerContext ctx) throws Exception {
		logger.debug("Channel active.");
		this.session.startSession();
	}

	@Override
	public void channelInactive(final ChannelHandlerContext ctx) throws Exception {
		logger.debug("Channel inactive.");
		this.session.endOfInput();
	}

	@Override
	protected void channelRead0(final ChannelHandlerContext ctx, final ProtocolMessage msg) throws Exception {
		logger.debug("Message was received: {}", msg);
		this.session.handleMessage(msg);
	}

	public ProtocolSession getSession() {
		return this.session;
	}
}
