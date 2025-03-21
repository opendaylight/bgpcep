/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.bgp.topology.provider;

import com.google.common.io.BaseEncoding;
import java.util.Arrays;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.IsisAreaIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.NodeIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.object.type.LinkCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.object.type.link._case.LinkDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.routes.linkstate.routes.LinkstateRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.node.identifier.CRouterIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.node.identifier.c.router.identifier.IsisNodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.node.identifier.c.router.identifier.IsisPseudonodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.node.identifier.c.router.identifier.OspfNodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.node.identifier.c.router.identifier.OspfPseudonodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.node.identifier.c.router.identifier.isis.pseudonode._case.IsisPseudonode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.node.identifier.c.router.identifier.ospf.pseudonode._case.OspfPseudonode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.IsoSystemIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class UriBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(UriBuilder.class);
    private static final char HEX_SEPARATOR = '.';
    private static final int AREA_ID_MAX_SIZE = 7;
    private final StringBuilder sb;

    UriBuilder(final UriBuilder base, final String type) {
        this.sb = new StringBuilder(base.sb);
        this.sb.append("type=").append(type);
    }

    UriBuilder(final LinkstateRoute route) {
        this.sb = new StringBuilder("bgpls://");

        if (route.getRouteDistinguisher() != null) {
            String rd;
            if (route.getRouteDistinguisher().getRdAs() != null) {
                rd = route.getRouteDistinguisher().getRdAs().getValue();
            } else if (route.getRouteDistinguisher().getRdIpv4() != null) {
                rd = route.getRouteDistinguisher().getRdIpv4().getValue();
            } else  {
                rd = route.getRouteDistinguisher().getRdTwoOctetAs().getValue();
            }
            this.sb.append(rd).append(':');
        }

        this.sb.append(route.getProtocolId().toString()).append(':')
                .append(route.getIdentifier().getValue().toString()).append('/');
    }

    UriBuilder add(final String name, final Object value) {
        if (value != null) {
            this.sb.append('&').append(name).append('=').append(value.toString());
        }
        return this;
    }

    UriBuilder add(final LinkCase link) {
        addPrefix("local-", link.getLocalNodeDescriptors());
        addPrefix("remote-", link.getRemoteNodeDescriptors());

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
        add("local-id", ld.getLinkLocalIdentifier());
        add("remote-id", ld.getLinkRemoteIdentifier());
        return this;
    }

    private static String isoId(final byte[] bytes) {
        final StringBuilder sBuilder = new StringBuilder();
        int id = 0;
        while (id < bytes.length) {
            sBuilder.append(BaseEncoding.base16().encode(new byte[] { bytes[id++], bytes[id++] }));
            if (id != bytes.length) {
                sBuilder.append(HEX_SEPARATOR);
            }
        }
        return sBuilder.toString();
    }

    /**
     * Creates a String representation of ISO system identifier
     * in format XX.XX.XX where X is one byte.
     *
     * @param systemId IsoSystemIdentifier object
     * @return String representation of ISO Identifier
     */
    public static String isoId(final IsoSystemIdentifier systemId) {
        return isoId(systemId.getValue());
    }

    private static String formatRouterIdentifier(final CRouterIdentifier routerIdentifier) {
        if (routerIdentifier == null) {
            return null;
        }
        if (routerIdentifier instanceof IsisNodeCase) {
            return isoId(((IsisNodeCase) routerIdentifier).getIsisNode().getIsoSystemId());
        }
        if (routerIdentifier instanceof IsisPseudonodeCase) {
            final IsisPseudonode r = ((IsisPseudonodeCase) routerIdentifier).getIsisPseudonode();
            return isoId(r.getIsIsRouterIdentifier().getIsoSystemId().getValue()) + '.'
                    + BaseEncoding.base16().encode(new byte[] { r.getPsn().byteValue() });
        }
        if (routerIdentifier instanceof OspfNodeCase) {
            return ((OspfNodeCase) routerIdentifier).getOspfNode().getOspfRouterId().toString();
        }
        if (routerIdentifier instanceof OspfPseudonodeCase) {
            final OspfPseudonode r = ((OspfPseudonodeCase) routerIdentifier).getOspfPseudonode();
            return r.getOspfRouterId().toString() + ':' + r.getLanInterface().getValue();
        }
        LOG.warn("Unhandled router identifier type {}, fallback to toString()",
                routerIdentifier.implementedInterface());
        return routerIdentifier.toString();
    }

    UriBuilder addPrefix(final String prefix, final NodeIdentifier node) {
        if (node.getAsNumber() != null) {
            add(prefix + "as", node.getAsNumber().getValue());
        }
        if (node.getDomainId() != null) {
            add(prefix + "domain", node.getDomainId().getValue());
        }
        if (node.getAreaId() != null) {
            add(prefix + "area", node.getAreaId().getValue());
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

    /**
     * Creates string representation of IS-IS Network Entity Title,
     * based on Area Identifier and System Identifier.
     *
     * @param areaId IS-IS Area Identifier
     * @param systemId string representation of ISO SYSTEM-ID
     * @return ISO NET ID
     */
    public static String toIsoNetId(final IsisAreaIdentifier areaId, final String systemId) {
        final byte[] value = areaId.getValue();
        //first byte is AFI
        //ISIS area identifier might have variable length, but need to fit the IsoNetId pattern
        return BaseEncoding.base16().encode(value, 0, 1) + HEX_SEPARATOR
                + UriBuilder.isoId(Arrays.copyOfRange(value, 1, AREA_ID_MAX_SIZE)) + HEX_SEPARATOR + systemId;
    }
}
