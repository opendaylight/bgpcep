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
import org.opendaylight.protocol.pcep.PCEPObject;
import org.opendaylight.protocol.pcep.impl.PCEPTlvParser;
import org.opendaylight.protocol.pcep.object.PCEPNotificationObject;
import org.opendaylight.protocol.pcep.spi.AbstractObjectParser;
import org.opendaylight.protocol.pcep.spi.HandlerRegistry;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.NotificationObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.OverloadDurationTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.notification.object.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.notification.object.tlvs.OverloadDurationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcntf.message.pcntf.message.notifications.NotificationsBuilder;

/**
 * Parser for {@link org.opendaylight.protocol.pcep.object.PCEPNotificationObject PCEPNotificationObject}
 */
public class PCEPNotificationObjectParser extends AbstractObjectParser<NotificationsBuilder> {

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

	public PCEPNotificationObjectParser(final HandlerRegistry registry) {
		super(registry);
	}

	@Override
	public NotificationObject parseObject(final ObjectHeader header, final byte[] bytes) throws PCEPDeserializerException,
			PCEPDocumentedException {
		if (bytes == null || bytes.length == 0)
			throw new IllegalArgumentException("Array of bytes is mandatory. Can't be null or empty.");

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
		if (tlv instanceof OverloadDurationTlv && builder.getType() == 2 && builder.getValue() == 1)
			builder.setTlvs(new TlvsBuilder().setOverloadDuration(
					new OverloadDurationBuilder().setDuration(((OverloadDurationTlv) tlv).getDuration()).build()).build());
	}

	public byte[] put(final PCEPObject obj) {
		if (!(obj instanceof PCEPNotificationObject))
			throw new IllegalArgumentException("Wrong instance of PCEPObject. Passed " + obj.getClass()
					+ ". Needed PCEPNotificationObject.");

		final PCEPNotificationObject notObj = (PCEPNotificationObject) obj;

		final byte[] tlvs = PCEPTlvParser.put(notObj.getTlvs());
		final byte[] retBytes = new byte[TLVS_OFFSET + tlvs.length];

		ByteArray.copyWhole(tlvs, retBytes, TLVS_OFFSET);

		retBytes[NT_F_OFFSET] = ByteArray.shortToBytes(notObj.getType())[1];
		retBytes[NV_F_OFFSET] = ByteArray.shortToBytes(notObj.getValue())[1];

		return retBytes;
	}
}
