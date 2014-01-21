/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.bgp.topology.provider;

import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.NodeIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.bgp.rib.rib.loc.rib.tables.routes.linkstate.routes._case.linkstate.routes.LinkstateRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.bgp.rib.rib.loc.rib.tables.routes.linkstate.routes._case.linkstate.routes.linkstate.route.object.type.LinkCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.bgp.rib.rib.loc.rib.tables.routes.linkstate.routes._case.linkstate.routes.linkstate.route.object.type.link._case.LinkDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.node.identifier.CRouterIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.node.identifier.c.router.identifier.IsisNodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.node.identifier.c.router.identifier.IsisPseudonodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.node.identifier.c.router.identifier.OspfNodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.node.identifier.c.router.identifier.OspfPseudonodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.node.identifier.c.router.identifier.isis.pseudonode._case.IsisPseudonode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.node.identifier.c.router.identifier.ospf.pseudonode._case.OspfPseudonode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.primitives.UnsignedBytes;
import com.google.common.primitives.UnsignedInteger;

final class UriBuilder {
	private static final Logger LOG = LoggerFactory.getLogger(UriBuilder.class);
	private final StringBuilder sb;

	UriBuilder(final UriBuilder base, final String type) {
		this.sb = new StringBuilder(base.sb);
		this.sb.append("type=").append(type);
	}

	UriBuilder(final LinkstateRoute route) {
		this.sb = new StringBuilder("bgpls://");

		if (route.getDistinguisher() != null) {
			this.sb.append(route.getDistinguisher().getValue().toString()).append(':');
		}

		this.sb.append(route.getProtocolId().toString()).append(':').append(route.getIdentifier().getValue().toString()).append('/');
	}

	UriBuilder add(final String name, final Object value) {
		if (value != null) {
			this.sb.append('&').append(name).append('=').append(value.toString());
		}
		return this;
	}

	UriBuilder add(final LinkCase link) {
		add("local-", link.getLocalNodeDescriptors());
		add("remote-", link.getRemoteNodeDescriptors());

		final LinkDescriptors ld = link.getLinkDescriptors();
		if (ld.getIpv4InterfaceAddress() != null) {
			add("ipv4-iface", ld.getIpv4InterfaceAddress().getValue());
		}
		if (ld.getIpv4NeighborAddress() != null) {
			add("ipv4-neigh", ld.getIpv4NeighborAddress().getValue());
		}
		if (ld.getIpv6InterfaceAddress() != null) {
			add("ipv6-iface", ld.getIpv6InterfaceAddress().getValue());
		}
		if (ld.getIpv6NeighborAddress() != null) {
			add("ipv6-neigh", ld.getIpv6NeighborAddress().getValue());
		}
		if (ld.getMultiTopologyId() != null) {
			add("mt", ld.getMultiTopologyId().getValue());
		}
		if (ld.getLinkLocalIdentifier() != null) {
			add("local-id", UnsignedInteger.fromIntBits(ByteArray.bytesToInt(ld.getLinkLocalIdentifier())));
		}
		if (ld.getLinkRemoteIdentifier() != null) {
			add("remote-id", UnsignedInteger.fromIntBits(ByteArray.bytesToInt(ld.getLinkRemoteIdentifier())));
		}
		return this;
	}

	private final String isoId(final byte[] bytes) {
		final StringBuilder sb = new StringBuilder();

		for (int i = 0; i < bytes.length - 1; i++) {
			sb.append(UnsignedBytes.toInt(bytes[i]));
			sb.append('.');
		}

		sb.append(UnsignedBytes.toInt(bytes[bytes.length - 1]));
		return sb.toString();
	}

	private final String formatRouterIdentifier(final CRouterIdentifier routerIdentifier) {
		if (routerIdentifier == null) {
			return null;
		}

		if (routerIdentifier instanceof IsisNodeCase) {
			return isoId(((IsisNodeCase)routerIdentifier).getIsisNode().getIsoSystemId().getValue());
		} else if (routerIdentifier instanceof IsisPseudonodeCase) {
			final IsisPseudonode r = ((IsisPseudonodeCase)routerIdentifier).getIsisPseudonode();
			return isoId(r.getIsIsRouterIdentifier().getIsoSystemId().getValue()) + '.' + r.getPsn();
		} else if (routerIdentifier instanceof OspfNodeCase) {
			return ByteArray.bytesToHexString(((OspfNodeCase)routerIdentifier).getOspfNode().getOspfRouterId());
		} else if (routerIdentifier instanceof OspfPseudonodeCase) {
			final OspfPseudonode r = ((OspfPseudonodeCase)routerIdentifier).getOspfPseudonode();
			return ByteArray.bytesToHexString(r.getOspfRouterId()) + ':' + ByteArray.bytesToHexString(r.getLanInterface().getValue());
		} else {
			LOG.warn("Unhandled router identifier type {}, fallback to toString()", routerIdentifier.getImplementedInterface());
			return routerIdentifier.toString();
		}
	}

	UriBuilder add(final String prefix, final NodeIdentifier node) {
		if (node.getAsNumber() != null) {
			add(prefix + "as", node.getAsNumber().getValue());
		}
		if (node.getDomainId() != null) {
			add(prefix + "domain", ByteArray.bytesToHexString(node.getDomainId().getValue()));
		}
		if (node.getAreaId() != null) {
			add(prefix + "area", ByteArray.bytesToHexString(node.getAreaId().getValue()));
		}
		add(prefix + "router", formatRouterIdentifier(node.getCRouterIdentifier()));
		return this;
	}

	@Override
	public String toString() {
		final String ret = this.sb.toString();
		LOG.trace("New URI {}", ret);
		return ret;
	}
}
