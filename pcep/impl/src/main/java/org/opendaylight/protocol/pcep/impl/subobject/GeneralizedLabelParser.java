/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.subobject;

import org.opendaylight.protocol.pcep.spi.LabelParser;
import org.opendaylight.protocol.pcep.spi.LabelSerializer;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.label.subobject.LabelType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.label.subobject.label.type.GeneralizedLabelCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.label.subobject.label.type.GeneralizedLabelCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.label.subobject.label.type.generalized.label._case.GeneralizedLabelBuilder;

/**
 * Parser for {@link GeneralizedLabelCase}
 */
public class GeneralizedLabelParser implements LabelParser, LabelSerializer {

	public static final int CTYPE = 2;

	@Override
	public LabelType parseLabel(final byte[] buffer) throws PCEPDeserializerException {
		if (buffer == null || buffer.length == 0) {
			throw new IllegalArgumentException("Array of bytes is mandatory. Can't be null or empty.");
		}

		return new GeneralizedLabelCaseBuilder().setGeneralizedLabel(new GeneralizedLabelBuilder().setGeneralizedLabel(buffer).build()).build();
	}

	@Override
	public byte[] serializeLabel(final LabelType subobject) {
		if (!(subobject instanceof GeneralizedLabelCase)) {
			throw new IllegalArgumentException("Unknown Label Subobject instance. Passed " + subobject.getClass()
					+ ". Needed GeneralizedLabelCase.");
		}
		return ((GeneralizedLabelCase) subobject).getGeneralizedLabel().getGeneralizedLabel();
	}

	@Override
	public int getType() {
		return CTYPE;
	}
}
