/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.Iterator;

import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.AsPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.AtomicAggregate;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.ClusterId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.Origin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.OriginatorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpOrigin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.ClusterIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeWithValue;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;

public class AttributeOperationsTest {

    static final NodeIdentifier ORIGIN_NID = new NodeIdentifier(QName.cachedReference(QName.create(BestPathSelectorTest.ATTRS_EXTENSION_Q, Origin.QNAME.getLocalName())));
    static final NodeIdentifier ORIGIN_VALUE_NID = new NodeIdentifier(QName.cachedReference(QName.create(BestPathSelectorTest.ATTRS_EXTENSION_Q, "value")));
    static final NodeIdentifier AS_PATH_NID = new NodeIdentifier(QName.cachedReference(QName.create(BestPathSelectorTest.ATTRS_EXTENSION_Q, AsPath.QNAME.getLocalName())));
    static final NodeIdentifier ATOMIC_NID = new NodeIdentifier(QName.create(BestPathSelectorTest.ATTRS_EXTENSION_Q, AtomicAggregate.QNAME.getLocalName()));
    static final NodeIdentifier CLUSTER_C_NID = new NodeIdentifier(QName.create(BestPathSelectorTest.ATTRS_EXTENSION_Q, ClusterId.QNAME.getLocalName()));
    static final NodeIdentifier CLUSTER_NID = new NodeIdentifier(QName.create(BestPathSelectorTest.ATTRS_EXTENSION_Q, "cluster"));
    static final NodeIdentifier ORIGINATOR_C_NID = new NodeIdentifier(QName.create(BestPathSelectorTest.ATTRS_EXTENSION_Q, OriginatorId.QNAME.getLocalName()));
    static final NodeIdentifier ORIGINATOR_NID = new NodeIdentifier(QName.create(BestPathSelectorTest.ATTRS_EXTENSION_Q, "originator"));


    @Test
    public void testExportedAttributesSetFirst() {
        final Long ourAs = 72L;
        final ContainerNode attributesSetBefore = Builders.containerBuilder().withNodeIdentifier(new NodeIdentifier(BestPathSelectorTest.ATTRS_EXTENSION_Q))
            .addChild(Builders.containerBuilder().withNodeIdentifier(AS_PATH_NID)
                .addChild(Builders.unkeyedListBuilder().withNodeIdentifier(BestPathSelectorTest.SEGMENTS_NID)
                    .addChild(BestPathSelectorTest.SET_SEGMENT)
                    .addChild(BestPathSelectorTest.LIST_SEGMENT)
                    .build())
            .build())
            .build();
        final AttributeOperations operations  = AttributeOperations.getInstance(attributesSetBefore);
        final ContainerNode exportedAttributes = operations.exportedAttributes(attributesSetBefore, ourAs);

        // make sure our AS is prepended to the list (as the AS-PATH starts with AS-SET)
        final UnkeyedListEntryNode as = checkAsList(exportedAttributes).getValue().iterator().next();
        assertTrue(as.getChild(BestPathSelectorTest.AS_NID).isPresent());
        assertEquals(ourAs, as.getChild(BestPathSelectorTest.AS_NID).get().getValue());
    }

    @Test
    public void testExportedAttributesListFirst() {
        final Long ourAs = 72L;
        final ContainerNode attributesListBefore = Builders.containerBuilder().withNodeIdentifier(new NodeIdentifier(BestPathSelectorTest.ATTRS_EXTENSION_Q))
            .addChild(Builders.containerBuilder().withNodeIdentifier(AS_PATH_NID)
                .addChild(Builders.unkeyedListBuilder().withNodeIdentifier(BestPathSelectorTest.SEGMENTS_NID)
                    .addChild(BestPathSelectorTest.LIST_SEGMENT)
                    .addChild(BestPathSelectorTest.SET_SEGMENT)
                    .build())
            .build())
            .build();
        final AttributeOperations operations  = AttributeOperations.getInstance(attributesListBefore);
        final ContainerNode exportedAttributes = operations.exportedAttributes(attributesListBefore, ourAs);

        // make sure our AS is appended to the a-list (as the AS-PATH starts with A-LIST)
        final Iterator<UnkeyedListEntryNode> as = checkAsList(exportedAttributes).getValue().iterator();

        final UnkeyedListEntryNode a1 = as.next();
        assertTrue(a1.getChild(BestPathSelectorTest.AS_NID).isPresent());
        assertEquals(ourAs, a1.getChild(BestPathSelectorTest.AS_NID).get().getValue());

        final UnkeyedListEntryNode a2 = as.next();
        assertTrue(a2.getChild(BestPathSelectorTest.AS_NID).isPresent());
        assertEquals(new Long(1), a2.getChild(BestPathSelectorTest.AS_NID).get().getValue());

        final UnkeyedListEntryNode a3 = as.next();
        assertTrue(a3.getChild(BestPathSelectorTest.AS_NID).isPresent());
        assertEquals(new Long(2), a3.getChild(BestPathSelectorTest.AS_NID).get().getValue());

        final UnkeyedListEntryNode a4 = as.next();
        assertTrue(a4.getChild(BestPathSelectorTest.AS_NID).isPresent());
        assertEquals(new Long(3), a4.getChild(BestPathSelectorTest.AS_NID).get().getValue());
    }

    @Test
    public void testExportedAttributesEmptyWithTransitive() {
        final Long ourAs = 72L;
        final ContainerNode attributesSetBefore = Builders.containerBuilder().withNodeIdentifier(new NodeIdentifier(BestPathSelectorTest.ATTRS_EXTENSION_Q))
            .addChild(Builders.containerBuilder().withNodeIdentifier(ORIGIN_NID)
                .addChild(Builders.leafBuilder().withNodeIdentifier(ORIGIN_VALUE_NID).withValue(BgpOrigin.Egp).build())
            .build())
            .addChild(Builders.containerBuilder().withNodeIdentifier(AS_PATH_NID)
                .addChild(Builders.unkeyedListBuilder().withNodeIdentifier(BestPathSelectorTest.SEGMENTS_NID).build())
            .build())
            .addChild(Builders.containerBuilder().withNodeIdentifier(ATOMIC_NID).build())
            .build();
        final AttributeOperations operations  = AttributeOperations.getInstance(attributesSetBefore);
        final ContainerNode exportedAttributes = operations.exportedAttributes(attributesSetBefore, ourAs);

        // Origin should be within exportedAttributes as it is Transitive
        assertTrue(exportedAttributes.getChild(ORIGIN_NID).isPresent());

        // AS-PATH should also be there with our AS
        final Collection<UnkeyedListEntryNode> asList = checkAsList(exportedAttributes).getValue();
        assertEquals(1, asList.size());
        final UnkeyedListEntryNode as = asList.iterator().next();
        assertTrue(as.getChild(BestPathSelectorTest.AS_NID).isPresent());
        assertEquals(ourAs, as.getChild(BestPathSelectorTest.AS_NID).get().getValue());

        // Atomic Aggregate should be filtered out
        assertFalse(exportedAttributes.getChild(ATOMIC_NID).isPresent());
    }

    private UnkeyedListNode checkAsList(final ContainerNode exportedAttributes) {
        assertTrue(NormalizedNodes.findNode(exportedAttributes, AS_PATH_NID, BestPathSelectorTest.SEGMENTS_NID).isPresent());
        final UnkeyedListNode segments = (UnkeyedListNode) NormalizedNodes.findNode(exportedAttributes, AS_PATH_NID, BestPathSelectorTest.SEGMENTS_NID).get();
        final UnkeyedListEntryNode seg = segments.getValue().iterator().next();
        assertTrue(NormalizedNodes.findNode(seg, BestPathSelectorTest.C_SEGMENTS_NID, BestPathSelectorTest.A_LIST_NID, BestPathSelectorTest.AS_SEQ_NID).isPresent());
        final UnkeyedListNode list = (UnkeyedListNode) NormalizedNodes.findNode(seg, BestPathSelectorTest.C_SEGMENTS_NID, BestPathSelectorTest.A_LIST_NID, BestPathSelectorTest.AS_SEQ_NID).get();
        return list;
    }

    @Test
    public void testReflectedAttributesOriginatorAndClusterNotPresent() {
        final Ipv4Address originatorId = new Ipv4Address("127.0.0.2");
        final ClusterIdentifier clusterId = new ClusterIdentifier("10.10.10.10");
        final ContainerNode attributes = Builders.containerBuilder().withNodeIdentifier(new NodeIdentifier(BestPathSelectorTest.ATTRS_EXTENSION_Q))
            .addChild(Builders.containerBuilder().withNodeIdentifier(ORIGIN_NID)
                .addChild(Builders.leafBuilder().withNodeIdentifier(ORIGIN_VALUE_NID).withValue(BgpOrigin.Egp).build())
            .build())
            .build();
        final AttributeOperations operations  = AttributeOperations.getInstance(attributes);
        final ContainerNode reflectedAttributes = operations.reflectedAttributes(attributes, originatorId, clusterId);

        // Origin should be within reflectedAttributes as part of original attributes
        assertTrue(reflectedAttributes.getChild(ORIGIN_NID).isPresent());

        // ClusterIdentifier should be prepended
        final Collection<?> clusters = checkCluster(reflectedAttributes).getValue();
        assertEquals(1, clusters.size());
        assertEquals(clusterId, ((LeafSetEntryNode<?>)clusters.iterator().next()).getValue());

        // OriginatorId should be added
        assertTrue(reflectedAttributes.getChild(ORIGINATOR_C_NID).isPresent());
        assertEquals(originatorId.getValue(), ((ContainerNode)reflectedAttributes.getChild(ORIGINATOR_C_NID).get()).getChild(ORIGINATOR_NID).get().getValue());
    }

    @Test
    public void testReflectedAttributesOriginatorAndClusterPresent() {
        final Ipv4Address originatorId = new Ipv4Address("127.0.0.2");
        final ClusterIdentifier ourClusterId = new ClusterIdentifier("1.1.1.1");
        final ClusterIdentifier clusterId1 = new ClusterIdentifier("10.10.10.10");
        final ClusterIdentifier clusterId2 = new ClusterIdentifier("11.11.11.11");
        final ContainerNode attributes = Builders.containerBuilder().withNodeIdentifier(new NodeIdentifier(BestPathSelectorTest.ATTRS_EXTENSION_Q))
            .addChild(Builders.containerBuilder().withNodeIdentifier(ORIGINATOR_C_NID)
                .addChild(Builders.leafBuilder().withNodeIdentifier(ORIGINATOR_NID).withValue("127.0.0.2").build())
            .build())
            .addChild(Builders.containerBuilder().withNodeIdentifier(CLUSTER_C_NID)
                .addChild(Builders.orderedLeafSetBuilder().withNodeIdentifier(CLUSTER_NID)
                    .addChild(Builders.leafSetEntryBuilder().withNodeIdentifier(new NodeWithValue(CLUSTER_NID.getNodeType(), clusterId1)).withValue(clusterId1).build())
                    .addChild(Builders.leafSetEntryBuilder().withNodeIdentifier(new NodeWithValue(CLUSTER_NID.getNodeType(), clusterId2)).withValue(clusterId2).build())
                .build())
            .build())
            .build();
        final AttributeOperations operations  = AttributeOperations.getInstance(attributes);
        final ContainerNode reflectedAttributes = operations.reflectedAttributes(attributes, originatorId, ourClusterId);

        // ClusterIdentifier should be prepended and other entries should be preserved
        final Collection<?> clusters = checkCluster(reflectedAttributes).getValue();
        assertEquals(3, clusters.size());
        final Iterator<?> cl = clusters.iterator();

        final LeafSetEntryNode<?> c1 = (LeafSetEntryNode<?>) cl.next();
        assertEquals(ourClusterId, c1.getValue());

        final LeafSetEntryNode<?> c2 = (LeafSetEntryNode<?>) cl.next();
        assertEquals(clusterId1, c2.getValue());

        final LeafSetEntryNode<?> c3 = (LeafSetEntryNode<?>) cl.next();
        assertEquals(clusterId2, c3.getValue());

        // OriginatorId should be the same
        assertTrue(reflectedAttributes.getChild(ORIGINATOR_C_NID).isPresent());
        assertEquals(originatorId.getValue(), ((ContainerNode)reflectedAttributes.getChild(ORIGINATOR_C_NID).get()).getChild(ORIGINATOR_NID).get().getValue());
    }

    private LeafSetNode<?> checkCluster(final ContainerNode reflectedAttributes) {
        assertTrue(reflectedAttributes.getChild(CLUSTER_C_NID).isPresent());
        final ContainerNode clusterContainer = (ContainerNode) reflectedAttributes.getChild(CLUSTER_C_NID).get();
        final LeafSetNode<?> clusters = (LeafSetNode<?>) clusterContainer.getChild(CLUSTER_NID).get();
        return clusters;
    }
}
