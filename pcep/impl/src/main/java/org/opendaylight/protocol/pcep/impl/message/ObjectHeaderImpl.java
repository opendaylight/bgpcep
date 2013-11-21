/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.message;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ObjectHeader;
import org.opendaylight.yangtools.yang.binding.DataContainer;

/**
 * Header parser for PCEP object
 */
public class ObjectHeaderImpl implements ObjectHeader {

	private final boolean processed;
	private final boolean ignored;

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
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (this.getClass() != obj.getClass()) {
			return false;
		}
		final ObjectHeaderImpl other = (ObjectHeaderImpl) obj;
		if (this.ignored != other.ignored) {
			return false;
		}
		if (this.processed != other.processed) {
			return false;
		}
		return true;
	}
}
