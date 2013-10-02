/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

import org.opendaylight.controller.sal.binding.api.data.DataModification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.PathAttributes;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Preconditions;

/**
 * A single RIB table entry, which holds multiple versions of the entry's state and elects the authoritative based on
 * ordering specified by the supplied comparator.
 * 
 * @param <ID> Identifier type
 */
@ThreadSafe
final class RIBEntry {
	/*
	 * TODO: we could dramatically optimize performance by using the comparator
	 *       to retain the candidate states ordered -- thus selection would occur
	 *       automatically through insertion, without the need of a second walk.
	 */
	private final Map<BGPPeer, PathAttributes> candidates = new HashMap<>();
	private final Comparator<PathAttributes> comparator;
	private final InstanceIdentifier name;

	@GuardedBy("this")
	private PathAttributes currentState = null;


	RIBEntry(final InstanceIdentifier name, final Comparator<PathAttributes> comparator) {
		this.name = Preconditions.checkNotNull(name);
		this.comparator = Preconditions.checkNotNull(comparator);
	}

	private PathAttributes findCandidate(final PathAttributes initial) {
		PathAttributes newState = initial;
		for (final PathAttributes s : this.candidates.values()) {
			if (this.comparator.compare(newState, s) > 0) {
				newState = s;
			}
		}

		return newState;
	}

	private void electCandidate(final DataModification transaction, final PathAttributes candidate) {
		if (this.currentState == null || !this.currentState.equals(candidate)) {
			transaction.putRuntimeData(name, candidate);
			this.currentState = candidate;
		}
	}

	synchronized boolean removeState(final DataModification transaction, final BGPPeer peer) {
		this.candidates.remove(peer);

		final PathAttributes candidate = findCandidate(null);
		if (candidate != null) {
			electCandidate(transaction, candidate);
			return true;
		} else {
			transaction.removeRuntimeData(name);
			return false;
		}
	}

	synchronized void setState(final DataModification transaction, final BGPPeer peer, final PathAttributes state) {
		this.candidates.put(peer, state);
		electCandidate(transaction, findCandidate(state));
	}

	synchronized PathAttributes getState() {
		return this.currentState;
	}
}
