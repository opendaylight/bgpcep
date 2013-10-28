/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.subobject;

import java.util.Arrays;
import java.util.BitSet;

import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.EROSubobjectParser;
import org.opendaylight.protocol.pcep.spi.EROSubobjectSerializer;
import org.opendaylight.protocol.pcep.spi.LabelHandlerRegistry;
import org.opendaylight.protocol.pcep.spi.LabelParser;
import org.opendaylight.protocol.pcep.spi.LabelSerializer;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.Subobjects;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.SubobjectsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.CLabel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.LabelSubobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.basic.explicit.route.subobjects.subobject.type.LabelBuilder;

import com.google.common.base.Preconditions;

public class EROLabelSubobjectParser implements EROSubobjectParser, EROSubobjectSerializer {

	public static final int TYPE = 3;

	private static final int RES_F_LENGTH = 1;

	private static final int C_TYPE_F_LENGTH = 1;

	private static final int RES_F_OFFSET = 0;

	private static final int C_TYPE_F_OFFSET = RES_F_OFFSET + RES_F_LENGTH;

	private static final int HEADER_LENGTH = C_TYPE_F_OFFSET + C_TYPE_F_LENGTH;

	private static final int U_FLAG_OFFSET = 0;

	private final LabelHandlerRegistry registry;

	public EROLabelSubobjectParser(final LabelHandlerRegistry labelReg) {
		this.registry = Preconditions.checkNotNull(labelReg);
	}

	@Override
	public Subobjects parseSubobject(final byte[] buffer, final boolean loose) throws PCEPDeserializerException {
		if (buffer == null || buffer.length == 0)
			throw new IllegalArgumentException("Array of bytes is mandatory. Can't be null or empty.");
		if (buffer.length < HEADER_LENGTH)
			throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + buffer.length + "; Expected: >"
					+ HEADER_LENGTH + ".");

		final BitSet reserved = ByteArray.bytesToBitSet(Arrays.copyOfRange(buffer, RES_F_OFFSET, RES_F_LENGTH));

		final short c_type = (short) (buffer[C_TYPE_F_OFFSET] & 0xFF);

		final LabelParser parser = this.registry.getLabelParser(c_type);

		if (parser == null) {
			throw new PCEPDeserializerException("Unknown C-TYPE for ero label subobject. Passed: " + c_type);
		}

		final LabelBuilder builder = new LabelBuilder();
		builder.setUniDirectional(reserved.get(U_FLAG_OFFSET));
		builder.setLabelType(parser.parseLabel(ByteArray.cutBytes(buffer, HEADER_LENGTH)));
		return new SubobjectsBuilder().setLoose(loose).setSubobjectType(builder.build()).build();
	}

	@Override
	public byte[] serializeSubobject(final Subobjects subobject) {
		Preconditions.checkNotNull(subobject.getSubobjectType(), "Subobject type cannot be empty.");

		final LabelSubobject label = (LabelSubobject) subobject.getSubobjectType();

		final LabelSerializer serializer = this.registry.getLabelSerializer((CLabel) label);

		if (serializer == null)
			throw new IllegalArgumentException("Unknown EROLabelSubobject instance. Passed " + label.getClass());

		final byte[] labelbytes = serializer.serializeLabel((CLabel) label);

		final byte[] retBytes = new byte[labelbytes.length + HEADER_LENGTH];

		System.arraycopy(labelbytes, 0, retBytes, HEADER_LENGTH, labelbytes.length);

		final BitSet reserved = new BitSet();
		reserved.set(U_FLAG_OFFSET, label.isUniDirectional());
		System.arraycopy(ByteArray.bitSetToBytes(reserved, RES_F_LENGTH), 0, retBytes, RES_F_OFFSET, RES_F_LENGTH);

		retBytes[C_TYPE_F_OFFSET] = (byte) serializer.getType();

		return retBytes;
	}

	@Override
	public int getType() {
		return TYPE;
	}
}
