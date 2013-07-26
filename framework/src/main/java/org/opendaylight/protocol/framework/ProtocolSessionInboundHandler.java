/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.framework;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.MessageList;

final class ProtocolSessionInboundHandler extends ChannelInboundHandlerAdapter {

	private final ProtocolSession session;

	public ProtocolSessionInboundHandler(final ProtocolSession session) {
		this.session = session;
	}

	@Override
	public void messageReceived(final ChannelHandlerContext ctx, final MessageList<Object> msgs) {
		final MessageList<ProtocolMessage> pmsgs = msgs.cast();
		for (final ProtocolMessage msg : pmsgs) {
			this.session.handleMessage(msg);
		}
	}
}
