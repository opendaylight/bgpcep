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

public enum PathType {

    CLASSPATH(0),
    PATH(1);

    private int value;
    private static final Map<Integer, PathType> VALUE_MAP;

    static {
        final ImmutableMap.Builder<Integer, PathType> b = ImmutableMap.builder();
        for (final PathType enumItem : PathType.values())
        {
            b.put(enumItem.value, enumItem);
        }
        VALUE_MAP = b.build();
    }

    private PathType(final int value) {
        this.value = value;
    }

    public int getIntValue() {
        return value;
    }

    public static PathType forValue(final int valueArg) {
        return VALUE_MAP.get(valueArg);
    }
}

