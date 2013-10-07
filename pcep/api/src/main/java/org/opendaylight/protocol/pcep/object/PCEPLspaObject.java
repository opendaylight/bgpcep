/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.object;

import java.util.Collections;
import java.util.List;

import org.opendaylight.protocol.pcep.PCEPObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;

import com.google.common.base.Objects.ToStringHelper;

/**
 * Structure for PCEP LSPA Object.
 * 
 * @see <a href="http://tools.ietf.org/html/rfc5440#section-7.11">PCEP LSPA Object</a>
 * @see <a href="http://tools.ietf.org/html/rfc3209#section-4.7">SessionAttribute Object</a>
 * @see <a href="http://tools.ietf.org/html/draft-crabbe-pce-stateful-pce-protection-00#section-4.1"> The Standby flag
 *      in the LSPA object</a>
 */
public class PCEPLspaObject extends PCEPObject {

	private final long excludedAny;

	private final long includeAny;

	private final long includeAll;

	private final short setupPriority;

	private final short holdingPriority;

	private final boolean standByPath;

	private final boolean localProtected;

	private final List<Tlv> tlvs;

	/**
	 * Constructs object only with mandatory objects.
	 * 
	 * @param excludedAny long
	 * @param includeAny long
	 * @param includeAll long
	 * @param setupPriority short
	 * @param holdingPriority short
	 * @param standByPath boolean
	 * @param localProtected boolean
	 * @param processed boolean
	 * @param ignored boolean
	 */
	public PCEPLspaObject(final long excludedAny, final long includeAny, final long includeAll, final short setupPriority,
			final short holdingPriority, final boolean standByPath, final boolean localProtected, final boolean processed,
			final boolean ignored) {
		this(excludedAny, includeAny, includeAll, setupPriority, holdingPriority, standByPath, localProtected, null, processed, ignored);
	}

	/**
	 * Constructs object also with optional objects.
	 * 
	 * @param excludedAny long
	 * @param includeAny long
	 * @param includeAll long
	 * @param setupPriority short
	 * @param holdingPriority short
	 * @param localProtected boolean
	 * @param tlvs List<PCEPTlv>
	 * @param processed boolean
	 * @param ignored boolean
	 */
	public PCEPLspaObject(final long excludedAny, final long includeAny, final long includeAll, final short setupPriority,
			final short holdingPriority, final boolean standByPath, final boolean localProtected, final List<Tlv> tlvs,
			final boolean processed, final boolean ignored) {
		super(processed, ignored);
		this.excludedAny = excludedAny;
		this.includeAny = includeAny;
		this.includeAll = includeAll;
		this.setupPriority = setupPriority;
		this.holdingPriority = holdingPriority;
		this.standByPath = standByPath;
		this.localProtected = localProtected;
		if (tlvs != null)
			this.tlvs = tlvs;
		else
			this.tlvs = Collections.emptyList();
	}

	/**
	 * A 32-bit vector representing a set of attribute filters associated with a tunnel any of which renders a link
	 * unacceptable.
	 * 
	 * @return long
	 */
	public long getExcludeAny() {
		return this.excludedAny;
	}

	/**
	 * A 32-bit vector representing a set of attribute filters associated with a tunnel any of which renders a link
	 * acceptable (with respect to this test). A null set (all bits set to zero) automatically passes.
	 * 
	 * @return long
	 */
	public long getIncludeAny() {
		return this.includeAny;
	}

	/**
	 * A 32-bit vector representing a set of attribute filters associated with a tunnel all of which must be present for
	 * a link to be acceptable (with respect to this test). A null set (all bits set to zero) automatically passes.
	 * 
	 * @return long
	 */
	public long getIncludeAll() {
		return this.includeAll;
	}

	/**
	 * The priority of TE LSP with respect to taking resources, in the range of 0 to 7 (validation not included). The
	 * value 0 is the highest priority. The Setup Priority is used in deciding whether this session can preempt another
	 * session.
	 * 
	 * @return short
	 */
	public short getSetupPriority() {
		return this.setupPriority;
	}

	/**
	 * The priority of TE LSP with respect to holding resources, in the range of 0 to 7 (validation not included). The
	 * value 0 is the highest priority. Holding Priority is used in deciding whether this session can be preempted by
	 * another session.
	 * 
	 * @return short
	 */
	public short getHoldingPriority() {
		return this.holdingPriority;
	}

	/**
	 * Corresponds to the "Local Protection Desired" bit ([RFC3209]) of SESSION-ATTRIBUTE Object. When set, this means
	 * that the computed path must include links protected with Fast REroute as defined in [RFC4090].
	 * 
	 * @return boolean
	 */
	public boolean isLocalProtected() {
		return this.localProtected;
	}

	/**
	 * The protection path setup regimen (standby or not) is specified in the path using a new per-path flag in the LSPA
	 * object, the S (standby) flag
	 * 
	 * @return boolean
	 */
	public boolean isStandByPath() {
		return this.standByPath;
	}

	/**
	 * Gets list of {@link PCEPTlv}
	 * 
	 * @return List<PCEPTlv>. Can't be null, but may be empty.
	 */
	public List<Tlv> getTlvs() {
		return this.tlvs;
	}

	@Override
	public Boolean isIgnore() {
		return super.isIgnored();
	}

	@Override
	public Boolean isProcessingRule() {
		return super.isProcessed();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + (int) (this.excludedAny ^ (this.excludedAny >>> 32));
		result = prime * result + this.holdingPriority;
		result = prime * result + (int) (this.includeAll ^ (this.includeAll >>> 32));
		result = prime * result + (int) (this.includeAny ^ (this.includeAny >>> 32));
		result = prime * result + (this.localProtected ? 1231 : 1237);
		result = prime * result + this.setupPriority;
		result = prime * result + (this.standByPath ? 1231 : 1237);
		result = prime * result + ((this.tlvs == null) ? 0 : this.tlvs.hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (!(obj instanceof PCEPLspaObject))
			return false;
		final PCEPLspaObject other = (PCEPLspaObject) obj;
		if (this.excludedAny != other.excludedAny)
			return false;
		if (this.holdingPriority != other.holdingPriority)
			return false;
		if (this.includeAll != other.includeAll)
			return false;
		if (this.includeAny != other.includeAny)
			return false;
		if (this.localProtected != other.localProtected)
			return false;
		if (this.setupPriority != other.setupPriority)
			return false;
		if (this.standByPath != other.standByPath)
			return false;
		if (this.tlvs == null) {
			if (other.tlvs != null)
				return false;
		} else if (!this.tlvs.equals(other.tlvs))
			return false;
		return true;
	}

	@Override
	protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
		toStringHelper.add("excludedAny", this.excludedAny);
		toStringHelper.add("includeAny", this.includeAny);
		toStringHelper.add("includeAll", this.includeAll);
		toStringHelper.add("setupPriority", this.setupPriority);
		toStringHelper.add("holdingPriority", this.holdingPriority);
		toStringHelper.add("standByPath", this.standByPath);
		toStringHelper.add("localProtected", this.localProtected);
		toStringHelper.add("tlvs", this.tlvs);
		return super.addToStringAttributes(toStringHelper);
	}
}
