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

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.PathAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.LocRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

@ThreadSafe
public abstract class AbstractAdjRIBsIn<ID, DATA extends DataObject> implements AdjRIBsIn {
	protected static abstract class RIBEntryData<ID, DATA extends DataObject> {
		private final PathAttributes attributes;

		protected RIBEntryData(final PathAttributes attributes) {
			this.attributes = Preconditions.checkNotNull(attributes);
		}

		public PathAttributes getPathAttributes() {
			return this.attributes;
		}

		protected abstract DATA getDataObject(ID key);
	}

	/**
	 * A single RIB table entry, which holds multiple versions of the entry's state and elects the authoritative based
	 * on ordering specified by the supplied comparator.
	 * 
	 */
	private final class RIBEntry {
		/*
		 * TODO: we could dramatically optimize performance by using the comparator
		 *       to retain the candidate states ordered -- thus selection would occur
		 *       automatically through insertion, without the need of a second walk.
		 */
		private final Map<Peer, RIBEntryData<ID, DATA>> candidates = new HashMap<>();
		private final ID key;

		@GuardedBy("this")
		private InstanceIdentifier<?> name;
		@GuardedBy("this")
		private RIBEntryData<ID, DATA> currentState;

		RIBEntry(final ID key) {
			this.key = Preconditions.checkNotNull(key);
		}

		private InstanceIdentifier<?> getName() {
			if (this.name == null) {
				this.name = identifierForKey(AbstractAdjRIBsIn.this.basePath, this.key);
			}
			return this.name;
		}

		private RIBEntryData<ID, DATA> findCandidate(final RIBEntryData<ID, DATA> initial) {
			RIBEntryData<ID, DATA> newState = initial;
			for (final RIBEntryData<ID, DATA> s : this.candidates.values()) {
				if (newState == null || AbstractAdjRIBsIn.this.comparator.compare(newState.attributes, s.attributes) > 0) {
					newState = s;
				}
			}

			return newState;
		}

		private void electCandidate(final DataModificationTransaction transaction, final RIBEntryData<ID, DATA> candidate) {
			LOG.trace("Electing state {} to supersede {}", candidate, currentState);

			if (this.currentState == null || !this.currentState.equals(candidate)) {
				transaction.putRuntimeData(getName(), candidate.getDataObject(this.key));
				this.currentState = candidate;
			}
		}

		synchronized boolean removeState(final DataModificationTransaction transaction, final Peer peer) {
			final RIBEntryData<ID, DATA> data = this.candidates.remove(peer);
			LOG.trace("Removed data {}", data);

			final RIBEntryData<ID, DATA> candidate = findCandidate(null);
			if (candidate != null) {
				electCandidate(transaction, candidate);
			} else {
				LOG.trace("Final candidate disappeared, removing entry {}", name);
				transaction.removeRuntimeData(this.name);
			}

			return candidates.isEmpty();
		}

		synchronized void setState(final DataModificationTransaction transaction, final Peer peer, final RIBEntryData<ID, DATA> state) {
			this.candidates.put(Preconditions.checkNotNull(peer), Preconditions.checkNotNull(state));
			electCandidate(transaction, findCandidate(state));
		}
	}

	private static final Logger LOG = LoggerFactory.getLogger(AbstractAdjRIBsIn.class);
	private final Comparator<PathAttributes> comparator;
	private final InstanceIdentifier<Tables> basePath;
	@GuardedBy("this")
	private final Map<ID, RIBEntry> entries = new HashMap<>();

	protected AbstractAdjRIBsIn(final Comparator<PathAttributes> comparator, final TablesKey key) {
		this.comparator = Preconditions.checkNotNull(comparator);
		this.basePath = InstanceIdentifier.builder(LocRib.class).child(Tables.class, key).toInstance();
	}

	@Override
	public synchronized void clear(final DataModificationTransaction trans, final Peer peer) {
		final Iterator<Map.Entry<ID, RIBEntry>> i = this.entries.entrySet().iterator();
		while (i.hasNext()) {
			final Map.Entry<ID, RIBEntry> e = i.next();

			if (e.getValue().removeState(trans, peer)) {
				i.remove();
			}
		}
	}

	protected abstract InstanceIdentifier<?> identifierForKey(final InstanceIdentifier<Tables> basePath, final ID id);

	protected synchronized void add(final DataModificationTransaction trans, final Peer peer, final ID id, final RIBEntryData<ID, DATA> data) {
		RIBEntry e = this.entries.get(Preconditions.checkNotNull(id));
		if (e == null) {
			e = new RIBEntry(id);
			this.entries.put(id, e);
		}

		e.setState(trans, peer, data);
	}

	protected synchronized void remove(final DataModificationTransaction trans, final Peer peer, final ID id) {
		final RIBEntry e = this.entries.get(id);
		if (e != null && e.removeState(trans, peer)) {
			LOG.debug("Removed last state, removing entry for {}", id);
			this.entries.remove(id);
		}
	}
}
