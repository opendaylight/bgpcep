/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.concurrent.ThreadSafe;

import org.opendaylight.protocol.bgp.concepts.BGPObject;
import org.opendaylight.protocol.bgp.concepts.BGPTableType;
import org.opendaylight.protocol.bgp.linkstate.LinkIdentifier;
import org.opendaylight.protocol.bgp.linkstate.NodeIdentifier;
import org.opendaylight.protocol.bgp.linkstate.PrefixIdentifier;
import org.opendaylight.protocol.bgp.parser.BGPLink;
import org.opendaylight.protocol.bgp.parser.BGPLinkState;
import org.opendaylight.protocol.bgp.parser.BGPNode;
import org.opendaylight.protocol.bgp.parser.BGPNodeState;
import org.opendaylight.protocol.bgp.parser.BGPPrefix;
import org.opendaylight.protocol.bgp.parser.BGPPrefixState;
import org.opendaylight.protocol.bgp.parser.BGPRoute;
import org.opendaylight.protocol.bgp.parser.BGPRouteState;
import org.opendaylight.protocol.bgp.rib.RIB;
import org.opendaylight.protocol.bgp.rib.RIBChangedEvent;
import org.opendaylight.protocol.bgp.rib.RIBEvent;
import org.opendaylight.protocol.bgp.rib.RIBEventListener;
import org.opendaylight.protocol.concepts.InitialListenerEvents;
import org.opendaylight.protocol.concepts.ListenerRegistration;
import org.opendaylight.protocol.concepts.Prefix;

import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;
import com.google.common.base.Preconditions;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

@ThreadSafe
public final class RIBImpl implements RIB {
	private final RIBTable<LinkIdentifier, BGPLinkState> links = new RIBTable<>();
	private final RIBTable<NodeIdentifier, BGPNodeState> nodes = new RIBTable<>();
	private final RIBTable<PrefixIdentifier<?>, BGPPrefixState> prefixes = new RIBTable<>();
	private final RIBTable<Prefix<?>, BGPRouteState> routes = new RIBTable<>();
	private final EventBus bus;
	private final String name;

	public RIBImpl(final String name) {
		this.name = Preconditions.checkNotNull(name);
		this.bus = new EventBus(name);
	}

	synchronized void updateTables(final BGPPeer peer, final Set<BGPObject> addedObjects, final Set<?> removedObjects) {
		final Map<LinkIdentifier, BGPLinkState> l = new HashMap<>();
		final Map<NodeIdentifier, BGPNodeState> n = new HashMap<>();
		final Map<PrefixIdentifier<?>, BGPPrefixState> p = new HashMap<>();
		final Map<Prefix<?>, BGPRouteState> r = new HashMap<>();

		for (final Object id : removedObjects)
			if (id instanceof Prefix<?>)
				this.routes.remove(r, peer, (Prefix<?>) id);
			else if (id instanceof LinkIdentifier)
				this.links.remove(l, peer, (LinkIdentifier) id);
			else if (id instanceof NodeIdentifier)
				this.nodes.remove(n, peer, (NodeIdentifier) id);
			else if (id instanceof PrefixIdentifier<?>)
				this.prefixes.remove(p, peer, (PrefixIdentifier<?>) id);
			else
				throw new IllegalArgumentException("Unsupported identifier " + id.getClass());

		for (final BGPObject o : addedObjects)
			if (o instanceof BGPLink) {
				final BGPLink link = (BGPLink) o;
				this.links.add(l, peer, link.getLinkIdentifier(), link.currentState());
			} else if (o instanceof BGPNode) {
				final BGPNode node = (BGPNode) o;
				this.nodes.add(n, peer, node.getNodeIdentifier(), node.currentState());
			} else if (o instanceof BGPPrefix<?>) {
				final BGPPrefix<?> prefix = (BGPPrefix<?>) o;
				this.prefixes.add(p, peer, prefix.getPrefixIdentifier(), prefix.currentState());
			} else if (o instanceof BGPRoute) {
				final BGPRoute route = (BGPRoute) o;
				this.routes.add(r, peer, route.getName(), route.currentState());
			} else
				throw new IllegalArgumentException("Unsupported identifier " + o.getClass());

		if (!l.isEmpty() || !n.isEmpty() || !p.isEmpty() || !r.isEmpty())
			this.bus.post(new RIBChangedEvent(l, n, p, r));
	}

	synchronized void clearTable(final BGPPeer peer, final BGPTableType t) {
		switch (t.getAddressFamily()) {
		case Ipv4:
		case Ipv6:
			this.bus.post(new RIBChangedEvent(this.routes.clear(peer)));
			break;
		case Linkstate:
			this.bus.post(new RIBChangedEvent(this.links.clear(peer), this.nodes.clear(peer), this.prefixes.clear(peer)));
			break;
		}
	}

	@Override
	synchronized public InitialListenerEvents<RIBEventListener, RIBEvent> registerListener(final RIBEventListener listener) {
		final List<RIBEvent> events = new ArrayList<>();

		events.add(new RIBChangedEvent(this.routes.currentState()));
		events.add(new RIBChangedEvent(this.links.currentState(), this.nodes.currentState(), this.prefixes.currentState()));

		final Object wrapper = new Object() {
			@Subscribe
			public void notifyListener(final RIBChangedEvent event) {
				listener.onRIBEvent(event);
			}
		};
		this.bus.register(wrapper);

		return new InitialListenerEvents<RIBEventListener, RIBEvent>(new ListenerRegistration<RIBEventListener>() {
			@Override
			public void close() {
				RIBImpl.this.bus.unregister(wrapper);
			}

			@Override
			public RIBEventListener getListener() {
				return listener;
			}
		}, events);
	}

	@Override
	public final String toString() {
		return addToStringAttributes(Objects.toStringHelper(this)).toString();
	}

	protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
		toStringHelper.add("name", this.name);
		return toStringHelper;
	}
}
