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

import org.opendaylight.protocol.pcep.impl.object.RROSubobjectUtil;
import org.opendaylight.protocol.pcep.spi.LabelHandlerRegistry;
import org.opendaylight.protocol.pcep.spi.LabelParser;
import org.opendaylight.protocol.pcep.spi.LabelSerializer;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.RROSubobjectParser;
import org.opendaylight.protocol.pcep.spi.RROSubobjectSerializer;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.reported.route.object.rro.Subobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.reported.route.object.rro.SubobjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.label.subobject.LabelType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.record.route.subobjects.subobject.type.LabelCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.record.route.subobjects.subobject.type.LabelCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.record.route.subobjects.subobject.type.label._case.Label;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.record.route.subobjects.subobject.type.label._case.LabelBuilder;

import com.google.common.base.Preconditions;
import com.google.common.primitives.UnsignedBytes;

public class RROLabelSubobjectParser implements RROSubobjectParser, RROSubobjectSerializer {

	public static final int TYPE = 3;

	public static final int RES_F_LENGTH = 1;

	public static final int C_TYPE_F_LENGTH = 1;

	public static final int RES_F_OFFSET = 0;

	public static final int C_TYPE_F_OFFSET = RES_F_OFFSET + RES_F_LENGTH;

	public static final int HEADER_LENGTH = C_TYPE_F_OFFSET + C_TYPE_F_LENGTH;

	public static final int U_FLAG_OFFSET = 0;
	public static final int G_FLAG_OFFSET = 7;

	private final LabelHandlerRegistry registry;

	public RROLabelSubobjectParser(final LabelHandlerRegistry labelReg) {
		this.registry = Preconditions.checkNotNull(labelReg);
	}

	@Override
	public Subobject parseSubobject(final byte[] buffer) throws PCEPDeserializerException {
		if (buffer == null || buffer.length == 0) {
			throw new IllegalArgumentException("Array of bytes is mandatory. Can't be null or empty.");
		}
		if (buffer.length < HEADER_LENGTH) {
			throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + buffer.length + "; Expected: >"
					+ HEADER_LENGTH + ".");
		}
		final BitSet reserved = ByteArray.bytesToBitSet(Arrays.copyOfRange(buffer, RES_F_OFFSET, RES_F_LENGTH));

		final short cType = (short) UnsignedBytes.toInt(buffer[C_TYPE_F_OFFSET]);

		final LabelParser parser = this.registry.getLabelParser(cType);

		if (parser == null) {
			throw new PCEPDeserializerException("Unknown C-TYPE for ero label subobject. Passed: " + cType);
		}
		final LabelBuilder builder = new LabelBuilder();
		builder.setUniDirectional(reserved.get(U_FLAG_OFFSET));
		builder.setGlobal(reserved.get(G_FLAG_OFFSET));
		builder.setLabelType(parser.parseLabel(ByteArray.cutBytes(buffer, HEADER_LENGTH)));
		return new SubobjectBuilder().setSubobjectType(new LabelCaseBuilder().setLabel(builder.build()).build()).build();
	}

	@Override
	public byte[] serializeSubobject(final Subobject subobject) {
		final Label label = ((LabelCase) subobject.getSubobjectType()).getLabel();
		final LabelType l = label.getLabelType();
		final LabelSerializer parser = this.registry.getLabelSerializer(l);
		if (parser == null) {
			throw new IllegalArgumentException("Unknown RROLabelSubobject instance. Passed " + subobject.getSubobjectType().getClass());
		}
		final byte[] labelbytes = parser.serializeLabel(l);
		final byte[] retBytes = new byte[labelbytes.length + HEADER_LENGTH];
		System.arraycopy(labelbytes, 0, retBytes, HEADER_LENGTH, labelbytes.length);

		final BitSet reserved = new BitSet();
		reserved.set(U_FLAG_OFFSET, label.isUniDirectional());
		reserved.set(G_FLAG_OFFSET, label.isGlobal());
		System.arraycopy(ByteArray.bitSetToBytes(reserved, RES_F_LENGTH), 0, retBytes, RES_F_OFFSET, RES_F_LENGTH);
		retBytes[C_TYPE_F_OFFSET] = UnsignedBytes.checkedCast(parser.getType());
		return RROSubobjectUtil.formatSubobject(TYPE, retBytes);
	}

	@Override
	public int getType() {
		return TYPE;
	}
}
