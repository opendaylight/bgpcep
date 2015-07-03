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
import com.google.common.primitives.Bytes;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Address;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev150206.sid.sub.tlvs.subtlv.type.Ipv4EroBackupCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev150206.sid.sub.tlvs.subtlv.type.Ipv4EroCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev150206.sid.sub.tlvs.subtlv.type.Ipv6EroBackupCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev150206.sid.sub.tlvs.subtlv.type.Ipv6EroCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev150206.sid.sub.tlvs.subtlv.type.SidLabelCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev150206.sid.sub.tlvs.subtlv.type.UnnumberedEroBackupCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev150206.sid.sub.tlvs.subtlv.type.UnnumberedEroCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.IsoSystemIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.TeMetric;

public class SrAttributeParserTest {

    private static final byte[] ipv6aBytes = { 0x20, 1, 0x0d, (byte) 0xb8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1 };
    private static final Ipv6Address ipv6a = new Ipv6Address("2001:db8::1");
    private static final byte[] ipv6bBytes = { 0x20, 1, 0x0d, (byte) 0xb8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2 };
    private static final Ipv6Address ipv6b = new Ipv6Address("2001:db8::2");
    private static final byte[] fecIpv6Bytes = { 0x20, 1, 0, 0, 0, 0, 0, 1 };
    private static final Ipv6Prefix fecIpv6 = new Ipv6Prefix("2001:0:0:1::/64");

    /*
      0                   1                   2                   3
      0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |      Type     |     Length    |     Flags     |     Weight    |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |            Range              | Prefix Length |  FEC Prefix   |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     //           FEC Prefix (continued, variable)                  //
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |                    SubTLVs (variable)                         |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

     tools.ietf.org/html/draft-ietf-isis-segment-routing-extensions-02#section-2.4
     */

    @Test
    public void testSidLabelBindingv4() {
        // expected bytes according to format above from Flags
        final byte[] expectedBytes = { (byte)0xC0, 24,
            0, 42,
            32, 127, 0, 0, 0,
            1, 4, 1, 2, 3, 4,
            2, 4, 0, 0, 0, 8,
            3, 5, (byte)0x80, 10, 0, 0, 1,
            6, 5, (byte)0x80, 10, 0, 0, 2,
            5, 9, (byte)0x80, 1, 2, 3, 4, 0, 0, 1, (byte)0xF4};

        final List<SubTlvs> subs = new ArrayList<>();
        subs.add(new SubTlvsBuilder().setSubtlvType(new SidLabelCaseBuilder().setSid(new SidLabel(new byte[] {1,2,3,4})).build()).build());
        subs.add(new SubTlvsBuilder().setSubtlvType(new EroMetricCaseBuilder().setEroMetric(new TeMetric((long) 8)).build()).build());
        subs.add(new SubTlvsBuilder().setSubtlvType(new Ipv4EroCaseBuilder().setLoose(true).setAddress(new IpAddress(new Ipv4Address("10.0.0.1"))).build()).build());
        subs.add(new SubTlvsBuilder().setSubtlvType(new Ipv4EroBackupCaseBuilder().setLoose(true).setAddress(new IpAddress(new Ipv4Address("10.0.0.2"))).build()).build());
        subs.add(new SubTlvsBuilder().setSubtlvType(new UnnumberedEroCaseBuilder().setLoose(true).setRouterId(new byte[] {1,2,3,4}).setInterfaceId((long)500).build()).build());
        final SrSidLabel srSidLabelToSerialize = new SrSidLabelBuilder().setSidLabelFlags(new SidLabelFlags(Boolean.TRUE, Boolean.TRUE))
            .setWeight(new Weight((short) 24)).setFecPrefix(new IpPrefix(new Ipv4Prefix("127.0.0.0/32"))).setValueRange(42).setSubTlvs(subs).build();

        final ByteBuf serializedSrSidLabel = Unpooled.buffer();
        SrNodeAttributesParser.serializeSidLabelBinding(srSidLabelToSerialize, serializedSrSidLabel);
        assertArrayEquals(expectedBytes, ByteArray.getAllBytes(serializedSrSidLabel));

        final SrSidLabel parsedSrSidLabel = SrNodeAttributesParser.parseSidLabelBinding(serializedSrSidLabel);
        assertEquals(srSidLabelToSerialize, parsedSrSidLabel);
    }

    @Test
    public void testSidLabelBindingv6() {
        // expected bytes according to format above from Flags
        final byte[] expectedBytes = Bytes.concat(new byte[] {
            (byte)0xC0, 24,
            0, 42,
            (byte)(fecIpv6Bytes.length * Byte.SIZE) }, fecIpv6Bytes, new byte[] {
                4, (byte)(ipv6aBytes.length+1), (byte)0x80}, ipv6aBytes, new byte[] {
                    7, (byte)(ipv6bBytes.length+1), (byte)0x80}, ipv6bBytes, new byte[] {
                        8, 21, (byte)0x80, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 , 0, 0, 1, (byte)0xF4}); // UnnumberedEro: type 5, length 20 for ipv6 1 + 8 + 8 + 3B

        final List<SubTlvs> subs = new ArrayList<>();
        subs.add(new SubTlvsBuilder().setSubtlvType(new Ipv6EroCaseBuilder().setLoose(true).setAddress(new IpAddress(ipv6a)).build()).build());
        subs.add(new SubTlvsBuilder().setSubtlvType(new Ipv6EroBackupCaseBuilder().setLoose(true).setAddress(new IpAddress(ipv6b)).build()).build());
        subs.add(new SubTlvsBuilder().setSubtlvType(new UnnumberedEroBackupCaseBuilder().setLoose(true).setRouterId(new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16}).setInterfaceId((long)500).build()).build());
        final SrSidLabel srSidLabelToSerialize = new SrSidLabelBuilder().setSidLabelFlags(new SidLabelFlags(Boolean.TRUE, Boolean.TRUE))
            .setWeight(new Weight((short) 24)).setFecPrefix(new IpPrefix(fecIpv6)).setValueRange(42).setSubTlvs(subs).build();

        final ByteBuf serializedSrSidLabel = Unpooled.buffer();
        SrNodeAttributesParser.serializeSidLabelBinding(srSidLabelToSerialize, serializedSrSidLabel);
        assertArrayEquals(expectedBytes, ByteArray.getAllBytes(serializedSrSidLabel));

        final SrSidLabel parsedSrSidLabel = SrNodeAttributesParser.parseSidLabelBinding(serializedSrSidLabel);
        assertEquals(srSidLabelToSerialize, parsedSrSidLabel);
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
    public void testSrPrefix() {
        final byte[] bytes = { (byte)0xFC, 0, 1, 2, 3, 4 };
        final SrPrefix p = new SrPrefixBuilder()
            .setFlags(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev150206.PrefixSid.Flags(true, true, true, true, true, true))
            .setAlgorithm(Algorithm.ShortestPathFirst).setSid(new SidLabel(new byte[] {1, 2, 3, 4})).build();
        assertEquals(p, SrPrefixAttributesParser.parseSrPrefix(Unpooled.wrappedBuffer(bytes)));
        final ByteBuf b = Unpooled.buffer();
        SrPrefixAttributesParser.serializeSrPrefix(p, b);
        assertArrayEquals(bytes, ByteArray.readAllBytes(b));
    }

    @Test
    public void testSrAdjId() {
        final byte[] tested = { (byte)-80, 10, 1, 24, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24 };
        final byte[] sidLabel = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24 };
        final SrAdjId srAdjId = new SrAdjIdBuilder().setFlags(new AdjacencyFlags(Boolean.TRUE, Boolean.FALSE, Boolean.TRUE, Boolean.FALSE, Boolean.TRUE)).setWeight(new Weight((short) 10)).setSid(new SidLabel(sidLabel)).build();
        final ByteBuf serializedData = SrLinkAttributesParser.serializeAdjacencySegmentIdentifier(srAdjId);
        assertEquals(srAdjId, SrLinkAttributesParser.parseAdjacencySegmentIdentifier(Unpooled.wrappedBuffer(tested)));
        assertArrayEquals(tested, ByteArray.readAllBytes(serializedData));
    }

    @Test
    public void testSrLanAdjId() {
        final byte[] tested = { (byte)-80, 10, 1, 2, 3, 4, 5, 6, 1, 24, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24 };
        final byte[] sidLabel = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24 };
        final byte[] systemId = { 1, 2, 3, 4, 5, 6 };
        final SrLanAdjId srLanAdjId = new SrLanAdjIdBuilder().setFlags(new AdjacencyFlags(Boolean.TRUE, Boolean.FALSE, Boolean.TRUE, Boolean.FALSE, Boolean.TRUE)).setWeight(new Weight((short)10)).setIsoSystemId(new IsoSystemIdentifier(systemId)).setSid(new SidLabel(sidLabel)).build();
        final ByteBuf serializedData = SrLinkAttributesParser.serializeLanAdjacencySegmentIdentifier(srLanAdjId);
        assertEquals(srLanAdjId, SrLinkAttributesParser.parseLanAdjacencySegmentIdentifier(Unpooled.wrappedBuffer(tested)));
        assertArrayEquals(tested, ByteArray.readAllBytes(serializedData));
    }

    @Test(expected=UnsupportedOperationException.class)
    public void testSrLinkAttributesPrivateConstructor() throws Throwable {
        final Constructor<SrLinkAttributesParser> c = SrLinkAttributesParser.class.getDeclaredConstructor();
        c.setAccessible(true);
        try {
            c.newInstance();
        } catch (final InvocationTargetException e) {
            throw e.getCause();
        }
    }

    @Test(expected=UnsupportedOperationException.class)
    public void testSrNodeAttributesPrivateConstructor() throws Throwable {
        final Constructor<SrNodeAttributesParser> c = SrNodeAttributesParser.class.getDeclaredConstructor();
        c.setAccessible(true);
        try {
            c.newInstance();
        } catch (final InvocationTargetException e) {
            throw e.getCause();
        }
    }

    @Test(expected=UnsupportedOperationException.class)
    public void testSrPrefixAttributesPrivateConstructor() throws Throwable {
        final Constructor<SrPrefixAttributesParser> c = SrPrefixAttributesParser.class.getDeclaredConstructor();
        c.setAccessible(true);
        try {
            c.newInstance();
        } catch (final InvocationTargetException e) {
            throw e.getCause();
        }
    }
}
