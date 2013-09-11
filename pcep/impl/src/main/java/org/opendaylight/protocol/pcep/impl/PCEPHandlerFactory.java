/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import io.netty.channel.ChannelHandler;

import org.opendaylight.protocol.framework.ProtocolHandlerFactory;
import org.opendaylight.protocol.framework.ProtocolMessageDecoder;
import org.opendaylight.protocol.framework.ProtocolMessageEncoder;
import org.opendaylight.protocol.pcep.PCEPMessage;

public class PCEPHandlerFactory extends ProtocolHandlerFactory<PCEPMessage> {
	private final ProtocolMessageEncoder<PCEPMessage> encoder;
	final PCEPMessageFactory msgFactory;

	public PCEPHandlerFactory() {
		super(new PCEPMessageFactory());
		this.msgFactory = new PCEPMessageFactory();
		this.encoder = new ProtocolMessageEncoder<PCEPMessage>(this.msgFactory);
	}

	@Override
	public ChannelHandler[] getEncoders() {
		return new ChannelHandler[] { this.encoder };
	}

	@Override
	public ChannelHandler[] getDecoders() {
		return new ChannelHandler[] { new PCEPMessageHeaderDecoder(), new ProtocolMessageDecoder<PCEPMessage>(this.msgFactory) };
	}
}
