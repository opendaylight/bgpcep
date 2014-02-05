/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.ietf.stateful07;

import java.util.BitSet;

import org.opendaylight.protocol.pcep.spi.AbstractObjectWithTlvsParser;
import org.opendaylight.protocol.pcep.spi.ObjectUtil;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.TlvHandlerRegistry;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.OperationalStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.PlspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.error.code.tlv.LspErrorCode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.identifiers.tlv.LspIdentifiers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.object.Lsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.object.LspBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.object.lsp.Tlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.object.lsp.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.rsvp.error.spec.tlv.RsvpErrorSpec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.symbolic.path.name.tlv.SymbolicPathName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;

import com.google.common.base.Preconditions;

/**
 * Parser for {@link Lsp}
 */
public class Stateful07LspObjectParser extends AbstractObjectWithTlvsParser<TlvsBuilder> {

	public static final int CLASS = 32;

	public static final int TYPE = 1;

	/*
	 * offset of TLVs offset of other fields are not defined as constants
	 * because of non-standard mapping of bits
	 */
	private static final int TLVS_OFFSET = 4;

	/*
	 * 12b extended to 16b so first 4b are restricted (belongs to LSP ID)
	 */
	private static final int DELEGATE_FLAG_OFFSET = 15;
	private static final int SYNC_FLAG_OFFSET = 14;
	private static final int REMOVE_FLAG_OFFSET = 13;
	private static final int ADMINISTRATIVE_FLAG_OFFSET = 12;
	private static final int OPERATIONAL_OFFSET = 9;

	public Stateful07LspObjectParser(final TlvHandlerRegistry tlvReg) {
		super(tlvReg);
	}

	@Override
	public Lsp parseObject(final ObjectHeader header, final byte[] bytes) throws PCEPDeserializerException {
		if (bytes == null || bytes.length == 0) {
			throw new IllegalArgumentException("Array of bytes is mandatory. Can't be null or empty.");
		}
		final BitSet flags = ByteArray.bytesToBitSet(ByteArray.subByte(bytes, 2, 2));

		final LspBuilder builder = new LspBuilder();
		builder.setIgnore(header.isIgnore());
		builder.setProcessingRule(header.isProcessingRule());

		builder.setPlspId(new PlspId((ByteArray.bytesToLong(ByteArray.subByte(bytes, 0, 2)) & 0xFFFF) << 4 | (bytes[2] & 0xFF) >> 4));
		builder.setDelegate(flags.get(DELEGATE_FLAG_OFFSET));
		builder.setSync(flags.get(SYNC_FLAG_OFFSET));
		builder.setRemove(flags.get(REMOVE_FLAG_OFFSET));
		builder.setAdministrative(flags.get(ADMINISTRATIVE_FLAG_OFFSET));
		short s = 0;
		s |= flags.get(OPERATIONAL_OFFSET + 2) ? 1 : 0;
		s |= (flags.get(OPERATIONAL_OFFSET + 1) ? 1 : 0) << 1;
		s |= (flags.get(OPERATIONAL_OFFSET) ? 1 : 0) << 2;
		builder.setOperational(OperationalStatus.forValue(s));
		final TlvsBuilder b = new TlvsBuilder();
		parseTlvs(b, ByteArray.cutBytes(bytes, TLVS_OFFSET));
		builder.setTlvs(b.build());
		return builder.build();
	}

	@Override
	public void addTlv(final TlvsBuilder builder, final Tlv tlv) {
		if (tlv instanceof LspErrorCode) {
			builder.setLspErrorCode((LspErrorCode) tlv);
		} else if (tlv instanceof LspIdentifiers) {
			builder.setLspIdentifiers((LspIdentifiers) tlv);
		} else if (tlv instanceof RsvpErrorSpec) {
			builder.setRsvpErrorSpec((RsvpErrorSpec) tlv);
		} else if (tlv instanceof SymbolicPathName) {
			builder.setSymbolicPathName((SymbolicPathName) tlv);
		}
	}

	@Override
	public byte[] serializeObject(final Object object) {
		if (!(object instanceof Lsp)) {
			throw new IllegalArgumentException("Wrong instance of PCEPObject. Passed " + object.getClass() + ". Needed LspObject.");
		}
		final Lsp specObj = (Lsp) object;

		final byte[] tlvs = serializeTlvs(specObj.getTlvs());
		final byte[] retBytes = new byte[TLVS_OFFSET + tlvs.length + getPadding(TLVS_OFFSET + tlvs.length, PADDED_TO)];

		Preconditions.checkArgument(specObj.getPlspId() != null, "PLSP-ID not present");
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
		if (specObj.getOperational() != null) {
			final int op = specObj.getOperational().getIntValue();
			retBytes[3] |= (op & 7) << 4;
		}
		ByteArray.copyWhole(tlvs, retBytes, TLVS_OFFSET);
		return ObjectUtil.formatSubobject(TYPE, CLASS, object.isProcessingRule(), object.isIgnore(), retBytes);
	}

	public byte[] serializeTlvs(final Tlvs tlvs) {
		if (tlvs == null) {
			return new byte[0];
		}
		int finalLength = 0;
		byte[] lspErrBytes = null;
		byte[] lspIdBytes = null;
		byte[] rsvpErrBytes = null;
		byte[] symbBytes = null;
		if (tlvs.getLspErrorCode() != null) {
			lspErrBytes = serializeTlv(tlvs.getLspErrorCode());
			finalLength += lspErrBytes.length;
		}
		if (tlvs.getLspIdentifiers() != null) {
			lspIdBytes = serializeTlv(tlvs.getLspIdentifiers());
			finalLength += lspIdBytes.length;
		}
		if (tlvs.getRsvpErrorSpec() != null) {
			rsvpErrBytes = serializeTlv(tlvs.getRsvpErrorSpec());
			finalLength += rsvpErrBytes.length;
		}
		if (tlvs.getSymbolicPathName() != null) {
			symbBytes = serializeTlv(tlvs.getSymbolicPathName());
			finalLength += symbBytes.length;
		}
		int offset = 0;
		final byte[] result = new byte[finalLength];
		if (lspErrBytes != null) {
			ByteArray.copyWhole(lspErrBytes, result, offset);
			offset += lspErrBytes.length;
		}
		if (lspIdBytes != null) {
			ByteArray.copyWhole(lspIdBytes, result, offset);
			offset += lspIdBytes.length;
		}
		if (rsvpErrBytes != null) {
			ByteArray.copyWhole(rsvpErrBytes, result, offset);
			offset += rsvpErrBytes.length;
		}
		if (symbBytes != null) {
			ByteArray.copyWhole(symbBytes, result, offset);
			offset += symbBytes.length;
		}
		return result;
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
