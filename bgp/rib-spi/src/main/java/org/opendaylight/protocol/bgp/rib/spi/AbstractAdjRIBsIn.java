/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

import org.opendaylight.controller.sal.binding.api.data.DataModification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.PathAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.LocRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Preconditions;

@ThreadSafe
public abstract class AbstractAdjRIBsIn<ID> implements AdjRIBsIn {
	/**
	 * A single RIB table entry, which holds multiple versions of the entry's state and elects the authoritative based on
	 * ordering specified by the supplied comparator.
	 *
	 */
	private final class RIBEntry {
		/*
		 * TODO: we could dramatically optimize performance by using the comparator
		 *       to retain the candidate states ordered -- thus selection would occur
		 *       automatically through insertion, without the need of a second walk.
		 */
		private final Map<Peer, PathAttributes> candidates = new HashMap<>();
		private final InstanceIdentifier name;

		@GuardedBy("this")
		private PathAttributes currentState = null;


		RIBEntry(final InstanceIdentifier name) {
			this.name = Preconditions.checkNotNull(name);
		}

		private PathAttributes findCandidate(final PathAttributes initial) {
			PathAttributes newState = initial;
			for (final PathAttributes s : this.candidates.values()) {
				if (comparator.compare(newState, s) > 0) {
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

		synchronized boolean removeState(final DataModification transaction, final Peer peer) {
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

		synchronized void setState(final DataModification transaction, final Peer peer, final PathAttributes state) {
			this.candidates.put(peer, state);
			electCandidate(transaction, findCandidate(state));
		}

		synchronized PathAttributes getState() {
			return this.currentState;
		}
	}

	private final Comparator<PathAttributes> comparator;
	private final InstanceIdentifier basePath;
	@GuardedBy("this")
	private final Map<ID, RIBEntry> entries = new HashMap<>();

	protected AbstractAdjRIBsIn(final Comparator<PathAttributes> comparator, final TablesKey key) {
		this.comparator = Preconditions.checkNotNull(comparator);
		basePath = InstanceIdentifier.builder().node(LocRib.class).node(Tables.class, key).toInstance();
	}

	@Override
	public synchronized void clear(final DataModification trans, final Peer peer) {
		final Iterator<Map.Entry<ID, RIBEntry>> i = this.entries.entrySet().iterator();
		while (i.hasNext()) {
			final Map.Entry<ID, RIBEntry> e = i.next();

			if (e.getValue().removeState(trans, peer)) {
				i.remove();
			}
		}
	}

	synchronized Map<ID, PathAttributes> currentState() {
		final Map<ID, PathAttributes> ret = new HashMap<>();

		for (final Entry<ID, RIBEntry> e : this.entries.entrySet()) {
			ret.put(e.getKey(), e.getValue().getState());
		}

		return ret;
	}

	protected abstract InstanceIdentifier identifierForKey(final InstanceIdentifier basePath, final ID id);

	protected synchronized void add(final DataModification trans, final Peer peer, final ID id, final PathAttributes attrs) {
		RIBEntry e = this.entries.get(id);
		if (e == null) {
			e = new RIBEntry(identifierForKey(basePath, id));
			this.entries.put(id, e);
		}

		e.setState(trans, peer, attrs);
	}

	protected synchronized void remove(final DataModification trans, final Peer peer, final ID id) {
		final RIBEntry e = this.entries.get(id);
		if (e != null && e.removeState(trans, peer)) {
			this.entries.remove(id);
		}
	}
}
