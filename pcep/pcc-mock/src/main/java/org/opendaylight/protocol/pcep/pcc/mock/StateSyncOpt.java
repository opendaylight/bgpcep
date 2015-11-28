/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.pcc.mock;

enum StateSyncOpt {
    Inactive(0),
    AvoidanceProcedure(1),
    IncrementalProcedure(2);

    int value;
    private static final java.util.Map<java.lang.Integer, StateSyncOpt> VALUE_MAP;

    static {
        final com.google.common.collect.ImmutableMap.Builder<java.lang.Integer, StateSyncOpt> b = com.google.common.collect.ImmutableMap.builder();
        for (StateSyncOpt enumItem : StateSyncOpt.values())
        {
            b.put(enumItem.value, enumItem);
        }

        VALUE_MAP = b.build();
    }

    private StateSyncOpt(int value) {
        this.value = value;
    }

    /**
     * @return integer value
     */
    public int getIntValue() {
        return value;
    }

    /**
     * @param valueArg
     * @return corresponding stateSyncOpt item
     */
    public static StateSyncOpt forValue(int valueArg) {
        return VALUE_MAP.get(valueArg);
    }
}
