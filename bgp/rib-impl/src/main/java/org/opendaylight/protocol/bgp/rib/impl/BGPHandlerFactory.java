/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import io.netty.channel.ChannelHandler;

import org.opendaylight.protocol.bgp.parser.BGPMessageFactory;
import org.opendaylight.protocol.framework.ProtocolHandlerFactory;
import org.opendaylight.protocol.framework.ProtocolMessageDecoder;
import org.opendaylight.protocol.framework.ProtocolMessageEncoder;
import org.opendaylight.yangtools.yang.binding.Notification;

/**
 * BGP specific factory for protocol inbound/outbound handlers.
 */
public class BGPHandlerFactory extends ProtocolHandlerFactory<Notification> {
	private final ProtocolMessageEncoder<Notification> encoder;

	public BGPHandlerFactory(final BGPMessageFactory msgFactory) {
		super(msgFactory);
		this.encoder = new ProtocolMessageEncoder<Notification>(this.msgFactory);
	}

	@Override
	public ChannelHandler[] getEncoders() {
		return new ChannelHandler[] { this.encoder };
	}

	@Override
	public ChannelHandler[] getDecoders() {
		return new ChannelHandler[] { new BGPMessageHeaderDecoder(), new ProtocolMessageDecoder<Notification>(this.msgFactory) };
	}
}
