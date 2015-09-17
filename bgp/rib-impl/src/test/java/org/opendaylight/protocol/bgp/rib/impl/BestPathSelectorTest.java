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
import com.google.common.collect.Sets;
import com.google.common.primitives.UnsignedInteger;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.as.path.Segments;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.as.path.SegmentsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpOrigin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.CSegment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.c.segment.AListCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.c.segment.ASetCase;
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
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableUnkeyedListNodeBuilder;

public class BestPathSelectorTest {

    protected static final QName extensionQName = QName.create("urn:opendaylight:params:xml:ns:yang:bgp-inet", "2015-03-05", "attributes");
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

    protected static final QName asNumberQ = QName.create(extensionQName, "as-number");
    protected static final NodeIdentifier segmentsNid = new NodeIdentifier(QName.create(extensionQName, Segments.QNAME.getLocalName()));
    private static final NodeIdentifier cSegmentsNid = new NodeIdentifier(QName.create(extensionQName, CSegment.QNAME.getLocalName()));
    private static final NodeIdentifier asSetNid = new NodeIdentifier(QName.create(extensionQName, "as-set"));
    private static final NodeIdentifier aSetNid = new NodeIdentifier(QName.create(extensionQName, ASet.QNAME.getLocalName()));
    private static final NodeIdentifier aListNid = new NodeIdentifier(QName.create(extensionQName, AList.QNAME.getLocalName()));
    private static final NodeIdentifier asSeqNid = new NodeIdentifier(QName.create(extensionQName, AsSequence.QNAME.getLocalName()));
    private static final NodeIdentifier asNid = new NodeIdentifier(QName.create(extensionQName, "as"));

    static final UnkeyedListEntryNode SET_SEGMENT = Builders.unkeyedListEntryBuilder().withNodeIdentifier(segmentsNid)
        .addChild(Builders.choiceBuilder().withNodeIdentifier(cSegmentsNid)
            .addChild(Builders.containerBuilder().withNodeIdentifier(aSetNid)
                .addChild(Builders.leafSetBuilder().withNodeIdentifier(asSetNid)
                    .addChild(Builders.leafSetEntryBuilder().withNodeIdentifier(new NodeWithValue(asNumberQ, 10L)).withValue(10L).build())
                    .addChild(Builders.leafSetEntryBuilder().withNodeIdentifier(new NodeWithValue(asNumberQ, 11L)).withValue(11L).build())
                .build())
            .build())
        .build())
    .build();

    static final UnkeyedListEntryNode SEQ_SEGMENT = Builders.unkeyedListEntryBuilder().withNodeIdentifier(segmentsNid)
        .addChild(Builders.choiceBuilder().withNodeIdentifier(cSegmentsNid)
            .addChild(Builders.containerBuilder().withNodeIdentifier(aListNid)
                .addChild(Builders.unkeyedListBuilder().withNodeIdentifier(asSeqNid)
                    .addChild(Builders.unkeyedListEntryBuilder().withNodeIdentifier(asSeqNid)
                        .addChild(Builders.leafBuilder().withNodeIdentifier(asNid).withValue(1L).build())
                    .build())
                    .addChild(Builders.unkeyedListEntryBuilder().withNodeIdentifier(asSeqNid)
                        .addChild(Builders.leafBuilder().withNodeIdentifier(asNid).withValue(2L).build())
                    .build())
                    .addChild(Builders.unkeyedListEntryBuilder().withNodeIdentifier(asSeqNid)
                        .addChild(Builders.leafBuilder().withNodeIdentifier(asNid).withValue(3L).build())
                    .build())
                .build())
            .build())
        .build())
    .build();

    static final UnkeyedListEntryNode SEQ_SEGMENT2 = Builders.unkeyedListEntryBuilder().withNodeIdentifier(segmentsNid)
        .addChild(Builders.choiceBuilder().withNodeIdentifier(cSegmentsNid)
            .addChild(Builders.containerBuilder().withNodeIdentifier(aListNid)
                .addChild(Builders.unkeyedListBuilder().withNodeIdentifier(asSeqNid)
                    .addChild(Builders.unkeyedListEntryBuilder().withNodeIdentifier(asSeqNid)
                        .addChild(Builders.leafBuilder().withNodeIdentifier(asNid).withValue(20L).build())
                    .build())
                    .addChild(Builders.unkeyedListEntryBuilder().withNodeIdentifier(asSeqNid)
                        .addChild(Builders.leafBuilder().withNodeIdentifier(asNid).withValue(2L).build())
                    .build())
                    .addChild(Builders.unkeyedListEntryBuilder().withNodeIdentifier(asSeqNid)
                        .addChild(Builders.leafBuilder().withNodeIdentifier(asNid).withValue(3L).build())
                    .build())
                .build())
            .build())
        .build())
    .build();

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
    public void testBestPathSelectionOptions() {
        this.selector.processPath(this.ROUTER_ID2, createStateFromPrefMedOriginASPath());
        BestPath processedPath = this.selector.result();
        assertEquals(1, processedPath.getState().getOrigin().getIntValue());

        addIgpOrigin(); // prefer the path with the lowest origin type
        this.selector.processPath(this.ROUTER_ID2, this.dataContBuilder.build());
        processedPath = this.selector.result();
        assertEquals(0, processedPath.getState().getOrigin().getIntValue());

        addEgpOrigin();
        this.selector.processPath(this.ROUTER_ID2, this.dataContBuilder.build());
        processedPath = this.selector.result();
        assertEquals(0, processedPath.getState().getOrigin().getIntValue());

        // prefer the path with the lowest multi-exit discriminator (MED)
        assertEquals(4321L, (long) processedPath.getState().getMultiExitDisc());
        addIgpOrigin();
        addLowerMultiExitDisc();
        this.selector.processPath(this.ROUTER_ID2, this.dataContBuilder.build());
        processedPath = this.selector.result();
        assertEquals(1234L, (long) processedPath.getState().getMultiExitDisc());

        addHigherMultiExitDisc();
        this.selector.processPath(this.ROUTER_ID2, this.dataContBuilder.build());
        processedPath = this.selector.result();
        assertEquals(1234L, (long) processedPath.getState().getMultiExitDisc());

        addLowerMultiExitDisc();
        addAsPath(SEQ_SEGMENT2); // change of AS ... prefer eBGP over iBGP paths
        assertEquals(1L, (long) processedPath.getState().getPeerAs());
        assertEquals(3, processedPath.getState().getAsPathLength());
        this.selector.processPath(this.ROUTER_ID2, this.dataContBuilder.build());
        processedPath = this.selector.result();
        assertEquals(1L, (long) processedPath.getState().getPeerAs());
        assertEquals(3, processedPath.getState().getAsPathLength());
    }

    @Test
    public void testBestPathForNonEquality() {
        this.selector.processPath(this.ROUTER_ID3, createStateFromPrefMedOrigin());
        final BestPath processedPath = this.selector.result();

        assertNotEquals(this.originBestPath.getRouterId(), processedPath.getRouterId());
        assertNotEquals(this.originBestPath.getState().getLocalPref(), processedPath.getState().getLocalPref());
        assertNotEquals(this.originBestPath.getState().getMultiExitDisc(), processedPath.getState().getMultiExitDisc());
        assertNotEquals(this.originBestPath.getState().getOrigin(), processedPath.getState().getOrigin());
        assertNotEquals(this.originBestPath.getState().getPeerAs(), processedPath.getState().getPeerAs());
        assertNotEquals(this.originBestPath.getState().getAsPathLength(), processedPath.getState().getAsPathLength());
    }

    private ContainerNode createStateFromPrefMedOrigin() {
        this.dataContBuilder = createContBuilder(this.extensionQName);
        addLowerLocalRef();
        addLowerMultiExitDisc();
        addIgpOrigin();
        return this.dataContBuilder.build();
    }

    private ContainerNode createStateFromPrefMedOriginASPath() {
        this.dataContBuilder = createContBuilder(this.extensionQName);
        addHigherLocalRef();
        addHigherMultiExitDisc();
        addEgpOrigin();
        addAsPath(SEQ_SEGMENT);
        return this.dataContBuilder.build();
    }

    private void addLowerLocalRef() {
        this.dataContBuilder.addChild(createContBuilder(this.localPrefQName).addChild(createValueBuilder(123L, this.localPrefQName, "pref").build()).build());
    }

    private void addHigherLocalRef() {
        this.dataContBuilder.addChild(createContBuilder(this.localPrefQName).addChild(createValueBuilder(321L, this.localPrefQName, "pref").build()).build());
    }

    private void addLowerMultiExitDisc() {
        this.dataContBuilder.addChild(createContBuilder(this.multiExitDiscQName).addChild(createValueBuilder(1234L, this.multiExitDiscQName, "med").build()).build());
    }

    private void addHigherMultiExitDisc() {
        this.dataContBuilder.addChild(createContBuilder(this.multiExitDiscQName).addChild(createValueBuilder(4321L, this.multiExitDiscQName, "med").build()).build());
    }

    private void addIgpOrigin() {
        this.dataContBuilder.addChild(createContBuilder(this.originQName).addChild(createValueBuilder("igp", this.originQName, "value").build()).build());
    }

    private void addEgpOrigin() {
        this.dataContBuilder.addChild(createContBuilder(this.originQName).addChild(createValueBuilder("egp", this.originQName, "value").build()).build());
    }

    private void addAsPath(final UnkeyedListEntryNode segment) {
        final DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> asPathContBuilder = ImmutableContainerNodeSchemaAwareBuilder.create();
        asPathContBuilder.withNodeIdentifier(new NodeIdentifier(this.asPathQName));

        final CollectionNodeBuilder<UnkeyedListEntryNode, UnkeyedListNode> segments = ImmutableUnkeyedListNodeBuilder.create();
        segments.withNodeIdentifier(this.segmentsNid);
        segments.addChild(segment);
        asPathContBuilder.addChild(segments.build());
        this.dataContBuilder.addChild(asPathContBuilder.build());
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
        // to be extracted from
        final CollectionNodeBuilder<UnkeyedListEntryNode, UnkeyedListNode> builder = Builders.unkeyedListBuilder();
        builder.withNodeIdentifier(this.segmentsNid);
        builder.addChild(SET_SEGMENT);
        builder.addChild(SEQ_SEGMENT);

        // expected
        final List<AsSequence> sequences = new ArrayList<>();
        sequences.add(new AsSequenceBuilder().setAs(new AsNumber(1L)).build());
        sequences.add(new AsSequenceBuilder().setAs(new AsNumber(2L)).build());
        sequences.add(new AsSequenceBuilder().setAs(new AsNumber(3L)).build());
        final List<Segments> expected = new ArrayList<>();
        expected.add(new SegmentsBuilder().setCSegment(new ASetCaseBuilder().setASet(new ASetBuilder().setAsSet(Lists.newArrayList(new AsNumber(11L), new AsNumber(10L))).build()).build()).build());
        expected.add(new SegmentsBuilder().setCSegment(new AListCaseBuilder().setAList(new AListBuilder().setAsSequence(sequences).build()).build()).build());
        // test
        final List<Segments> actual = this.STATE.extractSegments(builder.build());
        assertEquals(expected.size(), actual.size());
        assertEquals(Sets.newHashSet(1,2,3), Sets.newHashSet(1,3,2));
        assertEquals(Sets.newHashSet(((ASetCase)expected.get(0).getCSegment()).getASet().getAsSet()), Sets.newHashSet(((ASetCase)actual.get(0).getCSegment()).getASet().getAsSet()));
        assertEquals(expected.get(1), actual.get(1));
    }

    @Test(expected=IllegalArgumentException.class)
    public void testBgpOrigin() {
        final ContainerNode containerIncom = this.dataContBuilder.addChild(createContBuilder(this.originQName).addChild(createValueBuilder("incomplete", this.originQName, "value").build()).build()).build();
        this.selector.processPath(this.ROUTER_ID3, containerIncom);
        final BestPath processedPathIncom = this.selector.result();
        assertEquals(BgpOrigin.Incomplete, processedPathIncom.getState().getOrigin());

        final ContainerNode containerException = this.dataContBuilder.addChild(createContBuilder(this.originQName).addChild(createValueBuilder("LOL", this.originQName, "value").build()).build()).build();
        this.selector.processPath(this.ROUTER_ID3, containerException);
        final BestPath processedPathException = this.selector.result();
        processedPathException.getState().getOrigin();
    }
}
