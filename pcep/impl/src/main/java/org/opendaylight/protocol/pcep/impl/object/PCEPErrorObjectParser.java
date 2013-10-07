/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.object;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.PCEPDocumentedException;
import org.opendaylight.protocol.pcep.PCEPErrors;
import org.opendaylight.protocol.pcep.impl.Util;
import org.opendaylight.protocol.pcep.spi.AbstractObjectParser;
import org.opendaylight.protocol.pcep.spi.HandlerRegistry;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.PcepErrorObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ReqMissingTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcep.error.object.Tlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcep.error.object.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcep.error.object.tlvs.ReqMissingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.Errors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.ErrorsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parser for {@link org.opendaylight.protocol.pcep.object.PCEPErrorObject PCEPErrorObject}
 */
public class PCEPErrorObjectParser extends AbstractObjectParser<ErrorsBuilder> {

	public static final int CLASS = 13;

	public static final int TYPE = 1;

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

	/**
	 * Bidirectional mapping of {@link org.opendaylight.protocol.pcep.PCEPErrors PCEPErrors} and
	 * {@link org.opendaylight.protocol.pcep.impl.object.PCEPErrorObjectParser.PCEPErrorIdentifier ErrorIdentifier}
	 */
	public static class PCEPErrorsMaping {
		private static final PCEPErrorsMaping instance = new PCEPErrorsMaping();

		private final Map<PCEPErrors, PCEPErrorIdentifier> errorsMap = new HashMap<PCEPErrors, PCEPErrorIdentifier>();
		private final Map<PCEPErrorIdentifier, PCEPErrors> errorIdsMap = new HashMap<PCEPErrorIdentifier, PCEPErrors>();

		private PCEPErrorsMaping() {
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

		public static PCEPErrorsMaping getInstance() {
			return instance;
		}
	}

	public static final int FLAGS_F_LENGTH = 1;
	public static final int ET_F_LENGTH = 1;
	public static final int EV_F_LENGTH = 1;

	public static final int FLAGS_F_OFFSET = 1; // added reserved field of size 1 byte
	public static final int ET_F_OFFSET = FLAGS_F_OFFSET + FLAGS_F_LENGTH;
	public static final int EV_F_OFFSET = ET_F_OFFSET + ET_F_LENGTH;
	public static final int TLVS_OFFSET = EV_F_OFFSET + EV_F_LENGTH;

	private final static Logger logger = LoggerFactory.getLogger(PCEPErrorObjectParser.class);

	public PCEPErrorObjectParser(final HandlerRegistry registry) {
		super(registry);
	}

	@Override
	public PcepErrorObject parseObject(final ObjectHeader header, final byte[] bytes) throws PCEPDeserializerException,
			PCEPDocumentedException {
		if (bytes == null)
			throw new IllegalArgumentException("Array of bytes is mandatory.");

		final ErrorsBuilder builder = new ErrorsBuilder();

		parseTlvs(builder, ByteArray.cutBytes(bytes, TLVS_OFFSET));

		builder.setIgnore(header.isIgnore());
		builder.setProcessingRule(header.isProcessingRule());

		builder.setType((short) (bytes[ET_F_OFFSET] & 0xFF));
		builder.setValue((short) (bytes[EV_F_OFFSET] & 0xFF));

		return builder.build();
	}

	@Override
	public void addTlv(final ErrorsBuilder builder, final Tlv tlv) {
		if (tlv instanceof ReqMissingTlv && builder.getType() == 7)
			builder.setTlvs(new TlvsBuilder().setReqMissing(
					new ReqMissingBuilder().setRequestId(((ReqMissingTlv) tlv).getRequestId()).build()).build());
	}

	@Override
	public byte[] serializeObject(final Object object) {
		if (!(object instanceof PcepErrorObject))
			throw new IllegalArgumentException("Wrong instance of PCEPObject. Passed " + object.getClass() + ". Needed PcepErrorObject.");

		final PcepErrorObject errObj = (PcepErrorObject) object;

		final byte[] tlvs = serializeTlvs(((Errors) errObj).getTlvs());
		int tlvsLength = 0;
		if (tlvs != null)
			tlvsLength = tlvs.length;
		final byte[] retBytes = new byte[TLVS_OFFSET + tlvsLength + Util.getPadding(TLVS_OFFSET + tlvs.length, PADDED_TO)];

		if (tlvs != null)
			ByteArray.copyWhole(tlvs, retBytes, TLVS_OFFSET);

		retBytes[ET_F_OFFSET] = ByteArray.shortToBytes(errObj.getType())[1];
		retBytes[EV_F_OFFSET] = ByteArray.shortToBytes(errObj.getValue())[1];

		return retBytes;
	}

	public byte[] serializeTlvs(final Tlvs tlvs) {
		if (tlvs.getReqMissing() != null) {
			return serializeTlv(new ReqMissingBuilder().setRequestId(tlvs.getReqMissing().getRequestId()).build());
		}
		return null;
	}

	@Override
	public int getObjectType() {
		return TYPE;
	}

	@Override
	public int getObjectClass() {
		return CLASS;
	}
}
