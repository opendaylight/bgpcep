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

import java.util.Arrays;

final class ProtocolSessionInboundHandler extends SimpleChannelInboundHandler<ProtocolMessage> {

	private final ProtocolSession session;

	public ProtocolSessionInboundHandler(final ProtocolSession session) {
		this.session = session;
	}

	@Override
	public void channelActive(final ChannelHandlerContext ctx) throws Exception {
		this.session.startSession();
	}

	@Override
	public void channelInactive(final ChannelHandlerContext ctx) throws Exception {
		this.session.close();
	}

	@Override
	protected void channelRead0(final ChannelHandlerContext ctx, final ProtocolMessage msg) throws Exception {
		System.out.println("out: " + Arrays.toString(ctx.channel().pipeline().names().toArray()));
		System.out.println("Something was received: " + msg);
		this.session.handleMessage(msg);
	}

	public ProtocolSession getSession() {
		return this.session;
	}
}
