/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOutboundHandler;

import org.opendaylight.protocol.bgp.parser.BGPMessageFactory;

import com.google.common.base.Preconditions;

/**
 * BGP specific factory for protocol inbound/outbound handlers.
 */
public class BGPHandlerFactory  {
	private final ChannelOutboundHandler encoder;
	private final BGPMessageFactory msgFactory;

	public BGPHandlerFactory(final BGPMessageFactory msgFactory) {
		this.msgFactory = Preconditions.checkNotNull(msgFactory);
		this.encoder = new BGPMessageToByteEncoder(msgFactory);
	}

	public ChannelHandler[] getEncoders() {
		return new ChannelHandler[] {
				this.encoder,
		};
	}

	public ChannelHandler[] getDecoders() {
		return new ChannelHandler[] {
				new BGPMessageHeaderDecoder(),
				new BGPByteToMessageDecoder(this.msgFactory),
		};
	}
}
