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
import org.opendaylight.protocol.pcep.impl.Util;
import org.opendaylight.protocol.pcep.spi.TlvHandlerRegistry;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.LspDbVersionTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.OfListTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.OpenObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.PredundancyGroupIdTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ProtocolVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.StatefulCapabilityTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.message.open.message.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.Tlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.tlvs.LspDbVersionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.tlvs.OfListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.tlvs.PredundancyGroupIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.tlvs.StatefulBuilder;

import com.google.common.primitives.UnsignedBytes;

/**
 * Parser for {@link OpenObject}
 */

public class PCEPOpenObjectParser extends AbstractObjectWithTlvsParser<OpenBuilder> {

	public static final int CLASS = 1;

	public static final int TYPE = 1;

	/*
	 * lengths of fields in bytes
	 */
	public static final int VER_FLAGS_MF_LENGTH = 1; // multi-field
	public static final int KEEPALIVE_F_LENGTH = 1;
	public static final int DEAD_TIMER_LENGTH = 1;
	public static final int SID_F_LENGTH = 1;

	/*
	 * lengths of subfields inside multi-field in bits
	 */
	public static final int VERSION_SF_LENGTH = 3;
	public static final int FLAGS_SF_LENGTH = 5;

	/*
	 * offsets of field in bytes
	 */

	public static final int VER_FLAGS_MF_OFFSET = 0;
	public static final int KEEPALIVE_F_OFFSET = VER_FLAGS_MF_OFFSET + VER_FLAGS_MF_LENGTH;
	public static final int DEAD_TIMER_OFFSET = KEEPALIVE_F_OFFSET + KEEPALIVE_F_LENGTH;
	public static final int SID_F_OFFSET = DEAD_TIMER_OFFSET + DEAD_TIMER_LENGTH;
	public static final int TLVS_OFFSET = SID_F_OFFSET + SID_F_LENGTH;

	/*
	 * offsets of subfields inside multi-field in bits
	 */

	public static final int VERSION_SF_OFFSET = 0;
	public static final int FLAGS_SF_OFFSET = VERSION_SF_LENGTH + VERSION_SF_OFFSET;

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

		parseTlvs(builder, ByteArray.cutBytes(bytes, TLVS_OFFSET));

		builder.setVersion(new ProtocolVersion((short) versionValue));
		builder.setProcessingRule(header.isProcessingRule());
		builder.setIgnore(header.isIgnore());
		builder.setDeadTimer((short) UnsignedBytes.toInt(bytes[DEAD_TIMER_OFFSET]));
		builder.setKeepalive((short) UnsignedBytes.toInt(bytes[KEEPALIVE_F_OFFSET]));
		builder.setSessionId((short) UnsignedBytes.toInt(bytes[SID_F_OFFSET]));
		return builder.build();
	}

	@Override
	public void addTlv(final OpenBuilder builder, final Tlv tlv) {
		final TlvsBuilder tbuilder = new TlvsBuilder();
		if (tlv instanceof OfListTlv) {
			tbuilder.setOfList(new OfListBuilder().setCodes(((OfListTlv) tlv).getCodes()).build());
		} else if (tlv instanceof StatefulCapabilityTlv) {
			tbuilder.setStateful(new StatefulBuilder().setFlags(((StatefulCapabilityTlv) tlv).getFlags()).build());
		} else if (tlv instanceof PredundancyGroupIdTlv) {
			tbuilder.setPredundancyGroupId(new PredundancyGroupIdBuilder().setIdentifier(((PredundancyGroupIdTlv) tlv).getIdentifier()).build());
		} else if (tlv instanceof LspDbVersionTlv) {
			tbuilder.setLspDbVersion(new LspDbVersionBuilder().build());
		}
		builder.setTlvs(tbuilder.build());
	}

	@Override
	public byte[] serializeObject(final Object object) {
		if (!(object instanceof OpenObject)) {
			throw new IllegalArgumentException("Wrong instance of PCEPObject. Passed " + object.getClass() + ". Needed OpenObject.");
		}
		final OpenObject open = (OpenObject) object;

		final byte versionFlagMF = (byte) (PCEP_VERSION << (Byte.SIZE - VERSION_SF_LENGTH));

		final byte[] tlvs = serializeTlvs(open.getTlvs());

		final byte[] bytes = new byte[TLVS_OFFSET + tlvs.length + Util.getPadding(TLVS_OFFSET + tlvs.length, PADDED_TO)];

		bytes[VER_FLAGS_MF_OFFSET] = versionFlagMF;
		bytes[KEEPALIVE_F_OFFSET] = ByteArray.shortToBytes(open.getKeepalive())[1];
		bytes[DEAD_TIMER_OFFSET] = ByteArray.shortToBytes(open.getDeadTimer())[1];
		bytes[SID_F_OFFSET] = ByteArray.shortToBytes(open.getSessionId())[1];
		ByteArray.copyWhole(tlvs, bytes, TLVS_OFFSET);

		return bytes;
	}

	public byte[] serializeTlvs(final Tlvs tlvs) {
		int finalLength = 0;
		if (tlvs.getLspDbVersion() != null) {
			final byte[] lspDbBytes = serializeTlv(new LspDbVersionBuilder().setVersion(tlvs.getLspDbVersion().getVersion()).build());
			finalLength = lspDbBytes.length;
		}
		if (tlvs.getOfList() != null) {
			final byte[] ofListBytes = serializeTlv(new OfListBuilder().setCodes(tlvs.getOfList().getCodes()).build());
			finalLength = ofListBytes.length;
		}

		// FIXME: finish

		final byte[] bytes = new byte[finalLength];

		// FIXME copy result bytes
		return bytes;
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
