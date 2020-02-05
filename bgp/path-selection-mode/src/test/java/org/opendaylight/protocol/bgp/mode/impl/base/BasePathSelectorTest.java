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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.opendaylight.protocol.bgp.mode.impl.BestPathStateImpl;
import org.opendaylight.protocol.bgp.rib.spi.RouterId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.AsPathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.LocalPrefBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.MultiExitDiscBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.OriginBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.as.path.SegmentsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.BgpOrigin;
import org.opendaylight.yangtools.yang.common.Uint32;

public class BasePathSelectorTest {
    private static final List<AsNumber> SEQ_SEGMENT = Arrays.asList(
        new AsNumber(Uint32.ONE), new AsNumber(Uint32.TWO), new AsNumber(Uint32.valueOf(3)));
    static final RouterId ROUTER_ID2 = RouterId.forPeerId(new PeerId("bgp://127.0.0.1"));
    private static final List<AsNumber> SEQ_SEGMENT2 = Arrays.asList(
        new AsNumber(Uint32.valueOf(20)), new AsNumber(Uint32.TWO), new AsNumber(Uint32.valueOf(3)));
    private static final RouterId ROUTER_ID = RouterId.forAddress("127.0.0.1");
    private static final RouterId ROUTER_ID3 = RouterId.forPeerId(new PeerId("bgp://127.0.0.2"));
    private final BasePathSelector selector = new BasePathSelector(20L);
    private final BestPathStateImpl state = new BestPathStateImpl(createStateFromPrefMedOriginASPath().build());
    private final BaseBestPath originBestPath = new BaseBestPath(ROUTER_ID, this.state);

    private static Attributes createStateFromPrefMedOrigin() {
        AttributesBuilder dataContBuilder = new AttributesBuilder();
        addLowerLocalRef(dataContBuilder);
        addLowerMultiExitDisc(dataContBuilder);
        addIgpOrigin(dataContBuilder);
        return dataContBuilder.build();
    }

    protected static AttributesBuilder createStateFromPrefMedOriginASPath() {
        AttributesBuilder dataContBuilder = new AttributesBuilder();
        addHigherLocalRef(dataContBuilder);
        addHigherMultiExitDisc(dataContBuilder);
        addEgpOrigin(dataContBuilder);
        addAsPath(dataContBuilder, SEQ_SEGMENT);
        return dataContBuilder;
    }

    private static void addLowerLocalRef(final AttributesBuilder dataContBuilder) {
        dataContBuilder.setLocalPref(new LocalPrefBuilder().setPref(Uint32.valueOf(123)).build());
    }

    private static void addHigherLocalRef(final AttributesBuilder dataContBuilder) {
        dataContBuilder.setLocalPref(new LocalPrefBuilder().setPref(Uint32.valueOf(321)).build());
    }

    private static void addLowerMultiExitDisc(final AttributesBuilder dataContBuilder) {
        dataContBuilder.setMultiExitDisc(new MultiExitDiscBuilder().setMed(Uint32.valueOf(1234)).build());
    }

    private static void addHigherMultiExitDisc(final AttributesBuilder dataContBuilder) {
        dataContBuilder.setMultiExitDisc(new MultiExitDiscBuilder().setMed(Uint32.valueOf(4321)).build());
    }

    private static void addIgpOrigin(final AttributesBuilder dataContBuilder) {
        dataContBuilder.setOrigin(new OriginBuilder().setValue(BgpOrigin.Igp).build());
    }

    private static void addEgpOrigin(final AttributesBuilder dataContBuilder) {
        dataContBuilder.setOrigin(new OriginBuilder().setValue(BgpOrigin.Egp).build());
    }

    private static void addAsPath(final AttributesBuilder dataContBuilder, final List<AsNumber> segment) {
        dataContBuilder.setAsPath(new AsPathBuilder().setSegments(
                Collections.singletonList(new SegmentsBuilder().setAsSequence(segment).build())).build());
    }

    @Test
    public void testBestPathWithHigherLocalPref() {
        this.selector.processPath(ROUTER_ID2, createStateFromPrefMedOrigin());   // local-pref 123
        BaseBestPath processedPath = this.selector.result();
        assertEquals(123L, processedPath.getState().getLocalPref().longValue());

        this.selector.processPath(ROUTER_ID2, createStateFromPrefMedOriginASPath().build());   // local-pref 321
        processedPath = this.selector.result();
        assertEquals(321L, processedPath.getState().getLocalPref().longValue());

        AttributesBuilder dataContBuilder = new AttributesBuilder();
        addLowerLocalRef(dataContBuilder); // prefer path with higher LOCAL_PREF
        this.selector.processPath(ROUTER_ID2, dataContBuilder.build());
        processedPath = this.selector.result();
        assertEquals(321L, processedPath.getState().getLocalPref().longValue());
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
        AttributesBuilder dataContBuilder = createStateFromPrefMedOriginASPath();
        this.selector.processPath(ROUTER_ID2, dataContBuilder.build());
        BaseBestPath processedPath = this.selector.result();
        assertEquals(1, processedPath.getState().getOrigin().getIntValue());

        addIgpOrigin(dataContBuilder); // prefer the path with the lowest origin type
        this.selector.processPath(ROUTER_ID2, dataContBuilder.build());
        processedPath = this.selector.result();
        assertEquals(0, processedPath.getState().getOrigin().getIntValue());

        addEgpOrigin(dataContBuilder);
        this.selector.processPath(ROUTER_ID2, dataContBuilder.build());
        processedPath = this.selector.result();
        assertEquals(0, processedPath.getState().getOrigin().getIntValue());

        // prefer the path with the lowest multi-exit discriminator (MED)
        assertEquals(4321L, processedPath.getState().getMultiExitDisc());
        addIgpOrigin(dataContBuilder);
        addLowerMultiExitDisc(dataContBuilder);
        this.selector.processPath(ROUTER_ID2, dataContBuilder.build());
        processedPath = this.selector.result();
        assertEquals(1234L, processedPath.getState().getMultiExitDisc());

        addHigherMultiExitDisc(dataContBuilder);
        this.selector.processPath(ROUTER_ID2, dataContBuilder.build());
        processedPath = this.selector.result();
        assertEquals(1234L, processedPath.getState().getMultiExitDisc());

        addLowerMultiExitDisc(dataContBuilder);
        addAsPath(dataContBuilder, SEQ_SEGMENT2);
        assertEquals(1L, processedPath.getState().getPeerAs());
        assertEquals(3, processedPath.getState().getAsPathLength());
        this.selector.processPath(ROUTER_ID2, dataContBuilder.build());
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
        this.selector.processPath(ROUTER_ID3, new AttributesBuilder().setOrigin(new OriginBuilder()
                .setValue(BgpOrigin.Incomplete).build()).build());
        final BaseBestPath processedPathIncom = this.selector.result();
        assertEquals(BgpOrigin.Incomplete, processedPathIncom.getState().getOrigin());
    }
}
