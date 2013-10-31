/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.object;

import java.util.Arrays;

import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.PCEPDocumentedException;
import org.opendaylight.protocol.pcep.spi.TlvHandlerRegistry;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.SrpIdNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.SrpObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcinitiate.message.pcinitiate.message.requests.SrpBuilder;

/**
 * Parser for {@link SrpObject}
 */
public final class PCEPSrpObjectParser extends AbstractObjectWithTlvsParser<SrpBuilder> {

	public static final int CLASS = 33;

	public static final int TYPE = 1;

	private static final int FLAGS_SIZE = 4;

	private static final int SRP_ID_SIZE = 4;

	private static final int MIN_SIZE = FLAGS_SIZE + SRP_ID_SIZE;

	public PCEPSrpObjectParser(final TlvHandlerRegistry tlvReg) {
		super(tlvReg);
	}

	@Override
	public SrpObject parseObject(final ObjectHeader header, final byte[] bytes) throws PCEPDeserializerException, PCEPDocumentedException {
		if (bytes == null || bytes.length == 0) {
			throw new IllegalArgumentException("Array of bytes is mandatory. Can't be null or empty.");
		}
		if (bytes.length < MIN_SIZE) {
			throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + bytes.length + "; Expected: >=" + MIN_SIZE
					+ ".");
		}
		if (header.isProcessingRule()) {
			throw new PCEPDeserializerException("Processed flag is set");
		}
		final SrpBuilder builder = new SrpBuilder();
		builder.setIgnore(header.isIgnore());
		builder.setProcessingRule(header.isProcessingRule());
		final byte[] srpId = ByteArray.subByte(bytes, FLAGS_SIZE, SRP_ID_SIZE);
		if (Arrays.equals(srpId, new byte[] { 0, 0, 0, 0 }) || Arrays.equals(srpId, new byte[] { 0xFFFFFFFF })) {
			throw new PCEPDeserializerException("Min/Max values for SRP ID are reserved.");
		}
		builder.setOperationId(new SrpIdNumber(ByteArray.bytesToLong(srpId)));
		return builder.build();
	}

	@Override
	public void addTlv(final SrpBuilder builder, final Tlv tlv) {
		// No tlvs defined
	}

	@Override
	public byte[] serializeObject(final Object object) {
		if (!(object instanceof SrpObject)) {
			throw new IllegalArgumentException("Wrong instance of PCEPObject. Passed " + object.getClass() + ". Needed SrpObject.");
		}
		final SrpObject srp = (SrpObject) object;
		final Long id = srp.getOperationId().getValue();
		if (id == 0 || id == 0xFFFFFFFFL) {
			throw new IllegalArgumentException("Min/Max values for SRP ID are reserved.");
		}
		final byte[] retBytes = new byte[MIN_SIZE];
		System.arraycopy(ByteArray.intToBytes(id.intValue()), 0, retBytes, FLAGS_SIZE, SRP_ID_SIZE);
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
