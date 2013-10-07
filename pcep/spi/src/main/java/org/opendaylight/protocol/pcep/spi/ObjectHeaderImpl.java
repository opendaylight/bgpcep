/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.spi;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ObjectHeader;
import org.opendaylight.yangtools.yang.binding.DataContainer;

/**
 * Header parser for {@link org.opendaylight.protocol.pcep.PCEPObject PCEPObject}
 */
public class ObjectHeaderImpl implements ObjectHeader {

	/*
	 * Common object header fields lengths in bytes
	 */
	public final static int OC_F_LENGTH = 1;
	public final static int OT_FLAGS_MF_LENGTH = 1; // multi-field
	public final static int OBJ_LENGTH_F_LENGTH = 2;

	/*
	 * size of fields inside of multi-filed in bits
	 */
	public final static int OT_SF_LENGTH = 4;
	public final static int FLAGS_SF_LENGTH = 4;

	/*
	 * offsets of fields inside of multi-field in bits
	 */
	public final static int OT_SF_OFFSET = 0;
	public final static int FLAGS_SF_OFFSET = OT_SF_OFFSET + OT_SF_LENGTH;

	/*
	 * flags offsets inside multi-filed
	 */
	public final static int P_FLAG_OFFSET = 6;
	public final static int I_FLAG_OFFSET = 7;

	/*
	 * Common object header fields offsets in bytes;
	 */
	public final static int OC_F_OFFSET = 0;
	public final static int OT_FLAGS_MF_OFFSET = OC_F_OFFSET + OC_F_LENGTH;
	public final static int OBJ_LENGTH_F_OFFSET = OT_FLAGS_MF_OFFSET + OT_FLAGS_MF_LENGTH;
	public final static int OBJ_BODY_OFFSET = OBJ_LENGTH_F_OFFSET + OBJ_LENGTH_F_LENGTH;

	/*
	 * Common object header length in bytes
	 */
	public final static int COMMON_OBJECT_HEADER_LENGTH = (OC_F_LENGTH + OT_FLAGS_MF_LENGTH + OBJ_LENGTH_F_LENGTH);

	public final boolean processed;
	public final boolean ignored;

	public ObjectHeaderImpl(final boolean processed, final boolean ignore) {
		this.processed = processed;
		this.ignored = ignore;

	}
	
	@Override
	public Class<? extends DataContainer> getImplementedInterface() {
		return ObjectHeader.class;
	}

	@Override
	public Boolean isIgnore() {
		return this.ignored;
	}

	@Override
	public Boolean isProcessingRule() {
		return this.processed;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("ObjectHeader [objClass=");
		builder.append(", processed=");
		builder.append(this.processed);
		builder.append(", ignored=");
		builder.append(this.ignored);
		builder.append("]");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (this.ignored ? 1231 : 1237);
		result = prime * result + (this.processed ? 1231 : 1237);
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (this.getClass() != obj.getClass())
			return false;
		final ObjectHeaderImpl other = (ObjectHeaderImpl) obj;
		if (this.ignored != other.ignored)
			return false;
		if (this.processed != other.processed)
			return false;
		return true;
	}
}
