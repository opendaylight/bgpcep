/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.evpn.impl.nlri;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev200120.ethernet.tag.id.EthernetTagId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev200120.ethernet.tag.id.EthernetTagIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev200120.evpn.EvpnChoice;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.RouteDistinguisher;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.RouteDistinguisherBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.MplsLabel;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class NlriModelUtil {
    private static final Logger LOG = LoggerFactory.getLogger(NlriModelUtil.class);

    static final NodeIdentifier ETI_NID = NodeIdentifier.create(QName.create(EvpnChoice.QNAME,
            "ethernet-tag-id").intern());
    static final NodeIdentifier VLAN_NID = NodeIdentifier.create(QName.create(EvpnChoice.QNAME,
            "vlan-id").intern());
    static final NodeIdentifier RD_NID = NodeIdentifier.create(QName.create(EvpnChoice.QNAME,
            "route-distinguisher").intern());
    static final NodeIdentifier ORI_NID = NodeIdentifier.create(QName.create(EvpnChoice.QNAME,
            "orig-route-ip").intern());
    static final NodeIdentifier MAC_NID = NodeIdentifier.create(QName.create(EvpnChoice.QNAME,
            "mac-address").intern());
    static final NodeIdentifier IP_NID = NodeIdentifier.create(QName.create(EvpnChoice.QNAME,
            "ip-address").intern());
    static final NodeIdentifier MPLS_NID = NodeIdentifier.create(QName.create(EvpnChoice.QNAME,
            "mpls-label").intern());
    static final NodeIdentifier MPLS1_NID = NodeIdentifier.create(QName.create(EvpnChoice.QNAME,
            "mpls-label1").intern());
    static final NodeIdentifier MPLS2_NID = NodeIdentifier.create(QName.create(EvpnChoice.QNAME,
            "mpls-label2").intern());

    private NlriModelUtil() {
        // Hidden on purpose
    }

    static RouteDistinguisher extractRouteDistinguisher(final DataContainerNode evpn) {
        return RouteDistinguisherBuilder.getDefaultInstance((String) evpn.findChildByArg(RD_NID).get().body());
    }

    static IpAddressNoZone extractOrigRouteIp(final DataContainerNode evpn) {
        return parseIpAddress((String) evpn.findChildByArg(ORI_NID).get().body());
    }

    static EthernetTagId extractETI(final ContainerNode evpn) {
        final ContainerNode eti = (ContainerNode) evpn.findChildByArg(ETI_NID).get();
        return new EthernetTagIdBuilder().setVlanId((Uint32) eti.findChildByArg(VLAN_NID).get().body()).build();
    }

    static MacAddress extractMAC(final DataContainerNode evpn) {
        return new MacAddress((String) evpn.findChildByArg(MAC_NID).get().body());
    }

    static IpAddressNoZone extractIp(final DataContainerNode evpn) {
        return evpn.findChildByArg(IP_NID)
            .map(child -> parseIpAddress((String) child.body()))
            .orElse(null);
    }

    static MplsLabel extractMplsLabel(final DataContainerNode evpn, final NodeIdentifier mplsNid) {
        return evpn.findChildByArg(mplsNid).map(child -> new MplsLabel((Uint32) child.body())).orElse(null);
    }

    private static IpAddressNoZone parseIpAddress(final String str) {
        try {
            return new IpAddressNoZone(new Ipv4AddressNoZone(str));
        } catch (IllegalArgumentException e) {
            LOG.debug("Failed to interpret {} as an Ipv4AddressNoZone", str, e);
        }
        return new IpAddressNoZone(new Ipv6AddressNoZone(str));
    }
}
