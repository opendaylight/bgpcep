/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.parser.message;

import static com.google.common.base.Preconditions.checkArgument;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.opendaylight.protocol.pcep.spi.AbstractMessageParser;
import org.opendaylight.protocol.pcep.spi.MessageUtil;
import org.opendaylight.protocol.pcep.spi.ObjectRegistry;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.PCEPErrors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev181109.PcntfBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.PcntfMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.notification.object.CNotification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcntf.message.PcntfMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcntf.message.pcntf.message.Notifications;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcntf.message.pcntf.message.notifications.NotificationsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcntf.message.pcntf.message.notifications.Rps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcntf.message.pcntf.message.notifications.RpsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.rp.object.Rp;

/**
 * Parser for {@link PcntfMessage}
 */
public class PCEPNotificationMessageParser extends AbstractMessageParser {

    public static final int TYPE = 5;

    public PCEPNotificationMessageParser(final ObjectRegistry registry) {
        super(registry);
    }

    @Override
    public void serializeMessage(final Message message, final ByteBuf out) {
        checkArgument(message instanceof PcntfMessage,
            "Wrong instance of Message. Passed instance of %s. Need PcntfMessage.", message.getClass());
        final ByteBuf buffer = Unpooled.buffer();
        for (final Notifications n : ((PcntfMessage) message).getPcntfMessage().getNotifications()) {
            if (n.getRps() != null) {
                for (final Rps rps : n.getRps()) {
                    serializeObject(rps.getRp(), buffer);
                }
            }
            checkArgument(n.getNotifications() != null && !n.getNotifications().isEmpty(),
                    "Message must contain at least one notification object");
            for (final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcntf
                    .message.pcntf.message.notifications.Notifications not : n.getNotifications()) {
                serializeObject(not.getCNotification(), buffer);
            }
        }
        MessageUtil.formatMessage(TYPE, buffer, out);
    }

    @Override
    protected Message validate(final List<Object> objects, final List<Message> errors)
            throws PCEPDeserializerException {
        checkArgument(objects != null, "Passed list can't be null.");
        if (objects.isEmpty()) {
            throw new PCEPDeserializerException("Notification message cannot be empty.");
        }

        final List<Notifications> compositeNotifications = new ArrayList<>();

        while (!objects.isEmpty()) {
            final Notifications comObj = getValidNotificationComposite(objects, errors);
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
        return new PcntfBuilder().setPcntfMessage(
            new PcntfMessageBuilder().setNotifications(compositeNotifications).build()).build();
    }

    private static Notifications getValidNotificationComposite(final List<Object> objects, final List<Message> errors) {
        final List<Rps> requestParameters = new ArrayList<>();
        final List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcntf.message
            .pcntf.message.notifications.Notifications> notifications = new ArrayList<>();
        Object obj;

        State state = State.INIT;
        while (!objects.isEmpty() && !state.equals(State.END)) {
            obj = objects.get(0);
            if ((state = insertObject(state, obj, errors, requestParameters, notifications)) == null) {
                return null;
            }
            if (!state.equals(State.END)) {
                objects.remove(0);
            }
        }

        if (notifications.isEmpty()) {
            return null;
        }

        return new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcntf.message
                .pcntf.message.NotificationsBuilder().setNotifications(notifications).setRps(requestParameters).build();
    }

    private static State insertObject(final State state, final Object obj, final List<Message> errors,
            final List<Rps> requestParameters, final List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcntf.message.pcntf.message.notifications.Notifications> notifications) {
        switch (state) {
            case INIT:
                if (obj instanceof Rp) {
                    final Rp rp = (Rp) obj;
                    if (rp.isProcessingRule()) {
                        errors.add(createErrorMsg(PCEPErrors.P_FLAG_NOT_SET, Optional.empty()));
                        return null;
                    }
                    requestParameters.add(new RpsBuilder().setRp(rp).build());
                    return State.INIT;
                }
            case RP_IN:
                if (obj instanceof CNotification) {
                    final CNotification n = (CNotification) obj;
                    notifications.add(new NotificationsBuilder().setCNotification(n).build());
                    return State.RP_IN;
                }
            case NOTIFICATION_IN:
            case END:
                return State.END;
            default:
                return state;
        }
    }

    private enum State {
        INIT, RP_IN, NOTIFICATION_IN, END
    }
}
