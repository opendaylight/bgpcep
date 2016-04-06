/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.evpn.impl;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.MplsLabel;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
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

    public static DataContainerNodeAttrBuilder<YangInstanceIdentifier.NodeIdentifier, ContainerNode> createContBuilder(final YangInstanceIdentifier
        .NodeIdentifier nid) {
        return ImmutableContainerNodeSchemaAwareBuilder.create().withNodeIdentifier(nid);
    }

    public static <T> ImmutableLeafNodeBuilder<T> createValueBuilder(final T value, YangInstanceIdentifier.NodeIdentifier nid) {
        final ImmutableLeafNodeBuilder<T> valueBuilder = new ImmutableLeafNodeBuilder<>();
        valueBuilder.withNodeIdentifier(nid).withValue(value);
        return valueBuilder;
    }
}
