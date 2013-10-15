/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import org.opendaylight.protocol.concepts.HandlerRegistry;
import org.opendaylight.protocol.pcep.spi.SubobjectHandlerRegistry;
import org.opendaylight.protocol.pcep.spi.SubobjectParser;
import org.opendaylight.protocol.pcep.spi.SubobjectSerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.CSubobject;
import org.opendaylight.yangtools.yang.binding.DataContainer;

import com.google.common.base.Preconditions;

public final class SimpleSubobjectHandlerFactory implements SubobjectHandlerRegistry {
	private final HandlerRegistry<DataContainer, SubobjectParser, SubobjectSerializer> handlers = new HandlerRegistry<>();

	@Override
	public AutoCloseable registerSubobjectParser(final int subobjectType, final SubobjectParser parser) {
		Preconditions.checkArgument(subobjectType >= 0 && subobjectType <= 65535);
		return handlers.registerParser(subobjectType, parser);
	}

	@Override
	public SubobjectParser getSubobjectParser(final int subobjectType) {
		Preconditions.checkArgument(subobjectType >= 0 && subobjectType <= 65535);
		return handlers.getParser(subobjectType);
	}

	@Override
	public AutoCloseable registerSubobjectSerializer(final Class<? extends CSubobject> subobjectClass, final SubobjectSerializer serializer) {
		return handlers.registerSerializer(subobjectClass, serializer);
	}

	@Override
	public SubobjectSerializer getSubobjectSerializer(final CSubobject subobject) {
		return handlers.getSerializer(subobject.getImplementedInterface());
	}
}
