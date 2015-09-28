/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerRole;
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

public class ExportPolicyTest {

    private static final Ipv4Address IPV4 = new Ipv4Address("1.2.3.4");
    private static final ClusterIdentifier CLUSTER = new ClusterIdentifier(IPV4);

    private static final ToReflectorClientExportPolicy REF_POLICY = new ToReflectorClientExportPolicy(IPV4, CLUSTER);
    private static final long LOCAL_AS = 8;
    private static final ToExternalExportPolicy EXT_POLICY = new ToExternalExportPolicy(LOCAL_AS);
    private static final ToInternalExportPolicy INT_POLICY = new ToInternalExportPolicy(IPV4, CLUSTER);
    private static final NodeIdentifier SEQ_LEAFLIST_NID = new NodeIdentifier(QName.create(BestPathSelectorTest.extensionQName, "as-sequence"));
    private static final NodeIdentifier C_SEGMENT_NID = new NodeIdentifier(QName.create(BestPathSelectorTest.extensionQName, "c-segment"));
    private static final NodeIdentifier A_List = new NodeIdentifier(QName.create(BestPathSelectorTest.extensionQName, "a-list"));

    @Test
    public void testEbgpEffectiveAttributes() {
        final ContainerNode clusterIn = createClusterInput();
        final PeerRole peerRole = PeerRole.Ebgp;
        assertEquals(clusterIn, REF_POLICY.effectiveAttributes(peerRole, clusterIn));
        assertEquals(clusterIn, INT_POLICY.effectiveAttributes(peerRole, clusterIn));

        final ContainerNode asPathIn = createPathInput(null);
        assertEquals(createPathInputWithAs(), EXT_POLICY.effectiveAttributes(peerRole, asPathIn));
    }

    @Test
    public void testIbgpEffectiveAttributes() {
        final ContainerNode clusterIn = createClusterInput();
        final PeerRole peerRole = PeerRole.Ibgp;
        assertEquals(createInputWithOriginator(), REF_POLICY.effectiveAttributes(peerRole, clusterIn));
        assertEquals(null, INT_POLICY.effectiveAttributes(peerRole, clusterIn));

        final ContainerNode asPathIn = createPathInput(null);
        assertEquals(createPathInputWithAs(), EXT_POLICY.effectiveAttributes(peerRole, asPathIn));
    }

    @Test
    public void testRrClientEffectiveAttributes() {
        final ContainerNode clusterIn = createClusterInput();
        final PeerRole peerRole = PeerRole.RrClient;
        assertEquals(createInputWithOriginator(), REF_POLICY.effectiveAttributes(peerRole, clusterIn));
        assertEquals(createInputWithOriginator(), INT_POLICY.effectiveAttributes(peerRole, clusterIn));

        final ContainerNode asPathIn = createPathInput(null);
        assertEquals(createPathInputWithAs(), EXT_POLICY.effectiveAttributes(peerRole, asPathIn));
    }

    @Test
    public void testInternalPeerEffectiveAttributes() {
        final ContainerNode clusterIn = createClusterInput();
        final PeerRole peerRole = PeerRole.Internal;
        assertEquals(createInternalOutput(), REF_POLICY.effectiveAttributes(peerRole, clusterIn));
        assertEquals(createInternalOutput(), INT_POLICY.effectiveAttributes(peerRole, clusterIn));

        final ContainerNode asPathIn = createPathInput(null);
        assertEquals(createPathInputWithAs(), EXT_POLICY.effectiveAttributes(peerRole, asPathIn));
    }

    /**
     * container cluster-id {
     *     leaf-list cluster {
     *         type cluster-identifier;
     *     }
     *     uses cluster-id;
     * }
     *
     * container originator-id {
     *     leaf originator {
     *         type ipv4-address;
     *     }
     *     uses originator-id;
     * }
     */
    private static ContainerNode createInputWithOriginator() {
        final DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> b = Builders.containerBuilder();
        b.withNodeIdentifier(new NodeIdentifier(QName.cachedReference(QName.create(BestPathSelectorTest.extensionQName, "attribute-container"))));
        b.withChild(createClusterId());
        b.withChild(createOriginatorId());
        return b.build();
    }

    private static ContainerNode createInternalOutput() {
        final DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> b = Builders.containerBuilder();
        b.withNodeIdentifier(new NodeIdentifier(QName.cachedReference(QName.create(BestPathSelectorTest.extensionQName, "attribute-container"))));
        b.withChild(createWithoutInternalClusterId());
        return b.build();
    }

    private static ContainerNode createClusterInput() {
        final DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> b = Builders.containerBuilder();
        b.withNodeIdentifier(new NodeIdentifier(QName.cachedReference(QName.create(BestPathSelectorTest.extensionQName, "attribute-container"))));
        b.withChild(createClusterIdInput());
        return b.build();
    }

    private static ContainerNode createWithoutInternalClusterId() {
        final QName clusterContQName = QName.cachedReference(QName.create(BestPathSelectorTest.extensionQName, "cluster-id"));
        final QName clusterQName = QName.cachedReference(QName.create(BestPathSelectorTest.extensionQName, "cluster"));
        final NodeIdentifier clusterContNid = new NodeIdentifier(clusterContQName);
        final NodeIdentifier clusterNid = new NodeIdentifier(clusterQName);

        final ClusterIdentifier cluster1 = new ClusterIdentifier(new Ipv4Address("1.1.1.1"));
        final ClusterIdentifier cluster2 = new ClusterIdentifier(new Ipv4Address("1.1.1.2"));
        return Builders.containerBuilder().withNodeIdentifier(clusterContNid).addChild(Builders.orderedLeafSetBuilder().withNodeIdentifier(clusterNid)
                .withChild(Builders.leafSetEntryBuilder().withNodeIdentifier(new NodeWithValue(clusterQName, cluster2.getValue())).withValue(cluster2.getValue()).build())
                .withChild(Builders.leafSetEntryBuilder().withNodeIdentifier(new NodeWithValue(clusterQName, cluster1.getValue())).withValue(cluster1.getValue()).build())
                .build()).build();
    }

    private static ContainerNode createClusterId() {
        final QName clusterContQName = QName.cachedReference(QName.create(BestPathSelectorTest.extensionQName, "cluster-id"));
        final QName clusterQName = QName.cachedReference(QName.create(BestPathSelectorTest.extensionQName, "cluster"));
        final NodeIdentifier clusterContNid = new NodeIdentifier(clusterContQName);
        final NodeIdentifier clusterNid = new NodeIdentifier(clusterQName);

        final ClusterIdentifier cluster1 = new ClusterIdentifier(new Ipv4Address("1.1.1.1"));
        final ClusterIdentifier cluster2 = new ClusterIdentifier(new Ipv4Address("1.1.1.2"));
        return Builders.containerBuilder().withNodeIdentifier(clusterContNid).addChild(
            Builders.orderedLeafSetBuilder().withNodeIdentifier(clusterNid)
                .withChild(Builders.leafSetEntryBuilder().withNodeIdentifier(new NodeWithValue(clusterQName, CLUSTER.getValue())).withValue(CLUSTER.getValue()).build())
                .withChild(Builders.leafSetEntryBuilder().withNodeIdentifier(new NodeWithValue(clusterQName, cluster2.getValue())).withValue(cluster2.getValue()).build())
                .withChild(Builders.leafSetEntryBuilder().withNodeIdentifier(new NodeWithValue(clusterQName, cluster1.getValue())).withValue(cluster1.getValue()).build())
                .build()).build();
    }

    private static ContainerNode createClusterIdInput() {
        final QName clusterContQName = QName.cachedReference(QName.create(BestPathSelectorTest.extensionQName, "cluster-id"));
        final QName clusterQName = QName.cachedReference(QName.create(BestPathSelectorTest.extensionQName, "cluster"));
        final NodeIdentifier clusterContNid = new NodeIdentifier(clusterContQName);
        final NodeIdentifier clusterNid = new NodeIdentifier(clusterQName);

        final ClusterIdentifier cluster1 = new ClusterIdentifier(new Ipv4Address("1.1.1.1"));
        final ClusterIdentifier cluster2 = new ClusterIdentifier(new Ipv4Address("1.1.1.2"));
        return Builders.containerBuilder().withNodeIdentifier(clusterContNid).addChild(
            Builders.orderedLeafSetBuilder().withNodeIdentifier(clusterNid)
            .withChild(Builders.leafSetEntryBuilder().withNodeIdentifier(new NodeWithValue(clusterQName, cluster2.getValue())).withValue(cluster2.getValue()).build())
            .withChild(Builders.leafSetEntryBuilder().withNodeIdentifier(new NodeWithValue(clusterQName, cluster1.getValue())).withValue(cluster1.getValue()).build())
            .build()).build();
    }

    private static ContainerNode createOriginatorId() {
        final QName originatorContQName = QName.cachedReference(QName.create(BestPathSelectorTest.extensionQName, "originator-id"));
        final QName originatorQName = QName.cachedReference(QName.create(BestPathSelectorTest.extensionQName, "originator"));
        final NodeIdentifier originatorContNid = new NodeIdentifier(originatorContQName);
        final NodeIdentifier originatorNid = new NodeIdentifier(originatorQName);

        return Builders.containerBuilder().withNodeIdentifier(originatorContNid).withChild(
            ImmutableNodes.leafNode(originatorNid, CLUSTER.getValue())).build();
    }

    /*
     * AsPath
     */
    private static ContainerNode createPathInputWithAs() {
        return createPathInput(createSequenceWithLocalAs());
    }

    private static ContainerNode createPathInput(final UnkeyedListEntryNode child) {
        final CollectionNodeBuilder<UnkeyedListEntryNode, UnkeyedListNode> segB = Builders.unkeyedListBuilder();
        segB.withNodeIdentifier(BestPathSelectorTest.segmentsNid);
        if (child != null) {
            segB.addChild(child);
        }
        segB.addChild(BestPathSelectorTest.SET_SEGMENT).addChild(BestPathSelectorTest.SEQ_SEGMENT);
        return Builders.containerBuilder().withNodeIdentifier(new NodeIdentifier(BestPathSelectorTest.extensionQName))
            .addChild(Builders.containerBuilder()
                .withNodeIdentifier(new NodeIdentifier(QName.cachedReference(QName.create(BestPathSelectorTest.extensionQName, "as-path"))))
                .addChild(segB.build()).build()).build();
    }

    private static UnkeyedListEntryNode createSequenceWithLocalAs() {
        final UnkeyedListEntryNode asSequence = Builders.unkeyedListEntryBuilder().withNodeIdentifier(SEQ_LEAFLIST_NID)
            .addChild(Builders.leafBuilder().withNodeIdentifier(BestPathSelectorTest.asNid)
                .withValue(LOCAL_AS)
                .build()).build();

        final CollectionNodeBuilder<UnkeyedListEntryNode, UnkeyedListNode> segB = Builders.unkeyedListBuilder();
        segB.withNodeIdentifier(SEQ_LEAFLIST_NID);
        segB.addChild(asSequence);

        return Builders.unkeyedListEntryBuilder().withNodeIdentifier(BestPathSelectorTest.segmentsNid)
            .addChild(Builders.choiceBuilder().withNodeIdentifier(C_SEGMENT_NID)
                .addChild(Builders.containerBuilder()
                    .withNodeIdentifier(A_List)
                    .addChild(segB.build()).build()).build()).build();
    }
}
