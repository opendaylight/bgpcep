/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.subobject;

import java.util.List;

import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.LabelParser;
import org.opendaylight.protocol.pcep.spi.LabelSerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.CLabel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.generalized.channel.set.label.Subobjects;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.generalized.channel.set.label.SubobjectsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.label.subobject.label.type.GeneralizedChannelSetLabel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.label.subobject.label.type.GeneralizedChannelSetLabelBuilder;

import com.google.common.collect.Lists;

/**
 * Parser for {@link GeneralizedChannelSetLabel}
 */
public class GeneralizedLabelChannelSetParser implements LabelParser, LabelSerializer {

	public static final int CTYPE = 4;

	@Override
	public GeneralizedChannelSetLabel parseLabel(final byte[] buffer) throws PCEPDeserializerException {
		if (buffer == null || buffer.length == 0) {
			throw new IllegalArgumentException("Array of bytes is mandatory. Can't be null or empty.");
		}
		// FIXME: finish
		final List<Subobjects> list = Lists.newArrayList();
		final SubobjectsBuilder subs = new SubobjectsBuilder();

		return new GeneralizedChannelSetLabelBuilder().setSubobjects(list).build();
	}

	@Override
	public byte[] serializeLabel(final CLabel subobject) {
		if (!(subobject instanceof GeneralizedChannelSetLabel)) {
			throw new IllegalArgumentException("Unknown Label Subobject instance. Passed " + subobject.getClass()
					+ ". Needed GeneralizedChannelSetLabel.");
		}

		// FIXME: finish
		return new byte[0];
	}

	@Override
	public int getType() {
		return CTYPE;
	}
}
