/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.message;

import io.netty.buffer.ByteBuf;

import java.util.Arrays;
import java.util.List;

import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.PCEPDocumentedException;
import org.opendaylight.protocol.pcep.PCEPErrorMapping;
import org.opendaylight.protocol.pcep.PCEPErrors;
import org.opendaylight.protocol.pcep.UnknownObject;
import org.opendaylight.protocol.pcep.spi.AbstractMessageParser;
import org.opendaylight.protocol.pcep.spi.ObjectHandlerRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.PcerrBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.PcntfBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.NotificationObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.PcntfMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.RpObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.PcerrMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.ErrorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcntf.message.PcntfMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcntf.message.pcntf.message.notifications.Notifications;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcntf.message.pcntf.message.notifications.Rps;

import com.google.common.collect.Lists;

/**
 * Parser for {@link PcntfMessage}
 */
public class PCEPNotificationMessageParser extends AbstractMessageParser {

	private final int TYPE = 5;

	public PCEPNotificationMessageParser(final ObjectHandlerRegistry registry) {
		super(registry);
	}

	@Override
	public void serializeMessage(final Message message, final ByteBuf buffer) {
		if (!(message instanceof PcntfMessage)) {
			throw new IllegalArgumentException("Wrong instance of Message. Passed instance of " + message.getClass()
					+ ". Needed PcntfMessage.");
		}
		final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcntf.message.PcntfMessage msg = ((PcntfMessage) message).getPcntfMessage();

		for (final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcntf.message.pcntf.message.Notifications n : msg.getNotifications()) {
			if (n.getRps() != null && !n.getRps().isEmpty()) {
				for (final Rps rps : n.getRps()) {
					buffer.writeBytes(serializeObject(rps));
				}
			}
			if (n.getNotifications() == null || n.getNotifications().isEmpty()) {
				throw new IllegalArgumentException("Message must contain at least one notification object");
			} else {
				for (final Notifications not : n.getNotifications()) {
					buffer.writeBytes(serializeObject(not));
				}
			}
		}
	}

	@Override
	public Message parseMessage(final byte[] buffer) throws PCEPDeserializerException, PCEPDocumentedException {
		if (buffer == null || buffer.length == 0) {
			throw new PCEPDeserializerException("Notification message cannot be empty.");
		}
		final List<Object> objs = parseObjects(buffer);

		return validate(objs);
	}

	public Message validate(final List<Object> objects) throws PCEPDeserializerException {
		if (objects == null) {
			throw new IllegalArgumentException("Passed list can't be null.");
		}

		final PCEPErrorMapping maping = PCEPErrorMapping.getInstance();

		final List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcntf.message.pcntf.message.Notifications> compositeNotifications = Lists.newArrayList();

		while (!objects.isEmpty()) {
			org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcntf.message.pcntf.message.Notifications comObj;
			try {
				comObj = getValidNotificationComposite(objects);
			} catch (final PCEPDocumentedException e) {
				final PcerrMessageBuilder b = new PcerrMessageBuilder();
				b.setErrors(Arrays.asList(new ErrorsBuilder().setType(maping.getFromErrorsEnum(e.getError()).type).setValue(
						maping.getFromErrorsEnum(e.getError()).value).build()));
				return new PcerrBuilder().setPcerrMessage(b.build()).build();
			}

			if (comObj == null) {
				break;
			}

			compositeNotifications.add(comObj);
		}

		if (compositeNotifications.isEmpty()) {
			throw new PCEPDeserializerException("Atleast one CompositeNotifiObject is mandatory.");
		}

		if (!objects.isEmpty()) {
			throw new PCEPDeserializerException("Unprocessed Objects: " + objects);
		}

		return new PcntfBuilder().setPcntfMessage(new PcntfMessageBuilder().setNotifications(compositeNotifications).build()).build();
	}

	private static org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcntf.message.pcntf.message.Notifications getValidNotificationComposite(
			final List<Object> objects) throws PCEPDocumentedException {
		final List<Rps> requestParameters = Lists.newArrayList();
		final List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcntf.message.pcntf.message.notifications.Notifications> notifications = Lists.newArrayList();
		Object obj;

		int state = 1;
		while (!objects.isEmpty()) {
			obj = objects.get(0);

			if (obj instanceof UnknownObject) {
				throw new PCEPDocumentedException("Unknown object", ((UnknownObject) obj).getError());
			}

			switch (state) {
			case 1:
				state = 2;
				if (obj instanceof RpObject) {
					final RpObject rp = (RpObject) obj;
					if (rp.isProcessingRule()) {
						throw new PCEPDocumentedException("Invalid setting of P flag.", PCEPErrors.P_FLAG_NOT_SET);
					}
					requestParameters.add((Rps) rp);
					state = 1;
					break;
				}
			case 2:
				if (obj instanceof NotificationObject) {
					final NotificationObject n = (NotificationObject) obj;
					notifications.add((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcntf.message.pcntf.message.notifications.Notifications) n);
					state = 2;
					break;
				}
				state = 3;
			}

			if (state == 3) {
				break;
			}

			objects.remove(obj);
		}

		if (notifications.isEmpty()) {
			return null;
		}

		return new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcntf.message.pcntf.message.NotificationsBuilder().setNotifications(
				notifications).setRps(requestParameters).build();
	}

	@Override
	public int getMessageType() {
		return this.TYPE;
	}
}
