/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.concepts;

import org.opendaylight.protocol.concepts.NetworkAddress;

/**
 * Basic structure of Extended Tunnel Identifier.
 * 
 * @see <a
 *      href="http://tools.ietf.org/html/draft-crabbe-pce-stateful-pce-02#section-7.2.2">LSP
 *      Identifiers TLVs</a>
 * @param <T>
 */
public abstract class AbstractExtendedTunnelIdentifier<T extends NetworkAddress<T>> implements Comparable<ExtendedTunnelIdentifier<T>>, ExtendedTunnelIdentifier<T> {

	private static final long serialVersionUID = 110737862492677555L;

	private final T identifier;

	protected AbstractExtendedTunnelIdentifier(final T identifier) {
		this.identifier = identifier;
	}

	@Override
	public T getIdentifier() {
		return this.identifier;
	}

	@Override
	public int compareTo(final ExtendedTunnelIdentifier<T> other) {
		if (this.identifier == other.getIdentifier())
			return 0;
		if (this.identifier == null)
			return -1;
		if (other.getIdentifier() == null)
			return 1;
		return this.identifier.compareTo(other.getIdentifier());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.identifier == null) ? 0 : this.identifier.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (this.getClass() != obj.getClass())
			return false;
		final AbstractExtendedTunnelIdentifier<?> other = (AbstractExtendedTunnelIdentifier<?>) obj;
		if (this.identifier == null) {
			if (other.identifier != null)
				return false;
		} else if (!this.identifier.equals(other.identifier))
			return false;
		return true;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("AbstractExtendedTunnelIdentifier [identifier=");
		builder.append(this.identifier);
		builder.append("]");
		return builder.toString();
	}
}
