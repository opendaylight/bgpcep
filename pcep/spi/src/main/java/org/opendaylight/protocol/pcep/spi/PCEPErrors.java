/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
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
 * Possible errors listed in various PCE RFC.
 *
 * @see <a href="http://tools.ietf.org/html/rfc5440#section-9.12">PCEP-ERROR Object(RFC5440)</a>
 * @see <a href="https://www.iana.org/assignments/pcep/pcep.xhtml#pcep-error-object">
 *      IANA - PCEP-ERROR Object Error Types and Values</a>
 */
public enum PCEPErrors {
    // PCEP session establishment failure: Error-Type = 1.

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
     * PCEP version not supported.
     */
    PCEP_VERSION_NOT_SUPPORTED(1, 8),

    /**
     * Capability not supported: Error-Type = 2.
     */
    CAPABILITY_NOT_SUPPORTED(2, 0),

    // Unknown Objects: Error-Type = 3.

    /**
     * Unrecognized object class.
     */
    UNRECOGNIZED_OBJ_CLASS(3, 1),
    /**
     * Unrecognized object Type.
     */
    UNRECOGNIZED_OBJ_TYPE(3, 2),

    // Not supported object: Error-Type = 4.

    /**
     * Not supported object class.
     */
    NOT_SUPPORTED_OBJ_CLASS(4, 1),
    /**
     * Not supported object Type.
     */
    NOT_SUPPORTED_OBJ_TYPE(4, 2),
    /**
     * Unsupported Parameter.
     */
    UNSUPPORTED_PARAMETER(4,4),
    /**
     * Unsupported network performance constraint.
     */
    UNSUPPORTED_PERFORMANCE(4,5),
    /**
     * BANDWIDTH object type 3 or 4 not supported.
     */
    UNSUPPORTED_BANDWIDTH(4,6),
    /**
     * Unsupported endpoint type in END-POINTS Generalized Endpoint object type.
     */
    UNSUPPORTED_ENSPOINT_TYPE(4,7),
    /**
     * Unsupported TLV present in END-POINTS Generalized Endpoint object type.
     */
    UNSUPPORTED_ENDPOINT_TLV(4,8),
    /**
     * Unsupported granularity in the RP object flags.
     */
    UNSUPPORTED_RP_FLAGS(4,9),

    // Policy violation: Error-Type = 5.

    /**
     * C bit of the METRIC object set (request rejected).
     */
    C_BIT_SET(5, 1),
    /**
     * O bit of the RP object cleared (request rejected).
     */
    O_BIT_SET(5, 2),
    /**
     * Objective function not allowed (request rejected).
     */
    OF_NOT_ALLOWED(5, 3),
    /**
     * OF bit of the RP object set (request rejected).
     */
    OF_BIT_SET(5, 4),
    /**
     * Global concurrent optimization not allowed (GCO extension).
     */
    GCO_NOT_ALLOWED(5, 5),
    /**
     * Monitoring message supported but rejected due to policy violation.
     */
    MONITORING_POLICY_VIOLATION(5, 6),
    /**
     * P2MP Path computation is not allowed.
     */
    P2MP_COMPUTATION_NOT_ALLOWED(5, 7),
    /**
     * Not allowed network performance constraint.
     */
    PERFORMANCE_CONSTRAINT_NOT_ALLOWED(5, 8),

    // Mandatory Object missing: Error-Type = 6.

    /**
     * RP object missing.
     */
    RP_MISSING(6, 1),
    /**
     * RRO missing for a re-optimization request (R bit of the RP object set).
     */
    RRO_MISSING(6, 2),
    /**
     * END-POINTS object missing.
     */
    END_POINTS_MISSING(6, 3),
    /**
     * MONITORING object missing.
     */
    MONITORING_OBJECT_MISSING(6, 4),

    // 5-7: Unassigned

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
     * LSP-DB-VERSION TLV missing.
     */
    LSP_DB_VERSION_MISSING(6, 12),
    /**
     * SLS Object missing.
     */
    SLS_OBJECT_MISSING(6, 13),
    /**
     * P2MP-LSP-IDENTIFIERS TLV missing.
     */
    P2MP_LSP_ID_TLV_MISSING(6,14),
    /**
     * DISJOINTNESS-CONFIGURATION TLV missing.
     */
    DISJOINT_CONFIG_TLV_MISSING(6, 15),
    /**
     * Scheduled TLV missing.
     */
    SCHEDULED_TLV_MISSING(6, 16),
    /**
     * CCI object missing.
     */
    CCI_OBJECT_MISSING(6, 17),
    /**
     * VIRTUAL-NETWORK-TLV missing.
     */
    VIRTUEL_NET_TLV_MISSING(6, 18),
    /**
     * Native IP object missing.
     */
    NATIVE_IP_OBJ_MISSING(6, 19),
    /**
     * LABEL-REQUEST TLV missing.
     */
    LABEL_REQUEST_TLV_MISSING(6, 20),
    /**
     * Missing SR Policy Mandatory TLV.
     */
    SR_POLICY_TLV_MISSING(6, 21),
    /**
     * Missing SR Policy Association.
     */
    SR_POLICY_ASSOCIATION_MISSING(6, 22),

    /**
     * Synchronized path computation request missing: Error-Type = 7.
     */
    SYNC_PATH_COMP_REQ_MISSING(7, 0),

    /**
     * Unknown request reference: Error-Type = 8.
     */
    UNKNOWN_REQ_REF(8, 0),

    /**
     * Attempt to establish a second PCEP session: Error-Type = 9.
     */
    ATTEMPT_2ND_SESSION(9, 0),

    // Reception of an invalid object: Error-Type = 10.

    /**
     * Reception of an object with P flag not set although the P flag must be set according to this specification.
     */
    P_FLAG_NOT_SET(10, 1),
    /**
     * Segment Routing error: ERO subobject with invalid SID value.
     */
    BAD_LABEL_VALUE(10, 2),
    /**
     * Segment Routing error: Unsupported number of Segment ERO subobjects.
     */
    UNSUPPORTED_NUMBER_OF_SR_ERO_SUBOBJECTS(10, 3),
    /**
     * Segment Routing error: Bad label format.
     */
    BAD_LABEL_FORMAT(10, 4),
    /**
     * Segment Routing error: Non-identical ERO subobjects.
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

    // FIXME: Add missing codes from 9 to 43

    /**
     * Missing SRPOLICY-CAPABILITY.
     */
    SR_POLICY_CAPABILITY_MISSING(10, 44),

    /**
     * Unrecognized EXRS SubObject: Error-Type = 11.
     */
    UNRECOGNIZED_EXRS_SUB_OBJ(11,0),

    // Diffserv-aware TE error: Error-Type = 12.

    /**
     * Unsupported Class-Type.
     */
    UNSUPPORTED_CT(12, 1),
    /**
     * Invalid Class-Type.
     */
    INVALID_CT(12, 2),
    /**
     * Class-Type and setup priority do not form a configured TE-class.
     */
    CT_AND_SETUP_PRIORITY_DO_NOT_FORM_TE_CLASS(12, 3),

    // BRPC procedure completion failure: Error-Type = 13.

    /**
     * BRPC procedure not supported by one or more PCEs along the domain path.
     */
    BRPC_NOT_SUPORTED_BY_PCE(13, 1),

    // Error-Type = 14 is unassigned

    // Global Concurrent Optimization Error: Error-Type = 15.

    /**
     * Insufficient memory (GCO extension).
     */
    INSUFFICIENT_MEMORY(15, 1),
    /**
     * Global concurrent optimization not supported (GCO extension).
     */
    GCO_NOT_SUPPORTED(15, 2),

    // P2MP Capability Error: Error-Type =16.

    /**
     * The PCE cannot satisfy the request due to insufficient memory.
     */
    CANNOT_SATISFY_P2MP_REQUEST_DUE_TO_INSUFFISIENT_MEMMORY(16, 1),
    /**
     * The PCE is not capable of P2MP computation.
     */
    NOT_CAPPABLE_P2MP_COMPUTATION(16, 2),

    // P2MP END-POINTS Error: Error-Type = 17.

    /**
     * The PCE is not capable to satisfy the request due to no END-POINTS with leaf type 2.
     */
    P2MP_NOT_CAPPABLE_SATISFY_REQ_DUE_LT2(17, 1),
    /**
     * The PCE is not capable to satisfy the request due to no END-POINTS with leaf type 3.
     */
    P2MP_NOT_CAPPABLE_SATISFY_REQ_DUE_LT3(17, 2),
    /**
     * The PCE is not capable to satisfy the request due to no END-POINTS with leaf type 4.
     */
    P2MP_NOT_CAPPABLE_SATISFY_REQ_DUE_LT4(17, 3),
    /**
     * The PCE is not capable to satisfy the request due to inconsistent END-POINTS.
     */
    P2MP_NOT_CAPPABLE_SATISFY_REQ_DUE_INCONSISTENT_EP(17, 4),

    // P2MP Fragmentation Error: Error-Type = 18.

    /**
     * P2MP Fragmented request failure.
     */
    P2MP_FRAGMENTATION_REQ_FAILURE(18, 1),
    /**
     * Fragmented Report failure.
     */
    P2MP_FRAGMENTATION_REP_FAILURE(18, 2),
    /**
     * Fragmented Update failure.
     */
    P2MP_FRAGMENTATION_UPD_FAILURE(18, 3),
    /**
     * Fragmented Instantiation failure.
     */
    P2MP_FRAGMENTATION_INIT_FAILURE(18, 4),

    // Invalid Operation: Error-Type = 19.

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
     * PCE-initiated LSP limit reached.
     */
    LSP_LIMIT_EXCEEDED(19, 6),
    /**
     * Delegation for PCE-initiated LSP cannot be revoked.
     */
    DELEGATION_NON_REVOKABLE(19, 7),
    /**
     * Non-zero PLSP-ID in LSP initiation request.
     */
    NON_ZERO_PLSPID(19, 8),
    /**
     * LSP is not PCE-initiated.
     */
    LSP_NOT_PCE_INITIATED(19, 9),

    /**
     * Auto-Bandwidth capability was not advertised.
     */
    AUTO_BANDWIDTH_CAPABILITY_NOT_ADVERTISED(19, 14),

    // FIXME: Add missing codes 10-13, 15-32

    // LSP State Synchronization Error: Error-Type = 20.

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
     * Attempt to trigger a synchronization when the PCE triggered synchronization capability has not been advertised.
     */
    UNEXPECTED_SYNCHRONIZATION_ATTEMPT(20, 4),
    /**
     * A PCC indicates to a PCE that it can not complete the state synchronization.
     */
    CANNOT_COMPLETE_STATE_SYNC(20, 5),
    /**
     * No sufficient LSP change information for incremental LSP state synchronization.
     */
    NO_SUFFICIENT_LSP_CHANGE(20, 6),
    /**
     * Received an invalid LSP DB Version Number.
     */
    INVALID_LSP_DB_VERSION(20, 7),

    // Invalid traffic engineering path setup type: Error-Type = 21.

    /**
     * Invalid traffic engineering path setup type: Unsupported path setup type.
     */
    UNSUPPORTED_PST(21, 1),
    /**
     * Invalid traffic engineering path setup type: Mismatched path setup type.
     */
    MISMATCHED_PST(21, 2),

    // Error-Type = 22 is unassigned.

    // Bad parameter value: Error-Type = 23.

    /**
     * SYMBOLIC-PATH-NAME in use.
     */
    USED_SYMBOLIC_PATH_NAME(23, 1),
    /**
     * Speaker identity included for an LSP that is not PCE initiated.
     */
    SPEAKER_ID_FOR_LSP_NOT_PCE_INIT(23, 2),

    // LSP instantiation error: Error-Type = 24.

    /**
     * LSP instantiation error: Unacceptable instantiation parameters.
     */
    LSP_UNACC_INST_PARAMS(24, 1),
    /**
     * LSP instantiation error: Internal error.
     */
    LSP_INTERNAL_ERROR(24, 2),
    /**
     * LSP instantiation error: RSVP signaling error.
     */
    LSP_RSVP_ERROR(24, 3),

    // PCEP StartTLS failure: Error-Type = 25.

    /**
     * Reception of StartTLS after any PCEP exchange.
     */
    STARTTLS_RCVD_INCORRECTLY(25, 1),
    /**
     * Reception of any other message apart from StartTLS, Open, or PCErr.
     */
    NON_STARTTLS_MSG_RCVD(25, 2),
    /**
     * Failure, connection without TLS not possible.
     */
    NOT_POSSIBLE_WITHOUT_TLS(25, 3),
    /**
     * Failure, connection without TLS possible.
     */
    POSSIBLE_WITHOUT_TLS(25, 4),
    /**
     * No StartTLS message (nor PCErr/Open) before StartTLSWait timer expiry.
     */
    STARTTLS_TIMER_EXP(25, 5),

    // Association Group Error: Error-Types = 26.

    /**
     * Association Type is not supported.
     */
    UNSUPPORTED_ASSOCIATION_TYPE(26, 1),
    /**
     * Too many LSPs in the association group.
     */
    TOO_MANY_LSP_IN_ASSOCIATION_GROUP(26, 2),
    /**
     * Too many association groups.
     */
    TOO_MANY_ASSOCIATION_GROUPS(26,3),
    /**
     * Association unknown.
     */
    UNKNOWN_ASSOCIATION(26, 4),
    /**
     * Operator-configured association information mismatch.
     */
    OPER_CONFIG_ASSOCIATION_MISMATCH(26, 5),
    /**
     * Association information mismatch.
     */
    ASSOCIATION_INFO_MISMATCH(26, 6),
    /**
     * Cannot join the association group.
     */
    CANNOT_JOIN_ASSOCIATION_GROUP(26, 7),
    /**
     * Association ID not in range.
     */
    ASSOCIATION_ID_NOT_IN_RANGE(26, 8),
    /**
     * Tunnel ID or End points mismatch for Path Protection Association.
     */
    PATH_PROTECTION_ID_MISMATCH(26, 9),
    /**
     * Attempt to add another working/protection LSP for Path Protection Association.
     */
    CANNOT_ADD_ANOTHER_PATH_PROTECTION(26, 10),
    /**
     * Protection type is not supported.
     */
    UNSUPPORTED_PROTECTION_TYPE(26, 11),
    /**
     * Not expecting policy parameters.
     */
    NOT_EXPECTING_POLICY_PARAMETERS(26, 12),
    /**
     * Unacceptable policy parameters.
     */
    UNACCEPTABLE_POLICY_PARAMETERS(26, 13),
    /**
     * Association group mismatch.
     */
    ASSOCIATION_GROUP_MISMATCH(26, 14),
    /**
     * Tunnel mismatch in the association group.
     */
    TUNNEL_MISMATCH_IN_ASSOCIATION(26, 15),
    /**
     * Path Setup Type not supported.
     */
    UNSUPPORTED_PATH_SETUP_TYPE(26, 16),
    /**
     * Bidirectional LSP direction mismatch.
     */
    BIDIRECTIONAL_DIRECTION_MISMATCH(26, 17),
    /**
     * Bidirectional LSP co-routed mismatch.
     */
    BIDIRECTIONAL_COROUTED_MISMATCH(26, 18),
    /**
     * Endpoint mismatch in the association group.
     */
    ENDPOINT_MISMATCH_IN_ASSOCIATION(26, 19),
    /**
     * SR Policy Identifers Mismatch.
     */
    SR_POLICY_ID_MISMATCH(26, 20),
    /**
     * SR Policy Candidate Path Identifier Mismatch.
     */
    SR_POLICY_CANDIDATE_MISMATCH(26, 21),

    // FIXME: Add Error-Type 27 to 31

    // Path Binding Error: Error-Types = 32.

    /**
     * Invalid SID.
     */
    INVALID_SID(32, 1),
    /**
     * Unable to allocate the specified binding value.
     */
    CANNOT_ALLOCATE_SPECIFIED_BINDING(32, 2),
    /**
     * Unable to allocate a new binding label/SID.
     */
    CANNOT_ALLOCATE_BINDING(32, 3),
    /**
     * Unable to remove the binding value.
     */
    CANNOT_REMOVE_BINDING(32, 4),
    /**
     * Inconsistent binding types.
     */
    INCONSISTENT_BINDING_TYPES(32, 5),

    // FIXME: Add Error-Type 33

    LAST_ERROR(255, 255);

    private static final ImmutableMap<PCEPErrorIdentifier, PCEPErrors> VALUE_MAP = Maps.uniqueIndex(
        Arrays.asList(values()), PCEPErrors::getErrorIdentifier);

    private PCEPErrorIdentifier errorId;

    public static PCEPErrors forValue(final Uint8 errorType, final Uint8 errorValue) {
        return VALUE_MAP.get(new PCEPErrorIdentifier(errorType, errorValue));
    }

    PCEPErrors(final int type, final int value) {
        this.errorId = new PCEPErrorIdentifier(Uint8.valueOf(type), Uint8.valueOf(value));
    }

    private PCEPErrorIdentifier getErrorIdentifier() {
        return errorId;
    }

    public Uint8 getErrorType() {
        return errorId.getType();
    }

    public Uint8 getErrorValue() {
        return errorId.getValue();
    }
}
