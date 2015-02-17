/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.opendaylight.protocol.bgp.linkstate.attribute.sr.SrLinkAttributesParser;
import org.opendaylight.protocol.bgp.linkstate.attribute.sr.SrNodeAttributesParser;
import org.opendaylight.protocol.bgp.linkstate.attribute.sr.SrPrefixAttributesParser;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.link.state.SrAdjId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.link.state.SrAdjIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.link.state.SrLanAdjId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.link.state.SrLanAdjIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.node.state.SrAlgorithm;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.node.state.SrAlgorithmBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.node.state.SrCapabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.node.state.SrCapabilitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.node.state.SrSidLabel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.node.state.SrSidLabelBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.prefix.state.SrPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.prefix.state.SrPrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev150206.AdjacencyFlags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev150206.Algorithm;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev150206.SidLabel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev150206.SidLabelBinding.SidLabelFlags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev150206.SrCapabilities.Flags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev150206.Weight;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev150206.sid.label.binding.SubTlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev150206.sid.label.binding.SubTlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev150206.sid.sub.tlvs.subtlv.type.EroMetricCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev150206.sid.sub.tlvs.subtlv.type.Ip4EroCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev150206.sid.sub.tlvs.subtlv.type.SidLabelCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev150206.sid.sub.tlvs.subtlv.type.UnnumberedEroCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.IsoSystemIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.TeMetric;

public class SrAttributeParserTest {

    @Test
    public void testSidLabelBindingv4() {
        final byte[] bytes = { (byte)0xC0, 24, 0, 42, 8, 127, 1, 4, 1, 2, 3, 4, 2, 4, 0, 0, 0, 8, 3, 5,(byte)0x80, 10, 0, 0, 1, 5, 9, (byte)0x80, 1, 2, 3, 4, 0, 0, 1, (byte)0xF4 };
        final List<SubTlvs> subs = new ArrayList<>();
        subs.add(new SubTlvsBuilder().setSubtlvType(new SidLabelCaseBuilder().setSid(new SidLabel(new byte[] {1,2,3,4})).build()).build());
        subs.add(new SubTlvsBuilder().setSubtlvType(new EroMetricCaseBuilder().setEroMetric(new TeMetric((long) 8)).build()).build());
        subs.add(new SubTlvsBuilder().setSubtlvType(new Ip4EroCaseBuilder().setLoose(true).setAddress(new IpAddress(new Ipv4Address("10.0.0.1"))).build()).build());
        subs.add(new SubTlvsBuilder().setSubtlvType(new UnnumberedEroCaseBuilder().setLoose(true).setRouterId(new byte[] {1,2,3,4}).setInterfaceId((long)500).build()).build());
        final SrSidLabel b4 = new SrSidLabelBuilder().setSidLabelFlags(new SidLabelFlags(Boolean.TRUE, Boolean.TRUE))
            .setWeight(new Weight((short) 24)).setFecPrefix(new IpPrefix(new Ipv4Prefix("127.0.0.0/8"))).setValueRange(42).setSubTlvs(subs).build();
        final ByteBuf b = Unpooled.buffer();
        SrNodeAttributesParser.serializeSidLabelBinding(b4, b);
        assertArrayEquals(bytes, ByteArray.readAllBytes(b));
    }

    @Test
    public void testSidLabelBindingv6() {
        final byte[] bytes = { (byte)0xC0, 24, 0, 42, 8, (byte)0x20 };
        final SrSidLabel b4 = new SrSidLabelBuilder().setSidLabelFlags(new SidLabelFlags(Boolean.TRUE, Boolean.TRUE))
            .setWeight(new Weight((short) 24)).setFecPrefix(new IpPrefix(new Ipv6Prefix("2001::1/8"))).setValueRange(42).build();
        final ByteBuf b = Unpooled.buffer();
        SrNodeAttributesParser.serializeSidLabelBinding(b4, b);
        assertArrayEquals(bytes, ByteArray.readAllBytes(b));
    }

    @Test
    public void testSrAlgorithm() {
        final byte[] bytes = { 0 };
        final SrAlgorithm alg = new SrAlgorithmBuilder().setAlgorithm(Lists.newArrayList(Algorithm.ShortestPathFirst)).build();
        final SrAlgorithm empty = new SrAlgorithmBuilder().setAlgorithm(Collections.<Algorithm>emptyList()).build();
        assertEquals(alg, SrNodeAttributesParser.parseSrAlgorithms(Unpooled.wrappedBuffer(bytes)));
        final ByteBuf b = Unpooled.buffer();
        assertEquals(empty, SrNodeAttributesParser.parseSrAlgorithms(b));
        SrNodeAttributesParser.serializeSrAlgorithms(alg, b);
        assertArrayEquals(bytes, ByteArray.readAllBytes(b));

        SrNodeAttributesParser.serializeSrAlgorithms(empty, b);
        assertEquals(0, b.readableBytes());
    }

    @Test
    public void testSrCapabilities() {
        final byte[] bytes = { (byte)0xC0, 0, 0, 10, 1, 4, 1, 2, 3, 4 };
        final SrCapabilities caps = new SrCapabilitiesBuilder().setFlags(new Flags(Boolean.TRUE, Boolean.TRUE)).setSid(new SidLabel(new byte[] {1,2,3,4})).setValueRange((long) 10).build();
        assertEquals(caps, SrNodeAttributesParser.parseSrCapabilities(Unpooled.wrappedBuffer(bytes)));
        final ByteBuf b = Unpooled.buffer();
        SrNodeAttributesParser.serializeSrCapabilities(caps, b);
        assertArrayEquals(bytes, ByteArray.readAllBytes(b));
    }

    @Test
    public void testSrAdjId() {
        final byte[] tested = { 13, 10, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24 };
        final byte[] sidLabel = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24 };
        final SrAdjId srAdjId = new SrAdjIdBuilder().setFlags(new AdjacencyFlags(Boolean.TRUE, Boolean.FALSE, Boolean.TRUE, Boolean.FALSE, Boolean.TRUE)).setWeight(new Weight((short) 10)).setSid(new SidLabel(sidLabel)).build();
        final ByteBuf serializedData = SrLinkAttributesParser.serializeAdjacencySegmentIdentifier(srAdjId);
        assertEquals(srAdjId, SrLinkAttributesParser.parseAdjacencySegmentIdentifier(Unpooled.wrappedBuffer(tested)));
        assertArrayEquals(tested, ByteArray.readAllBytes(serializedData));
    }

    @Test
    public void testSrLanAdjId() {
        final byte[] tested = { 13, 10, 1, 2, 3, 4, 5, 6, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24 };
        final byte[] sidLabel = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24 };
        final byte[] systemId = { 1, 2, 3, 4, 5, 6 };
        final SrLanAdjId srLanAdjId = new SrLanAdjIdBuilder().setFlags(new AdjacencyFlags(Boolean.TRUE, Boolean.FALSE, Boolean.TRUE, Boolean.FALSE, Boolean.TRUE)).setWeight(new Weight((short)10)).setIsoSystemId(new IsoSystemIdentifier(systemId)).setSid(new SidLabel(sidLabel)).build();
        final ByteBuf serializedData = SrLinkAttributesParser.serializeLanAdjacencySegmentIdentifier(srLanAdjId);
        assertEquals(srLanAdjId, SrLinkAttributesParser.parseLanAdjacencySegmentIdentifier(Unpooled.wrappedBuffer(tested)));
        assertArrayEquals(tested, ByteArray.readAllBytes(serializedData));
    }

    @Test
    public void testSrPrefix() {
        final byte[] tested = { 50, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24 };
        final byte[] sidLabel = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24 };
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev150206.PrefixSid.Flags flags = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev150206.PrefixSid.Flags(Boolean.FALSE, Boolean.TRUE, Boolean.FALSE, Boolean.TRUE, Boolean.FALSE, Boolean.TRUE);
        final SrPrefix srPrefix = new SrPrefixBuilder().setFlags(flags).setAlgorithm(Algorithm.forValue(0)).setSid(new SidLabel(sidLabel)).build();
        final ByteBuf serializedData = SrPrefixAttributesParser.serializeSrPrefix(srPrefix);
        assertEquals(srPrefix, SrPrefixAttributesParser.parseSrPrefix(Unpooled.wrappedBuffer(tested)));
        assertArrayEquals(tested, ByteArray.readAllBytes(serializedData));
    }
}
