/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.subobject;

import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.LabelParser;
import org.opendaylight.protocol.pcep.spi.LabelSerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.CLabel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.label.subobject.label.type.GeneralizedChannelSetLabel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.label.subobject.label.type.GeneralizedChannelSetLabelBuilder;

public class GeneralizedLabelChannelSetParser implements LabelParser, LabelSerializer {

	public static final int CTYPE = 4;

	@Override
	public GeneralizedChannelSetLabel parseLabel(final byte[] buffer) throws PCEPDeserializerException {
		if (buffer == null || buffer.length == 0)
			throw new IllegalArgumentException("Array of bytes is mandatory. Can't be null or empty.");
		// FIXME: finish
		return new GeneralizedChannelSetLabelBuilder().build();
	}

	@Override
	public byte[] serializeSubobject(final CLabel subobject) {
		if (!(subobject instanceof GeneralizedChannelSetLabel))
			throw new IllegalArgumentException("Unknown Label Subobject instance. Passed " + subobject.getClass()
					+ ". Needed GeneralizedChannelSetLabel.");
		// FIXME: finish
		return new byte[0];
	}

	@Override
	public int getType() {
		return CTYPE;
	}
}
