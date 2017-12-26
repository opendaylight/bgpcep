/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import java.io.Serializable;
import java.util.Map;


/**
 * Possible errors from implemented RFCs and drafts. Each error consists of error code and error subcode
 * (code/subcode in comments).
 *
 * @see <a href="http://tools.ietf.org/html/rfc4271#section-4.5">BGP Notification Message</a>
 */
public enum BGPError {
    /**
     * Connection Not Synchronized. 1/1
     */
    CONNECTION_NOT_SYNC((short) 1, (short) 1),
    /**
     * Bad Message Length. 1/2
     */
    BAD_MSG_LENGTH((short) 1, (short) 2),
    /**
     * Bad Message Type. 1/3
     */
    BAD_MSG_TYPE((short) 1, (short) 3),
    /**
     * Unspecific Open Message error.
     */
    UNSPECIFIC_OPEN_ERROR((short) 2, (short) 0),
    /**
     * Unsupported Version Number. 2/1
     */
    VERSION_NOT_SUPPORTED((short) 2, (short) 1),
    /**
     * Bad Peer AS. 2/2
     */
    BAD_PEER_AS((short) 2, (short) 2),
    /**
     * Bad BGP Identifier. 2/3
     */
    BAD_BGP_ID((short) 2, (short) 3),
    /**
     * Unsupported Optional Parameter. 2/4
     */
    OPT_PARAM_NOT_SUPPORTED((short) 2, (short) 4),
    /**
     * Unacceptable Hold Time. 2/6
     */
    HOLD_TIME_NOT_ACC((short) 2, (short) 6),
    /**
     * Malformed Attribute List. 3/1
     */
    MALFORMED_ATTR_LIST((short) 3, (short) 1),
    /**
     * Unrecognized Well-known Attribute. 3/2
     */
    WELL_KNOWN_ATTR_NOT_RECOGNIZED((short) 3, (short) 2),
    /**
     * Missing Well-known Attribute. 3/3
     */
    WELL_KNOWN_ATTR_MISSING((short) 3, (short) 3),
    /**
     * Attribute Flags Error. 3/4
     */
    ATTR_FLAGS_MISSING((short) 3, (short) 4),
    /**
     * Attribute Length Error. 3/5
     */
    ATTR_LENGTH_ERROR((short) 3, (short) 5),
    /**
     * Invalid ORIGIN Attribute. 3/6
     */
    ORIGIN_ATTR_NOT_VALID((short) 3, (short) 6),
    /**
     * Invalid NEXT_HOP Attribute. 3/8
     */
    NEXT_HOP_NOT_VALID((short) 3, (short) 8),
    /**
     * Optional Attribute Error. 3/9
     */
    OPT_ATTR_ERROR((short) 3, (short) 9),
    /**
     * Invalid Network Field. 3/10
     */
    NETWORK_NOT_VALID((short) 3, (short) 10),
    /**
     * Malformed AS_PATH. 3/11
     */
    AS_PATH_MALFORMED((short) 3, (short) 11),
    /**
     * Hold Timer Expired. 4/0
     */
    HOLD_TIMER_EXPIRED((short) 4, (short) 0),
    /**
     * Finite State Machine Error. 5/0
     */
    FSM_ERROR((short) 5, (short) 0),
    /**
     * Cease. 6/0
     */
    CEASE((short) 6, (short) 0),
    /**
     * Maximum Number of Prefixes Reached. 6/1
     */
    MAX_NUMBER_OF_PREFIXES_REACHED((short) 6, (short) 1),
    /**
     * Administrative Shutdown. 6/2
     */
    ADMINISTRATIVE_SHUTDOWN((short) 6, (short) 2),
    /**
     * Peer De-configured. 6/3
     */
    PEER_DECONFIGURED((short) 6, (short) 3),
    /**
     * Administrative Reset. 6/4
     */
    ADMINISTRATIVE_RESTART((short) 6, (short) 4),
    /**
     * Connection Rejected. 6/5
     */
    CONNECTION_REJECTED((short) 6, (short) 5),
    /**
     * Other Configuration Change. 6/6
     */
    OTHER_CONFIGURATION_CHANGE((short) 6, (short) 6),
    /**
     * Connection Collision Resolution. 6/7
     */
    CONNECTION_COLLISION_RESOLUTION((short) 6, (short) 7),
    /**
     * Out of Resources. 6/8
     */
    OUT_OF_RESOURCES((short) 6, (short) 8),
    /**
     * Unsupported Capability. 2/7
     */
    UNSUPPORTED_CAPABILITY((short) 2, (short) 7);

    public static final String MANDATORY_ATTR_MISSING_MSG = "Well known mandatory attribute missing: ";

    private static final Map<BGPErrorIdentifier, BGPError> VALUE_MAP;

    static {
        VALUE_MAP = Maps.newHashMap();
        for (final BGPError enumItem : BGPError.values()) {
            VALUE_MAP.put(enumItem.getErrorIdentifier(), enumItem);
        }
    }

    private final BGPErrorIdentifier errorId;

    BGPError(final short code, final short subcode) {
        this.errorId = new BGPErrorIdentifier(code, subcode);
    }

    public static BGPError forValue(final int code, final int subcode) {
        final BGPError e = VALUE_MAP.get(new BGPErrorIdentifier((short) code, (short) subcode));
        Preconditions.checkArgument(e != null, "BGP Error code %s and subcode %s not recognized.",
                code, subcode);
        return e;
    }

    public short getCode() {
        return this.errorId.code;
    }

    public short getSubcode() {
        return this.errorId.subcode;
    }

    private BGPErrorIdentifier getErrorIdentifier() {
        return this.errorId;
    }

    /**
     * Caret for combination of Error-type and Error-value.
     */
    private static class BGPErrorIdentifier implements Serializable {
        private static final long serialVersionUID = 5722575354944165734L;
        final short code;
        final short subcode;

        BGPErrorIdentifier(final short code, final short subcode) {
            this.code = code;
            this.subcode = subcode;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + this.code;
            result = prime * result + this.subcode;
            return result;
        }

        @Override
        public boolean equals(final java.lang.Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || this.getClass() != obj.getClass()) {
                return false;
            }
            final BGPErrorIdentifier other = (BGPErrorIdentifier) obj;
            return this.code == other.code && this.subcode == other.subcode;
        }

        @Override
        public String toString() {
            return "type " + this.code + " value " + this.subcode;
        }
    }
}
