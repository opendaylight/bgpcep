/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate;

import java.io.Serializable;

/**
 * Class representing per-Topology information about a Node. A node
 * in this context is typically a router, which has a few properties
 * which may be virtualized so as to have different values in topologies
 * in which the node participates.
 */
public final class TopologyNodeInformation implements Serializable {
	private static final long serialVersionUID = -3310738851831793539L;
	private final boolean attached, overloaded;

	/**
	 * Create a new per-topology object.
	 *
	 * @param isAttached Flag indicating the node is attached
	 * @param isOverloaded Flag indicating the node is overloaded
	 */
	public TopologyNodeInformation(final boolean isAttached, final boolean isOverloaded) {
		this.attached = isAttached;
		this.overloaded = isOverloaded;
	}

	/**
	 * Check if node is attached.
	 *
	 * @return True if the node is attached
	 */
	public boolean isAttached() {
		return this.attached;
	}

	/**
	 * Check if the node is overloaded. An overloaded noded should
	 * not be assigned new traffic.
	 *
	 * @return True if the node experiences overload
	 */
	public boolean isOverloaded() {
		return this.overloaded;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (this.attached ? 1231 : 1237);
		result = prime * result + (this.overloaded ? 1231 : 1237);
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof TopologyNodeInformation))
			return false;
		final TopologyNodeInformation other = (TopologyNodeInformation) obj;
		if (this.attached != other.attached)
			return false;
		if (this.overloaded != other.overloaded)
			return false;
		return true;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("TopologyNodeInformation [attached=");
		sb.append(this.attached);
		sb.append(", overloaded=");
		sb.append(this.overloaded);
		sb.append("]");
		return sb.toString();
	}
}

