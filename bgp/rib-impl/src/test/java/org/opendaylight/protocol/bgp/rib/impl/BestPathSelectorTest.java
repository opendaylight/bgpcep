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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.AsPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.LocalPref;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.MultiExitDisc;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.Origin;
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

    private final QName DATA_QNAME = QName.create("data");
    private final UnsignedInteger ROUTER_ID = RouterIds.routerIdForAddress("127.0.0.1");
    private final UnsignedInteger ROUTER_ID2 = RouterIds.routerIdForPeerId(new PeerId("bgp://127.0.0.1"));
    private final UnsignedInteger ROUTER_ID3 = RouterIds.routerIdForPeerId(new PeerId("bgp://127.0.0.2"));
    private final BestPathState STATE = new BestPathState(createStateFromPrefMedOriginASPath());
    private final BestPath originBestPath = new BestPath(this.ROUTER_ID, this.STATE);
    private final BestPathSelector selector = new BestPathSelector(20L);
    private DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> dataContBuilder;

    @Test
    public void testBestPathForEquality() {
        this.selector.processPath(this.ROUTER_ID2, createStateFromPrefMedOriginASPath());
        final BestPath processedPath = this.selector.result();

        assertEquals(this.originBestPath.getRouterId(), processedPath.getRouterId());
        assertEquals(this.originBestPath.getState().getAsPathLength(), processedPath.getState().getAsPathLength());
        assertEquals(this.originBestPath.getState().getLocalPref(), processedPath.getState().getLocalPref());
        assertEquals(this.originBestPath.getState().getMultiExitDisc(), processedPath.getState().getMultiExitDisc());
        assertEquals(this.originBestPath.getState().getOrigin(), processedPath.getState().getOrigin());
        assertEquals(this.originBestPath.getState().getPeerAs(), processedPath.getState().getPeerAs());
    }

    @Test
    public void testBestPathWithHigherLocalPref() {
        this.selector.processPath(this.ROUTER_ID2, createStateFromPrefMedOrigin());   // local-pref 123
        BestPath processedPath = this.selector.result();
        assertEquals(123L, processedPath.getState().getLocalPref().longValue());

        this.selector.processPath(this.ROUTER_ID2, createStateFromPrefMedOriginASPath());   // local-pref 321
        processedPath = this.selector.result();
        assertEquals(321L, processedPath.getState().getLocalPref().longValue());

        this.selector.processPath(this.ROUTER_ID2, createStateFromPrefMedOrigin());   // local-pref 123
        processedPath = this.selector.result();
        assertEquals(321L, processedPath.getState().getLocalPref().longValue());
    }

    @Test
    public void testBestPathForNonEquality() {
        this.selector.processPath(this.ROUTER_ID3, createStateFromPrefMedOrigin());
        final BestPath processedPath = this.selector.result();

        assertNotEquals(this.originBestPath.getRouterId(), processedPath.getRouterId());
        assertEquals(this.originBestPath.getState().getAsPathLength(), processedPath.getState().getAsPathLength());
        assertNotEquals(this.originBestPath.getState().getLocalPref(), processedPath.getState().getLocalPref());
        assertNotEquals(this.originBestPath.getState().getMultiExitDisc(), processedPath.getState().getMultiExitDisc());
        assertNotEquals(this.originBestPath.getState().getOrigin(), processedPath.getState().getOrigin());
        assertEquals(0L, (long)processedPath.getState().getPeerAs());
    }

    private ContainerNode createStateFromPrefMedOrigin() {
        this.dataContBuilder = createContBuilder(this.DATA_QNAME);
        // local pref
        this.dataContBuilder.addChild(createContBuilder(LocalPref.QNAME).addChild(createValueBuilder(123L, LocalPref.QNAME, "pref").build()).build());
        // multi exit disc
        this.dataContBuilder.addChild(createContBuilder(MultiExitDisc.QNAME).addChild(createValueBuilder(1234L, MultiExitDisc.QNAME, "med").build()).build());
        // origin
        this.dataContBuilder.addChild(createContBuilder(Origin.QNAME).addChild(createValueBuilder("igp", Origin.QNAME, "value").build()).build());
        return this.dataContBuilder.build();
    }

    private ContainerNode createStateFromPrefMedOriginASPath() {
        this.dataContBuilder = createContBuilder(this.DATA_QNAME);
        // local pref
        this.dataContBuilder.addChild(createContBuilder(LocalPref.QNAME).addChild(createValueBuilder(321L, LocalPref.QNAME, "pref").build()).build());
        // multi exit disc
        this.dataContBuilder.addChild(createContBuilder(MultiExitDisc.QNAME).addChild(createValueBuilder(4321L, MultiExitDisc.QNAME, "med").build()).build());
        // origin
        this.dataContBuilder.addChild(createContBuilder(Origin.QNAME).addChild(createValueBuilder("egp", Origin.QNAME, "value").build()).build());
        // as path
        final DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> asPathContBuilder = ImmutableContainerNodeSchemaAwareBuilder.create();
        asPathContBuilder.withNodeIdentifier(new NodeIdentifier(AsPath.QNAME));

        final CollectionNodeBuilder<UnkeyedListEntryNode, UnkeyedListNode> segments = ImmutableUnkeyedListNodeBuilder.create();
        segments.withNodeIdentifier(new NodeIdentifier(AsPath.QNAME));
        final DataContainerNodeAttrBuilder<NodeIdentifier, UnkeyedListEntryNode> segmentBuilder = ImmutableUnkeyedListEntryNodeBuilder.create();
        segmentBuilder.withNodeIdentifier(new NodeIdentifier(AsPath.QNAME));
        final ImmutableLeafNodeBuilder<Long> segmentLeaf = new ImmutableLeafNodeBuilder<>();
        segmentLeaf.withNodeIdentifier(new NodeIdentifier(QName.create(AsPath.QNAME, "segments"))).withValue(123454L);
        segmentBuilder.addChild(segmentLeaf.build());
        segments.addChild(segmentBuilder.build());

        asPathContBuilder.addChild(segments.build());
        this.dataContBuilder.addChild(asPathContBuilder.build());
        return this.dataContBuilder.build();
    }

    private DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> createContBuilder(final QName qname) {
        return ImmutableContainerNodeSchemaAwareBuilder.create().withNodeIdentifier(new NodeIdentifier(qname));
    }

    private <T> ImmutableLeafNodeBuilder<T> createValueBuilder(final T value, final QName qname, final String localName) {
        final ImmutableLeafNodeBuilder<T> valueBuilder = new ImmutableLeafNodeBuilder<>();
        valueBuilder.withNodeIdentifier(new NodeIdentifier(QName.create(qname, localName))).withValue(value);
        return valueBuilder;
    }
}
