/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.framework;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;

public final class ProtocolSessionOutboundHandler extends ChannelOutboundHandlerAdapter {

	@Override
	public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
		// TODO:
		cause.printStackTrace();
		ctx.close();
	}

	public void writeDown(final ChannelHandlerContext ctx, final ProtocolMessage msg) throws Exception {
		this.write(ctx, msg, ctx.newPromise());
	}
}
