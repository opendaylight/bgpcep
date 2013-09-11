/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import io.netty.channel.ChannelHandler;

import org.opendaylight.protocol.bgp.parser.BGPMessage;
import org.opendaylight.protocol.bgp.parser.BGPMessageFactory;
import org.opendaylight.protocol.framework.ProtocolHandlerFactory;
import org.opendaylight.protocol.framework.ProtocolMessageDecoder;
import org.opendaylight.protocol.framework.ProtocolMessageEncoder;

public class BGPHandlerFactory extends ProtocolHandlerFactory<BGPMessage> {
	private final ProtocolMessageEncoder<BGPMessage> encoder;
	final BGPMessageFactory msgFactory;

	public BGPHandlerFactory(final BGPMessageFactory msgFactory) {
		super(msgFactory);
		this.msgFactory = msgFactory;
		this.encoder = new ProtocolMessageEncoder<BGPMessage>(this.msgFactory);
	}

	@Override
	public ChannelHandler[] getEncoders() {
		return new ChannelHandler[] { this.encoder };
	}

	@Override
	public ChannelHandler[] getDecoders() {
		return new ChannelHandler[] { new BGPMessageHeaderDecoder(), new ProtocolMessageDecoder<BGPMessage>(this.msgFactory) };
	}
}
