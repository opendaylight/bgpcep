/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.evpn.impl.nlri;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.ethernet.tag.id.EthernetTagId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.ethernet.tag.id.EthernetTagIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.evpn.EvpnChoice;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.RouteDistinguisher;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.RouteDistinguisherBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.MplsLabel;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerNode;

final class NlriModelUtil {
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
        throw new UnsupportedOperationException();
    }

    static RouteDistinguisher extractRouteDistinguisher(final DataContainerNode<? extends PathArgument> evpn) {
        return RouteDistinguisherBuilder.getDefaultInstance((String) evpn.getChild(RD_NID).get().getValue());
    }

    static IpAddress extractOrigRouteIp(final DataContainerNode<? extends PathArgument> evpn) {
        return IpAddressBuilder.getDefaultInstance((String) evpn.getChild(ORI_NID).get().getValue());
    }


    static EthernetTagId extractETI(final ContainerNode evpn) {
        final ContainerNode eti = (ContainerNode) evpn.getChild(ETI_NID).get();
        return new EthernetTagIdBuilder().setVlanId((Long) eti.getChild(VLAN_NID).get().getValue()).build();
    }

    static MacAddress extractMAC(final DataContainerNode<? extends PathArgument> evpn) {
        return new MacAddress((String) evpn.getChild(MAC_NID).get().getValue());
    }


    static IpAddress extractIp(final DataContainerNode<? extends PathArgument> evpn) {
        if (evpn.getChild(IP_NID).isPresent()) {
            return IpAddressBuilder.getDefaultInstance((String) evpn.getChild(IP_NID).get().getValue());
        }
        return null;
    }

    static MplsLabel extractMplsLabel(final DataContainerNode<? extends PathArgument> evpn,
            final NodeIdentifier mplsNid) {
        if (evpn.getChild(mplsNid).isPresent()) {
            return new MplsLabel((Long) evpn.getChild(mplsNid).get().getValue());
        }
        return null;
    }
}
