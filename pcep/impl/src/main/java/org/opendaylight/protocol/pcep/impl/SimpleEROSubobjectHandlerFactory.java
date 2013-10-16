/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import org.opendaylight.protocol.concepts.HandlerRegistry;
import org.opendaylight.protocol.pcep.spi.EROSubobjectHandlerRegistry;
import org.opendaylight.protocol.pcep.spi.EROSubobjectParser;
import org.opendaylight.protocol.pcep.spi.EROSubobjectSerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.Subobjects;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.CSubobject;
import org.opendaylight.yangtools.yang.binding.DataContainer;

import com.google.common.base.Preconditions;

public final class SimpleEROSubobjectHandlerFactory implements EROSubobjectHandlerRegistry {
	private final HandlerRegistry<DataContainer, EROSubobjectParser, EROSubobjectSerializer> handlers = new HandlerRegistry<>();

	@Override
	public AutoCloseable registerSubobjectParser(final int subobjectType, final EROSubobjectParser parser) {
		Preconditions.checkArgument(subobjectType >= 0 && subobjectType <= 65535);
		return this.handlers.registerParser(subobjectType, parser);
	}

	@Override
	public EROSubobjectParser getSubobjectParser(final int subobjectType) {
		Preconditions.checkArgument(subobjectType >= 0 && subobjectType <= 65535);
		return this.handlers.getParser(subobjectType);
	}

	@Override
	public AutoCloseable registerSubobjectSerializer(final Class<? extends CSubobject> subobjectClass,
			final EROSubobjectSerializer serializer) {
		return this.handlers.registerSerializer(subobjectClass, serializer);
	}

	@Override
	public EROSubobjectSerializer getSubobjectSerializer(final Subobjects subobject) {
		return this.handlers.getSerializer(subobject.getImplementedInterface());
	}
}
