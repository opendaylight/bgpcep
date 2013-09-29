/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.concurrent.ThreadSafe;

import org.opendaylight.protocol.bgp.concepts.BGPObject;
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
import org.opendaylight.protocol.bgp.parser.BGPTableType;
import org.opendaylight.protocol.concepts.Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.LinkstateAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv6AddressFamily;

import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;
import com.google.common.base.Preconditions;

@ThreadSafe
public final class RIBImpl {
	private final RIBTable<LinkIdentifier, BGPLinkState> links = new RIBTable<>();
	private final RIBTable<NodeIdentifier, BGPNodeState> nodes = new RIBTable<>();
	private final RIBTable<PrefixIdentifier<?>, BGPPrefixState> prefixes = new RIBTable<>();
	private final RIBTable<Prefix<?>, BGPRouteState> routes = new RIBTable<>();
	private final String name;

	public RIBImpl(final String name) {
		this.name = Preconditions.checkNotNull(name);
	}

	synchronized void updateTables(final BGPPeer peer, final Set<BGPObject> addedObjects, final Set<?> removedObjects) {
		final Map<LinkIdentifier, BGPLinkState> l = new HashMap<>();
		final Map<NodeIdentifier, BGPNodeState> n = new HashMap<>();
		final Map<PrefixIdentifier<?>, BGPPrefixState> p = new HashMap<>();
		final Map<Prefix<?>, BGPRouteState> r = new HashMap<>();

		for (final Object id : removedObjects) {
			if (id instanceof Prefix<?>) {
				this.routes.remove(r, peer, (Prefix<?>) id);
			} else if (id instanceof LinkIdentifier) {
				this.links.remove(l, peer, (LinkIdentifier) id);
			} else if (id instanceof NodeIdentifier) {
				this.nodes.remove(n, peer, (NodeIdentifier) id);
			} else if (id instanceof PrefixIdentifier<?>) {
				this.prefixes.remove(p, peer, (PrefixIdentifier<?>) id);
			} else {
				throw new IllegalArgumentException("Unsupported identifier " + id.getClass());
			}
		}

		for (final BGPObject o : addedObjects) {
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
			} else {
				throw new IllegalArgumentException("Unsupported identifier " + o.getClass());
			}
		}

		// FIXME: push into MD SAL
	}

	synchronized void clearTable(final BGPPeer peer, final BGPTableType t) {
		if (Ipv4AddressFamily.class == t.getAddressFamily() || Ipv6AddressFamily.class == t.getAddressFamily()) {
			this.routes.clear(peer);
		} else if (LinkstateAddressFamily.class == t.getAddressFamily()) {
			this.links.clear(peer);
			this.nodes.clear(peer);
			this.prefixes.clear(peer);
		}
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
