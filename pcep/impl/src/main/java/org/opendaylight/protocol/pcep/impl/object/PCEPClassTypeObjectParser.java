/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.object;

import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.PCEPDocumentedException;
import org.opendaylight.protocol.pcep.PCEPErrors;
import org.opendaylight.protocol.pcep.spi.TlvHandlerRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.classtype.object.ClassType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.classtype.object.ClassTypeBuilder;

import com.google.common.primitives.UnsignedBytes;

/**
 * Parser for {@link ClassType}
 */
public class PCEPClassTypeObjectParser extends AbstractObjectWithTlvsParser<ClassTypeBuilder> {

	public static final int CLASS = 22;

	public static final int TYPE = 1;

	/**
	 * Length of Class Type field in bits.
	 */
	private static final int CT_F_LENGTH = 3;

	/**
	 * Reserved field bit length.
	 */
	private static final int RESERVED = 29;

	/**
	 * Size of the object in bytes.
	 */
	private static final int SIZE = (RESERVED + CT_F_LENGTH) / 8;

	public PCEPClassTypeObjectParser(final TlvHandlerRegistry tlvReg) {
		super(tlvReg);
	}

	@Override
	public ClassType parseObject(final ObjectHeader header, final byte[] bytes) throws PCEPDeserializerException, PCEPDocumentedException {
		if (bytes == null) {
			throw new IllegalArgumentException("Byte array is mandatory.");
		}
		if (bytes.length != SIZE) {
			throw new PCEPDeserializerException("Size of byte array doesn't match defined size. Expected: " + SIZE + "; Passed: "
					+ bytes.length);
		}
		if (!header.isProcessingRule()) {
			throw new PCEPDocumentedException("Processed bit not set", PCEPErrors.P_FLAG_NOT_SET);
		}
		final ClassTypeBuilder builder = new ClassTypeBuilder();

		builder.setIgnore(header.isIgnore());
		builder.setProcessingRule(header.isProcessingRule());

		final short ct = (short) UnsignedBytes.toInt(bytes[SIZE - 1]);
		builder.setClassType(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ClassType(ct));

		if (ct < 0 || ct > 8) {
			throw new PCEPDocumentedException("Invalid class type " + ct, PCEPErrors.INVALID_CT);
		}
		return builder.build();
	}

	@Override
	public void addTlv(final ClassTypeBuilder builder, final Tlv tlv) {
		// No tlvs defined
	}

	@Override
	public byte[] serializeObject(final Object object) {
		if (!(object instanceof ClassType)) {
			throw new IllegalArgumentException("Wrong instance of PCEPObject. Passed " + object.getClass() + ". Needed ClasstypeObject.");
		}
		final byte[] retBytes = new byte[SIZE];
		retBytes[SIZE - 1] = UnsignedBytes.checkedCast(((ClassType) object).getClassType().getValue());
		return retBytes;
	}

	@Override
	public int getObjectType() {
		return TYPE;
	}

	@Override
	public int getObjectClass() {
		return CLASS;
	}
}
