/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.spi;

import com.google.common.collect.ImmutableMap;
import java.util.Map;

/**
 * Internal session state.
 */
public enum State {
    /**
     * The session object is created by the negotiator in OpenConfirm state. While in this state, the session object
     * is half-alive, e.g. the timers are running, but the session is not completely up, e.g. it has not been
     * announced to the listener. If the session is torn down in this state, we do not inform the listener.
     */
    OPEN_CONFIRM((short) 0),
    /**
     * The session has been completely established.
     */
    UP((short) 1),
    /**
     * The session has been closed. It will not be resurrected.
     */
    IDLE((short) 2);

    private static final Map<Short, State> VALUE_MAP;

    static {
        final ImmutableMap.Builder<Short, State> b = ImmutableMap.builder();
        for (final State enumItem : State.values()) {
            b.put(enumItem.getValue(), enumItem);
        }
        VALUE_MAP = b.build();
    }

    private final short value;

    State(final short value) {
        this.value = value;
    }

    public static State forValue(final short value) {
        return VALUE_MAP.get(value);
    }

    public short getValue() {
        return this.value;
    }
}