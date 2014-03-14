/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.object;

import org.opendaylight.protocol.pcep.spi.AbstractObjectWithTlvsParser;
import org.opendaylight.protocol.pcep.spi.ObjectUtil;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.TlvRegistry;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.notification.object.CNotification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.notification.object.CNotificationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.notification.object.c.notification.Tlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.notification.object.c.notification.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.overload.duration.tlv.OverloadDuration;

import com.google.common.primitives.UnsignedBytes;

/**
 * Parser for {@link CNotification}
 */
public class PCEPNotificationObjectParser extends AbstractObjectWithTlvsParser<CNotificationBuilder> {

	public static final int CLASS = 12;

	public static final int TYPE = 1;

	/*
	 * lengths of fields
	 */
	private static final int FLAGS_F_LENGTH = 1;
	private static final int NT_F_LENGTH = 1;
	private static final int NV_F_LENGTH = 1;

	/*
	 * offsets of fields
	 */
	private static final int FLAGS_F_OFFSET = 1;
	private static final int NT_F_OFFSET = FLAGS_F_OFFSET + FLAGS_F_LENGTH;
	private static final int NV_F_OFFSET = NT_F_OFFSET + NT_F_LENGTH;
	private static final int TLVS_OFFSET = NV_F_OFFSET + NV_F_LENGTH;

	public PCEPNotificationObjectParser(final TlvRegistry tlvReg) {
		super(tlvReg);
	}

	@Override
	public CNotification parseObject(final ObjectHeader header, final byte[] bytes) throws PCEPDeserializerException {
		if (bytes == null || bytes.length == 0) {
			throw new IllegalArgumentException("Array of bytes is mandatory. Can't be null or empty.");
		}
		final CNotificationBuilder builder = new CNotificationBuilder();
		builder.setIgnore(header.isIgnore());
		builder.setProcessingRule(header.isProcessingRule());
		builder.setType((short) UnsignedBytes.toInt(bytes[NT_F_OFFSET]));
		builder.setValue((short) UnsignedBytes.toInt(bytes[NV_F_OFFSET]));
		parseTlvs(builder, ByteArray.cutBytes(bytes, TLVS_OFFSET));
		return builder.build();
	}

	@Override
	public void addTlv(final CNotificationBuilder builder, final Tlv tlv) {
		if (tlv instanceof OverloadDuration && builder.getType() == 2 && builder.getValue() == 1) {
			builder.setTlvs(new TlvsBuilder().setOverloadDuration((OverloadDuration) tlv).build());
		}
	}

	@Override
	public byte[] serializeObject(final Object object) {
		if (!(object instanceof CNotification)) {
			throw new IllegalArgumentException("Wrong instance of PCEPObject. Passed " + object.getClass() + ". Needed NotificationObject.");
		}
		final CNotification notObj = (CNotification) object;

		final byte[] tlvs = serializeTlvs(notObj.getTlvs());

		final byte[] retBytes = new byte[TLVS_OFFSET + tlvs.length + getPadding(TLVS_OFFSET + tlvs.length, PADDED_TO)];
		if (tlvs.length != 0) {
			ByteArray.copyWhole(tlvs, retBytes, TLVS_OFFSET);
		}
		retBytes[NT_F_OFFSET] = UnsignedBytes.checkedCast(notObj.getType());
		retBytes[NV_F_OFFSET] = UnsignedBytes.checkedCast(notObj.getValue());
		return ObjectUtil.formatSubobject(TYPE, CLASS, object.isProcessingRule(), object.isIgnore(), retBytes);
	}

	public byte[] serializeTlvs(final Tlvs tlvs) {
		if (tlvs == null) {
			return new byte[0];
		} else if (tlvs.getOverloadDuration() != null) {
			return serializeTlv(tlvs.getOverloadDuration());
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
