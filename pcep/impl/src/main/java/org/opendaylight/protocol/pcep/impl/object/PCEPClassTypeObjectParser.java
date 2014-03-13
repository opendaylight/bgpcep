/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.object;

import org.opendaylight.protocol.pcep.spi.AbstractObjectWithTlvsParser;
import org.opendaylight.protocol.pcep.spi.ObjectUtil;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.PCEPErrors;
import org.opendaylight.protocol.pcep.spi.TlvRegistry;
import org.opendaylight.protocol.pcep.spi.UnknownObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.classtype.object.ClassType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.classtype.object.ClassTypeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.primitives.UnsignedBytes;

/**
 * Parser for {@link ClassType}
 */
public class PCEPClassTypeObjectParser extends AbstractObjectWithTlvsParser<ClassTypeBuilder> {
	private static final Logger LOG = LoggerFactory.getLogger(PCEPClassTypeObjectParser.class);

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

	public PCEPClassTypeObjectParser(final TlvRegistry tlvReg) {
		super(tlvReg);
	}

	@Override
	public Object parseObject(final ObjectHeader header, final byte[] bytes) throws PCEPDeserializerException {
		if (bytes == null) {
			throw new IllegalArgumentException("Byte array is mandatory.");
		}
		if (bytes.length != SIZE) {
			throw new PCEPDeserializerException("Size of byte array doesn't match defined size. Expected: " + SIZE + "; Passed: "
					+ bytes.length);
		}
		if (!header.isProcessingRule()) {
			// LOG.debug("Processed bit not set on CLASS TYPE OBJECT, ignoring it");
			return null;
		}
		final ClassTypeBuilder builder = new ClassTypeBuilder();

		builder.setIgnore(header.isIgnore());
		builder.setProcessingRule(header.isProcessingRule());

		final short ct = (short) UnsignedBytes.toInt(bytes[SIZE - 1]);
		builder.setClassType(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ClassType(ct));

		final Object obj = builder.build();
		if (ct < 0 || ct > 8) {
			LOG.debug("Invalid class type {}", ct);
			return new UnknownObject(PCEPErrors.INVALID_CT, obj);
		}
		return obj;
	}

	@Override
	public byte[] serializeObject(final Object object) {
		if (!(object instanceof ClassType)) {
			throw new IllegalArgumentException("Wrong instance of PCEPObject. Passed " + object.getClass() + ". Needed ClasstypeObject.");
		}
		final byte[] retBytes = new byte[SIZE];
		retBytes[SIZE - 1] = UnsignedBytes.checkedCast(((ClassType) object).getClassType().getValue());
		return ObjectUtil.formatSubobject(TYPE, CLASS, object.isProcessingRule(), object.isIgnore(), retBytes);
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
