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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.NotificationObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.OverloadDurationTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.notification.object.Tlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.notification.object.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.notification.object.tlvs.OverloadDurationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcntf.message.pcntf.message.notifications.NotificationsBuilder;

/**
 * Parser for {@link NotificationObject}
 */
public class PCEPNotificationObjectParser extends AbstractObjectWithTlvsParser<NotificationsBuilder> {

	public static final int CLASS = 12;

	public static final int TYPE = 1;

	/*
	 * lengths of fields
	 */
	public static final int FLAGS_F_LENGTH = 1;
	public static final int NT_F_LENGTH = 1;
	public static final int NV_F_LENGTH = 1;

	/*
	 * offsets of fields
	 */
	public static final int FLAGS_F_OFFSET = 1; // added reserved filed of size 1
	public static final int NT_F_OFFSET = FLAGS_F_OFFSET + FLAGS_F_LENGTH;
	public static final int NV_F_OFFSET = NT_F_OFFSET + NT_F_LENGTH;
	public static final int TLVS_OFFSET = NV_F_OFFSET + NV_F_LENGTH;

	public PCEPNotificationObjectParser(final TlvHandlerRegistry tlvReg) {
		super(tlvReg);
	}

	@Override
	public NotificationObject parseObject(final ObjectHeader header, final byte[] bytes) throws PCEPDeserializerException,
	PCEPDocumentedException {
		if (bytes == null || bytes.length == 0) {
			throw new IllegalArgumentException("Array of bytes is mandatory. Can't be null or empty.");
		}

		final NotificationsBuilder builder = new NotificationsBuilder();

		parseTlvs(builder, ByteArray.cutBytes(bytes, TLVS_OFFSET));

		builder.setIgnore(header.isIgnore());
		builder.setProcessingRule(header.isProcessingRule());

		builder.setType((short) (bytes[NT_F_OFFSET] & 0xFF));
		builder.setValue((short) (bytes[NV_F_OFFSET] & 0xFF));

		return builder.build();
	}

	@Override
	public void addTlv(final NotificationsBuilder builder, final Tlv tlv) {
		if (tlv instanceof OverloadDurationTlv && builder.getType() == 2 && builder.getValue() == 1) {
			builder.setTlvs(new TlvsBuilder().setOverloadDuration(
					new OverloadDurationBuilder().setDuration(((OverloadDurationTlv) tlv).getDuration()).build()).build());
		}
	}

	@Override
	public byte[] serializeObject(final Object object) {
		if (!(object instanceof NotificationObject)) {
			throw new IllegalArgumentException("Wrong instance of PCEPObject. Passed " + object.getClass() + ". Needed NotificationObject.");
		}

		final NotificationObject notObj = (NotificationObject) object;

		final byte[] tlvs = serializeTlvs(notObj.getTlvs());
		int tlvsLength = 0;
		if (tlvs != null) {
			tlvsLength = tlvs.length;
		}
		final byte[] retBytes = new byte[TLVS_OFFSET + tlvsLength + Util.getPadding(TLVS_OFFSET + tlvs.length, PADDED_TO)];

		if (tlvs != null) {
			ByteArray.copyWhole(tlvs, retBytes, TLVS_OFFSET);
		}

		retBytes[NT_F_OFFSET] = ByteArray.shortToBytes(notObj.getType())[1];
		retBytes[NV_F_OFFSET] = ByteArray.shortToBytes(notObj.getValue())[1];

		return retBytes;
	}

	public byte[] serializeTlvs(final Tlvs tlvs) {
		if (tlvs.getOverloadDuration() != null) {
			// FIXME : add
			// return serializeTlv(new NoPathVectorBuilder().setFlags(tlvs.getNoPathVector()).build());
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
