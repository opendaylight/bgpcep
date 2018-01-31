/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.openconfig.routing.policy.statement;

import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeWithValue;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetEntryNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeAttrBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeSchemaAwareBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableLeafNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableLeafSetEntryNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableLeafSetNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableUnkeyedListNodeBuilder;

public final class ImportAttributeTestUtil {
    private static final QName DATA_QNAME = QName.create("urn:opendaylight:params:xml:ns:yang:bgp-inet",
            "2013-09-19", "attributes").intern();
    private static final QName LOCALPREF = QName.create("urn:opendaylight:params:xml:ns:yang:bgp-inet",
            "2013-09-19", "local-pref").intern();
    private static final QName CLUSTERID = QName.create("urn:opendaylight:params:xml:ns:yang:bgp-inet",
            "2013-09-19", "cluster-id").intern();
    private static final QName CLUSTER = QName.create("urn:opendaylight:params:xml:ns:yang:bgp-inet",
            "2013-09-19", "cluster").intern();
    private static final QName ASPATH = QName.create("urn:opendaylight:params:xml:ns:yang:bgp-inet",
            "2013-09-19", "as-path").intern();
    private static final QName SEGMENT = QName.create("urn:opendaylight:params:xml:ns:yang:bgp-inet",
            "2013-09-19", "segments").intern();
    private static final QName UNRECOGNIZED = QName.create("urn:opendaylight:params:xml:ns:yang:bgp-inet",
            "2013-09-19", "unrecognized-attributes");
    private static final QName NEXTHOP = QName.create("urn:opendaylight:params:xml:ns:yang:bgp-inet",
            "2013-09-19", "c-next-hop").intern();
    private static final QName IPV4NH = QName.create("urn:opendaylight:params:xml:ns:yang:bgp-inet",
            "2013-09-19", "ipv4-next-hop").intern();
    private static final QName ORIGINATOR = QName.create("urn:opendaylight:params:xml:ns:yang:bgp-inet",
            "2013-09-19", "originator-id").intern();
    private static final QName MED = QName.create("urn:opendaylight:params:xml:ns:yang:bgp-inet",
            "2013-09-19", "multi-exit-disc").intern();
    private static final QName ORIGIN = QName.create("urn:opendaylight:params:xml:ns:yang:bgp-inet",
            "2013-09-19", "origin").intern();

    private static final ContainerNode AS_PATH = createContBuilder(ASPATH)
            .addChild(ImmutableUnkeyedListNodeBuilder.create()
                    .withNodeIdentifier(new NodeIdentifier(SEGMENT)).build()).build();

    private ImportAttributeTestUtil() {
        throw new UnsupportedOperationException();
    }

    public static ContainerNode createInput() {
        DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> dataContBuilder = createContBuilder(DATA_QNAME);
        // local pref
        dataContBuilder.addChild(createLocalPref());

        // cluster pref
        dataContBuilder.addChild(createClusterPref("404.40.40.40"));

        // as-path pref
        dataContBuilder.addChild(AS_PATH);

        // unrecognized
        dataContBuilder.addChild(ImmutableNodes.mapNodeBuilder(UNRECOGNIZED).build());

        // c-next-hop pref
        dataContBuilder.addChild(createNexHop());

        // originator pref
        dataContBuilder.addChild(createContBuilder(ORIGINATOR)
                .addChild(createValueBuilder("41.41.41.41", ORIGINATOR, "originator").build()).build());

        // origin pref
        dataContBuilder.addChild(createOrigin());

        // multi-exit-disc pref
        dataContBuilder.addChild(createContBuilder(MED)
                .addChild(createValueBuilder("0", MED, "med").build()).build());
        return dataContBuilder.build();
    }

    private static ContainerNode createClusterPref(final String cluster) {
        LeafSetEntryNode<Object> entry1 = ImmutableLeafSetEntryNodeBuilder.create().withNodeIdentifier(
                new NodeWithValue<>(CLUSTER, cluster)).withValue(cluster).build();
        return createContBuilder(CLUSTERID).addChild(ImmutableLeafSetNodeBuilder.create().withNodeIdentifier(
                new NodeIdentifier(QName.create(CLUSTER, "cluster"))).withChild(entry1).build()).build();
    }

    private static ContainerNode createLocalPref() {
        return createContBuilder(LOCALPREF).addChild(createValueBuilder(100L, LOCALPREF, "pref")
                .build()).build();
    }


    public static ContainerNode createOutput() {
        DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> dataContExpected = createContBuilder(DATA_QNAME);

        dataContExpected.addChild(AS_PATH);
        dataContExpected.addChild(createNexHop());
        dataContExpected.addChild(createOrigin());

        return dataContExpected.build();
    }

    private static ContainerNode createOrigin() {
        return createContBuilder(ORIGIN).addChild(createValueBuilder("igp", ORIGIN, "value")
                .build()).build();
    }

    /**
     * c-next-hop pref.
     */
    private static ChoiceNode createNexHop() {
        final ContainerNode cNextHop = createContBuilder(IPV4NH)
                .addChild(createValueBuilder("199.20.160.41", IPV4NH, "global").build()).build();
        final DataContainerNodeBuilder<NodeIdentifier, ChoiceNode> nextHop = Builders.choiceBuilder();
        nextHop.withNodeIdentifier(new NodeIdentifier(NEXTHOP));
        return nextHop.addChild(cNextHop).build();
    }

    private static DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> createContBuilder(final QName qname) {
        return ImmutableContainerNodeSchemaAwareBuilder.create().withNodeIdentifier(new NodeIdentifier(qname));
    }

    private static <T> ImmutableLeafNodeBuilder<T> createValueBuilder(
            final T value, final QName qname, final String localName) {
        final ImmutableLeafNodeBuilder<T> valueBuilder = new ImmutableLeafNodeBuilder<>();
        valueBuilder.withNodeIdentifier(new NodeIdentifier(QName.create(qname, localName))).withValue(value);
        return valueBuilder;
    }
}
