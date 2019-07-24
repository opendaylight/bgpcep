/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.mode.impl.base;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import com.google.common.collect.Lists;
import java.util.List;
import org.junit.Test;
import org.opendaylight.protocol.bgp.mode.impl.BestPathStateImpl;
import org.opendaylight.protocol.bgp.rib.spi.RouterId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.AsPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.LocalPref;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.MultiExitDisc;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.Origin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.as.path.Segments;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.BgpOrigin;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeWithValue;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeBuilder;

public class BasePathSelectorTest {
    static final RouterId ROUTER_ID2 = RouterId.forPeerId(new PeerId("bgp://127.0.0.1"));

    private static final QName AS_SEQUENCE = QName.create(Segments.QNAME, "as-sequence");
    private static final List<Uint32> SEQ_SEGMENT = List.of(Uint32.ONE, Uint32.TWO, Uint32.valueOf(3));
    private static final List<Uint32> SEQ_SEGMENT2 = List.of(Uint32.valueOf(20), Uint32.TWO, Uint32.valueOf(3));
    private static final RouterId ROUTER_ID = RouterId.forAddress("127.0.0.1");
    private static final RouterId ROUTER_ID3 = RouterId.forPeerId(new PeerId("bgp://127.0.0.2"));
    private final BasePathSelector selector = new BasePathSelector(20L);
    private final BestPathStateImpl state = new BestPathStateImpl(createStateFromPrefMedOriginASPath().build());
    private final BaseBestPath originBestPath = new BaseBestPath(ROUTER_ID, this.state);

    private static ContainerNode createStateFromPrefMedOrigin() {
        return Builders.containerBuilder()
            .withNodeIdentifier(new NodeIdentifier(Attributes.QNAME))
            .withChild(lowerLocalPref())
            .withChild(lowerMultiExitDisc())
            .withChild(igpOrigin())
            .build();
    }

    protected static DataContainerNodeBuilder<NodeIdentifier, ContainerNode> createStateFromPrefMedOriginASPath() {
        return Builders.containerBuilder()
            .withNodeIdentifier(new NodeIdentifier(Attributes.QNAME))
            .withChild(higherLocalPref())
            .withChild(higherMultiExitDisc())
            .withChild(egpOrigin())
            .withChild(asPath(SEQ_SEGMENT));
    }

    private static ContainerNode lowerLocalPref() {
        return localPref(Uint32.valueOf(123));
    }

    private static ContainerNode higherLocalPref() {
        return localPref(Uint32.valueOf(321));
    }

    private static ContainerNode localPref(final Uint32 pref) {
        return Builders.containerBuilder()
            .withNodeIdentifier(new NodeIdentifier(LocalPref.QNAME))
            .withChild(ImmutableNodes.leafNode(QName.create(LocalPref.QNAME, "pref"), pref))
            .build();
    }

    private static ContainerNode lowerMultiExitDisc() {
        return multiExitDisc(Uint32.valueOf(1234));
    }

    private static ContainerNode higherMultiExitDisc() {
        return multiExitDisc(Uint32.valueOf(4321));
    }

    private static ContainerNode multiExitDisc(final Uint32 med) {
        return Builders.containerBuilder()
            .withNodeIdentifier(new NodeIdentifier(MultiExitDisc.QNAME))
            .withChild(ImmutableNodes.leafNode(QName.create(MultiExitDisc.QNAME, "med"), med))
            .build();
    }

    private static ContainerNode egpOrigin() {
        return origin(BgpOrigin.Egp);
    }

    private static ContainerNode igpOrigin() {
        return origin(BgpOrigin.Igp);
    }

    private static ContainerNode origin(final BgpOrigin value) {
        return Builders.containerBuilder()
            .withNodeIdentifier(new NodeIdentifier(Origin.QNAME))
            .withChild(ImmutableNodes.leafNode(QName.create(Origin.QNAME, "value"), value.getName()))
            .build();
    }

    private static ContainerNode asPath(final List<Uint32> segment) {
        return Builders.containerBuilder()
            .withNodeIdentifier(new NodeIdentifier(AsPath.QNAME))
            .withChild(Builders.unkeyedListBuilder()
                .withNodeIdentifier(new NodeIdentifier(Segments.QNAME))
                .withChild(Builders.unkeyedListEntryBuilder()
                    .withNodeIdentifier(new NodeIdentifier(Segments.QNAME))
                    .withChild(Builders.orderedLeafSetBuilder()
                        .withNodeIdentifier(new NodeIdentifier(AS_SEQUENCE))
                        .withValue(Lists.transform(segment, asn -> Builders.leafSetEntryBuilder()
                            .withNodeIdentifier(new NodeWithValue<>(AS_SEQUENCE, asn))
                            .withValue(asn)
                            .build()))
                        .build())
                    .build())
                .build())
            .build();
    }

    @Test
    public void testBestPathWithHigherLocalPref() {
        // local-pref 123
        this.selector.processPath(ROUTER_ID2, createStateFromPrefMedOrigin());
        BaseBestPath processedPath = this.selector.result();
        assertEquals(Uint32.valueOf(123), processedPath.getState().getLocalPref());

        // local-pref 321
        this.selector.processPath(ROUTER_ID2, createStateFromPrefMedOriginASPath().build());
        processedPath = this.selector.result();
        assertEquals(Uint32.valueOf(321), processedPath.getState().getLocalPref());

        this.selector.processPath(ROUTER_ID2, Builders.containerBuilder()
            .withNodeIdentifier(new NodeIdentifier(Attributes.QNAME))
            // prefer path with higher LOCAL_PREF
            .withChild(lowerLocalPref())
            .build());
        processedPath = this.selector.result();
        assertEquals(Uint32.valueOf(321), processedPath.getState().getLocalPref());
    }

    @Test
    public void testBestPathForEquality() {
        this.selector.processPath(ROUTER_ID2, createStateFromPrefMedOriginASPath().build());
        final BaseBestPath processedPath = this.selector.result();

        assertEquals(this.originBestPath.getPeerId(), processedPath.getPeerId());
        assertEquals(this.originBestPath.getState().getLocalPref(), processedPath.getState().getLocalPref());
        assertEquals(this.originBestPath.getState().getMultiExitDisc(), processedPath.getState().getMultiExitDisc());
        assertEquals(this.originBestPath.getState().getOrigin(), processedPath.getState().getOrigin());
        assertEquals(this.originBestPath.getState().getPeerAs(), processedPath.getState().getPeerAs());
        assertEquals(this.originBestPath.getState().getAsPathLength(), processedPath.getState().getAsPathLength());
    }

    @Test
    public void testBestPathSelectionOptions() {
        DataContainerNodeBuilder<NodeIdentifier, ContainerNode> dataContBuilder = createStateFromPrefMedOriginASPath();
        this.selector.processPath(ROUTER_ID2, dataContBuilder.build());
        BaseBestPath processedPath = this.selector.result();
        assertEquals(BgpOrigin.Egp, processedPath.getState().getOrigin());

        // prefer the path with the lowest origin type
        this.selector.processPath(ROUTER_ID2, dataContBuilder.withChild(igpOrigin()).build());
        processedPath = this.selector.result();
        assertEquals(BgpOrigin.Igp, processedPath.getState().getOrigin());

        this.selector.processPath(ROUTER_ID2, dataContBuilder.withChild(egpOrigin()).build());
        processedPath = this.selector.result();
        assertEquals(BgpOrigin.Igp, processedPath.getState().getOrigin());
        assertEquals(4321L, processedPath.getState().getMultiExitDisc());

        // prefer the path with the lowest multi-exit discriminator (MED)
        this.selector.processPath(ROUTER_ID2, dataContBuilder
            .withChild(igpOrigin())
            .withChild(lowerMultiExitDisc())
            .build());
        processedPath = this.selector.result();
        assertEquals(1234L, processedPath.getState().getMultiExitDisc());

        this.selector.processPath(ROUTER_ID2, dataContBuilder.withChild(higherMultiExitDisc()).build());
        processedPath = this.selector.result();
        assertEquals(1234L, processedPath.getState().getMultiExitDisc());
        assertEquals(1L, processedPath.getState().getPeerAs());
        assertEquals(3, processedPath.getState().getAsPathLength());

        this.selector.processPath(ROUTER_ID2, dataContBuilder
            .withChild(lowerMultiExitDisc())
            .withChild(asPath(SEQ_SEGMENT2))
            .build());
        processedPath = this.selector.result();
        assertEquals(1L, processedPath.getState().getPeerAs());
        assertEquals(3, processedPath.getState().getAsPathLength());
    }

    @Test
    public void testBestPathForNonEquality() {
        this.selector.processPath(ROUTER_ID3, createStateFromPrefMedOrigin());
        final BaseBestPath processedPath = this.selector.result();

        assertNotEquals(this.originBestPath.getPeerId(), processedPath.getPeerId());
        assertNotEquals(this.originBestPath.getState().getLocalPref(), processedPath.getState().getLocalPref());
        assertNotEquals(this.originBestPath.getState().getMultiExitDisc(), processedPath.getState().getMultiExitDisc());
        assertNotEquals(this.originBestPath.getState().getOrigin(), processedPath.getState().getOrigin());
        assertNotEquals(this.originBestPath.getState().getPeerAs(), processedPath.getState().getPeerAs());
        assertNotEquals(this.originBestPath.getState().getAsPathLength(), processedPath.getState().getAsPathLength());
    }

    @Test
    public void testBgpOrigin() {
        this.selector.processPath(ROUTER_ID3, Builders.containerBuilder()
            .withNodeIdentifier(new NodeIdentifier(Attributes.QNAME))
            .withChild(origin(BgpOrigin.Incomplete))
            .build());
        final BaseBestPath processedPathIncom = this.selector.result();
        assertEquals(BgpOrigin.Incomplete, processedPathIncom.getState().getOrigin());
    }
}
