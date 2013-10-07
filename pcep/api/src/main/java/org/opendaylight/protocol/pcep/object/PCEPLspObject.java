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
import org.opendaylight.protocol.pcep.PCEPTlv;

import com.google.common.base.Objects.ToStringHelper;

/**
 * Structure of PCEP LSP Object.
 * 
 * @see <a href="http://tools.ietf.org/html/draft-crabbe-pce-stateful-pce-02#section-8.3">PCEP LSP Object</a>
 */
public class PCEPLspObject extends PCEPObject {

	private final int lspID;

	private final boolean delegate;

	private final boolean sync;

	private final boolean operational;

	private final boolean remove;

	private final List<PCEPTlv> tlvs;

	/**
	 * Constructs PCEP LSP Object only with mandatory values.
	 * 
	 * @param lspID int
	 * @param delegate boolean
	 * @param sync boolean
	 * @param operational boolean
	 * @param remove boolean
	 */
	public PCEPLspObject(final int lspID, final boolean delegate, final boolean sync, final boolean operational, final boolean remove) {
		this(lspID, delegate, sync, operational, remove, null);
	}

	/**
	 * Constructs PCEP LSP Object also with optional Objects.
	 * 
	 * @param lspID int
	 * @param delegate boolean
	 * @param sync boolean
	 * @param operational boolean
	 * @param remove boolean
	 * @param tlvs List<PCEPTlv>
	 */
	public PCEPLspObject(final int lspID, final boolean delegate, final boolean sync, final boolean operational, final boolean remove,
			final List<PCEPTlv> tlvs) {
		super(false, false);
		this.lspID = lspID;
		this.delegate = delegate;
		this.sync = sync;
		this.operational = operational;
		this.remove = remove;
		if (tlvs != null)
			this.tlvs = tlvs;
		else
			this.tlvs = Collections.emptyList();
	}

	/**
	 * Gets integer representation of LSP ID.
	 * 
	 * @return int
	 */
	public int getLspID() {
		return this.lspID;
	}

	/**
	 * Gets Delegate flag.
	 * 
	 * @return boolean
	 */
	public boolean isDelegate() {
		return this.delegate;
	}

	/**
	 * Gets Sync flag.
	 * 
	 * @return boolean
	 */
	public boolean isSync() {
		return this.sync;
	}

	/**
	 * Gets Operational flag.
	 * 
	 * @return boolean
	 */
	public boolean isOperational() {
		return this.operational;
	}

	/**
	 * Gets Remove flag.
	 * 
	 * @return boolean
	 */
	public boolean isRemove() {
		return this.remove;
	}

	/**
	 * Gets list of {@link PCEPTlv}
	 * 
	 * @return List<PCEPTlv>. Can't be null, but may be empty.
	 */
	public List<PCEPTlv> getTlvs() {
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + (this.delegate ? 1231 : 1237);
		result = prime * result + this.lspID;
		result = prime * result + (this.operational ? 1231 : 1237);
		result = prime * result + (this.remove ? 1231 : 1237);
		result = prime * result + (this.sync ? 1231 : 1237);
		result = prime * result + ((this.tlvs == null) ? 0 : this.tlvs.hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (this.getClass() != obj.getClass())
			return false;
		final PCEPLspObject other = (PCEPLspObject) obj;
		if (this.delegate != other.delegate)
			return false;
		if (this.lspID != other.lspID)
			return false;
		if (this.operational != other.operational)
			return false;
		if (this.remove != other.remove)
			return false;
		if (this.sync != other.sync)
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
		toStringHelper.add("lspID", this.lspID);
		toStringHelper.add("delegate", this.delegate);
		toStringHelper.add("sync", this.sync);
		toStringHelper.add("operational", this.operational);
		toStringHelper.add("remove", this.remove);
		toStringHelper.add("tlvs", this.tlvs);
		return super.addToStringAttributes(toStringHelper);
	}
}
