/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser;

/**
 * Possible errors from implemented RFCs and drafts. Each error consists of error code and error subcode (code/subcode
 * in comments).
 * 
 * @see <a href="http://tools.ietf.org/html/rfc4271#section-4.5">BGP Notification Message</a>
 */
public enum BGPError {
	/**
	 * Connection Not Synchronized. 1/1
	 */
	CONNECTION_NOT_SYNC,
	/**
	 * Bad Message Length. 1/2
	 */
	BAD_MSG_LENGTH,
	/**
	 * Bad Message Type. 1/3
	 */
	BAD_MSG_TYPE,
	/**
	 * Unspecific Open Message error.
	 */
	UNSPECIFIC_OPEN_ERROR,
	/**
	 * Unsupported Version Number. 2/1
	 */
	VERSION_NOT_SUPPORTED,
	/**
	 * Bad Peer AS. 2/2
	 */
	BAD_PEER_AS,
	/**
	 * Bad BGP Identifier. 2/3
	 */
	BAD_BGP_ID,
	/**
	 * Unsupported Optional Parameter. 2/4
	 */
	OPT_PARAM_NOT_SUPPORTED,
	/**
	 * Unacceptable Hold Time. 2/6
	 */
	HOLD_TIME_NOT_ACC,
	/**
	 * Malformed Attribute List. 3/1
	 */
	MALFORMED_ATTR_LIST,
	/**
	 * Unrecognized Well-known Attribute. 3/2
	 */
	WELL_KNOWN_ATTR_NOT_RECOGNIZED,
	/**
	 * Missing Well-known Attribute. 3/3
	 */
	WELL_KNOWN_ATTR_MISSING,
	/**
	 * Attribute Flags Error. 3/4
	 */
	ATTR_FLAGS_MISSING,
	/**
	 * Attribute Length Error. 3/5
	 */
	ATTR_LENGTH_ERROR,
	/**
	 * Invalid ORIGIN Attribute. 3/6
	 */
	ORIGIN_ATTR_NOT_VALID,
	/**
	 * Invalid NEXT_HOP Attribute. 3/8
	 */
	NEXT_HOP_NOT_VALID,
	/**
	 * Optional Attribute Error. 3/9
	 */
	OPT_ATTR_ERROR,
	/**
	 * Invalid Network Field. 3/10
	 */
	NETWORK_NOT_VALID,
	/**
	 * Malformed AS_PATH. 3/11
	 */
	AS_PATH_MALFORMED,
	/**
	 * Hold Timer Expired. 4/0
	 */
	HOLD_TIMER_EXPIRED,
	/**
	 * Finite State Machine Error. 5/0
	 */
	FSM_ERROR,
	/**
	 * Cease. 6/0
	 */
	CEASE
}
