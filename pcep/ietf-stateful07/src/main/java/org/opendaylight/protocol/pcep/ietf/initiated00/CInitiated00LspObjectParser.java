/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.ietf.initiated00;

import io.netty.buffer.ByteBuf;

import java.util.BitSet;

import org.opendaylight.protocol.pcep.ietf.stateful07.Stateful07LspObjectParser;
import org.opendaylight.protocol.pcep.spi.ObjectUtil;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.TlvRegistry;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev131126.Lsp1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev131126.Lsp1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.OperationalStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.PlspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.object.Lsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.object.LspBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.object.lsp.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ObjectHeader;

import com.google.common.base.Preconditions;

/**
 * Parser for {@link Lsp}
 */
public final class CInitiated00LspObjectParser extends Stateful07LspObjectParser {

	private static final int CREATE_FLAG_OFFSET = 8;

	public CInitiated00LspObjectParser(final TlvRegistry tlvReg) {
		super(tlvReg);
	}

	@Override
	public Lsp parseObject(final ObjectHeader header, final ByteBuf bytes) throws PCEPDeserializerException {
		Preconditions.checkArgument(bytes != null && bytes.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
		final LspBuilder builder = new LspBuilder();
		builder.setIgnore(header.isIgnore());
		builder.setProcessingRule(header.isProcessingRule());
		int[] plspIdRaw = new int[] {
				bytes.readUnsignedByte(),
				bytes.readUnsignedByte(),
				bytes.getUnsignedByte(2),
		};
		builder.setPlspId(new PlspId((long) ((plspIdRaw[0] << 12) | (plspIdRaw[1] << 4) | (plspIdRaw[2] >> 4))));
		final BitSet flags = ByteArray.bytesToBitSet(ByteArray.readBytes(bytes, 2));
		builder.setDelegate(flags.get(DELEGATE_FLAG_OFFSET));
		builder.setSync(flags.get(SYNC_FLAG_OFFSET));
		builder.setRemove(flags.get(REMOVE_FLAG_OFFSET));
		builder.setAdministrative(flags.get(ADMINISTRATIVE_FLAG_OFFSET));
		builder.addAugmentation(Lsp1.class, new Lsp1Builder().setCreate(flags.get(CREATE_FLAG_OFFSET)).build());
		short s = 0;
		s |= flags.get(OPERATIONAL_OFFSET + 2) ? 1 : 0;
		s |= (flags.get(OPERATIONAL_OFFSET + 1) ? 1 : 0) << 1;
		s |= (flags.get(OPERATIONAL_OFFSET) ? 1 : 0) << 2;
		builder.setOperational(OperationalStatus.forValue(s));
		final TlvsBuilder b = new TlvsBuilder();
		parseTlvs(b, bytes.slice());
		builder.setTlvs(b.build());
		return builder.build();
	}

	@Override
	public byte[] serializeObject(final Object object) {
		if (!(object instanceof Lsp)) {
			throw new IllegalArgumentException("Wrong instance of PCEPObject. Passed " + object.getClass() + ". Needed LspObject.");
		}
		final Lsp specObj = (Lsp) object;

		final byte[] tlvs = serializeTlvs(specObj.getTlvs());
		final byte[] retBytes = new byte[TLVS_OFFSET + tlvs.length + getPadding(TLVS_OFFSET + tlvs.length, PADDED_TO)];

		final int lspID = specObj.getPlspId().getValue().intValue();
		retBytes[0] = (byte) (lspID >> 12);
		retBytes[1] = (byte) (lspID >> 4);
		retBytes[2] = (byte) (lspID << 4);
		if (specObj.isDelegate() != null && specObj.isDelegate()) {
			retBytes[3] |= 1 << (Byte.SIZE - (DELEGATE_FLAG_OFFSET - Byte.SIZE) - 1);
		}
		if (specObj.isRemove() != null && specObj.isRemove()) {
			retBytes[3] |= 1 << (Byte.SIZE - (REMOVE_FLAG_OFFSET - Byte.SIZE) - 1);
		}
		if (specObj.isSync() != null && specObj.isSync()) {
			retBytes[3] |= 1 << (Byte.SIZE - (SYNC_FLAG_OFFSET - Byte.SIZE) - 1);
		}
		if (specObj.isAdministrative() != null && specObj.isAdministrative()) {
			retBytes[3] |= 1 << (Byte.SIZE - (ADMINISTRATIVE_FLAG_OFFSET - Byte.SIZE) - 1);
		}
		if (specObj.getAugmentation(Lsp1.class) != null && specObj.getAugmentation(Lsp1.class).isCreate()) {
			retBytes[3] |= 1 << (Byte.SIZE - (CREATE_FLAG_OFFSET - Byte.SIZE) - 1);
		}
		if (specObj.getOperational() != null) {
			final int op = specObj.getOperational().getIntValue();
			retBytes[3] |= (op & 7) << 4;
		}
		ByteArray.copyWhole(tlvs, retBytes, TLVS_OFFSET);
		return ObjectUtil.formatSubobject(TYPE, CLASS, object.isProcessingRule(), object.isIgnore(), retBytes);
	}
}
