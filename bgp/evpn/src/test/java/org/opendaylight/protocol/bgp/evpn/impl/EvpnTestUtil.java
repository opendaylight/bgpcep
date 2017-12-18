/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.evpn.impl;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.RouteDistinguisher;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.RouteDistinguisherBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.MplsLabel;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeAttrBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeSchemaAwareBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableLeafNodeBuilder;

public final class EvpnTestUtil {
    public static final int VALUE_SIZE = 9;
    public static final long LD = 33686018;
    public static final String MAC_MODEL = "f2:0c:dd:80:9f:f7";
    public static final MacAddress MAC = new MacAddress(MAC_MODEL);
    public static final long AS_MODEL = 16843009;
    public static final AsNumber AS_NUMBER = new AsNumber(AS_MODEL);
    public static final Integer PORT = 514;
    public static final MplsLabel MPLS_LABEL = new MplsLabel(24001L);
    public static final int COMMUNITY_VALUE_SIZE = 6;
    public static final long VLAN = 10L;
    public static final String IP_MODEL = "127.0.0.1";
    public static final IpAddress IP = new IpAddress(new Ipv4Address(IP_MODEL));
    public static final String IPV6_MODEL = "2001::1";
    public static final IpAddress IPV6 = new IpAddress(new Ipv6Address(IPV6_MODEL));
    public static final long MPLS_LABEL_MODEL = 24001L;
    public static final MplsLabel MPLS_LABEL2 = new MplsLabel(24002L);
    public static final long MPLS_LABEL2_MODEL = 24002L;
    public static final String RD_MODEL = "1.2.3.4:258";
    public static final RouteDistinguisher RD = RouteDistinguisherBuilder.getDefaultInstance(RD_MODEL);

    public static DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> createContBuilder(
            final NodeIdentifier nid) {
        return ImmutableContainerNodeSchemaAwareBuilder.create().withNodeIdentifier(nid);
    }

    public static <T> ImmutableLeafNodeBuilder<T> createValueBuilder(final T value, NodeIdentifier nid) {
        final ImmutableLeafNodeBuilder<T> valueBuilder = new ImmutableLeafNodeBuilder<>();
        valueBuilder.withNodeIdentifier(nid).withValue(value);
        return valueBuilder;
    }
}
