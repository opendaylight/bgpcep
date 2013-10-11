/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message.open;

import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.impl.SimpleAddressFamilyRegistry;
import org.opendaylight.protocol.bgp.parser.impl.SimpleSubsequentAddressFamilyRegistry;
import org.opendaylight.protocol.bgp.parser.spi.CapabilityParser;
import org.opendaylight.protocol.bgp.parser.spi.CapabilityRegistry;
import org.opendaylight.protocol.bgp.parser.spi.CapabilitySerializer;
import org.opendaylight.protocol.concepts.HandlerRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.open.bgp.parameters.CParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.open.bgp.parameters.c.parameters.CAs4Bytes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.open.bgp.parameters.c.parameters.CMultiprotocol;
import org.opendaylight.yangtools.yang.binding.DataContainer;

import com.google.common.base.Preconditions;

public final class SimpleCapabilityRegistry implements CapabilityRegistry {
	public static final CapabilityRegistry INSTANCE;

	static {
		final SimpleCapabilityRegistry reg = new SimpleCapabilityRegistry();

		final MultiProtocolCapabilityHandler multi =
				new MultiProtocolCapabilityHandler(iSimpleAddressFamilyRegistry.INSTANCE, SimpleSubsequentAddressFamilyRegistry.INSTANCE);
		reg.registerCapabilityParser(MultiProtocolCapabilityHandler.CODE, multi);
		reg.registerCapabilitySerializer(CMultiprotocol.class, multi);

		final As4CapabilityHandler as4 = new As4CapabilityHandler();
		reg.registerCapabilityParser(As4CapabilityHandler.CODE, as4);
		reg.registerCapabilitySerializer(CAs4Bytes.class, as4);

		INSTANCE = reg;
	}

	private final HandlerRegistry<DataContainer, CapabilityParser, CapabilitySerializer> handlers = new HandlerRegistry<>();

	@Override
	public AutoCloseable registerCapabilityParser(final int messageType, final CapabilityParser parser) {
		Preconditions.checkArgument(messageType >= 0 && messageType <= 255);
		return handlers.registerParser(messageType, parser);
	}

	@Override
	public AutoCloseable registerCapabilitySerializer(final Class<? extends CParameters> paramClass, final CapabilitySerializer serializer) {
		return handlers.registerSerializer(paramClass, serializer);
	}

	@Override
	public CParameters parseCapability(final int type, final byte[] bytes) throws BGPDocumentedException, BGPParsingException {
		final CapabilityParser parser = handlers.getParser(type);
		if (parser == null) {
			return null;
		}

		return parser.parseCapability(bytes);
	}

	@Override
	public byte[] serializeCapability(final CParameters capability) {
		final CapabilitySerializer serializer = handlers.getSerializer(capability.getImplementedInterface());
		if (serializer == null) {
			return null;
		}

		return serializer.serializeCapability(capability);
	}
}
