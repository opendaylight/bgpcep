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
import org.opendaylight.protocol.pcep.spi.AbstractObjectParser;
import org.opendaylight.protocol.pcep.spi.HandlerRegistry;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.OfId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.OfObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.attributes.OfBuilder;

/**
 * Parser for {@link OfObject}
 */
public class PCEPObjectiveFunctionObjectParser extends AbstractObjectParser<OfBuilder> {

	public static final int CLASS = 21;

	public static final int TYPE = 1;
	/*
	 * lengths of fields
	 */
	public static final int OF_CODE_F_LENGTH = 2;

	/*
	 * offsets of fields
	 */
	public static final int OF_CODE_F_OFFSET = 0;
	public static final int TLVS_OFFSET = OF_CODE_F_OFFSET + OF_CODE_F_LENGTH + 2; // added reserved field of size 2

	public PCEPObjectiveFunctionObjectParser(final HandlerRegistry registry) {
		super(registry);
	}

	@Override
	public OfObject parseObject(final ObjectHeader header, final byte[] bytes) throws PCEPDeserializerException, PCEPDocumentedException {
		if (bytes == null || bytes.length == 0)
			throw new IllegalArgumentException("Array of bytes is mandatory. Can't be null or empty.");

		final OfBuilder builder = new OfBuilder();

		parseTlvs(builder, ByteArray.cutBytes(bytes, TLVS_OFFSET));

		builder.setIgnore(header.isIgnore());
		builder.setProcessingRule(header.isProcessingRule());
		builder.setCode(new OfId(ByteArray.bytesToInt(ByteArray.subByte(bytes, OF_CODE_F_OFFSET, OF_CODE_F_LENGTH)) & 0xFFFF));

		return builder.build();
	}

	@Override
	public void addTlv(final OfBuilder builder, final Tlv tlv) {
		// No tlvs defined
	}

	@Override
	public byte[] serializeObject(final Object object) {
		if (!(object instanceof OfObject))
			throw new IllegalArgumentException("Wrong instance of PCEPObject. Passed " + object.getClass()
					+ ". Needed PCEPObjectiveFunction.");

		final OfObject specObj = (OfObject) object;
		// FIXME
		// final byte[] tlvs = PCEPTlvParser.put(specObj.getTlvs());
		final byte[] retBytes = new byte[TLVS_OFFSET + 0];

		// ByteArray.copyWhole(tlvs, retBytes, TLVS_OFFSET);

		ByteArray.copyWhole(ByteArray.shortToBytes(specObj.getCode().getValue().shortValue()), retBytes, OF_CODE_F_OFFSET);

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
