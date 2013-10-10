/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message.update;

import org.opendaylight.protocol.bgp.parser.spi.AttributeParser;
import org.opendaylight.protocol.bgp.parser.spi.AttributeRegistry;
import org.opendaylight.protocol.bgp.parser.spi.AttributeSerializer;
import org.opendaylight.protocol.concepts.HandlerRegistry;
import org.opendaylight.yangtools.yang.binding.DataObject;

import com.google.common.base.Preconditions;

public final class AttributeRegistryImpl extends HandlerRegistry<DataObject, AttributeParser, AttributeSerializer>
implements AttributeRegistry {
	public static final AttributeRegistry INSTANCE;

	static {
		final AttributeRegistry reg = new AttributeRegistryImpl();

		// FIXME: fix registry

		INSTANCE = reg;
	}

	@Override
	public AutoCloseable registerAttributeParser(final int messageType, final AttributeParser parser) {
		Preconditions.checkArgument(messageType >= 0 && messageType <= 255);
		return super.registerParser(messageType, parser);
	}

	@Override
	public AttributeParser getAttributeParser(final int messageType) {
		return super.getParser(messageType);
	}

	@Override
	public AutoCloseable registerAttributeSerializer(final Class<? extends DataObject> paramClass, final AttributeSerializer serializer) {
		return super.registerSerializer(paramClass, serializer);
	}

	@Override
	public AttributeSerializer getAttributeSerializer(final DataObject attribute) {
		return super.getSerializer(attribute);
	}
}
