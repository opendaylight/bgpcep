/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.spi.pojo;

import org.opendaylight.protocol.concepts.HandlerRegistry;
import org.opendaylight.protocol.pcep.spi.LabelHandlerRegistry;
import org.opendaylight.protocol.pcep.spi.LabelParser;
import org.opendaylight.protocol.pcep.spi.LabelSerializer;
import org.opendaylight.protocol.util.Values;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.label.subobject.LabelType;
import org.opendaylight.yangtools.yang.binding.DataContainer;

import com.google.common.base.Preconditions;

public class SimpleLabelHandlerRegistry implements LabelHandlerRegistry {
	private final HandlerRegistry<DataContainer, LabelParser, LabelSerializer> handlers = new HandlerRegistry<>();

	public AutoCloseable registerLabelParser(final int cType, final LabelParser parser) {
		Preconditions.checkArgument(cType >= 0 && cType <= Values.UNSIGNED_BYTE_MAX_VALUE);
		return this.handlers.registerParser(cType, parser);
	}

	public AutoCloseable registerLabelSerializer(final Class<? extends LabelType> labelClass, final LabelSerializer serializer) {
		return this.handlers.registerSerializer(labelClass, serializer);
	}

	@Override
	public LabelParser getLabelParser(final int cType) {
		Preconditions.checkArgument(cType >= 0 && cType <= Values.UNSIGNED_BYTE_MAX_VALUE);
		return this.handlers.getParser(cType);
	}

	@Override
	public LabelSerializer getLabelSerializer(final LabelType label) {
		return this.handlers.getSerializer(label.getImplementedInterface());
	}
}
