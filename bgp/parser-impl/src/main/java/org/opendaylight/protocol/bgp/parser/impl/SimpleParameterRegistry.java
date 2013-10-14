/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl;

import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.ParameterParser;
import org.opendaylight.protocol.bgp.parser.spi.ParameterRegistry;
import org.opendaylight.protocol.bgp.parser.spi.ParameterSerializer;
import org.opendaylight.protocol.concepts.HandlerRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.open.BgpParameters;
import org.opendaylight.yangtools.yang.binding.DataContainer;

import com.google.common.base.Preconditions;

public final class SimpleParameterRegistry implements ParameterRegistry {
	private static final class Holder {
		private static final ParameterRegistry INSTANCE = new SimpleParameterRegistry();
	}

	private final HandlerRegistry<DataContainer, ParameterParser, ParameterSerializer> handlers = new HandlerRegistry<>();

	private SimpleParameterRegistry() {

	}

	public static ParameterRegistry getInstance() {
		return Holder.INSTANCE;
	}

	@Override
	public AutoCloseable registerParameterParser(final int messageType, final ParameterParser parser) {
		Preconditions.checkArgument(messageType >= 0 && messageType <= 255);
		return handlers.registerParser(messageType, parser);
	}

	@Override
	public AutoCloseable registerParameterSerializer(final Class<? extends BgpParameters> paramClass, final ParameterSerializer serializer) {
		return handlers.registerSerializer(paramClass, serializer);
	}

	@Override
	public BgpParameters parseParameter(final int parameterType, final byte[] bytes) throws BGPParsingException, BGPDocumentedException {
		final ParameterParser parser = handlers.getParser(parameterType);
		if (parser == null) {
			return null;
		}

		return parser.parseParameter(bytes);
	}

	@Override
	public byte[] serializeParameter(final BgpParameters parameter) {
		final ParameterSerializer serializer = handlers.getSerializer(parameter.getImplementedInterface());
		if (serializer == null) {
			return null;
		}

		return serializer.serializeParameter(parameter);
	}
}
