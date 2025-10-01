/*
 * Copyright (c) 2025 Orange.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.spi;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.util.Arrays;
import org.opendaylight.yangtools.yang.common.Uint8;

/**
 * PCEP Notification Object Type/Value.
 *
 * @see <a href="http://tools.ietf.org/html/rfc5440#section-7.14">NOTIFICATION Object(RFC5440)</a>
 * @see <a href="https://www.iana.org/assignments/pcep/pcep.xhtml#notification-object">Notification Object</a>
 */
public enum PCEPNotifications {
    // Pending Request Cancelled Notifiction-Type = 1.

    /**
     * PCC cancels a set of pending requests.
     */
    PCC_CANCEL_PRENDING_REQ(1, 1),
    /**
     * PCE cancels a set of pending requests.
     */
    PCE_CANCEL_PRENDING_REQ(1, 2),

    // PCE Congestion: Notification-Type = 2.

    /**
     * PCE in congested state.
     */
    PCE_CONGESTED(2, 1),
    /**
     * PCE no longer in congested state.
     */
    PCE_NOT_CONGESTED(2, 2),

    // Unassigned: Notification-Type = 3.

    // Stateful PCE resource limit exceeded: Notification-Type = 4.

    /**
     * Entering resource limit exceeded state.
     */
    RESOURCE_LIMIT_EXCEEDED(4, 1),

    // Auto-Bandwidth Overwhelm State: Notification-Type = 5.

    /**
     * Entering Auto-Bandwidth Overwhelm State.
     */
    AUTO_BANDWIDTH_OVERWHELMED(5, 1),
    /**
     * Clearing Auto-Bandwidth Overwhelm State.
     */
    AUTO_BANDWIDTH_NOT_OVERWHELMED(5, 2),

    LAST_NOTIFICATION(255, 255);

    private static final ImmutableMap<PCEPNotificationIdentifier, PCEPNotifications> VALUE_MAP = Maps.uniqueIndex(
        Arrays.asList(values()), PCEPNotifications::getNotificationIdentifier);

    private PCEPNotificationIdentifier notifId;

    public static PCEPNotifications forValue(final Uint8 errorType, final Uint8 errorValue) {
        return VALUE_MAP.get(new PCEPNotificationIdentifier(errorType, errorValue));
    }

    PCEPNotifications(final int type, final int value) {
        this.notifId = new PCEPNotificationIdentifier(Uint8.valueOf(type), Uint8.valueOf(value));
    }

    private PCEPNotificationIdentifier getNotificationIdentifier() {
        return notifId;
    }

    public Uint8 getNotificationType() {
        return notifId.getType();
    }

    public Uint8 getNotificationValue() {
        return notifId.getValue();
    }
}
