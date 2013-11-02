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
import org.opendaylight.protocol.pcep.spi.TlvHandlerRegistry;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.PcepErrorObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcep.error.object.Tlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcep.error.object.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.Errors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.ErrorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.req.missing.tlv.ReqMissing;

import com.google.common.primitives.UnsignedBytes;

/**
 * Parser for {@link org.opendaylight.protocol.pcep.object.PCEPErrorObject PCEPErrorObject}
 */
public class PCEPErrorObjectParser extends AbstractObjectWithTlvsParser<ErrorsBuilder> {

	public static final int CLASS = 13;

	public static final int TYPE = 1;

	private static final int FLAGS_F_LENGTH = 1;
	private static final int ET_F_LENGTH = 1;
	private static final int EV_F_LENGTH = 1;

	private static final int FLAGS_F_OFFSET = 1;
	private static final int ET_F_OFFSET = FLAGS_F_OFFSET + FLAGS_F_LENGTH;
	private static final int EV_F_OFFSET = ET_F_OFFSET + ET_F_LENGTH;
	private static final int TLVS_OFFSET = EV_F_OFFSET + EV_F_LENGTH;

	public PCEPErrorObjectParser(final TlvHandlerRegistry tlvReg) {
		super(tlvReg);
	}

	@Override
	public PcepErrorObject parseObject(final ObjectHeader header, final byte[] bytes) throws PCEPDeserializerException,
			PCEPDocumentedException {
		if (bytes == null) {
			throw new IllegalArgumentException("Array of bytes is mandatory.");
		}

		final ErrorsBuilder builder = new ErrorsBuilder();
		builder.setIgnore(header.isIgnore());
		builder.setProcessingRule(header.isProcessingRule());
		builder.setType((short) UnsignedBytes.toInt(bytes[ET_F_OFFSET]));
		builder.setValue((short) UnsignedBytes.toInt(bytes[EV_F_OFFSET]));
		parseTlvs(builder, ByteArray.cutBytes(bytes, TLVS_OFFSET));
		return builder.build();
	}

	@Override
	public void addTlv(final ErrorsBuilder builder, final Tlv tlv) {
		if (tlv instanceof ReqMissing && builder.getType() == 7) {
			builder.setTlvs(new TlvsBuilder().setReqMissing((ReqMissing) tlv).build());
		}
	}

	@Override
	public byte[] serializeObject(final Object object) {
		if (!(object instanceof PcepErrorObject)) {
			throw new IllegalArgumentException("Wrong instance of PCEPObject. Passed " + object.getClass() + ". Needed PcepErrorObject.");
		}
		final PcepErrorObject errObj = (PcepErrorObject) object;

		final byte[] tlvs = serializeTlvs(((Errors) errObj).getTlvs());

		final byte[] retBytes = new byte[TLVS_OFFSET + tlvs.length + getPadding(TLVS_OFFSET + tlvs.length, PADDED_TO)];
		if (tlvs.length != 0) {
			ByteArray.copyWhole(tlvs, retBytes, TLVS_OFFSET);
		}
		retBytes[ET_F_OFFSET] = UnsignedBytes.checkedCast(errObj.getType());
		retBytes[EV_F_OFFSET] = UnsignedBytes.checkedCast(errObj.getValue());
		return retBytes;
	}

	public byte[] serializeTlvs(final Tlvs tlvs) {
		if (tlvs == null) {
			return new byte[0];
		} else if (tlvs.getReqMissing() != null) {
			return serializeTlv(tlvs.getReqMissing());
		}
		return new byte[0];
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
