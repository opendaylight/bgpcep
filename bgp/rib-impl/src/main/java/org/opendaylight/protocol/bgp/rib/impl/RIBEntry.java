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

import javax.annotation.concurrent.ThreadSafe;

import org.opendaylight.protocol.bgp.parser.AbstractBGPObjectState;
import org.opendaylight.protocol.concepts.Identifier;
import org.opendaylight.protocol.concepts.NamedObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

/**
 * A single RIB table entry, which holds multiple versions of the entry's state and elects the authoritative based on
 * ordering specified by the supplied comparator.
 * 
 * @param <ID> Identifier type
 * @param <STATE> State type
 */
@ThreadSafe
final class RIBEntry<ID extends Identifier, STATE extends AbstractBGPObjectState<?>> implements NamedObject<ID> {
	private static final Logger LOG = LoggerFactory.getLogger(RIBEntry.class);

	/*
	 * TODO: we could dramatically optimize performance by using the comparator
	 *       to retain the candidate states ordered -- thus selection would occur
	 *       automatically through insertion, without the need of a second walk.
	 */
	private final Map<BGPPeer, STATE> candidates = new HashMap<>();
	private final Comparator<STATE> comparator;
	private STATE currentState = null;
	private final ID name;

	RIBEntry(final ID name, final Comparator<STATE> comparator) {
		this.name = Preconditions.checkNotNull(name);
		this.comparator = Preconditions.checkNotNull(comparator);
	}

	@Override
	public ID getName() {
		return this.name;
	}

	private STATE findCandidate(final STATE initial) {
		STATE newState = initial;
		for (final STATE s : this.candidates.values())
			if (this.comparator.compare(newState, s) > 0)
				newState = s;

		return newState;
	}

	private void electCandidate(final Map<ID, STATE> transaction, final STATE candidate) {
		LOG.trace("Electing state {} to supersede {}", candidate, currentState);

		if (this.currentState == null || !this.currentState.equals(candidate)) {
			transaction.put(this.name, candidate);
			this.currentState = candidate;
		}
	}

	synchronized boolean removeState(final Map<ID, STATE> transaction, final BGPPeer peer) {
		LOG.trace("Removing candidate {}", peer);

		final STATE s = this.candidates.remove(peer);
		LOG.trace("Removed state {}", s);

		final STATE candidate = findCandidate(null);
		LOG.trace("New candidate state {}", candidate);

		electCandidate(transaction, candidate);
		return candidates.isEmpty();
	}

	synchronized void setState(final Map<ID, STATE> transaction, final BGPPeer peer, final STATE state) {
		this.candidates.put(peer, state);
		electCandidate(transaction, findCandidate(state));
	}

	synchronized STATE getState() {
		return this.currentState;
	}
}
