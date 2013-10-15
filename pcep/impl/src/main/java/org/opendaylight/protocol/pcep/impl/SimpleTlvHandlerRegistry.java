/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import org.opendaylight.protocol.concepts.HandlerRegistry;
import org.opendaylight.protocol.pcep.spi.TlvHandlerRegistry;
import org.opendaylight.protocol.pcep.spi.TlvParser;
import org.opendaylight.protocol.pcep.spi.TlvSerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.opendaylight.yangtools.yang.binding.DataContainer;

import com.google.common.base.Preconditions;

/**
 *
 */
public final class SimpleTlvHandlerRegistry implements TlvHandlerRegistry {
	private final HandlerRegistry<DataContainer, TlvParser, TlvSerializer> handlers = new HandlerRegistry<>();

	@Override
	public AutoCloseable registerTlvParser(final int tlvType, final TlvParser parser) {
		Preconditions.checkArgument(tlvType >= 0 && tlvType < 65535);
		return handlers.registerParser(tlvType, parser);
	}

	@Override
	public TlvParser getTlvParser(final int tlvType) {
		return handlers.getParser(tlvType);
	}

	@Override
	public AutoCloseable registerTlvSerializer(final Class<? extends Tlv> tlvClass, final TlvSerializer serializer) {
		return handlers.registerSerializer(tlvClass, serializer);
	}

	@Override
	public TlvSerializer getTlvSerializer(final Tlv tlv) {
		return handlers.getSerializer(tlv.getImplementedInterface());
	}
}
