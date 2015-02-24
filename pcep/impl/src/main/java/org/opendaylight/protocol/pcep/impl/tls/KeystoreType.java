/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.tls;

import com.google.common.collect.ImmutableMap;
import java.util.Map;

public enum KeystoreType {

    JKS(0),
    PKCS12(1);

    int value;
    private static final Map<Integer, KeystoreType> VALUE_MAP;

    static {
        final ImmutableMap.Builder<java.lang.Integer, KeystoreType> b = ImmutableMap.builder();
        for (final KeystoreType enumItem : KeystoreType.values())
        {
            b.put(enumItem.value, enumItem);
        }
        VALUE_MAP = b.build();
    }

    private KeystoreType(final int value) {
        this.value = value;
    }

    public int getIntValue() {
        return value;
    }

    public static KeystoreType forValue(final int valueArg) {
        return VALUE_MAP.get(valueArg);
    }
}

