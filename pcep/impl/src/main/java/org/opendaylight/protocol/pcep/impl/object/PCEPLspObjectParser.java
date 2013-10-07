/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.object;

import java.util.BitSet;

import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.PCEPDocumentedException;
import org.opendaylight.protocol.pcep.PCEPObject;
import org.opendaylight.protocol.pcep.impl.PCEPTlvParser;
import org.opendaylight.protocol.pcep.object.PCEPLspObject;
import org.opendaylight.protocol.pcep.spi.AbstractObjectParser;
import org.opendaylight.protocol.pcep.spi.HandlerRegistry;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.LspObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcinitiate.message.pcinitiate.message.requests.LspBuilder;

/**
 * Parser for {@link org.opendaylight.protocol.pcep.object.PCEPLspObject PCEPLspObject}
 */
public class PCEPLspObjectParser extends AbstractObjectParser<LspBuilder> {

	/*
	 * offset of TLVs offset of other fields are not defined as constants
	 * because of non-standard mapping of bits
	 */
	public static final int TLVS_OFFSET = 4;

	/*
	 * 12b extended to 16b so first 4b are restricted (belongs to LSP ID)
	 */
	private static final int DELEGATE_FLAG_OFFSET = 15;
	private static final int OPERATIONAL_FLAG_OFFSET = 13;
	private static final int SYNC_FLAG_OFFSET = 14;
	private static final int REMOVE_FLAG_OFFSET = 12;

	public PCEPLspObjectParser(final HandlerRegistry registry) {
		super(registry);
	}

	@Override
	public LspObject parseObject(final ObjectHeader header, final byte[] bytes) throws PCEPDeserializerException, PCEPDocumentedException {
		if (bytes == null || bytes.length == 0)
			throw new IllegalArgumentException("Array of bytes is mandatory. Can't be null or empty.");

		final BitSet flags = ByteArray.bytesToBitSet(ByteArray.subByte(bytes, 2, 2));

		final LspBuilder builder = new LspBuilder();

		parseTlvs(builder, ByteArray.cutBytes(bytes, TLVS_OFFSET));

		builder.setIgnore(header.isIgnore());
		builder.setProcessingRule(header.isProcessingRule());

		// builder.setPlspId(new PlspId(ByteArray.bytesToLong(ByteArray.subByte(bytes, 0, 2)) & 0xFFFF) << 4 | (bytes[2]
		// & 0xFF) >> 4));
		builder.setDelegate(flags.get(DELEGATE_FLAG_OFFSET));
		builder.setSync(flags.get(SYNC_FLAG_OFFSET));
		// builder.setOperational(Operational.flags.get(OPERATIONAL_FLAG_OFFSET));
		builder.setRemove(flags.get(REMOVE_FLAG_OFFSET));

		return builder.build();
	}

	@Override
	public void addTlv(final LspBuilder builder, final Tlv tlv) {
		// FIXME : finish
	}

	public byte[] put(final PCEPObject obj) {
		if (!(obj instanceof PCEPLspObject))
			throw new IllegalArgumentException("Wrong instance of PCEPObject. Passed " + obj.getClass() + ". Needed PCEPLspObject.");

		final PCEPLspObject specObj = (PCEPLspObject) obj;

		final byte[] tlvs = PCEPTlvParser.put(specObj.getTlvs());

		final byte[] retBytes = new byte[tlvs.length + TLVS_OFFSET];

		final int lspID = specObj.getLspID();
		retBytes[0] = (byte) (lspID >> 12);
		retBytes[1] = (byte) (lspID >> 4);
		retBytes[2] = (byte) (lspID << 4);
		if (specObj.isDelegate())
			retBytes[3] |= 1 << (Byte.SIZE - (DELEGATE_FLAG_OFFSET - Byte.SIZE) - 1);
		if (specObj.isOperational())
			retBytes[3] |= 1 << (Byte.SIZE - (OPERATIONAL_FLAG_OFFSET - Byte.SIZE) - 1);
		if (specObj.isRemove())
			retBytes[3] |= 1 << (Byte.SIZE - (REMOVE_FLAG_OFFSET - Byte.SIZE) - 1);
		if (specObj.isSync())
			retBytes[3] |= 1 << (Byte.SIZE - (SYNC_FLAG_OFFSET - Byte.SIZE) - 1);

		ByteArray.copyWhole(tlvs, retBytes, TLVS_OFFSET);

		return retBytes;
	}
}
