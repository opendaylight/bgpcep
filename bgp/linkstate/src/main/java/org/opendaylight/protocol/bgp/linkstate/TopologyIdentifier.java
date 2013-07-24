/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.linkstate;

import java.nio.ByteBuffer;

import org.opendaylight.protocol.concepts.AbstractIdentifier;
import com.google.common.base.Objects.ToStringHelper;

/**
 * Topology identifier. This identifier is used in multi-topology networks,
 * e.g. when there are multiple logical network built on top of a set of
 * physical components. Usually routing happens only within a single topology,
 * but nodes resident in multiple topologies can act as inter-topology
 * gateways.
 */
public final class TopologyIdentifier extends AbstractIdentifier<TopologyIdentifier> {
	private static final long serialVersionUID = 8386493752725436667L;
	private final short id;

	/**
	 * Construct a new topology identifier for a numeric ID.
	 *
	 * @param id Topology identifier. Valid range is 0-4095.
	 */
	public TopologyIdentifier(final long id) {
		if (id < 0 || id > 4095)
			throw new IllegalArgumentException("Invalid topology identifier");
		this.id = (short) id;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + this.id;
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
		final TopologyIdentifier other = (TopologyIdentifier) obj;
		if (this.id != other.id)
			return false;
		return true;
	}

	@Override
	protected byte[] getBytes() {
		final ByteBuffer bb = ByteBuffer.allocate(Short.SIZE);
		bb.putShort(this.id);
		return bb.array();
	}

	/**
	 * Return ID attribute of Topology Identifier.
	 *
	 * @return Topology identifier
	 */
	public short getId() {
		return this.id;
	}

	@Override
	protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
		return toStringHelper.add("id", id);
	}
}
