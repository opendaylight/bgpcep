/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message.open;

import org.opendaylight.protocol.bgp.parser.impl.AbstractRegistryImpl;
import org.opendaylight.protocol.bgp.parser.spi.ParameterParser;
import org.opendaylight.protocol.bgp.parser.spi.ParameterRegistry;
import org.opendaylight.protocol.bgp.parser.spi.ParameterSerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.open.BgpParameters;

import com.google.common.base.Preconditions;

public final class ParameterRegistryImpl extends AbstractRegistryImpl<BgpParameters, ParameterParser, ParameterSerializer> implements ParameterRegistry {
	public static final ParameterRegistry INSTANCE;

	static {
		final ParameterRegistry reg = new ParameterRegistryImpl();

		// FIXME: fix registry

		INSTANCE = reg;
	}

	@Override
	public AutoCloseable registerParameterParser(final int messageType, final ParameterParser parser) {
		Preconditions.checkArgument(messageType >= 0 && messageType <= 255);
		return super.registerParser(messageType, parser);
	}

	@Override
	public ParameterParser getParameterParser(final int messageType) {
		return super.getParser(messageType);
	}

	@Override
	public AutoCloseable registerParameterSerializer(final Class<? extends BgpParameters> paramClass, final ParameterSerializer serializer) {
		return super.registerSerializer(paramClass, serializer);
	}

	@Override
	public ParameterSerializer getParameterSerializer(final BgpParameters message) {
		return super.getSerializer(message);
	}
}
