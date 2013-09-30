/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.ProtocolId;

/**
 *
 */
public abstract class AbstractLinkstateMP<T> implements MPReach<T> {

	private final boolean reachable;

	private final long identifier;

	private final ProtocolId sourceProtocol;

	protected AbstractLinkstateMP(final long identifier, final ProtocolId sourceProtocol, final boolean reachable) {
		this.identifier = identifier;
		this.sourceProtocol = sourceProtocol;
		this.reachable = reachable;
	}

	@Override
	public boolean isReachable() {
		return this.reachable;
	}

	public long getIdentifier() {
		return this.identifier;
	}

	public ProtocolId getSourceProtocol() {
		return this.sourceProtocol;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (this.identifier ^ (this.identifier >>> 32));
		result = prime * result + (this.reachable ? 1231 : 1237);
		result = prime * result + ((this.sourceProtocol == null) ? 0 : this.sourceProtocol.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (this.getClass() != obj.getClass())
			return false;
		final AbstractLinkstateMP<?> other = (AbstractLinkstateMP<?>) obj;
		if (this.identifier != other.identifier)
			return false;
		if (this.reachable != other.reachable)
			return false;
		if (this.sourceProtocol != other.sourceProtocol)
			return false;
		return true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("AbstractLinkstateMP [reachable=");
		builder.append(this.reachable);
		builder.append(", identifier=");
		builder.append(this.identifier);
		builder.append(", sourceProtocol=");
		builder.append(this.sourceProtocol);
		builder.append("]");
		return builder.toString();
	}
}
