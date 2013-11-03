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
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.OpenObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ProtocolVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.db.version.tlv.LspDbVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.of.list.tlv.OfList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.message.open.message.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.Tlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.predundancy.group.id.tlv.PredundancyGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.stateful.capability.tlv.Stateful;

import com.google.common.primitives.UnsignedBytes;

/**
 * Parser for {@link OpenObject}
 */

public class PCEPOpenObjectParser extends AbstractObjectWithTlvsParser<TlvsBuilder> {

	public static final int CLASS = 1;

	public static final int TYPE = 1;

	/*
	 * lengths of fields in bytes
	 */
	private static final int VER_FLAGS_MF_LENGTH = 1;
	private static final int KEEPALIVE_F_LENGTH = 1;
	private static final int DEAD_TIMER_LENGTH = 1;
	private static final int SID_F_LENGTH = 1;

	/*
	 * lengths of subfields inside multi-field in bits
	 */
	private static final int VERSION_SF_LENGTH = 3;

	/*
	 * offsets of field in bytes
	 */
	private static final int VER_FLAGS_MF_OFFSET = 0;
	private static final int KEEPALIVE_F_OFFSET = VER_FLAGS_MF_OFFSET + VER_FLAGS_MF_LENGTH;
	private static final int DEAD_TIMER_OFFSET = KEEPALIVE_F_OFFSET + KEEPALIVE_F_LENGTH;
	private static final int SID_F_OFFSET = DEAD_TIMER_OFFSET + DEAD_TIMER_LENGTH;
	private static final int TLVS_OFFSET = SID_F_OFFSET + SID_F_LENGTH;

	/*
	 * offsets of subfields inside multi-field in bits
	 */
	private static final int VERSION_SF_OFFSET = 0;

	private static final int PCEP_VERSION = 1;

	public PCEPOpenObjectParser(final TlvHandlerRegistry tlvReg) {
		super(tlvReg);
	}

	@Override
	public OpenObject parseObject(final ObjectHeader header, final byte[] bytes) throws PCEPDeserializerException, PCEPDocumentedException {
		if (bytes == null || bytes.length == 0) {
			throw new IllegalArgumentException("Array of bytes is mandatory. Can't be null or empty.");
		}
		final int versionValue = ByteArray.copyBitsRange(bytes[VER_FLAGS_MF_OFFSET], VERSION_SF_OFFSET, VERSION_SF_LENGTH);

		if (versionValue != PCEP_VERSION) {
			throw new PCEPDocumentedException("Unsupported PCEP version " + versionValue, PCEPErrors.PCEP_VERSION_NOT_SUPPORTED);
		}
		final OpenBuilder builder = new OpenBuilder();
		builder.setVersion(new ProtocolVersion((short) versionValue));
		builder.setProcessingRule(header.isProcessingRule());
		builder.setIgnore(header.isIgnore());
		builder.setDeadTimer((short) UnsignedBytes.toInt(bytes[DEAD_TIMER_OFFSET]));
		builder.setKeepalive((short) UnsignedBytes.toInt(bytes[KEEPALIVE_F_OFFSET]));
		builder.setSessionId((short) UnsignedBytes.toInt(bytes[SID_F_OFFSET]));

		final TlvsBuilder tbuilder = new TlvsBuilder();
		parseTlvs(tbuilder, ByteArray.cutBytes(bytes, TLVS_OFFSET));
		builder.setTlvs(tbuilder.build());
		return builder.build();
	}

	@Override
	public void addTlv(final TlvsBuilder tbuilder, final Tlv tlv) {
		if (tlv instanceof OfList) {
			tbuilder.setOfList((OfList) tlv);
		} else if (tlv instanceof Stateful) {
			tbuilder.setStateful((Stateful) tlv);
		} else if (tlv instanceof PredundancyGroupId) {
			tbuilder.setPredundancyGroupId((PredundancyGroupId) tlv);
		} else if (tlv instanceof LspDbVersion) {
			tbuilder.setLspDbVersion((LspDbVersion) tlv);
		}
	}

	@Override
	public byte[] serializeObject(final Object object) {
		if (!(object instanceof OpenObject)) {
			throw new IllegalArgumentException("Wrong instance of PCEPObject. Passed " + object.getClass() + ". Needed OpenObject.");
		}
		final OpenObject open = (OpenObject) object;

		final byte versionFlagMF = (byte) (PCEP_VERSION << (Byte.SIZE - VERSION_SF_LENGTH));

		final byte[] tlvs = serializeTlvs(open.getTlvs());

		final byte[] bytes = new byte[TLVS_OFFSET + tlvs.length + getPadding(TLVS_OFFSET + tlvs.length, PADDED_TO)];

		bytes[VER_FLAGS_MF_OFFSET] = versionFlagMF;
		bytes[KEEPALIVE_F_OFFSET] = UnsignedBytes.checkedCast(open.getKeepalive());
		bytes[DEAD_TIMER_OFFSET] = UnsignedBytes.checkedCast(open.getDeadTimer());
		bytes[SID_F_OFFSET] = UnsignedBytes.checkedCast(open.getSessionId());
		if (tlvs.length != 0) {
			ByteArray.copyWhole(tlvs, bytes, TLVS_OFFSET);
		}
		return bytes;
	}

	public byte[] serializeTlvs(final Tlvs tlvs) {
		if (tlvs == null) {
			return new byte[0];
		}
		int finalLength = 0;
		byte[] ofListBytes = null;
		byte[] statefulBytes = null;
		byte[] predundancyBytes = null;
		byte[] lspDbBytes = null;
		if (tlvs.getOfList() != null) {
			ofListBytes = serializeTlv(tlvs.getOfList());
			finalLength += ofListBytes.length;
		}
		if (tlvs.getStateful() != null) {
			statefulBytes = serializeTlv(tlvs.getStateful());
			finalLength += statefulBytes.length;
		}
		if (tlvs.getPredundancyGroupId() != null) {
			predundancyBytes = serializeTlv(tlvs.getPredundancyGroupId());
			finalLength += predundancyBytes.length;
		}
		if (tlvs.getLspDbVersion() != null) {
			lspDbBytes = serializeTlv(tlvs.getLspDbVersion());
			finalLength += lspDbBytes.length;
		}
		int offset = 0;
		final byte[] result = new byte[finalLength];
		if (ofListBytes != null) {
			ByteArray.copyWhole(ofListBytes, result, offset);
			offset += ofListBytes.length;
		}
		if (statefulBytes != null) {
			ByteArray.copyWhole(statefulBytes, result, offset);
			offset += statefulBytes.length;
		}
		if (lspDbBytes != null) {
			ByteArray.copyWhole(lspDbBytes, result, offset);
			offset += lspDbBytes.length;
		}
		if (predundancyBytes != null) {
			ByteArray.copyWhole(predundancyBytes, result, offset);
			offset += predundancyBytes.length;
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
