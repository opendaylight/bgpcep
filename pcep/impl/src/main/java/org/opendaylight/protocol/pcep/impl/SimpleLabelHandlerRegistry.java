/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import org.opendaylight.protocol.concepts.HandlerRegistry;
import org.opendaylight.protocol.pcep.spi.LabelHandlerRegistry;
import org.opendaylight.protocol.pcep.spi.LabelParser;
import org.opendaylight.protocol.pcep.spi.LabelSerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.CLabel;
import org.opendaylight.yangtools.yang.binding.DataContainer;

import com.google.common.base.Preconditions;

public class SimpleLabelHandlerRegistry implements LabelHandlerRegistry {
	private final HandlerRegistry<DataContainer, LabelParser, LabelSerializer> handlers = new HandlerRegistry<>();

	@Override
	public AutoCloseable registerLabelParser(final int cType, final LabelParser parser) {
		Preconditions.checkArgument(cType >= 0 && cType <= 255);
		return handlers.registerParser(cType, parser);
	}

	@Override
	public LabelParser getLabelParser(final int cType) {
		Preconditions.checkArgument(cType >= 0 && cType <= 255);
		return handlers.getParser(cType);
	}

	@Override
	public AutoCloseable registerLabelSerializer(final Class<? extends CLabel> labelClass, final LabelSerializer serializer) {
		return handlers.registerSerializer(labelClass, serializer);
	}

	@Override
	public LabelSerializer getLabelSerializer(final CLabel label) {
		return handlers.getSerializer(label.getImplementedInterface());
	}
}
