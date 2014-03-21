/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOutboundHandler;

import org.opendaylight.protocol.pcep.spi.MessageRegistry;

import com.google.common.base.Preconditions;

/**
 * PCEP specific factory for protocol inbound/outbound handlers.
 */
public final class PCEPHandlerFactory {
	private final MessageRegistry registry;
	private final ChannelOutboundHandler encoder;

	public PCEPHandlerFactory(final MessageRegistry registry) {
		this.registry = Preconditions.checkNotNull(registry);
		this.encoder = new PCEPMessageToByteEncoder(registry);
	}

	public ChannelHandler[] getEncoders() {
		return new ChannelHandler[] { this.encoder };
	}

	public ChannelHandler[] getDecoders() {
		return new ChannelHandler[] {
				new PCEPMessageHeaderDecoder(),
				new PCEPByteToMessageDecoder(registry),
		};
	}
}
