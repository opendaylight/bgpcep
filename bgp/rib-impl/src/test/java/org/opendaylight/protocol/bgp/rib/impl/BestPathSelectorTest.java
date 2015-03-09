/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import com.google.common.primitives.UnsignedInteger;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.AsPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.LocalPref;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.MultiExitDisc;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.Origin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerId;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListNode;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.CollectionNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeAttrBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeSchemaAwareBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableLeafNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableUnkeyedListEntryNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableUnkeyedListNodeBuilder;

public class BestPathSelectorTest {

    private final UnsignedInteger ROUTER_ID = RouterIds.routerIdForAddress("127.0.0.1");
    private final UnsignedInteger ROUTER_ID2 = RouterIds.routerIdForPeerId(new PeerId("bgp://127.0.0.1"));
    private final UnsignedInteger ROUTER_ID3 = RouterIds.routerIdForPeerId(new PeerId("bgp://127.0.0.2"));
    private final BestPathState STATE = new BestPathState(createStateFromPrefMedOriginASPath());

    @Test
    public void testBestPathForEquality() {
        final BestPath originBestPath = new BestPath(ROUTER_ID, STATE);
        final BestPathSelector selector = new BestPathSelector(20L);
        selector.processPath(ROUTER_ID2, createStateFromPrefMedOriginASPath());
        final BestPath processedPath = selector.result();

        assertEquals(originBestPath.getRouterId(), processedPath.getRouterId());
        assertEquals(originBestPath.getState().getAsPathLength(), processedPath.getState().getAsPathLength());
        assertEquals(originBestPath.getState().getLocalPref(), processedPath.getState().getLocalPref());
        assertEquals(originBestPath.getState().getMultiExitDisc(), processedPath.getState().getMultiExitDisc());
        assertEquals(originBestPath.getState().getOrigin(), processedPath.getState().getOrigin());
        assertEquals(originBestPath.getState().getPeerAs(), processedPath.getState().getPeerAs());
    }

    @Test
    public void testBestPathForNonEquality() {
        final BestPath originBestPath = new BestPath(ROUTER_ID, STATE);
        final BestPathSelector selector = new BestPathSelector(20L);
        selector.processPath(ROUTER_ID3, createStateFromPrefMedOrigin());
        final BestPath processedPath = selector.result();

        assertNotEquals(originBestPath.getRouterId(), processedPath.getRouterId());
        assertEquals(originBestPath.getState().getAsPathLength(), processedPath.getState().getAsPathLength());
        assertNotEquals(originBestPath.getState().getLocalPref(), processedPath.getState().getLocalPref());
        assertNotEquals(originBestPath.getState().getMultiExitDisc(), processedPath.getState().getMultiExitDisc());
        assertNotEquals(originBestPath.getState().getOrigin(), processedPath.getState().getOrigin());
        assertEquals(0L, (long)processedPath.getState().getPeerAs());
    }

    private ContainerNode createStateFromPrefMedOrigin() {
        final DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> dataContBuilder = ImmutableContainerNodeSchemaAwareBuilder.create();
        dataContBuilder.withNodeIdentifier(new NodeIdentifier(QName.create("data")));
        // local pref
        final DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> localPrefContBuilder = ImmutableContainerNodeSchemaAwareBuilder.create();
        localPrefContBuilder.withNodeIdentifier(new NodeIdentifier(LocalPref.QNAME));

        final ImmutableLeafNodeBuilder<Long> prefBuilder = new ImmutableLeafNodeBuilder<>();
        prefBuilder.withNodeIdentifier(new NodeIdentifier(QName.create(LocalPref.QNAME, "pref")))
            .withValue(123L);

        localPrefContBuilder.addChild(prefBuilder.build());
        dataContBuilder.addChild(localPrefContBuilder.build());
        // multi exit disc
        final DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> medContBuilder = ImmutableContainerNodeSchemaAwareBuilder.create();
        medContBuilder.withNodeIdentifier(new NodeIdentifier(MultiExitDisc.QNAME));

        final ImmutableLeafNodeBuilder<Long> medBuilder = new ImmutableLeafNodeBuilder<>();
        medBuilder.withNodeIdentifier(new NodeIdentifier(QName.create(LocalPref.QNAME, "med")))
            .withValue(1234L);

        medContBuilder.addChild(medBuilder.build());
        dataContBuilder.addChild(medContBuilder.build());
        // origin
        final DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> originContBuilder = ImmutableContainerNodeSchemaAwareBuilder.create();
        originContBuilder.withNodeIdentifier(new NodeIdentifier(Origin.QNAME));

        final ImmutableLeafNodeBuilder<String> valueBuilder = new ImmutableLeafNodeBuilder<>();
        valueBuilder.withNodeIdentifier(new NodeIdentifier(QName.create(Origin.QNAME, "value")))
            .withValue("egp");

        originContBuilder.addChild(valueBuilder.build());
        dataContBuilder.addChild(originContBuilder.build());

        return dataContBuilder.build();
    }

    private ContainerNode createStateFromPrefMedOriginASPath() {
        final DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> dataContBuilder = ImmutableContainerNodeSchemaAwareBuilder.create();
        dataContBuilder.withNodeIdentifier(new NodeIdentifier(QName.create("data")));
        // local pref
        final DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> localPrefContBuilder = ImmutableContainerNodeSchemaAwareBuilder.create();
        localPrefContBuilder.withNodeIdentifier(new NodeIdentifier(LocalPref.QNAME));

        final ImmutableLeafNodeBuilder<Long> prefBuilder = new ImmutableLeafNodeBuilder<>();
        prefBuilder.withNodeIdentifier(new NodeIdentifier(QName.create(LocalPref.QNAME, "pref")))
            .withValue(321L);

        localPrefContBuilder.addChild(prefBuilder.build());
        dataContBuilder.addChild(localPrefContBuilder.build());
        // multi exit disc
        final DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> medContBuilder = ImmutableContainerNodeSchemaAwareBuilder.create();
        medContBuilder.withNodeIdentifier(new NodeIdentifier(MultiExitDisc.QNAME));

        final ImmutableLeafNodeBuilder<Long> medBuilder = new ImmutableLeafNodeBuilder<>();
        medBuilder.withNodeIdentifier(new NodeIdentifier(QName.create(LocalPref.QNAME, "med")))
            .withValue(4321L);

        medContBuilder.addChild(medBuilder.build());
        dataContBuilder.addChild(medContBuilder.build());
        // origin
        final DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> originContBuilder = ImmutableContainerNodeSchemaAwareBuilder.create();
        originContBuilder.withNodeIdentifier(new NodeIdentifier(Origin.QNAME));

        final ImmutableLeafNodeBuilder<String> valueBuilder = new ImmutableLeafNodeBuilder<>();
        valueBuilder.withNodeIdentifier(new NodeIdentifier(QName.create(Origin.QNAME, "value")))
            .withValue("igp");

        originContBuilder.addChild(valueBuilder.build());
        dataContBuilder.addChild(originContBuilder.build());
        // as path
        final DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> asPathContBuilder = ImmutableContainerNodeSchemaAwareBuilder.create();
        asPathContBuilder.withNodeIdentifier(new NodeIdentifier(AsPath.QNAME));

        final CollectionNodeBuilder<UnkeyedListEntryNode, UnkeyedListNode> segments = ImmutableUnkeyedListNodeBuilder.create();
        segments.withNodeIdentifier(new NodeIdentifier(AsPath.QNAME));
        final DataContainerNodeAttrBuilder<NodeIdentifier, UnkeyedListEntryNode> segmentBuilder = ImmutableUnkeyedListEntryNodeBuilder.create();
        segmentBuilder.withNodeIdentifier(new NodeIdentifier(AsPath.QNAME));
        final ImmutableLeafNodeBuilder<Long> segmentLeaf = new ImmutableLeafNodeBuilder<>();
        segmentLeaf.withNodeIdentifier(new NodeIdentifier(QName.create(AsPath.QNAME, "segments")))
            .withValue(123454L);
        segmentBuilder.addChild(segmentLeaf.build());
        segments.addChild(segmentBuilder.build());

        asPathContBuilder.addChild(segments.build());
        dataContBuilder.addChild(asPathContBuilder.build());

        return dataContBuilder.build();
    }
}
