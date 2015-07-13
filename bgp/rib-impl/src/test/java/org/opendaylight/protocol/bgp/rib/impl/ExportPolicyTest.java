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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.ClusterId;
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

    private final Ipv4Address ipv4 = new Ipv4Address("1.2.3.4");
    private final ClusterIdentifier cluster = new ClusterIdentifier(this.ipv4);

    private final ToReflectorClientExportPolicy refPolicy = new ToReflectorClientExportPolicy(this.ipv4, this.cluster);
    private final long localAs = 8;
    private final ToExternalExportPolicy extPolicy = new ToExternalExportPolicy(this.localAs);
    private final ToInternalExportPolicy intPolicy = new ToInternalExportPolicy(this.ipv4, this.cluster);

    @Test
    public void testEbgpEffectiveAttributes() {
        final ContainerNode clusterIn = createClusterInput();
        final PeerRole peerRole = PeerRole.Ebgp;
        assertEquals(clusterIn, this.refPolicy.effectiveAttributes(peerRole, clusterIn));
        assertEquals(clusterIn, this.intPolicy.effectiveAttributes(peerRole, clusterIn));

        final ContainerNode asPathIn = createPathInput(null);
        assertEquals(createPathInputWithAs(), this.extPolicy.effectiveAttributes(peerRole, asPathIn));
    }

    @Test
    public void testIbgpEffectiveAttributes() {
        final ContainerNode clusterIn = createClusterInput();
        final PeerRole peerRole = PeerRole.Ibgp;
        assertEquals(createInputWithOriginator(), this.refPolicy.effectiveAttributes(peerRole, clusterIn));
        assertEquals(null, this.intPolicy.effectiveAttributes(peerRole, clusterIn));

        final ContainerNode asPathIn = createPathInput(null);
        assertEquals(createPathInputWithAs(), this.extPolicy.effectiveAttributes(peerRole, asPathIn));
    }

    @Test
    public void testRrClientEffectiveAttributes() {
        final ContainerNode clusterIn = createClusterInput();
        final PeerRole peerRole = PeerRole.RrClient;
        assertEquals(createInputWithOriginator(), this.refPolicy.effectiveAttributes(peerRole, clusterIn));
        assertEquals(createInputWithOriginator(), this.intPolicy.effectiveAttributes(peerRole, clusterIn));

        final ContainerNode asPathIn = createPathInput(null);
        assertEquals(createPathInputWithAs(), this.extPolicy.effectiveAttributes(peerRole, asPathIn));
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
    private ContainerNode createInputWithOriginator() {
        final DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> b = Builders.containerBuilder();
        b.withNodeIdentifier(new NodeIdentifier(QName.cachedReference(QName.create(BestPathSelectorTest.ATTRS_EXTENSION_Q, "attribute-container"))));
        b.withChild(createClusterId());
        b.withChild(createOriginatorId());
        return b.build();
    }

    private ContainerNode createClusterInput() {
        final DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> b = Builders.containerBuilder();
        b.withNodeIdentifier(new NodeIdentifier(QName.cachedReference(QName.create(BestPathSelectorTest.ATTRS_EXTENSION_Q, "attribute-container"))));
        b.withChild(createClusterId());
        return b.build();
    }

    private ContainerNode createClusterId() {
        final QName clusterContQName = QName.cachedReference(QName.create(BestPathSelectorTest.ATTRS_EXTENSION_Q, "cluster-id"));
        final QName clusterQName = QName.cachedReference(QName.create(BestPathSelectorTest.ATTRS_EXTENSION_Q, "cluster"));
        final NodeIdentifier clusterContNid = new NodeIdentifier(clusterContQName);
        final NodeIdentifier clusterNid = new NodeIdentifier(clusterQName);

        final ClusterIdentifier cluster1 = new ClusterIdentifier(new Ipv4Address("1.1.1.1"));
        final ClusterIdentifier cluster2 = new ClusterIdentifier(new Ipv4Address("1.1.1.2"));
        return Builders.containerBuilder().withNodeIdentifier(clusterContNid).addChild(
            Builders.orderedLeafSetBuilder().withNodeIdentifier(clusterNid)
            .withChild(Builders.leafSetEntryBuilder().withNodeIdentifier(new NodeWithValue(ClusterId.QNAME, this.cluster)).withValue(this.cluster).build())
            .withChild(Builders.leafSetEntryBuilder().withNodeIdentifier(new NodeWithValue(ClusterId.QNAME, cluster2)).withValue(cluster2).build())
            .withChild(Builders.leafSetEntryBuilder().withNodeIdentifier(new NodeWithValue(ClusterId.QNAME, cluster1)).withValue(cluster1).build())
            .build()).build();
    }

    private ContainerNode createOriginatorId() {
        final QName originatorContQName = QName.cachedReference(QName.create(BestPathSelectorTest.ATTRS_EXTENSION_Q, "originator-id"));
        final QName originatorQName = QName.cachedReference(QName.create(BestPathSelectorTest.ATTRS_EXTENSION_Q, "originator"));
        final NodeIdentifier originatorContNid = new NodeIdentifier(originatorContQName);
        final NodeIdentifier originatorNid = new NodeIdentifier(originatorQName);

        return Builders.containerBuilder().withNodeIdentifier(originatorContNid).withChild(
            ImmutableNodes.leafNode(originatorNid, this.cluster.getValue())).build();
    }

    /*
     * AsPath
     */
    private ContainerNode createPathInputWithAs() {
        return createPathInput(createSequenceWithLocalAs());
    }

    private ContainerNode createPathInput(final UnkeyedListEntryNode child) {
        final CollectionNodeBuilder<UnkeyedListEntryNode, UnkeyedListNode> segB = Builders.unkeyedListBuilder();
        segB.withNodeIdentifier(BestPathSelectorTest.SEGMENTS_NID);
        if (child != null) {
            segB.addChild(child);
        }
        segB.addChild(BestPathSelectorTest.SET_SEGMENT).addChild(BestPathSelectorTest.SEQ_SEGMENT);
        return Builders.containerBuilder().withNodeIdentifier(new NodeIdentifier(BestPathSelectorTest.ATTRS_EXTENSION_Q))
            .addChild(Builders.containerBuilder().withNodeIdentifier(new NodeIdentifier(QName.cachedReference(QName.create(BestPathSelectorTest.ATTRS_EXTENSION_Q, "as-path"))))
                .addChild(segB.build()).build()).build();
    }

    private UnkeyedListEntryNode createSequenceWithLocalAs() {
        return Builders.unkeyedListEntryBuilder().withNodeIdentifier(BestPathSelectorTest.SEGMENTS_NID)
            .addChild(Builders.orderedLeafSetBuilder().withNodeIdentifier(BestPathSelectorTest.SEQ_LEAFLIST_NID)
                .addChild(Builders.leafSetEntryBuilder().withNodeIdentifier(new NodeWithValue(BestPathSelectorTest.AS_NUMBER_Q, this.localAs)).withValue(this.localAs).build())
                .build()).build();
    }
}
