/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.yang.common.Uint8;

/**
 * Possible errors from implemented RFCs and drafts. Each error consists of error code and error subcode
 * (code/subcode in comments).
 *
 * @see <a href="http://tools.ietf.org/html/rfc4271#section-4.5">BGP Notification Message</a>
 */
public enum BGPError {
    /**
     * Unspecific header error. 1/0, <a href="https://www.rfc-editor.org/errata_search.php?eid=4493">Errata 4493</a>.
     */
    UNSPECIFIC_HEADER_ERROR(1, 0),
        /**
     * Connection Not Synchronized. 1/1
     */
    CONNECTION_NOT_SYNC(1, 1),
    /**
     * Bad Message Length. 1/2
     */
    BAD_MSG_LENGTH(1, 2),
    /**
     * Bad Message Type. 1/3
     */
    BAD_MSG_TYPE(1, 3),
    /**
     * Unspecific Open Message error, <a href="https://www.rfc-editor.org/errata_search.php?eid=4493">Errata 4493</a>.
     */
    UNSPECIFIC_OPEN_ERROR(2, 0),
    /**
     * Unsupported Version Number. 2/1
     */
    VERSION_NOT_SUPPORTED(2, 1),
    /**
     * Bad Peer AS. 2/2
     */
    BAD_PEER_AS(2, 2),
    /**
     * Bad BGP Identifier. 2/3
     */
    BAD_BGP_ID(2, 3),
    /**
     * Unsupported Optional Parameter. 2/4
     */
    OPT_PARAM_NOT_SUPPORTED(2, 4),
    /**
     * Unacceptable Hold Time. 2/6
     */
    HOLD_TIME_NOT_ACC(2, 6),
    /**
     * Unspecific UPDATE error. 3/0, <a href="https://www.rfc-editor.org/errata_search.php?eid=4493">Errata 4493</a>.
     */
    UNSPECIFIC_UPDATE_ERROR(3, 0),
    /**
     * Malformed Attribute List. 3/1
     */
    MALFORMED_ATTR_LIST(3, 1),
    /**
     * Unrecognized Well-known Attribute. 3/2
     */
    WELL_KNOWN_ATTR_NOT_RECOGNIZED(3, 2),
    /**
     * Missing Well-known Attribute. 3/3
     */
    WELL_KNOWN_ATTR_MISSING(3, 3),
    /**
     * Attribute Flags Error. 3/4
     */
    ATTR_FLAGS_MISSING(3, 4),
    /**
     * Attribute Length Error. 3/5
     */
    ATTR_LENGTH_ERROR(3, 5),
    /**
     * Invalid ORIGIN Attribute. 3/6
     */
    ORIGIN_ATTR_NOT_VALID(3, 6),
    /**
     * Invalid NEXT_HOP Attribute. 3/8
     */
    NEXT_HOP_NOT_VALID(3, 8),
    /**
     * Optional Attribute Error. 3/9
     */
    OPT_ATTR_ERROR(3, 9),
    /**
     * Invalid Network Field. 3/10
     */
    NETWORK_NOT_VALID(3, 10),
    /**
     * Malformed AS_PATH. 3/11
     */
    AS_PATH_MALFORMED(3, 11),
    /**
     * Hold Timer Expired. 4/0
     */
    HOLD_TIMER_EXPIRED(4, 0),
    /**
     * Finite State Machine Error. 5/0
     */
    FSM_ERROR(5, 0),
    /**
     * Cease. 6/0
     */
    CEASE(6, 0),
    /**
     * Maximum Number of Prefixes Reached. 6/1
     */
    MAX_NUMBER_OF_PREFIXES_REACHED(6, 1),
    /**
     * Administrative Shutdown. 6/2
     */
    ADMINISTRATIVE_SHUTDOWN(6, 2),
    /**
     * Peer De-configured. 6/3
     */
    PEER_DECONFIGURED(6, 3),
    /**
     * Administrative Reset. 6/4
     */
    ADMINISTRATIVE_RESTART(6, 4),
    /**
     * Connection Rejected. 6/5
     */
    CONNECTION_REJECTED(6, 5),
    /**
     * Other Configuration Change. 6/6
     */
    OTHER_CONFIGURATION_CHANGE(6, 6),
    /**
     * Connection Collision Resolution. 6/7
     */
    CONNECTION_COLLISION_RESOLUTION(6, 7),
    /**
     * Out of Resources. 6/8
     */
    OUT_OF_RESOURCES(6, 8),
    /**
     * Unsupported Capability. 2/7
     */
    UNSUPPORTED_CAPABILITY(2, 7);

    public static final String MANDATORY_ATTR_MISSING_MSG = "Well known mandatory attribute missing: ";

    private static final ImmutableMap<BGPErrorIdentifier, BGPError> VALUE_MAP = Maps.uniqueIndex(
        Arrays.asList(values()), BGPError::getErrorIdentifier);

    private final BGPErrorIdentifier errorId;

    BGPError(final int code, final int subcode) {
        this.errorId = new BGPErrorIdentifier(Uint8.valueOf(code), Uint8.valueOf(subcode));
    }

    public static BGPError forValue(final Uint8 code, final Uint8 subcode) {
        final BGPError e = VALUE_MAP.get(new BGPErrorIdentifier(code, subcode));
        checkArgument(e != null, "BGP Error code %s and subcode %s not recognized.", code, subcode);
        return e;
    }

    public @NonNull Uint8 getCode() {
        return this.errorId.code;
    }

    public @NonNull Uint8 getSubcode() {
        return this.errorId.subcode;
    }

    private BGPErrorIdentifier getErrorIdentifier() {
        return this.errorId;
    }

    /**
     * Caret for combination of Error-type and Error-value.
     */
    private static final class BGPErrorIdentifier implements Serializable {
        private static final long serialVersionUID = 5722575354944165734L;

        final @NonNull Uint8 code;
        final @NonNull Uint8 subcode;

        BGPErrorIdentifier(final Uint8 code, final Uint8 subcode) {
            this.code = requireNonNull(code);
            this.subcode = requireNonNull(subcode);
        }

        @Override
        public int hashCode() {
            return Objects.hash(code, subcode);
        }

        @Override
        public boolean equals(final java.lang.Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof BGPErrorIdentifier)) {
                return false;
            }
            final BGPErrorIdentifier other = (BGPErrorIdentifier) obj;
            return code.equals(other.code) && subcode.equals(other.subcode);
        }

        @Override
        public String toString() {
            return "type " + code + " value " + subcode;
        }
    }
}
