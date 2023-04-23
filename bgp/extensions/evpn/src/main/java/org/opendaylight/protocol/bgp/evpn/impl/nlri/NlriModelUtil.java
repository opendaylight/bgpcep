/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.evpn.impl.nlri;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.bgp.concepts.RouteDistinguisherUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev200120.ethernet.tag.id.EthernetTagId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev200120.ethernet.tag.id.EthernetTagIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev200120.evpn.EvpnChoice;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.RouteDistinguisher;
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

    static @Nullable RouteDistinguisher extractRouteDistinguisher(final DataContainerNode evpn) {
        return RouteDistinguisherUtil.extractRouteDistinguisher(evpn, RD_NID);
    }

    static @NonNull IpAddressNoZone extractOrigRouteIp(final DataContainerNode evpn) {
        return parseIpAddress((String) evpn.getChildByArg(ORI_NID).body());
    }

    static @NonNull EthernetTagId extractETI(final ContainerNode evpn) {
        final ContainerNode eti = (ContainerNode) evpn.getChildByArg(ETI_NID);
        return new EthernetTagIdBuilder().setVlanId((Uint32) eti.getChildByArg(VLAN_NID).body()).build();
    }

    static @NonNull MacAddress extractMAC(final DataContainerNode evpn) {
        return new MacAddress((String) evpn.getChildByArg(MAC_NID).body());
    }

    static @Nullable IpAddressNoZone extractIp(final DataContainerNode evpn) {
        final var ip = evpn.childByArg(IP_NID);
        return ip == null ? null : parseIpAddress((String) ip.body());
    }

    static @Nullable MplsLabel extractMplsLabel(final DataContainerNode evpn, final NodeIdentifier mplsNid) {
        final var label = evpn.childByArg(mplsNid);
        return label == null ? null : new MplsLabel((Uint32) label.body());
    }

    private static @NonNull IpAddressNoZone parseIpAddress(final String str) {
        try {
            return new IpAddressNoZone(new Ipv4AddressNoZone(str));
        } catch (IllegalArgumentException e) {
            LOG.debug("Failed to interpret {} as an Ipv4AddressNoZone", str, e);
        }
        return new IpAddressNoZone(new Ipv6AddressNoZone(str));
    }
}
