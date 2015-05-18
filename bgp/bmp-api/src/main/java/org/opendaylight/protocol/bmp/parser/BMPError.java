package org.opendaylight.protocol.bmp.parser;/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

import com.google.common.collect.Maps;

import java.util.Map;


/**
 * Created by cgasparini on 18.5.2015.
 */
public enum BMPError {
    /**
     * Bad Message Length. 1/1
     */
    BAD_MSG_LENGTH((short) 1, (short) 1),
    /**
     * Optional Attribute Error. 1/2
     */
    OPT_ATTR_ERROR((short) 1, (short) 2),
    /**
     * PDU Parsing Error. 1/3
     */
    PDU_PARSE_ERROR((short) 1, (short) 3),
    /**
     * Unspecific Open Message error. 1/4
     */
    UNSPECIFIC_OPEN_ERROR((short) 1, (short) 4),
    /**
     * Unspecific Update Message error. 1/5
     */
    UNSPECIFIC_UPDATE_ERROR((short) 1, (short) 5);

    private static final Map<BMPErrorIdentifier, BMPError> VALUE_MAP;

    static {
        VALUE_MAP = Maps.newHashMap();
        for (final BMPError enumItem : BMPError.values()) {
            VALUE_MAP.put(enumItem.getErrorIdentifier(), enumItem);
        }
    }

    private final BMPErrorIdentifier errorId;

    BMPError(final short code, final short subcode) {
        this.errorId = new BMPErrorIdentifier(code, subcode);
    }

    public short getCode() {
        return this.errorId.getCode();
    }

    public short getSubcode() {
        return this.errorId.getSubCode();
    }

    private BMPErrorIdentifier getErrorIdentifier() {
        return this.errorId;
    }

    public static BMPError forValue(final int code, final int subcode) {
        final BMPError e = VALUE_MAP.get(new BMPErrorIdentifier((short) code, (short) subcode));
        if (e != null) {
            return e;
        }
        throw new IllegalArgumentException("BGP Error code " + code + " and subcode " + subcode + " not recognized.");
    }
}
