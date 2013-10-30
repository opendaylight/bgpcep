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
import org.opendaylight.protocol.pcep.impl.Util;
import org.opendaylight.protocol.pcep.spi.TlvHandlerRegistry;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.PcepErrorObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ReqMissingTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcep.error.object.Tlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcep.error.object.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcep.error.object.tlvs.ReqMissingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.Errors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.ErrorsBuilder;

/**
 * Parser for {@link org.opendaylight.protocol.pcep.object.PCEPErrorObject PCEPErrorObject}
 */
public class PCEPErrorObjectParser extends AbstractObjectWithTlvsParser<ErrorsBuilder> {

	public static final int CLASS = 13;

	public static final int TYPE = 1;

	public static final int FLAGS_F_LENGTH = 1;
	public static final int ET_F_LENGTH = 1;
	public static final int EV_F_LENGTH = 1;

	public static final int FLAGS_F_OFFSET = 1; // added reserved field of size 1 byte
	public static final int ET_F_OFFSET = FLAGS_F_OFFSET + FLAGS_F_LENGTH;
	public static final int EV_F_OFFSET = ET_F_OFFSET + ET_F_LENGTH;
	public static final int TLVS_OFFSET = EV_F_OFFSET + EV_F_LENGTH;

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

		parseTlvs(builder, ByteArray.cutBytes(bytes, TLVS_OFFSET));

		builder.setIgnore(header.isIgnore());
		builder.setProcessingRule(header.isProcessingRule());

		builder.setType((short) (bytes[ET_F_OFFSET] & 0xFF));
		builder.setValue((short) (bytes[EV_F_OFFSET] & 0xFF));

		return builder.build();
	}

	@Override
	public void addTlv(final ErrorsBuilder builder, final Tlv tlv) {
		if (tlv instanceof ReqMissingTlv && builder.getType() == 7) {
			builder.setTlvs(new TlvsBuilder().setReqMissing(
					new ReqMissingBuilder().setRequestId(((ReqMissingTlv) tlv).getRequestId()).build()).build());
		}
	}

	@Override
	public byte[] serializeObject(final Object object) {
		if (!(object instanceof PcepErrorObject)) {
			throw new IllegalArgumentException("Wrong instance of PCEPObject. Passed " + object.getClass() + ". Needed PcepErrorObject.");
		}

		final PcepErrorObject errObj = (PcepErrorObject) object;

		final byte[] tlvs = serializeTlvs(((Errors) errObj).getTlvs());
		int tlvsLength = 0;
		if (tlvs != null) {
			tlvsLength = tlvs.length;
		}
		final byte[] retBytes = new byte[TLVS_OFFSET + tlvsLength + Util.getPadding(TLVS_OFFSET + tlvs.length, PADDED_TO)];

		if (tlvs != null) {
			ByteArray.copyWhole(tlvs, retBytes, TLVS_OFFSET);
		}

		retBytes[ET_F_OFFSET] = ByteArray.shortToBytes(errObj.getType())[1];
		retBytes[EV_F_OFFSET] = ByteArray.shortToBytes(errObj.getValue())[1];

		return retBytes;
	}

	public byte[] serializeTlvs(final Tlvs tlvs) {
		if (tlvs.getReqMissing() != null) {
			return serializeTlv(new ReqMissingBuilder().setRequestId(tlvs.getReqMissing().getRequestId()).build());
		}
		return null;
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
