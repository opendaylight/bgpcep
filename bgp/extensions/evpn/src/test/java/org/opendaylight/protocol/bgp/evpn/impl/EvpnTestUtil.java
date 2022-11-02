/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.evpn.impl;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.RouteDistinguisher;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.RouteDistinguisherBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.MplsLabel;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.builder.DataContainerNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;

public final class EvpnTestUtil {
    public static final int VALUE_SIZE = 9;
    public static final Uint32 LD = Uint32.valueOf(33686018);
    public static final String MAC_MODEL = "f2:0c:dd:80:9f:f7";
    public static final MacAddress MAC = new MacAddress(MAC_MODEL);
    public static final Uint32 AS_MODEL = Uint32.valueOf(16843009);
    public static final AsNumber AS_NUMBER = new AsNumber(AS_MODEL);
    public static final Uint16 PORT = Uint16.valueOf(514);
    public static final Uint32 MPLS_LABEL_MODEL = Uint32.valueOf(24001L);
    public static final MplsLabel MPLS_LABEL = new MplsLabel(MPLS_LABEL_MODEL);
    public static final int COMMUNITY_VALUE_SIZE = 6;
    public static final Uint32 VLAN = Uint32.TEN;
    public static final String IP_MODEL = "127.0.0.1";
    public static final IpAddressNoZone IP = new IpAddressNoZone(new Ipv4AddressNoZone(IP_MODEL));
    public static final String IPV6_MODEL = "2001::1";
    public static final IpAddressNoZone IPV6 = new IpAddressNoZone(new Ipv6AddressNoZone(IPV6_MODEL));
    public static final Uint32 MPLS_LABEL2_MODEL = Uint32.valueOf(24002L);
    public static final MplsLabel MPLS_LABEL2 = new MplsLabel(MPLS_LABEL2_MODEL);
    public static final String RD_MODEL = "1.2.3.4:258";
    public static final RouteDistinguisher RD = RouteDistinguisherBuilder.getDefaultInstance(RD_MODEL);

    private EvpnTestUtil() {
        // Hidden on purpose
    }

    public static DataContainerNodeBuilder<NodeIdentifier, ContainerNode> createContBuilder(final NodeIdentifier nid) {
        return Builders.containerBuilder().withNodeIdentifier(nid);
    }

    public static <T> LeafNode<T> createValue(final T value, final NodeIdentifier nid) {
        return ImmutableNodes.leafNode(nid, value);
    }
}
