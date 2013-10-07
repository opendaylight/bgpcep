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
import org.opendaylight.protocol.pcep.PCEPObject;
import org.opendaylight.protocol.pcep.object.PCEPClassTypeObject;
import org.opendaylight.protocol.pcep.spi.AbstractObjectParser;
import org.opendaylight.protocol.pcep.spi.HandlerRegistry;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ClassType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ClasstypeObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.attributes.ClassTypeBuilder;

/**
 * Parser for {@link org.opendaylight.protocol.pcep.object.PCEPClassTypeObject PCEPClassTypeObject}
 */
public class PCEPClassTypeObjectParser extends AbstractObjectParser<ClassTypeBuilder> {

	/**
	 * Length of Class Type field in bits.
	 */
	public static final int CT_F_LENGTH = 3;

	/**
	 * Reserved field bit length.
	 */
	public static final int RESERVED = 29;

	/**
	 * Size of the object in bytes.
	 */
	public static final int SIZE = (RESERVED + CT_F_LENGTH) / 8;

	public PCEPClassTypeObjectParser(final HandlerRegistry registry) {
		super(registry);
	}

	@Override
	public ClasstypeObject parseObject(final ObjectHeader header, final byte[] bytes) throws PCEPDeserializerException,
			PCEPDocumentedException {
		if (bytes == null)
			throw new IllegalArgumentException("Byte array is mandatory.");
		if (bytes.length != SIZE)
			throw new PCEPDeserializerException("Size of byte array doesn't match defined size. Expected: " + SIZE + "; Passed: "
					+ bytes.length);
		if (!header.isProcessingRule())
			throw new PCEPDocumentedException("Processed bit not set", PCEPErrors.P_FLAG_NOT_SET);

		final ClassTypeBuilder builder = new ClassTypeBuilder();

		builder.setIgnore(header.isIgnore());
		builder.setProcessingRule(header.isProcessingRule());

		final short ct = (short) (bytes[SIZE - 1] & 0xFF);
		builder.setClassType(new ClassType(ct));

		if (ct < 0 || ct > 8) {
			throw new PCEPDocumentedException("Invalid class type " + ct, PCEPErrors.INVALID_CT);
		}
		return builder.build();
	}

	@Override
	public void addTlv(final ClassTypeBuilder builder, final Tlv tlv) {
		// No tlvs defined
	}

	public byte[] put(final PCEPObject obj) {
		if (!(obj instanceof PCEPClassTypeObject))
			throw new IllegalArgumentException("Wrong instance of PCEPObject. Passed " + obj.getClass() + ". Needed PCEPClassTypeObject.");

		final byte[] retBytes = new byte[SIZE];
		retBytes[SIZE - 1] = ByteArray.shortToBytes(((PCEPClassTypeObject) obj).getClassType())[1];
		return retBytes;
	}
}
