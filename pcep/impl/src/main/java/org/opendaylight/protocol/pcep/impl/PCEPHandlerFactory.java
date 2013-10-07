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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;

/**
 * PCEP specific factory for protocol inbound/outbound handlers.
 */
public class PCEPHandlerFactory extends ProtocolHandlerFactory<Message> {
	private final ProtocolMessageEncoder<Message> encoder;

	public PCEPHandlerFactory() {
		super(new PCEPMessageFactory());
		this.encoder = new ProtocolMessageEncoder<Message>(this.msgFactory);
	}

	@Override
	public ChannelHandler[] getEncoders() {
		return new ChannelHandler[] { this.encoder };
	}

	@Override
	public ChannelHandler[] getDecoders() {
		return new ChannelHandler[] { new PCEPMessageHeaderDecoder(), new ProtocolMessageDecoder<Message>(this.msgFactory) };
	}
}
