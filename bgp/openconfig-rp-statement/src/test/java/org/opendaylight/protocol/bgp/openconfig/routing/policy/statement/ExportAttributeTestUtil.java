/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.openconfig.routing.policy.statement;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.as.path.Segments;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.ClusterIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeWithValue;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.CollectionNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeAttrBuilder;

public final class ExportAttributeTestUtil {
    public static final QName ATTRS_EXTENSION_Q = QName.create("urn:opendaylight:params:xml:ns:yang:bgp-inet",
            "2017-12-07", "attributes");
    public static final QName AS_NUMBER_Q = QName.create(ATTRS_EXTENSION_Q, "as-number");
    public static final NodeIdentifier SEGMENTS_NID = new NodeIdentifier(QName.create(ATTRS_EXTENSION_Q,
            Segments.QNAME.getLocalName()));
    public static final NodeIdentifier SEQ_LEAFLIST_NID = new NodeIdentifier(QName.create(ATTRS_EXTENSION_Q,
            "as-sequence"));
    public static final UnkeyedListEntryNode SEQ_SEGMENT = Builders.unkeyedListEntryBuilder()
            .withNodeIdentifier(SEGMENTS_NID).addChild(Builders.orderedLeafSetBuilder()
                    .withNodeIdentifier(SEQ_LEAFLIST_NID)
                    .addChild(Builders.leafSetEntryBuilder()
                            .withNodeIdentifier(new NodeWithValue<>(AS_NUMBER_Q, 1L)).withValue(1L).build())
                    .addChild(Builders.leafSetEntryBuilder()
                            .withNodeIdentifier(new NodeWithValue<>(AS_NUMBER_Q, 2L)).withValue(2L).build())
                    .addChild(Builders.leafSetEntryBuilder()
                            .withNodeIdentifier(new NodeWithValue<>(AS_NUMBER_Q, 3L)).withValue(3L).build())
                    .build()).build();
    public static final long LOCAL_AS = 8;
    public static final Ipv4Address IPV4 = new Ipv4Address("1.2.3.4");
    public static final ClusterIdentifier CLUSTER = new ClusterIdentifier(IPV4);
    private static final NodeIdentifier SET_LEAFLIST_NID = new NodeIdentifier(QName.create(ATTRS_EXTENSION_Q,
            "as-set"));
    public static final UnkeyedListEntryNode SET_SEGMENT = Builders.unkeyedListEntryBuilder()
            .withNodeIdentifier(SEGMENTS_NID).addChild(Builders.leafSetBuilder().withNodeIdentifier(SET_LEAFLIST_NID)
                    .addChild(Builders.leafSetEntryBuilder()
                            .withNodeIdentifier(new NodeWithValue<>(AS_NUMBER_Q, 10L)).withValue(10L).build())
                    .addChild(Builders.leafSetEntryBuilder()
                            .withNodeIdentifier(new NodeWithValue<>(AS_NUMBER_Q, 11L)).withValue(11L).build())
                    .build()).build();

    private ExportAttributeTestUtil() {
        throw new UnsupportedOperationException();
    }

    /**
     * container cluster-id. {
     * leaf-list cluster {
     * type cluster-identifier;
     * }
     * uses cluster-id;
     * container originator-id {
     * leaf originator {
     * type ipv4-address;
     * }
     * uses originator-id;
     * }
     */
    public static ContainerNode createInputWithOriginator() {
        final DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> b = Builders.containerBuilder();
        b.withNodeIdentifier(new NodeIdentifier(QName.create(ATTRS_EXTENSION_Q, "attribute-container")
                .intern()));
        b.withChild(createClusterId());
        b.withChild(createOriginatorId());
        return b.build();
    }

    public static ContainerNode createInternalOutput() {
        final DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> b = Builders.containerBuilder();
        b.withNodeIdentifier(new NodeIdentifier(QName.create(ATTRS_EXTENSION_Q, "attribute-container")
                .intern()));
        b.withChild(createWithoutInternalClusterId());
        return b.build();
    }

    public static ContainerNode createClusterInput() {
        final DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> b = Builders.containerBuilder();
        b.withNodeIdentifier(new NodeIdentifier(QName.create(ATTRS_EXTENSION_Q, "attribute-container")
                .intern()));
        b.withChild(createClusterIdInput());
        return b.build();
    }

    private static ContainerNode createWithoutInternalClusterId() {
        final QName clusterContQName = QName.create(ATTRS_EXTENSION_Q, "cluster-id").intern();
        final QName clusterQName = QName.create(ATTRS_EXTENSION_Q, "cluster").intern();
        final NodeIdentifier clusterContNid = new NodeIdentifier(clusterContQName);
        final NodeIdentifier clusterNid = new NodeIdentifier(clusterQName);

        final ClusterIdentifier cluster1 = new ClusterIdentifier(new Ipv4Address("1.1.1.1"));
        final ClusterIdentifier cluster2 = new ClusterIdentifier(new Ipv4Address("1.1.1.2"));
        return Builders.containerBuilder().withNodeIdentifier(clusterContNid)
                .addChild(Builders.orderedLeafSetBuilder().withNodeIdentifier(clusterNid)
                        .withChild(Builders.leafSetEntryBuilder()
                                .withNodeIdentifier(new NodeWithValue<>(clusterQName, cluster2.getValue()))
                                .withValue(cluster2.getValue()).build())
                        .withChild(Builders.leafSetEntryBuilder()
                                .withNodeIdentifier(new NodeWithValue<>(clusterQName, cluster1.getValue()))
                                .withValue(cluster1.getValue()).build()).build()).build();
    }

    private static ContainerNode createClusterId() {
        final QName clusterContQName = QName.create(ATTRS_EXTENSION_Q, "cluster-id").intern();
        final QName clusterQName = QName.create(ATTRS_EXTENSION_Q, "cluster").intern();
        final NodeIdentifier clusterContNid = new NodeIdentifier(clusterContQName);
        final NodeIdentifier clusterNid = new NodeIdentifier(clusterQName);

        final ClusterIdentifier cluster1 = new ClusterIdentifier(new Ipv4Address("1.1.1.1"));
        final ClusterIdentifier cluster2 = new ClusterIdentifier(new Ipv4Address("1.1.1.2"));
        return Builders.containerBuilder().withNodeIdentifier(clusterContNid).addChild(
                Builders.orderedLeafSetBuilder().withNodeIdentifier(clusterNid)
                        .withChild(Builders.leafSetEntryBuilder()
                                .withNodeIdentifier(new NodeWithValue<>(clusterQName, CLUSTER.getValue()))
                                .withValue(CLUSTER.getValue()).build())
                        .withChild(Builders.leafSetEntryBuilder()
                                .withNodeIdentifier(new NodeWithValue<>(clusterQName, cluster2.getValue()))
                                .withValue(cluster2.getValue()).build())
                        .withChild(Builders.leafSetEntryBuilder()
                                .withNodeIdentifier(new NodeWithValue<>(clusterQName, cluster1.getValue()))
                                .withValue(cluster1.getValue()).build())
                        .build()).build();
    }

    private static ContainerNode createClusterIdInput() {
        final QName clusterContQName = QName.create(ATTRS_EXTENSION_Q, "cluster-id").intern();
        final QName clusterQName = QName.create(ATTRS_EXTENSION_Q, "cluster").intern();
        final NodeIdentifier clusterContNid = new NodeIdentifier(clusterContQName);
        final NodeIdentifier clusterNid = new NodeIdentifier(clusterQName);

        final ClusterIdentifier cluster1 = new ClusterIdentifier(new Ipv4Address("1.1.1.1"));
        final ClusterIdentifier cluster2 = new ClusterIdentifier(new Ipv4Address("1.1.1.2"));
        return Builders.containerBuilder().withNodeIdentifier(clusterContNid).addChild(
                Builders.orderedLeafSetBuilder().withNodeIdentifier(clusterNid)
                        .withChild(Builders.leafSetEntryBuilder()
                                .withNodeIdentifier(new NodeWithValue<>(clusterQName, cluster2.getValue()))
                                .withValue(cluster2.getValue()).build())
                        .withChild(Builders.leafSetEntryBuilder()
                                .withNodeIdentifier(new NodeWithValue<>(clusterQName, cluster1.getValue()))
                                .withValue(cluster1.getValue()).build())
                        .build()).build();
    }

    private static ContainerNode createOriginatorId() {
        final QName originatorContQName = QName.create(ATTRS_EXTENSION_Q, "originator-id").intern();
        final QName originatorQName = QName.create(ATTRS_EXTENSION_Q, "originator").intern();
        final NodeIdentifier originatorContNid = new NodeIdentifier(originatorContQName);
        final NodeIdentifier originatorNid = new NodeIdentifier(originatorQName);

        return Builders.containerBuilder().withNodeIdentifier(originatorContNid).withChild(
                ImmutableNodes.leafNode(originatorNid, CLUSTER.getValue())).build();
    }

    /**
     * AsPath.
     */
    static ContainerNode createPathInputWithAs() {
        return createPathInput(createSequenceWithLocalAs());
    }

    static ContainerNode createPathInput(final UnkeyedListEntryNode child) {
        final CollectionNodeBuilder<UnkeyedListEntryNode, UnkeyedListNode> segB = Builders.unkeyedListBuilder();
        segB.withNodeIdentifier(SEGMENTS_NID);
        if (child != null) {
            segB.addChild(child);
        }
        segB.addChild(SET_SEGMENT).addChild(SEQ_SEGMENT);
        return Builders.containerBuilder().withNodeIdentifier(new NodeIdentifier(ATTRS_EXTENSION_Q))
                .addChild(Builders.containerBuilder()
                        .withNodeIdentifier(new NodeIdentifier(QName.create(ATTRS_EXTENSION_Q, "as-path")
                                .intern())).addChild(segB.build()).build()).build();
    }

    private static UnkeyedListEntryNode createSequenceWithLocalAs() {
        return Builders.unkeyedListEntryBuilder().withNodeIdentifier(SEGMENTS_NID)
                .addChild(Builders.orderedLeafSetBuilder().withNodeIdentifier(SEQ_LEAFLIST_NID)
                        .addChild(Builders.leafSetEntryBuilder()
                                .withNodeIdentifier(new NodeWithValue<>(AS_NUMBER_Q, LOCAL_AS))
                                .withValue(LOCAL_AS).build()).build()).build();
    }
}
