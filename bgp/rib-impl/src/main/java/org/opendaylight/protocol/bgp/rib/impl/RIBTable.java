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

import javax.annotation.concurrent.ThreadSafe;

import org.opendaylight.protocol.bgp.parser.AbstractBGPObjectState;

import org.opendaylight.protocol.concepts.Identifier;

@ThreadSafe
class RIBTable<ID extends Identifier, STATE extends AbstractBGPObjectState<?>> {
	private final Comparator<STATE> comparator = new BGPObjectComparator<>();
	private final Map<ID, RIBEntry<ID, STATE>> entries = new HashMap<>();

	RIBTable() {
	}

	synchronized void add(final Map<ID, STATE> transaction, final BGPPeer peer, final ID id, final STATE state) {
		RIBEntry<ID, STATE> e = this.entries.get(id);
		if (e == null) {
			e = new RIBEntry<ID, STATE>(id, this.comparator);
			this.entries.put(id, e);
		}

		e.setState(transaction, peer, state);
	}

	synchronized Map<ID, STATE> clear(final BGPPeer peer) {
		final Map<ID, STATE> transaction = new HashMap<>();

		final Iterator<Map.Entry<ID, RIBEntry<ID, STATE>>> i = this.entries.entrySet().iterator();
		while (i.hasNext()) {
			final Map.Entry<ID, RIBEntry<ID, STATE>> e = i.next();

			if (e.getValue().removeState(transaction, peer))
				i.remove();
		}

		return transaction;
	}

	synchronized void remove(final Map<ID, STATE> transaction, final BGPPeer peer, final ID id) {
		final RIBEntry<ID, STATE> e = this.entries.get(id);
		if (e != null && e.removeState(transaction, peer))
			this.entries.remove(id);
	}

	synchronized Map<ID, STATE> currentState() {
		final Map<ID, STATE> ret = new HashMap<>();

		for (final Entry<ID, RIBEntry<ID, STATE>> e : this.entries.entrySet())
			ret.put(e.getKey(), e.getValue().getState());

		return ret;
	}
}
