/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.spi;

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
	NON_OR_INVALID_OPEN_MSG,
	/**
	 * No Open message received before the expiration of the OpenWait timer.
	 */
	NO_OPEN_BEFORE_EXP_OPENWAIT,
	/**
	 * Unacceptable and non-negotiable session characteristics.
	 */
	NON_ACC_NON_NEG_SESSION_CHAR,
	/**
	 * Unacceptable but negotiable session characteristics.
	 */
	NON_ACC_NEG_SESSION_CHAR,
	/**
	 * Reception of a second Open message with still unacceptable session characteristics.
	 */
	SECOND_OPEN_MSG,
	/**
	 * Reception of a PCErr message proposing unacceptable session characteristics.
	 */
	PCERR_NON_ACC_SESSION_CHAR,
	/**
	 * No Keepalive or PCErr message received before the expiration of the KeepWait timer.
	 */
	NO_MSG_BEFORE_EXP_KEEPWAIT,
	/**
	 * Capability not supported.
	 */
	CAPABILITY_NOT_SUPPORTED,
	/**
	 * PCEP version not supported.
	 */
	PCEP_VERSION_NOT_SUPPORTED,
	/**
	 * Unrecognized object class.
	 */
	UNRECOGNIZED_OBJ_CLASS,
	/**
	 * Unrecognized object Type.
	 */
	UNRECOGNIZED_OBJ_TYPE,
	/**
	 * Not supported object class.
	 */
	NOT_SUPPORTED_OBJ_CLASS,
	/**
	 * Not supported object Type.
	 */
	NOT_SUPPORTED_OBJ_TYPE,
	/**
	 * C bit of the METRIC object set (request rejected).
	 */
	C_BIT_SET,
	/**
	 * O bit of the RP object cleared (request rejected).
	 */
	O_BIT_SET,
	/**
	 * Objective function not allowed (request rejected)
	 */
	OF_NOT_ALLOWED,
	/**
	 * OF bit of the RP object set (request rejected)
	 */
	OF_BIT_SET,
	/**
	 * Global concurrent optimization not allowed (GCO extension)
	 */
	GCO_NOT_ALLOWED,
	/**
	 * P2MP Path computation is not allowed
	 */
	P2MP_COMPUTATION_NOT_ALLOWED,
	/**
	 * RP object missing
	 */
	RP_MISSING,
	/**
	 * RRO missing for a reoptimization request (R bit of the RP object set).
	 */
	RRO_MISSING,
	/**
	 * END-POINTS object missing
	 */
	END_POINTS_MISSING,
	/**
	 * LSP cleanup TLV missing
	 */
	LSP_CLEANUP_TLV_MISSING,
	/**
	 * SYMBOLIC-PATH-NAME TLV missing
	 */
	SYMBOLIC_PATH_NAME_MISSING,
	/**
	 * Synchronized path computation request missing.
	 */
	SYNC_PATH_COMP_REQ_MISSING,
	/**
	 * Unknown request reference
	 */
	UNKNOWN_REQ_REF,
	/**
	 * Attempt to establish a second PCEP session.
	 */
	ATTEMPT_2ND_SESSION,
	/**
	 * LSP Object missing.
	 */
	LSP_MISSING,
	/**
	 * ERO Object missing for a path in an LSP Update Request where TE-LSP setup is requested.
	 */
	ERO_MISSING,
	/**
	 * Srp Object missing for a path in an LSP Update Request where TE-LSP setup is requested.
	 */
	SRP_MISSING,
	/**
	 * LSP-IDENTIFIERS TLV missing for a path in an LSP Update Request where TE-LSP setup is requested.
	 */
	LSP_IDENTIFIERS_TLV_MISSING,
	/**
	 * Reception of an object with P flag not set although the P flag must be set according to this specification.
	 */
	P_FLAG_NOT_SET,
	/**
	 * Insufficient memory (GCO extension)
	 */
	INSUFFICIENT_MEMORY,
	/**
	 * Global concurrent optimization not supported (GCO extension)
	 */
	GCO_NOT_SUPPORTED,
	/**
	 * Diffserv-aware TE error: Unsupported Class-Type.
	 */
	UNSUPPORTED_CT,
	/**
	 * Diffserv-aware TE error: Invalid Class-Type.
	 */
	INVALID_CT,
	/**
	 * Diffserv-aware TE error: Class-Type and setup priority do not form a configured TE-class.
	 */
	CT_AND_SETUP_PRIORITY_DO_NOT_FORM_TE_CLASS,

	/**
	 * The PCE cannot satisfy the request due to insufficient memory
	 */
	CANNOT_SATISFY_P2MP_REQUEST_DUE_TO_INSUFFISIENT_MEMMORY,
	/**
	 * The PCE is not capable of P2MP computation
	 */
	NOT_CAPPABLE_P2MP_COMPUTATION,
	/**
	 * The PCE is not capable to satisfy the request due to no END-POINTS with leaf type 2
	 */
	P2MP_NOT_CAPPABLE_SATISFY_REQ_DUE_LT2,
	/**
	 * The PCE is not capable to satisfy the request due to no END-POINTS with leaf type 3
	 */
	P2MP_NOT_CAPPABLE_SATISFY_REQ_DUE_LT3,
	/**
	 * The PCE is not capable to satisfy the request due to no END-POINTS with leaf type 4
	 */
	P2MP_NOT_CAPPABLE_SATISFY_REQ_DUE_LT4,
	/**
	 * The PCE is not capable to satisfy the request due to inconsistent END-POINTS
	 */
	P2MP_NOT_CAPPABLE_SATISFY_REQ_DUE_INCONSISTENT_EP,
	/**
	 * P2MP Fragmented request failure
	 */
	P2MP_FRAGMENTATION_FAILRUE,
	/**
	 * Attempted LSP Update Request for a non- delegated LSP. The PCEP-ERROR Object is followed by the LSP Object that
	 * identifies the LSP.
	 */
	UPDATE_REQ_FOR_NON_LSP,
	/**
	 * Attempted LSP Update Request if active stateful PCE capability was not negotiated active PCE.
	 */
	UPDATE_REQ_FOR_NO_STATEFUL,
	/**
	 * Attempted LSP Update Request for an LSP identified by an unknown PLSP-ID.
	 */
	UNKNOWN_PLSP_ID,
	/**
	 * A PCE indicates to a PCC that it has exceeded the resource limit allocated for its state, and thus it cannot
	 * accept and process its LSP State Report message.
	 */
	RESOURCE_LIMIT_EXCEEDED,
	/**
	 * PCE-initiated LSP limit reached
	 */
	LSP_LIMIT_EXCEEDED,
	/**
	 * Delegation for PCE-initiated LSP cannot be revoked
	 */
	DELEGATION_NON_REVOKABLE,
	/**
	 * Non-zero PLSP-ID in LSP initiation request
	 */
	NON_ZERO_PLSPID,
	/**
	 * A PCE indicates to a PCC that it can not process (an otherwise valid) LSP State Report. The PCEP-ERROR Object is
	 * followed by the LSP Object that identifies the LSP.
	 */
	CANNOT_PROCESS_STATE_REPORT,
	/**
	 * LSP Database version mismatch.
	 */
	LSP_DB_VERSION_MISMATCH,
	/**
	 * The LSP-DB-VERSION TLV Missing when State Synchronization Avoidance enabled.
	 */
	DB_VERSION_TLV_MISSING_WHEN_SYNC_ALLOWED,
	/**
	 * A PCC indicates to a PCE that it can not complete the state synchronization,
	 */
	CANNOT_COMPLETE_STATE_SYNC,
	/**
	 * SYMBOLIC-PATH-NAME in use
	 */
	USED_SYMBOLIC_PATH_NAME,
	/**
	 * LSP instantiation error: Unacceptable instantiation parameters
	 */
	LSP_UNACC_INST_PARAMS,
	/**
	 * LSP instantiation error: Internal error
	 */
	LSP_INTERNAL_ERROR,
	/**
	 * LSP instantiation error: RSVP signaling error
	 */
	LSP_RSVP_ERROR
}
