/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import org.opendaylight.protocol.concepts.HandlerRegistry;
import org.opendaylight.protocol.pcep.spi.RROSubobjectHandlerRegistry;
import org.opendaylight.protocol.pcep.spi.RROSubobjectParser;
import org.opendaylight.protocol.pcep.spi.RROSubobjectSerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.reported.route.object.Subobjects;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.CSubobject;
import org.opendaylight.yangtools.yang.binding.DataContainer;

import com.google.common.base.Preconditions;

public final class SimpleRROSubobjectHandlerFactory implements RROSubobjectHandlerRegistry {
	private final HandlerRegistry<DataContainer, RROSubobjectParser, RROSubobjectSerializer> handlers = new HandlerRegistry<>();

	@Override
	public AutoCloseable registerSubobjectParser(final int subobjectType, final RROSubobjectParser parser) {
		Preconditions.checkArgument(subobjectType >= 0 && subobjectType <= 65535);
		return this.handlers.registerParser(subobjectType, parser);
	}

	@Override
	public RROSubobjectParser getSubobjectParser(final int subobjectType) {
		Preconditions.checkArgument(subobjectType >= 0 && subobjectType <= 65535);
		return this.handlers.getParser(subobjectType);
	}

	@Override
	public AutoCloseable registerSubobjectSerializer(final Class<? extends CSubobject> subobjectClass,
			final RROSubobjectSerializer serializer) {
		return this.handlers.registerSerializer(subobjectClass, serializer);
	}

	@Override
	public RROSubobjectSerializer getSubobjectSerializer(final Subobjects subobject) {
		return this.handlers.getSerializer(subobject.getImplementedInterface());
	}
}
