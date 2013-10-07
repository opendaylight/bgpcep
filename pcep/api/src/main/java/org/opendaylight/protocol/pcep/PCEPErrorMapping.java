/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bidirectional mapping of {@link org.opendaylight.protocol.pcep.PCEPErrors PCEPErrors} and
 * {@link org.opendaylight.protocol.pcep.impl.object.PCEPErrorObjectParser.PCEPErrorIdentifier ErrorIdentifier}
 */
public final class PCEPErrorMapping {

	private final static Logger logger = LoggerFactory.getLogger(PCEPErrorMapping.class);

	/**
	 * Caret for combination of Error-type and Error-value
	 */
	public static class PCEPErrorIdentifier {
		public final short type;
		public final short value;

		private PCEPErrorIdentifier(final short type, final short value) {
			this.type = type;
			this.value = value;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + this.type;
			result = prime * result + this.value;
			return result;
		}

		@Override
		public boolean equals(final java.lang.Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (this.getClass() != obj.getClass())
				return false;
			final PCEPErrorIdentifier other = (PCEPErrorIdentifier) obj;
			if (this.type != other.type)
				return false;
			if (this.value != other.value)
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "type " + this.type + " value " + this.value;
		}
	}

	private static final PCEPErrorMapping instance = new PCEPErrorMapping();

	private final Map<PCEPErrors, PCEPErrorIdentifier> errorsMap = new HashMap<PCEPErrors, PCEPErrorIdentifier>();
	private final Map<PCEPErrorIdentifier, PCEPErrors> errorIdsMap = new HashMap<PCEPErrorIdentifier, PCEPErrors>();

	private PCEPErrorMapping() {
		this.fillIn();
	}

	private void fillIn() {
		this.fillIn(new PCEPErrorIdentifier((short) 1, (short) 1), PCEPErrors.NON_OR_INVALID_OPEN_MSG);
		this.fillIn(new PCEPErrorIdentifier((short) 1, (short) 2), PCEPErrors.NO_OPEN_BEFORE_EXP_OPENWAIT);
		this.fillIn(new PCEPErrorIdentifier((short) 1, (short) 3), PCEPErrors.NON_ACC_NON_NEG_SESSION_CHAR);
		this.fillIn(new PCEPErrorIdentifier((short) 1, (short) 4), PCEPErrors.NON_ACC_NEG_SESSION_CHAR);
		this.fillIn(new PCEPErrorIdentifier((short) 1, (short) 5), PCEPErrors.SECOND_OPEN_MSG);
		this.fillIn(new PCEPErrorIdentifier((short) 1, (short) 6), PCEPErrors.PCERR_NON_ACC_SESSION_CHAR);
		this.fillIn(new PCEPErrorIdentifier((short) 1, (short) 7), PCEPErrors.NO_MSG_BEFORE_EXP_KEEPWAIT);
		this.fillIn(new PCEPErrorIdentifier((short) 1, (short) 8), PCEPErrors.PCEP_VERSION_NOT_SUPPORTED);

		this.fillIn(new PCEPErrorIdentifier((short) 2, (short) 0), PCEPErrors.CAPABILITY_NOT_SUPPORTED);

		this.fillIn(new PCEPErrorIdentifier((short) 3, (short) 1), PCEPErrors.UNRECOGNIZED_OBJ_CLASS);
		this.fillIn(new PCEPErrorIdentifier((short) 3, (short) 2), PCEPErrors.UNRECOGNIZED_OBJ_TYPE);

		this.fillIn(new PCEPErrorIdentifier((short) 4, (short) 1), PCEPErrors.NOT_SUPPORTED_OBJ_CLASS);
		this.fillIn(new PCEPErrorIdentifier((short) 4, (short) 2), PCEPErrors.NOT_SUPPORTED_OBJ_TYPE);

		this.fillIn(new PCEPErrorIdentifier((short) 5, (short) 1), PCEPErrors.C_BIT_SET);
		this.fillIn(new PCEPErrorIdentifier((short) 5, (short) 2), PCEPErrors.O_BIT_SET);
		this.fillIn(new PCEPErrorIdentifier((short) 5, (short) 3), PCEPErrors.OF_NOT_ALLOWED);
		this.fillIn(new PCEPErrorIdentifier((short) 5, (short) 4), PCEPErrors.OF_BIT_SET);
		this.fillIn(new PCEPErrorIdentifier((short) 5, (short) 5), PCEPErrors.GCO_NOT_ALLOWED);
		this.fillIn(new PCEPErrorIdentifier((short) 5, (short) 7), PCEPErrors.P2MP_COMPUTATION_NOT_ALLOWED);

		this.fillIn(new PCEPErrorIdentifier((short) 6, (short) 1), PCEPErrors.RP_MISSING);
		this.fillIn(new PCEPErrorIdentifier((short) 6, (short) 2), PCEPErrors.RRO_MISSING);
		this.fillIn(new PCEPErrorIdentifier((short) 6, (short) 3), PCEPErrors.END_POINTS_MISSING);
		this.fillIn(new PCEPErrorIdentifier((short) 6, (short) 8), PCEPErrors.LSP_MISSING);
		this.fillIn(new PCEPErrorIdentifier((short) 6, (short) 9), PCEPErrors.ERO_MISSING);
		this.fillIn(new PCEPErrorIdentifier((short) 6, (short) 10), PCEPErrors.BANDWIDTH_MISSING);
		this.fillIn(new PCEPErrorIdentifier((short) 6, (short) 11), PCEPErrors.LSPA_MISSING);
		this.fillIn(new PCEPErrorIdentifier((short) 6, (short) 12), PCEPErrors.DB_VERSION_TLV_MISSING);

		this.fillIn(new PCEPErrorIdentifier((short) 6, (short) 13), PCEPErrors.LSP_CLEANUP_TLV_MISSING);
		this.fillIn(new PCEPErrorIdentifier((short) 6, (short) 14), PCEPErrors.SYMBOLIC_PATH_NAME_MISSING);

		this.fillIn(new PCEPErrorIdentifier((short) 7, (short) 0), PCEPErrors.SYNC_PATH_COMP_REQ_MISSING);

		this.fillIn(new PCEPErrorIdentifier((short) 8, (short) 0), PCEPErrors.UNKNOWN_REQ_REF);

		this.fillIn(new PCEPErrorIdentifier((short) 9, (short) 0), PCEPErrors.ATTEMPT_2ND_SESSION);

		this.fillIn(new PCEPErrorIdentifier((short) 10, (short) 1), PCEPErrors.P_FLAG_NOT_SET);

		this.fillIn(new PCEPErrorIdentifier((short) 12, (short) 1), PCEPErrors.UNSUPPORTED_CT);
		this.fillIn(new PCEPErrorIdentifier((short) 12, (short) 2), PCEPErrors.INVALID_CT);
		this.fillIn(new PCEPErrorIdentifier((short) 12, (short) 3), PCEPErrors.CT_AND_SETUP_PRIORITY_DO_NOT_FORM_TE_CLASS);

		this.fillIn(new PCEPErrorIdentifier((short) 15, (short) 1), PCEPErrors.INSUFFICIENT_MEMORY);
		this.fillIn(new PCEPErrorIdentifier((short) 15, (short) 2), PCEPErrors.GCO_NOT_SUPPORTED);

		this.fillIn(new PCEPErrorIdentifier((short) 16, (short) 1), PCEPErrors.CANNOT_SATISFY_P2MP_REQUEST_DUE_TO_INSUFFISIENT_MEMMORY);
		this.fillIn(new PCEPErrorIdentifier((short) 16, (short) 2), PCEPErrors.NOT_CAPPABLE_P2MP_COMPUTATION);

		this.fillIn(new PCEPErrorIdentifier((short) 17, (short) 1), PCEPErrors.P2MP_NOT_CAPPABLE_SATISFY_REQ_DUE_LT2);
		this.fillIn(new PCEPErrorIdentifier((short) 17, (short) 2), PCEPErrors.P2MP_NOT_CAPPABLE_SATISFY_REQ_DUE_LT3);
		this.fillIn(new PCEPErrorIdentifier((short) 17, (short) 3), PCEPErrors.P2MP_NOT_CAPPABLE_SATISFY_REQ_DUE_LT4);
		this.fillIn(new PCEPErrorIdentifier((short) 17, (short) 4), PCEPErrors.P2MP_NOT_CAPPABLE_SATISFY_REQ_DUE_INCONSISTENT_EP);

		this.fillIn(new PCEPErrorIdentifier((short) 18, (short) 1), PCEPErrors.P2MP_FRAGMENTATION_FAILRUE);

		this.fillIn(new PCEPErrorIdentifier((short) 19, (short) 1), PCEPErrors.UPDATE_REQ_FOR_NON_LSP);
		this.fillIn(new PCEPErrorIdentifier((short) 19, (short) 2), PCEPErrors.UPDATE_REQ_FOR_NO_STATEFUL);
		// TODO: value TBD
		this.fillIn(new PCEPErrorIdentifier((short) 19, (short) 3), PCEPErrors.LSP_LIMIT_REACHED);
		this.fillIn(new PCEPErrorIdentifier((short) 19, (short) 4), PCEPErrors.DELEGATION_NOT_REVOKED);

		this.fillIn(new PCEPErrorIdentifier((short) 20, (short) 1), PCEPErrors.CANNOT_PROCESS_STATE_REPORT);
		this.fillIn(new PCEPErrorIdentifier((short) 20, (short) 2), PCEPErrors.LSP_DB_VERSION_MISMATCH);
		this.fillIn(new PCEPErrorIdentifier((short) 20, (short) 3), PCEPErrors.DB_VERSION_TLV_MISSING_WHEN_SYNC_ALLOWED);

		this.fillIn(new PCEPErrorIdentifier((short) 23, (short) 1), PCEPErrors.USED_SYMBOLIC_PATH_NAME);
	}

	private void fillIn(final PCEPErrorIdentifier identifier, final PCEPErrors error) {
		this.errorsMap.put(error, identifier);
		this.errorIdsMap.put(identifier, error);
	}

	public PCEPErrorIdentifier getFromErrorsEnum(final PCEPErrors error) {
		final PCEPErrorIdentifier ei = this.errorsMap.get(error);
		if (ei == null) {
			logger.debug("Unknown PCEPErrors type: {}.", error);
			throw new NoSuchElementException("Unknown PCEPErrors type: " + error);
		}
		return ei;
	}

	public PCEPErrors getFromErrorIdentifier(final PCEPErrorIdentifier identifier) {
		final PCEPErrors e = this.errorIdsMap.get(identifier);
		if (e == null) {
			logger.debug("Unknown error type/value combination: {}.", identifier);
			throw new NoSuchElementException("Unknown error type/value combination: " + identifier);
		}
		return e;
	}

	public static PCEPErrorMapping getInstance() {
		return instance;
	}
}
