/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib;

import java.util.Collections;
import java.util.Map;

import org.opendaylight.protocol.bgp.parser.BGPLinkState;
import org.opendaylight.protocol.bgp.parser.BGPNodeState;
import org.opendaylight.protocol.bgp.parser.BGPPrefixState;
import org.opendaylight.protocol.bgp.parser.BGPRouteState;

import org.opendaylight.protocol.concepts.Prefix;
import org.opendaylight.protocol.bgp.linkstate.LinkIdentifier;
import org.opendaylight.protocol.bgp.linkstate.NodeIdentifier;
import org.opendaylight.protocol.bgp.linkstate.PrefixIdentifier;
import com.google.common.base.Preconditions;

public final class RIBChangedEvent extends RIBEvent {
	private final Map<LinkIdentifier, BGPLinkState> links;
	private final Map<NodeIdentifier, BGPNodeState> nodes;
	private final Map<PrefixIdentifier<?>, BGPPrefixState> prefixes;
	private final Map<Prefix<?>, BGPRouteState<?>> routes;

	public RIBChangedEvent(final Map<LinkIdentifier, BGPLinkState> links, final Map<NodeIdentifier, BGPNodeState> nodes,
			final Map<PrefixIdentifier<?>, BGPPrefixState> prefixes, final Map<Prefix<?>, BGPRouteState<?>> routes) {
		super();
		this.links = Collections.unmodifiableMap(Preconditions.checkNotNull(links));
		this.nodes = Collections.unmodifiableMap(Preconditions.checkNotNull(nodes));
		this.prefixes = Collections.unmodifiableMap(Preconditions.checkNotNull(prefixes));
		this.routes = Collections.unmodifiableMap(Preconditions.checkNotNull(routes));
	}

	public RIBChangedEvent(final Map<Prefix<?>, BGPRouteState<?>> routes) {
		this(Collections.<LinkIdentifier, BGPLinkState> emptyMap(), Collections.<NodeIdentifier, BGPNodeState> emptyMap(), Collections.<PrefixIdentifier<?>, BGPPrefixState> emptyMap(), routes);
	}

	public RIBChangedEvent(final Map<LinkIdentifier, BGPLinkState> links, final Map<NodeIdentifier, BGPNodeState> nodes,
			final Map<PrefixIdentifier<?>, BGPPrefixState> prefixes) {
		this(links, nodes, prefixes, Collections.<Prefix<?>, BGPRouteState<?>> emptyMap());
	}

	/**
	 * @return the links
	 */
	public final Map<LinkIdentifier, BGPLinkState> getLinks() {
		return this.links;
	}

	/**
	 * @return the nodes
	 */
	public final Map<NodeIdentifier, BGPNodeState> getNodes() {
		return this.nodes;
	}

	/**
	 * @return the prefixes
	 */
	public final Map<PrefixIdentifier<?>, BGPPrefixState> getPrefixes() {
		return this.prefixes;
	}

	/**
	 * @return the routes
	 */
	public final Map<Prefix<?>, BGPRouteState<?>> getRoutes() {
		return this.routes;
	}
}
