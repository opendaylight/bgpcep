/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.spi;

import com.google.common.collect.Maps;
import java.util.Map;

/**
 * Possible errors listed in RFC5440, RFC 5455 and stateful draft.
 *
 * @see <a href="http://tools.ietf.org/html/rfc5440#section-9.12">PCEP-ERROR Object(RFC5440)</a>, <a href=
 *      "http://tools.ietf.org/html/draft-ietf-pce-stateful-pce-07#section-8.4" >PCEP-ERROR Object(stateful draft)</a>,
 *      <a href="http://tools.ietf.org/html/rfc5455#section-3.6">Error Codes for CLASSTYPE Object(RFC5455)</a>, <a href=
 *      "http://www.ietf.org/id/draft-crabbe-pce-pce-initiated-lsp-00.txt#section-7.1" >PCEP-Error Object</a>
 */
public enum PCEPErrors {

    /**
     * Reception of an invalid Open message or a non Open message.
     */
    NON_OR_INVALID_OPEN_MSG(1, 1),
    /**
     * No Open message received before the expiration of the OpenWait timer.
     */
    NO_OPEN_BEFORE_EXP_OPENWAIT(1, 2),
    /**
     * Unacceptable and non-negotiable session characteristics.
     */
    NON_ACC_NON_NEG_SESSION_CHAR(1, 3),
    /**
     * Unacceptable but negotiable session characteristics.
     */
    NON_ACC_NEG_SESSION_CHAR(1, 4),
    /**
     * Reception of a second Open message with still unacceptable session characteristics.
     */
    SECOND_OPEN_MSG(1, 5),
    /**
     * Reception of a PCErr message proposing unacceptable session characteristics.
     */
    PCERR_NON_ACC_SESSION_CHAR(1, 6),
    /**
     * No Keepalive or PCErr message received before the expiration of the KeepWait timer.
     */
    NO_MSG_BEFORE_EXP_KEEPWAIT(1, 7),
    /**
     * Capability not supported.
     */
    CAPABILITY_NOT_SUPPORTED(2, 0),
    /**
     * PCEP version not supported.
     */
    PCEP_VERSION_NOT_SUPPORTED(1, 8),
    /**
     * Unrecognized object class.
     */
    UNRECOGNIZED_OBJ_CLASS(3, 1),
    /**
     * Unrecognized object Type.
     */
    UNRECOGNIZED_OBJ_TYPE(3, 2),
    /**
     * Not supported object class.
     */
    NOT_SUPPORTED_OBJ_CLASS(4, 1),
    /**
     * Not supported object Type.
     */
    NOT_SUPPORTED_OBJ_TYPE(4, 2),
    /**
     * C bit of the METRIC object set (request rejected).
     */
    C_BIT_SET(5, 1),
    /**
     * O bit of the RP object cleared (request rejected).
     */
    O_BIT_SET(5, 2),
    /**
     * Objective function not allowed (request rejected)
     */
    OF_NOT_ALLOWED(5, 3),
    /**
     * OF bit of the RP object set (request rejected)
     */
    OF_BIT_SET(5, 4),
    /**
     * Global concurrent optimization not allowed (GCO extension)
     */
    GCO_NOT_ALLOWED(5, 5),
    /**
     * P2MP Path computation is not allowed
     */
    P2MP_COMPUTATION_NOT_ALLOWED(5, 7),
    /**
     * RP object missing
     */
    RP_MISSING(6, 1),
    /**
     * RRO missing for a reoptimization request (R bit of the RP object set).
     */
    RRO_MISSING(6, 2),
    /**
     * END-POINTS object missing
     */
    END_POINTS_MISSING(6, 3),
    /**
     * LSP cleanup TLV missing
     */
    LSP_CLEANUP_TLV_MISSING(6, 13),
    /**
     * SYMBOLIC-PATH-NAME TLV missing
     */
    SYMBOLIC_PATH_NAME_MISSING(6, 14),
    /**
     * Synchronized path computation request missing.
     */
    SYNC_PATH_COMP_REQ_MISSING(7, 0),
    /**
     * Unknown request reference
     */
    UNKNOWN_REQ_REF(8, 0),
    /**
     * Attempt to establish a second PCEP session.
     */
    ATTEMPT_2ND_SESSION(9, 0),
    /**
     * LSP Object missing.
     */
    LSP_MISSING(6, 8),
    /**
     * ERO Object missing for a path in an LSP Update Request where TE-LSP setup is requested.
     */
    ERO_MISSING(6, 9),
    /**
     * Srp Object missing for a path in an LSP Update Request where TE-LSP setup is requested.
     */
    SRP_MISSING(6, 10),
    /**
     * LSP-IDENTIFIERS TLV missing for a path in an LSP Update Request where TE-LSP setup is requested.
     */
    LSP_IDENTIFIERS_TLV_MISSING(6, 11),
    /**
     * Reception of an object with P flag not set although the P flag must be set according to this specification.
     */
    P_FLAG_NOT_SET(10, 1),
    /**
     * Insufficient memory (GCO extension)
     */
    INSUFFICIENT_MEMORY(15, 1),
    /**
     * Global concurrent optimization not supported (GCO extension)
     */
    GCO_NOT_SUPPORTED(15, 2),
    /**
     * Diffserv-aware TE error: Unsupported Class-Type.
     */
    UNSUPPORTED_CT(12, 1),
    /**
     * Diffserv-aware TE error: Invalid Class-Type.
     */
    INVALID_CT(12, 2),
    /**
     * Diffserv-aware TE error: Class-Type and setup priority do not form a configured TE-class.
     */
    CT_AND_SETUP_PRIORITY_DO_NOT_FORM_TE_CLASS(12, 3),

    /**
     * The PCE cannot satisfy the request due to insufficient memory
     */
    CANNOT_SATISFY_P2MP_REQUEST_DUE_TO_INSUFFISIENT_MEMMORY(16, 1),
    /**
     * The PCE is not capable of P2MP computation
     */
    NOT_CAPPABLE_P2MP_COMPUTATION(16, 2),
    /**
     * The PCE is not capable to satisfy the request due to no END-POINTS with leaf type 2
     */
    P2MP_NOT_CAPPABLE_SATISFY_REQ_DUE_LT2(17, 1),
    /**
     * The PCE is not capable to satisfy the request due to no END-POINTS with leaf type 3
     */
    P2MP_NOT_CAPPABLE_SATISFY_REQ_DUE_LT3(17, 2),
    /**
     * The PCE is not capable to satisfy the request due to no END-POINTS with leaf type 4
     */
    P2MP_NOT_CAPPABLE_SATISFY_REQ_DUE_LT4(17, 3),
    /**
     * The PCE is not capable to satisfy the request due to inconsistent END-POINTS
     */
    P2MP_NOT_CAPPABLE_SATISFY_REQ_DUE_INCONSISTENT_EP(17, 4),
    /**
     * P2MP Fragmented request failure
     */
    P2MP_FRAGMENTATION_FAILRUE(18, 1),
    /**
     * Attempted LSP Update Request for a non- delegated LSP. The PCEP-ERROR Object is followed by the LSP Object that
     * identifies the LSP.
     */
    UPDATE_REQ_FOR_NON_LSP(19, 1),
    /**
     * Attempted LSP Update Request if active stateful PCE capability was not negotiated active PCE.
     */
    UPDATE_REQ_FOR_NO_STATEFUL(19, 2),
    /**
     * Attempted LSP Update Request for an LSP identified by an unknown PLSP-ID.
     */
    UNKNOWN_PLSP_ID(19, 3),
    /**
     * A PCE indicates to a PCC that it has exceeded the resource limit allocated for its state, and thus it cannot
     * accept and process its LSP State Report message.
     */
    RESOURCE_LIMIT_EXCEEDED(19, 4),
    /**
     * PCE-initiated LSP limit reached
     */
    LSP_LIMIT_EXCEEDED(19, 6),
    /**
     * Delegation for PCE-initiated LSP cannot be revoked
     */
    DELEGATION_NON_REVOKABLE(19, 7),
    /**
     * Non-zero PLSP-ID in LSP initiation request
     */
    NON_ZERO_PLSPID(19, 8),
    /**
     * A PCE indicates to a PCC that it can not process (an otherwise valid) LSP State Report. The PCEP-ERROR Object is
     * followed by the LSP Object that identifies the LSP.
     */
    CANNOT_PROCESS_STATE_REPORT(20, 1),
    /**
     * LSP Database version mismatch.
     */
    LSP_DB_VERSION_MISMATCH(20, 2),
    /**
     * The LSP-DB-VERSION TLV Missing when State Synchronization Avoidance enabled.
     */
    DB_VERSION_TLV_MISSING_WHEN_SYNC_ALLOWED(20, 3),
    /**
     * A PCC indicates to a PCE that it can not complete the state synchronization,
     */
    CANNOT_COMPLETE_STATE_SYNC(20, 5),
    /**
     * SYMBOLIC-PATH-NAME in use
     */
    USED_SYMBOLIC_PATH_NAME(23, 1),
    /**
     * LSP instantiation error: Unacceptable instantiation parameters
     */
    LSP_UNACC_INST_PARAMS(24, 1),
    /**
     * LSP instantiation error: Internal error
     */
    LSP_INTERNAL_ERROR(24, 2),
    /**
     * LSP instantiation error: RSVP signaling error
     */
    LSP_RSVP_ERROR(24, 3),
    /**
     * Segment Routing error: ERO subobject with invalid SID value
     */
    BAD_LABEL_VALUE(10, 2),
    /**
     * Segment Routing error: Unsupported number of Segment ERO subobjects
     */
    UNSUPPORTED_NUMBER_OF_SR_ERO_SUBOBJECTS(10, 3),
    /**
     * Segment Routing error: Bad label format
     */
    BAD_LABEL_FORMAT(10, 4),
    /**
     * Segment Routing error: Non-identical ERO subobjects
     */
    NON_IDENTICAL_ERO_SUBOBJECTS(10, 5),
    /**
     * Segment Routing error: Both SID and NAI are absent in ERO subobject.
     */
    SID_AND_NAI_ABSENT_IN_ERO(10, 6),
    /**
     * Segment Routing error: Both SID and NAI are absent in RRO subobject.
     */
    SID_AND_NAI_ABSENT_IN_RRO(10, 7),
    /**
     * Segment Routing error: Non-identical RRO subobjects.
     */
    SID_NON_IDENTICAL_RRO_SUBOBJECTS(10, 8),
    /**
     * Invalid traffic engineering path setup type: Unsupported path setup type
     */
    UNSUPPORTED_PST(21, 1),
    /**
     * Invalid traffic engineering path setup type: Mismatched path setup type
     */
    MISMATCHED_PST(21, 2),
    /**
     * MONITORING object missing
     */
    MONITORING_OBJECT_MISSING(6, 4),
    /**
     * Reception of StartTLS after any PCEP exchange
     * TODO: error code to be assigned by IANA
     */
    STARTTLS_RCVD_INCORRECTLY(30, 1),
    /**
     * Reception of non-StartTLS or non-PCErr message
     * TODO: error code to be assigned by IANA
     */
    NON_STARTTLS_MSG_RCVD(30, 2),
    /**
     * Failure, connection without TLS not possible
     * TODO: error code to be assigned by IANA
     */
    NOT_POSSIBLE_WITHOUT_TLS(30, 3),
    /**
     * Failure, connection without TLS possible
     * TODO: error code to be assigned by IANA
     */
    POSSIBLE_WITHOUT_TLS(30, 4),
    /**
     * No StartTLS message before StartTLSWait timer expired
     * TODO: error code to be assigned by IANA
     */
    STARTTLS_TIMER_EXP(30, 5),
    /**
     * LSP is not PCE-initiated
     */
    LSP_NOT_PCE_INITIATED(19, 9),
    /**
     * LSP-DB-VERSION TLV missing
     */
    LSP_DB_VERSION_MISSING(6, 12),
    /**
     * Attempt to trigger a synchronization when the
     * PCE triggered synchronization capability has not been advertised.
     */
    UNEXPECTED_SYNCHRONIZATION_ATTEMPT(20, 4),
    /**
     * No sufficient LSP change information for
     * incremental LSP state synchronization.
     */
    NO_SUFFICIENT_LSP_CHANGE(20, 6),
    /**
     * Received an invalid LSP DB Version Number
     */
    INVALID_LSP_DB_VERSION(20, 7);

    private PCEPErrorIdentifier errorId;
    private static final Map<PCEPErrorIdentifier, PCEPErrors> VALUE_MAP;

    static {
        VALUE_MAP = Maps.newHashMap();
        for (final PCEPErrors enumItem : PCEPErrors.values()) {
            VALUE_MAP.put(enumItem.getErrorIdentifier(), enumItem);
        }
    }

    public static PCEPErrors forValue(final short errorType, final short errorValue) {
        return VALUE_MAP.get(new PCEPErrorIdentifier(errorType, errorValue));
    }

    PCEPErrors(final int type, final int value) {
        this.errorId = new PCEPErrorIdentifier((short) type, (short) value);
    }

    private PCEPErrorIdentifier getErrorIdentifier() {
        return this.errorId;
    }

    public short getErrorType() {
        return this.errorId.getType();
    }

    public short getErrorValue() {
        return this.errorId.getValue();
    }
}
