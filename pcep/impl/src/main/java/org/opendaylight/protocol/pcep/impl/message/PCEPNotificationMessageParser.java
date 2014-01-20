/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.message;

import io.netty.buffer.ByteBuf;

import java.util.List;

import org.opendaylight.protocol.pcep.spi.AbstractMessageParser;
import org.opendaylight.protocol.pcep.spi.ObjectHandlerRegistry;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.PCEPErrors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.PcntfBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.PcntfMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.notification.object.CNotification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcntf.message.PcntfMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcntf.message.pcntf.message.notifications.Notifications;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcntf.message.pcntf.message.notifications.NotificationsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcntf.message.pcntf.message.notifications.Rps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcntf.message.pcntf.message.notifications.RpsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.rp.object.Rp;

import com.google.common.collect.Lists;

/**
 * Parser for {@link PcntfMessage}
 */
public class PCEPNotificationMessageParser extends AbstractMessageParser {

	public static final int TYPE = 5;

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
					buffer.writeBytes(serializeObject(rps.getRp()));
				}
			}
			if (n.getNotifications() == null || n.getNotifications().isEmpty()) {
				throw new IllegalArgumentException("Message must contain at least one notification object");
			} else {
				for (final Notifications not : n.getNotifications()) {
					buffer.writeBytes(serializeObject(not.getCNotification()));
				}
			}
		}
	}

	@Override
	protected Message validate(final List<Object> objects, final List<Message> errors) throws PCEPDeserializerException {
		if (objects == null) {
			throw new IllegalArgumentException("Passed list can't be null.");
		}
		if (objects.isEmpty()) {
			throw new PCEPDeserializerException("Notification message cannot be empty.");
		}

		final List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcntf.message.pcntf.message.Notifications> compositeNotifications = Lists.newArrayList();

		while (!objects.isEmpty()) {
			org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcntf.message.pcntf.message.Notifications comObj;
			comObj = getValidNotificationComposite(objects, errors);

			if (comObj == null) {
				break;
			}
			compositeNotifications.add(comObj);
		}
		if (compositeNotifications.isEmpty()) {
			throw new PCEPDeserializerException("Atleast one Notifications is mandatory.");
		}
		if (!objects.isEmpty()) {
			throw new PCEPDeserializerException("Unprocessed Objects: " + objects);
		}
		return new PcntfBuilder().setPcntfMessage(new PcntfMessageBuilder().setNotifications(compositeNotifications).build()).build();
	}

	private static org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcntf.message.pcntf.message.Notifications getValidNotificationComposite(
			final List<Object> objects, final List<Message> errors) {
		final List<Rps> requestParameters = Lists.newArrayList();
		final List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcntf.message.pcntf.message.notifications.Notifications> notifications = Lists.newArrayList();
		Object obj;

		State state = State.Init;
		while (!objects.isEmpty() && !state.equals(State.End)) {
			obj = objects.get(0);
			switch (state) {
			case Init:
				state = State.RpIn;
				if (obj instanceof Rp) {
					final Rp rp = (Rp) obj;
					if (rp.isProcessingRule()) {
						errors.add(createErrorMsg(PCEPErrors.P_FLAG_NOT_SET));
						return null;
					}
					requestParameters.add(new RpsBuilder().setRp(rp).build());
					state = State.Init;
					break;
				}
			case RpIn:
				state = State.NotificationIn;
				if (obj instanceof CNotification) {
					final CNotification n = (CNotification) obj;
					notifications.add(new NotificationsBuilder().setCNotification(n).build());
					state = State.RpIn;
					break;
				}
			case NotificationIn:
				state = State.End;
				break;
			case End:
				break;
			}
			if (!state.equals(State.End)) {
				objects.remove(0);
			}
		}

		if (notifications.isEmpty()) {
			return null;
		}

		return new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcntf.message.pcntf.message.NotificationsBuilder().setNotifications(
				notifications).setRps(requestParameters).build();
	}

	private enum State {
		Init, RpIn, NotificationIn, End
	}

	@Override
	public int getMessageType() {
		return TYPE;
	}
}
