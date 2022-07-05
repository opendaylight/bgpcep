/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.l3vpn.mcast;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Collection;
import java.util.List;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.protocol.bgp.l3vpn.mcast.nlri.L3vpnMcastNlriSerializer;
import org.opendaylight.protocol.bgp.parser.spi.PathIdUtil;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.l3vpn.mcast.rev180417.bgp.rib.rib.loc.rib.tables.routes.L3vpnMcastRoutesIpv6Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.l3vpn.mcast.rev180417.l3vpn.mcast.destination.L3vpnMcastDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.l3vpn.mcast.rev180417.l3vpn.mcast.routes.ipv6.L3vpnMcastRoutesIpv6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.l3vpn.mcast.rev180417.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationL3vpnMcastIpv6AdvertizedCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.l3vpn.mcast.rev180417.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationL3vpnMcastIpv6AdvertizedCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.l3vpn.mcast.rev180417.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.l3vpn.mcast.ipv6.advertized._case.DestinationIpv6L3vpnMcast;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.l3vpn.mcast.rev180417.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.l3vpn.mcast.ipv6.advertized._case.DestinationIpv6L3vpnMcastBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.l3vpn.mcast.rev180417.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationL3vpnMcastIpv6WithdrawnCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.l3vpn.mcast.rev180417.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationL3vpnMcastIpv6WithdrawnCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv6AddressFamily;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListEntryNode;

/**
 * Ipv6 L3VPN Multicast RIBSupport.
 *
 * @author Claudio D. Gasparini
 */
public final class L3VpnMcastIpv6RIBSupport
        extends AbstractL3vpnMcastIpRIBSupport<L3vpnMcastRoutesIpv6Case, L3vpnMcastRoutesIpv6> {
    public L3VpnMcastIpv6RIBSupport(final BindingNormalizedNodeSerializer mappingService) {
        super(mappingService,
                L3vpnMcastRoutesIpv6Case.class, L3vpnMcastRoutesIpv6Case.QNAME,
                L3vpnMcastRoutesIpv6.class,
                Ipv6AddressFamily.VALUE,
                DestinationIpv6L3vpnMcast.QNAME,
                L3vpnMcastDestination.QNAME);
    }

    @Override
    protected DestinationL3vpnMcastIpv6AdvertizedCase buildDestination(final Collection<MapEntryNode> routes) {
        return new DestinationL3vpnMcastIpv6AdvertizedCaseBuilder().setDestinationIpv6L3vpnMcast(
                new DestinationIpv6L3vpnMcastBuilder().setL3vpnMcastDestination(extractRoutes(routes)).build()).build();
    }

    @Override
    protected DestinationL3vpnMcastIpv6WithdrawnCase buildWithdrawnDestination(final Collection<MapEntryNode> routes) {
        return new DestinationL3vpnMcastIpv6WithdrawnCaseBuilder().setDestinationIpv6L3vpnMcast(
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.l3vpn.mcast.rev180417.update
                        .attributes.mp.unreach.nlri.withdrawn.routes.destination.type.destination.l3vpn.mcast.ipv6
                        .withdrawn._case.DestinationIpv6L3vpnMcastBuilder()
                        .setL3vpnMcastDestination(extractRoutes(routes)).build()).build();
    }

    @Override
    protected IpPrefix createPrefix(final String prefix) {
        return new IpPrefix(new Ipv6Prefix(prefix));
    }

    @Override
    public NodeIdentifierWithPredicates createRouteKey(final UnkeyedListEntryNode l3vpn) {
        final ByteBuf buffer = Unpooled.buffer();
        final L3vpnMcastDestination dest = extractDestinations(l3vpn);
        L3vpnMcastNlriSerializer.serializeNlri(List.of(dest), buffer);
        return PathIdUtil.createNidKey(routeQName(), routeKeyTemplate(),
                ByteArray.encodeBase64(buffer), l3vpn.findChildByArg(routePathIdNid()));
    }
}
