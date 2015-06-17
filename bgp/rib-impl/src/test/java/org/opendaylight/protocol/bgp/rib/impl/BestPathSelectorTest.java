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

import com.google.common.collect.Lists;
import com.google.common.primitives.UnsignedInteger;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.as.path.Segments;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.as.path.SegmentsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.CSegment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.c.segment.AListCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.c.segment.ASetCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.c.segment.a.list._case.AList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.c.segment.a.list._case.AListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.c.segment.a.list._case.a.list.AsSequence;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.c.segment.a.list._case.a.list.AsSequenceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.c.segment.a.set._case.ASet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.c.segment.a.set._case.ASetBuilder;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeWithValue;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.CollectionNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeAttrBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeSchemaAwareBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableLeafNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableUnkeyedListEntryNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableUnkeyedListNodeBuilder;

public class BestPathSelectorTest {

    private final QName extensionQName = QName.create("urn:opendaylight:params:xml:ns:yang:bgp-inet", "2015-03-05", "attributes");
    private final QName localPrefQName = QName.create(this.extensionQName, "local-pref");
    private final QName multiExitDiscQName = QName.create(this.extensionQName, "multi-exit-disc");
    private final QName originQName = QName.create(this.extensionQName, "origin");
    private final QName asPathQName = QName.create(this.extensionQName, "as-path");
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
        this.dataContBuilder = createContBuilder(this.extensionQName);
        // local pref
        this.dataContBuilder.addChild(createContBuilder(this.localPrefQName).addChild(createValueBuilder(123L, this.localPrefQName, "pref").build()).build());
        // multi exit disc
        this.dataContBuilder.addChild(createContBuilder(this.multiExitDiscQName).addChild(createValueBuilder(1234L, this.multiExitDiscQName, "med").build()).build());
        // origin
        this.dataContBuilder.addChild(createContBuilder(this.originQName).addChild(createValueBuilder("igp", this.originQName, "value").build()).build());
        return this.dataContBuilder.build();
    }

    private ContainerNode createStateFromPrefMedOriginASPath() {
        this.dataContBuilder = createContBuilder(this.extensionQName);
        // local pref
        this.dataContBuilder.addChild(createContBuilder(this.localPrefQName).addChild(createValueBuilder(321L, this.localPrefQName, "pref").build()).build());
        // multi exit disc
        this.dataContBuilder.addChild(createContBuilder(this.multiExitDiscQName).addChild(createValueBuilder(4321L, this.multiExitDiscQName, "med").build()).build());
        // origin
        this.dataContBuilder.addChild(createContBuilder(this.originQName).addChild(createValueBuilder("egp", this.originQName, "value").build()).build());
        // as path
        final DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> asPathContBuilder = ImmutableContainerNodeSchemaAwareBuilder.create();
        asPathContBuilder.withNodeIdentifier(new NodeIdentifier(this.asPathQName));

        final CollectionNodeBuilder<UnkeyedListEntryNode, UnkeyedListNode> segments = ImmutableUnkeyedListNodeBuilder.create();
        segments.withNodeIdentifier(new NodeIdentifier(this.asPathQName));
        final DataContainerNodeAttrBuilder<NodeIdentifier, UnkeyedListEntryNode> segmentBuilder = ImmutableUnkeyedListEntryNodeBuilder.create();
        segmentBuilder.withNodeIdentifier(new NodeIdentifier(this.asPathQName));
        final ImmutableLeafNodeBuilder<Long> segmentLeaf = new ImmutableLeafNodeBuilder<>();
        segmentLeaf.withNodeIdentifier(new NodeIdentifier(QName.create(this.asPathQName, "segments"))).withValue(123454L);
        segmentBuilder.addChild(segmentLeaf.build());
        segments.addChild(segmentBuilder.build());

        asPathContBuilder.addChild(segments.build());
        this.dataContBuilder.addChild(asPathContBuilder.build());
        return this.dataContBuilder.build();
    }

    private static DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> createContBuilder(final QName qname) {
        return ImmutableContainerNodeSchemaAwareBuilder.create().withNodeIdentifier(new NodeIdentifier(qname));
    }

    private static <T> ImmutableLeafNodeBuilder<T> createValueBuilder(final T value, final QName qname, final String localName) {
        final ImmutableLeafNodeBuilder<T> valueBuilder = new ImmutableLeafNodeBuilder<>();
        valueBuilder.withNodeIdentifier(new NodeIdentifier(QName.create(qname, localName))).withValue(value);
        return valueBuilder;
    }

    @Test
    public void testExtractSegments() {
        final QName asNumberQ = QName.create(this.extensionQName, "as-number");
        final NodeIdentifier segmentsNid = new NodeIdentifier(QName.create(this.extensionQName, Segments.QNAME.getLocalName()));
        final NodeIdentifier cSegmentsNid = new NodeIdentifier(QName.create(this.extensionQName, CSegment.QNAME.getLocalName()));
        final NodeIdentifier asSetNid = new NodeIdentifier(QName.create(this.extensionQName, "as-set"));
        final NodeIdentifier aSetNid = new NodeIdentifier(QName.create(this.extensionQName, ASet.QNAME.getLocalName()));
        final NodeIdentifier aListNid = new NodeIdentifier(QName.create(this.extensionQName, AList.QNAME.getLocalName()));
        final NodeIdentifier asSeqNid = new NodeIdentifier(QName.create(this.extensionQName, AsSequence.QNAME.getLocalName()));
        final NodeIdentifier asNid = new NodeIdentifier(QName.create(this.extensionQName, "as"));
        // to be extracted from
        final CollectionNodeBuilder<UnkeyedListEntryNode, UnkeyedListNode> builder = Builders.unkeyedListBuilder();
        builder.withNodeIdentifier(segmentsNid);
        builder.addChild(Builders.unkeyedListEntryBuilder().withNodeIdentifier(segmentsNid)
            .addChild(Builders.choiceBuilder().withNodeIdentifier(cSegmentsNid)
                .addChild(Builders.containerBuilder().withNodeIdentifier(aSetNid)
                    .addChild(Builders.leafSetBuilder().withNodeIdentifier(asSetNid)
                        .addChild(Builders.leafSetEntryBuilder().withNodeIdentifier(new NodeWithValue(asNumberQ, "10")).withValue("10").build())
                        .addChild(Builders.leafSetEntryBuilder().withNodeIdentifier(new NodeWithValue(asNumberQ, "11")).withValue("11").build())
                    .build())
                .build())
            .build())
        .build());
        builder.addChild(Builders.unkeyedListEntryBuilder().withNodeIdentifier(segmentsNid)
            .addChild(Builders.choiceBuilder().withNodeIdentifier(cSegmentsNid)
                .addChild(Builders.containerBuilder().withNodeIdentifier(aListNid)
                    .addChild(Builders.unkeyedListBuilder().withNodeIdentifier(asSeqNid)
                        .addChild(Builders.unkeyedListEntryBuilder().withNodeIdentifier(asSeqNid)
                            .addChild(Builders.leafBuilder().withNodeIdentifier(asNid).withValue("1").build())
                        .build())
                        .addChild(Builders.unkeyedListEntryBuilder().withNodeIdentifier(asSeqNid)
                            .addChild(Builders.leafBuilder().withNodeIdentifier(asNid).withValue("2").build())
                        .build())
                        .addChild(Builders.unkeyedListEntryBuilder().withNodeIdentifier(asSeqNid)
                            .addChild(Builders.leafBuilder().withNodeIdentifier(asNid).withValue("3").build())
                        .build())
                    .build())
                .build())
            .build())
        .build());

        // expected
        final List<AsSequence> sequences = new ArrayList<>();
        sequences.add(new AsSequenceBuilder().setAs(new AsNumber(1L)).build());
        sequences.add(new AsSequenceBuilder().setAs(new AsNumber(2L)).build());
        sequences.add(new AsSequenceBuilder().setAs(new AsNumber(3L)).build());
        final List<Segments> expected = new ArrayList<>();
        expected.add(new SegmentsBuilder().setCSegment(new ASetCaseBuilder().setASet(new ASetBuilder().setAsSet(Lists.newArrayList(new AsNumber(11L), new AsNumber(10L))).build()).build()).build());
        expected.add(new SegmentsBuilder().setCSegment(new AListCaseBuilder().setAList(new AListBuilder().setAsSequence(sequences).build()).build()).build());
        // test
        assertEquals(expected, this.STATE.extractSegments(builder.build()));
    }
}
