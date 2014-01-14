/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.annotation.concurrent.ThreadSafe;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.SymbolicPathName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.lsp.metadata.Metadata;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;

import com.google.common.base.Preconditions;

@ThreadSafe
final class TopologyNodeState {
	private final Map<SymbolicPathName, Metadata> metadata = new HashMap<>();
	private final long holdStateNanos;
	private final NodeId nodeId;
	private long lastReleased = 0;

	public TopologyNodeState(final NodeId nodeId, final long holdStateNanos) {
		Preconditions.checkArgument(holdStateNanos >= 0);
		this.nodeId = Preconditions.checkNotNull(nodeId);
		this.holdStateNanos = holdStateNanos;
	}

	public NodeId getNodeId() {
		return nodeId;
	}

	public synchronized Metadata getLspMetadata(final SymbolicPathName name) {
		return metadata.get(name);
	}

	public synchronized void setLspMetadata(final SymbolicPathName name, final Metadata value) {
		if (value == null) {
			metadata.remove(name);
		} else {
			metadata.put(name, value);
		}
	}

	public synchronized void removeLspMetadata(final SymbolicPathName name) {
		metadata.remove(name);
	}

	public synchronized void cleanupExcept(final Collection<SymbolicPathName> values) {
		final Iterator<SymbolicPathName> it = metadata.keySet().iterator();
		while (it.hasNext()) {
			if (!values.contains(it.next())) {
				it.remove();
			}
		}
	}

	public synchronized void released() {
		lastReleased = System.nanoTime();
	}

	public synchronized void taken() {
		final long now = System.nanoTime();

		if (now - lastReleased > holdStateNanos) {
			metadata.clear();
		}
	}
}