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
import org.opendaylight.protocol.bgp.parser.spi.CapabilityParser;
import org.opendaylight.protocol.bgp.parser.spi.CapabilityRegistry;
import org.opendaylight.protocol.bgp.parser.spi.CapabilitySerializer;
import org.opendaylight.protocol.concepts.HandlerRegistry;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.open.bgp.parameters.CParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.open.bgp.parameters.c.parameters.CAs4BytesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.open.bgp.parameters.c.parameters.c.as4.bytes.As4BytesCapabilityBuilder;

import com.google.common.base.Preconditions;

public final class SimpleCapabilityRegistry implements CapabilityRegistry {
	public static final CapabilityRegistry INSTANCE;

	static {
		final SimpleCapabilityRegistry reg = new SimpleCapabilityRegistry();

		reg.registerCapabilityParser(1, new CapabilityParser() {
			@Override
			public CParameters parseCapability(final byte[] bytes) throws BGPDocumentedException, BGPParsingException {
				return CapabilityParameterParser.parseMultiProtocolParameterValue(bytes);
			}
		});
		reg.registerCapabilityParser(65, new CapabilityParser() {
			@Override
			public CParameters parseCapability(final byte[] bytes) throws BGPDocumentedException, BGPParsingException {
				return new CAs4BytesBuilder().setAs4BytesCapability(
						new As4BytesCapabilityBuilder().setAsNumber(new AsNumber(ByteArray.bytesToLong(bytes))).build()).build();

			}
		});

		INSTANCE = reg;
	}

	private final HandlerRegistry<CParameters, CapabilityParser, CapabilitySerializer> handlers = new HandlerRegistry<>();

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
		final CapabilitySerializer serializer = handlers.getSerializer(capability);
		if (serializer == null) {
			return null;
		}

		return serializer.serializeCapability(capability);
	}
}
