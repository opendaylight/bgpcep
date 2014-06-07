/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static org.junit.Assert.assertTrue;

import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.PathAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.AsPathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.LocalPrefBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.MultiExitDiscBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.OriginBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.as.path.Segments;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.as.path.SegmentsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.PathAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpOrigin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.ClusterIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.c.segment.AListCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.c.segment.ASetCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.c.segment.a.list._case.AListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.c.segment.a.list._case.a.list.AsSequence;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.c.segment.a.list._case.a.list.AsSequenceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.c.segment.a.set._case.ASetBuilder;

/**
 * @see <a href="http://www.cisco.com/c/en/us/support/docs/ip/border-gateway-protocol-bgp/13753-25.html">BGP Best Path
 *      Selection</a>
 */
public class BestPathSelectionTest {

    private final BGPObjectComparator comparator = new BGPObjectComparator(new AsNumber(40L), new Ipv4Address("192.150.20.38"), new Ipv4Address("192.150.20.38"));

    private PathAttributes attr1;
    private PathAttributes attr2;
    private PathAttributes attr3;
    private PathAttributes attr4;
    private PathAttributes attr5;
    private PathAttributes attr6;
    private PathAttributes attr7;

    @Before
    public void setUp() {
        final AsPathBuilder asBuilder1 = new AsPathBuilder();
        final AsPathBuilder asBuilder2 = new AsPathBuilder();
        List<Segments> segs = new ArrayList<>();
        final List<AsNumber> ases = Lists.newArrayList(new AsNumber(100L), new AsNumber(30L));
        final List<AsSequence> seqs = Lists.newArrayList(new AsSequenceBuilder().setAs(new AsNumber(50L)).build());
        segs.add(new SegmentsBuilder().setCSegment(new AListCaseBuilder().setAList(new AListBuilder().setAsSequence(seqs).build()).build()).build());
        asBuilder1.setSegments(segs);
        segs = new ArrayList<>();
        segs.add(new SegmentsBuilder().setCSegment(new AListCaseBuilder().setAList(new AListBuilder().setAsSequence(seqs).build()).build()).build());
        segs.add(new SegmentsBuilder().setCSegment(new ASetCaseBuilder().setASet(new ASetBuilder().setAsSet(ases).build()).build()).build());
        asBuilder2.setSegments(segs);

        final List<ClusterIdentifier> clusters = new ArrayList<>();
        clusters.add(new ClusterIdentifier(new Ipv4Address("0.0.0.0")));
        clusters.add(new ClusterIdentifier(new Ipv4Address("0.0.0.0")));

        final PathAttributesBuilder builder = new PathAttributesBuilder();
        builder.setLocalPref(new LocalPrefBuilder().setPref(100L).build());
        this.attr1 = builder.build();
        builder.setLocalPref(new LocalPrefBuilder().setPref(230L).build());
        builder.setAsPath(asBuilder2.build());
        this.attr2 = builder.build();
        builder.setAsPath(asBuilder1.build());
        builder.setOrigin(new OriginBuilder().setValue(BgpOrigin.Incomplete).build());
        this.attr3 = builder.build();
        builder.setOrigin(new OriginBuilder().setValue(BgpOrigin.Egp).build());
        builder.setMultiExitDisc(new MultiExitDiscBuilder().setMed(15L).build());
        this.attr4 = builder.build();
        builder.setMultiExitDisc(new MultiExitDiscBuilder().setMed(12L).build());
        this.attr5 = builder.build();
        builder.setAsPath(new AsPathBuilder().setSegments(new ArrayList<Segments>()).build());
        builder.setClusterId(new ArrayList<ClusterIdentifier>());
        this.attr6 = builder.build();
        builder.setClusterId(clusters);
        this.attr7 = builder.build();
    }

    @Test
    public void testCompare() {
        assertTrue(this.comparator.compare(this.attr1, this.attr2) < 0);
        assertTrue(this.comparator.compare(this.attr2, this.attr1) > 0);

        assertTrue(this.comparator.compare(this.attr2, this.attr3) < 0);
        assertTrue(this.comparator.compare(this.attr3, this.attr2) > 0);

        assertTrue(this.comparator.compare(this.attr3, this.attr4) < 0);
        assertTrue(this.comparator.compare(this.attr4, this.attr3) > 0);

        assertTrue(this.comparator.compare(this.attr4, this.attr5) < 0);
        assertTrue(this.comparator.compare(this.attr5, this.attr4) > 0);

        assertTrue(this.comparator.compare(this.attr5, this.attr6) < 0);
        assertTrue(this.comparator.compare(this.attr6, this.attr5) > 0);

        assertTrue(this.comparator.compare(this.attr6, this.attr7) < 0);
        assertTrue(this.comparator.compare(this.attr7, this.attr6) > 0);
    }

    @Test
    public void testByteCompare() {
        assertTrue(BGPObjectComparator.compareByteArrays(new byte[] { (byte) 192, (byte) 150, 20, 38 }, new byte[] { (byte) 192,
            (byte) 168, 25, 1 }) < 0);
        assertTrue(BGPObjectComparator.compareByteArrays(new byte[] { (byte) 192, (byte) 168, 25, 1 }, new byte[] { (byte) 192, (byte) 150,
            20, 38 }) > 0);
    }
}
