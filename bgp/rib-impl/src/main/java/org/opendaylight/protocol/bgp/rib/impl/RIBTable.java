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
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.InstanceIdentifierBuilder;

import com.google.common.base.Preconditions;

@ThreadSafe
final class RIBTable<ID> {
	private static final Comparator<PathAttributes> comparator = new BGPObjectComparator();
	private final Class<? extends LocRib> root;
	private final TablesKey key;

	@GuardedBy("this")
	private final Map<ID, RIBEntry> entries = new HashMap<>();

	RIBTable(final Class<? extends LocRib> root, final TablesKey key) {
		this.root = Preconditions.checkNotNull(root);
		this.key = Preconditions.checkNotNull(key);
	}

	synchronized void clear(final DataModification trans, final BGPPeer peer) {
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

	private final InstanceIdentifier identifierForId(final ID id) {
		// FIXME: bug codegen people to allow us to pre-instantiate this
		InstanceIdentifierBuilder builder = InstanceIdentifier.builder().node(root).node(Tables.class, key);

		//			if (id instanceof Prefix<?>) {
		//				trans.removeRuntimeData(ii);
		//				trans.addRuntimeData();
		//				this.routes.remove(r, peer,  id);
		//			} else if (id instanceof LinkIdentifier) {
		//				this.links.remove(l, peer, (LinkIdentifier) id);
		//			} else if (id instanceof NodeIdentifier) {
		//				this.nodes.remove(n, peer, (NodeIdentifier) id);
		//			} else if (id instanceof PrefixIdentifier<?>) {
		//				this.prefixes.remove(p, peer, (PrefixIdentifier<?>) id);
		//			} else {
		//				throw new IllegalArgumentException("Unsupported identifier " + id.getClass());
		//			}

		return builder.toInstance();
	}

	synchronized void add(final DataModification trans, final BGPPeer peer, final ID id, final PathAttributes attrs) {
		RIBEntry e = this.entries.get(id);
		if (e == null) {
			e = new RIBEntry(identifierForId(id), comparator);
			this.entries.put(id, e);
		}

		e.setState(trans, peer, attrs);
	}

	synchronized void remove(final DataModification trans, final BGPPeer peer, final ID id) {
		final RIBEntry e = this.entries.get(id);
		if (e != null && e.removeState(trans, peer)) {
			this.entries.remove(id);
		}
	}
}
