/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.spi.pojo;

import org.opendaylight.protocol.concepts.HandlerRegistry;
import org.opendaylight.protocol.pcep.spi.RROSubobjectHandlerRegistry;
import org.opendaylight.protocol.pcep.spi.RROSubobjectParser;
import org.opendaylight.protocol.pcep.spi.RROSubobjectSerializer;
import org.opendaylight.protocol.util.Util;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.record.route.subobjects.SubobjectType;
import org.opendaylight.yangtools.yang.binding.DataContainer;

import com.google.common.base.Preconditions;

public final class SimpleRROSubobjectHandlerRegistry implements RROSubobjectHandlerRegistry {
	private final HandlerRegistry<DataContainer, RROSubobjectParser, RROSubobjectSerializer> handlers = new HandlerRegistry<>();

	public AutoCloseable registerSubobjectParser(final int subobjectType, final RROSubobjectParser parser) {
		Preconditions.checkArgument(subobjectType >= 0 && subobjectType <= Util.UNSIGNED_SHORT_MAX_VALUE);
		return this.handlers.registerParser(subobjectType, parser);
	}

	@Override
	public RROSubobjectParser getSubobjectParser(final int subobjectType) {
		Preconditions.checkArgument(subobjectType >= 0 && subobjectType <= Util.UNSIGNED_SHORT_MAX_VALUE);
		return this.handlers.getParser(subobjectType);
	}

	public AutoCloseable registerSubobjectSerializer(final Class<? extends SubobjectType> subobjectClass,
			final RROSubobjectSerializer serializer) {
		return this.handlers.registerSerializer(subobjectClass, serializer);
	}

	@Override
	public RROSubobjectSerializer getSubobjectSerializer(final SubobjectType subobject) {
		return this.handlers.getSerializer(subobject.getImplementedInterface());
	}
}
