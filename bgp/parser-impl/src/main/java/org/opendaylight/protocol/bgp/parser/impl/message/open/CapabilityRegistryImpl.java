/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message.open;

import org.opendaylight.protocol.bgp.parser.impl.AbstractRegistryImpl;
import org.opendaylight.protocol.bgp.parser.spi.CapabilityParser;
import org.opendaylight.protocol.bgp.parser.spi.CapabilityRegistry;
import org.opendaylight.protocol.bgp.parser.spi.CapabilitySerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.open.bgp.parameters.CParameters;

import com.google.common.base.Preconditions;

public final class CapabilityRegistryImpl extends AbstractRegistryImpl<CParameters, CapabilityParser, CapabilitySerializer> implements CapabilityRegistry {
	public static final CapabilityRegistry INSTANCE;

	static {
		final CapabilityRegistry reg = new CapabilityRegistryImpl();

		// FIXME: fix registry

		INSTANCE = reg;
	}

	@Override
	public AutoCloseable registerCapabilityParser(final int messageType, final CapabilityParser parser) {
		Preconditions.checkArgument(messageType >= 0 && messageType <= 255);
		return super.registerParser(messageType, parser);
	}

	@Override
	public CapabilityParser getCapabilityParser(final int messageType) {
		return super.getParser(messageType);
	}

	@Override
	public AutoCloseable registerCapabilitySerializer(final Class<? extends CParameters> paramClass, final CapabilitySerializer serializer) {
		return super.registerSerializer(paramClass, serializer);
	}

	@Override
	public CapabilitySerializer getCapabilitySerializer(final CParameters capability) {
		return super.getSerializer(capability);
	}
}
