/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.bgp.topology.provider;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.NodeIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.loc.rib.tables.routes.linkstate.routes._case.linkstate.routes.LinkstateRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.loc.rib.tables.routes.linkstate.routes._case.linkstate.routes.linkstate.route.object.type.LinkCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.loc.rib.tables.routes.linkstate.routes._case.linkstate.routes.linkstate.route.object.type.link._case.LinkDescriptors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class UriBuilder {
	private static final Logger LOG = LoggerFactory.getLogger(UriBuilder.class);
	private final StringBuilder sb;

	UriBuilder(final UriBuilder base, final String type) {
		sb = new StringBuilder(base.sb);
		sb.append("type=").append(type);
	}

	UriBuilder(final LinkstateRoute route) {
		sb = new StringBuilder("bgpls://");

		if (route.getDistinguisher() != null) {
			sb.append(route.getDistinguisher().getValue().toString()).append(':');
		}

		sb.append(route.getProtocolId().toString()).append(':').append(route.getIdentifier().getValue().toString()).append('/');
	}

	UriBuilder add(final String name, final Object value) {
		if (value != null) {
			sb.append('&').append(name).append('=').append(value.toString());
		}
		return this;
	}

	UriBuilder add(final LinkCase link) {
		add("local-", link.getLocalNodeDescriptors());
		add("remote-", link.getRemoteNodeDescriptors());

		final LinkDescriptors ld = link.getLinkDescriptors();
		add("ipv4-iface", ld.getIpv4InterfaceAddress());
		add("ipv4-neigh", ld.getIpv4NeighborAddress());
		add("ipv6-iface", ld.getIpv6InterfaceAddress());
		add("ipv6-neigh", ld.getIpv6NeighborAddress());
		add("mt", ld.getMultiTopologyId());
		add("local-id", ld.getLinkLocalIdentifier());
		add("remote-id", ld.getLinkRemoteIdentifier());
		return this;
	}

	UriBuilder add(final String prefix, final NodeIdentifier node) {
		add(prefix + "as", node.getAsNumber());
		add(prefix + "domain", node.getDomainId());
		add(prefix + "area", node.getAreaId());;
		add(prefix + "router", node.getCRouterIdentifier());
		return this;
	}

	@Override
	public String toString() {
		final String ret = sb.toString();
		LOG.trace("New URI {}", ret);
		return ret;
	}
}
