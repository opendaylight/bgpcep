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
import org.opendaylight.protocol.bgp.linkstate.attribute.sr.RangeTlvParser;
import org.opendaylight.protocol.bgp.linkstate.attribute.sr.SrLinkAttributesParser;
import org.opendaylight.protocol.bgp.linkstate.attribute.sr.SrNodeAttributesParser;
import org.opendaylight.protocol.bgp.linkstate.attribute.sr.SrPrefixAttributesParser;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.link.state.SrAdjId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.link.state.SrAdjIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.link.state.SrLanAdjId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.link.state.SrLanAdjIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.node.state.SrAlgorithm;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.node.state.SrAlgorithmBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.node.state.SrCapabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.node.state.SrCapabilitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.prefix.state.SrPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.prefix.state.SrPrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.prefix.state.SrRange;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.prefix.state.SrRangeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.Algorithm;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.Weight;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.binding.sid.tlv.BindingSubTlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.binding.sid.tlv.BindingSubTlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.binding.sub.tlvs.binding.sub.tlv.EroMetricCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.binding.sub.tlvs.binding.sub.tlv.Ipv4EroBackupCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.binding.sub.tlvs.binding.sub.tlv.Ipv4EroCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.binding.sub.tlvs.binding.sub.tlv.Ipv6EroBackupCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.binding.sub.tlvs.binding.sub.tlv.Ipv6EroCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.binding.sub.tlvs.binding.sub.tlv.PrefixSidCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.binding.sub.tlvs.binding.sub.tlv.SidLabelCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.binding.sub.tlvs.binding.sub.tlv.UnnumberedInterfaceIdBackupEroCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.binding.sub.tlvs.binding.sub.tlv.UnnumberedInterfaceIdEroCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.range.sub.tlvs.range.sub.tlv.BindingSidTlvCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.range.sub.tlvs.range.sub.tlv.PrefixSidTlvCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.range.sub.tlvs.range.sub.tlv.SidLabelTlvCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.range.tlv.SubTlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.range.tlv.SubTlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.sid.label.index.sid.label.index.Ipv6AddressCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.sid.label.index.sid.label.index.LocalLabelCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.sid.label.index.sid.label.index.SidCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.IsoSystemIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.MplsLabel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.TeMetric;

public class SrAttributeParserTest {

    private static final byte[] IPV6_A_BYTES = { 0x20, 1, 0x0d, (byte) 0xb8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1 };
    private static final Ipv6Address IPV6_A = new Ipv6Address("2001:db8::1");
    private static final byte[] IPV6_B_BYTES = { 0x20, 1, 0x0d, (byte) 0xb8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2 };
    private static final Ipv6Address IPV6_B = new Ipv6Address("2001:db8::2");

    @Test
    public void testSrAlgorithm() {
        final byte[] bytes = { 0 };
        final SrAlgorithm alg = new SrAlgorithmBuilder().setAlgorithms(Lists.newArrayList(Algorithm.ShortestPathFirst)).build();
        final SrAlgorithm empty = new SrAlgorithmBuilder().setAlgorithms(Collections.<Algorithm>emptyList()).build();
        assertEquals(alg, SrNodeAttributesParser.parseSrAlgorithms(Unpooled.wrappedBuffer(bytes)));
        final ByteBuf b = Unpooled.buffer();
        assertEquals(empty, SrNodeAttributesParser.parseSrAlgorithms(b));
        SrNodeAttributesParser.serializeSrAlgorithms(alg, b);
        assertArrayEquals(bytes, ByteArray.readAllBytes(b));

        SrNodeAttributesParser.serializeSrAlgorithms(empty, b);
        assertEquals(0, b.readableBytes());
    }

    // tools.ietf.org/html/draft-gredler-idr-bgp-ls-segment-routing-ext-00#section-2.1.1
    @Test
    public void testSrCapabilities() {
        final byte[] bytes = { (byte)0xC0, 0, 0, 0, 10, 4, (byte)0x89, 0, 4, 1, 2, 3, 4 };
        final SrCapabilities caps = new SrCapabilitiesBuilder().setMplsIpv4(Boolean.TRUE).setMplsIpv6(Boolean.TRUE).setSrIpv6(Boolean.FALSE).setSidLabelIndex(new SidCaseBuilder().setSid(16909060L).build()).setRangeSize((long) 10).build();
        assertEquals(caps, SrNodeAttributesParser.parseSrCapabilities(Unpooled.wrappedBuffer(bytes)));
        final ByteBuf b = Unpooled.buffer();
        SrNodeAttributesParser.serializeSrCapabilities(caps, b);
        assertArrayEquals(bytes, ByteArray.readAllBytes(b));
    }

    // tools.ietf.org/html/draft-gredler-idr-bgp-ls-segment-routing-ext-00#section-2.3.1
    @Test
    public void testSrPrefix() {
        final byte[] bytes = { (byte)0xFC, 0, 0, 0, 1, 2, 3, 4 };
        final SrPrefix prefix = new SrPrefixBuilder()
            .setFlags(new byte[] {(byte)0xfc})
            .setAlgorithm(Algorithm.ShortestPathFirst).setSidLabelIndex(new SidCaseBuilder().setSid(16909060L).build()).build();
        assertEquals(prefix, SrPrefixAttributesParser.parseSrPrefix(Unpooled.wrappedBuffer(bytes)));
        final ByteBuf serializedPrefix = Unpooled.buffer();
        SrPrefixAttributesParser.serializeSrPrefix(prefix, serializedPrefix);
        assertArrayEquals(bytes, ByteArray.readAllBytes(serializedPrefix));
    }

    // tools.ietf.org/html/draft-gredler-idr-bgp-ls-segment-routing-ext-00#section-2.3.2
    @Test
    public void testRangeTlv() {
        final byte[] tested = {
            0, 0, 0, 5,
            4, (byte)0x89, 0, 4, 1, 2, 3, 4, // sid
            4, (byte)0x86, 0, 7, 0, 1, 0, 0, 1, 2, 0, // prefix
            4, (byte)0x88, 0, 0x58, 5, 0, 0, 0, // binding sid
            // binding sub-tlvs
            4, (byte)0x86, 0, 8, 0, 1, 0, 0, 1, 2, 3, 4, // prefix
            4, (byte)0x89, 0, 4, 1, 2, 3, 4, // sid
            4, (byte)0x8a, 0, 4, 0, 0, 0, 6, // ero metric
            4, (byte)0x8b, 0, 8, 0, 0, 0, 0, 9, 8, 7, 6,  // IPv4 ERO
            4, (byte)0x8d, 0, 0x0c, 0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 2, 2, // Unnumbered Interface ID ERO Sub-TLV
            4, (byte)0x8e, 0, 8, 0, 0, 0, 0, 3, 4, 5, 6, // IPv4 ERO backup
            4, (byte)0x90, 0, 0x0c, 0, 0, 0, 0, 3, 3, 3, 3, 4, 4, 4, 4, // Unnumbered Interface ID ERO Sub-TLV Backup
        };
        final SrRange parsedRange = RangeTlvParser.parseSrRange(Unpooled.wrappedBuffer(tested));

        final List<SubTlvs> rangeSubTlvs = new ArrayList<SubTlvs>();
        addSubTlvs(rangeSubTlvs);
        final SrRange expected = new SrRangeBuilder().setInterArea(Boolean.FALSE).setRangeSize(5).setSubTlvs(rangeSubTlvs).build();

        assertEquals(expected, parsedRange);

        final ByteBuf serializedRange = Unpooled.buffer();
        RangeTlvParser.serializeSrRange(parsedRange, serializedRange);
        assertArrayEquals(tested, ByteArray.getAllBytes(serializedRange));
    }

    private void addSubTlvs(final List<SubTlvs> rangeSubTlvs) {
        rangeSubTlvs.add(new SubTlvsBuilder().setRangeSubTlv(
            new SidLabelTlvCaseBuilder()
                .setSidLabelIndex(new SidCaseBuilder().setSid(16909060L).build()).build()).build());
        rangeSubTlvs.add(new SubTlvsBuilder().setRangeSubTlv(
            new PrefixSidTlvCaseBuilder()
                .setFlags(new byte[] {0})
                .setAlgorithm(Algorithm.StrictShortestPathFirst)
                .setSidLabelIndex(new LocalLabelCaseBuilder().setLocalLabel(new MplsLabel(4128L)).build()).build()).build());
        final List<BindingSubTlvs> bindingSubTlvs = new ArrayList<BindingSubTlvs>();
        addBindingSubTlvs(bindingSubTlvs);
        rangeSubTlvs.add(new SubTlvsBuilder().setRangeSubTlv(
            new BindingSidTlvCaseBuilder()
                .setWeight(new Weight((short) 5))
                .setFlags(new byte[] {0})
                .setBindingSubTlvs(bindingSubTlvs).build()).build());
    }

    private void addBindingSubTlvs(final List<BindingSubTlvs> bindingSubTlvs) {
        bindingSubTlvs.add(new BindingSubTlvsBuilder().setBindingSubTlv(
            new PrefixSidCaseBuilder()
                .setFlags(new byte[] {0})
                .setAlgorithm(Algorithm.StrictShortestPathFirst)
                .setSidLabelIndex(new SidCaseBuilder().setSid(16909060L).build())
                .build()).build());
        bindingSubTlvs.add(new BindingSubTlvsBuilder().setBindingSubTlv(
            new SidLabelCaseBuilder().setSidLabelIndex(new SidCaseBuilder().setSid(16909060L).build()).build()).build());
        bindingSubTlvs.add(new BindingSubTlvsBuilder().setBindingSubTlv(
            new EroMetricCaseBuilder().setEroMetric(new TeMetric(6L)).build()).build());
        bindingSubTlvs.add(new BindingSubTlvsBuilder().setBindingSubTlv(
            new Ipv4EroCaseBuilder().setLoose(Boolean.FALSE).setAddress(new Ipv4Address("9.8.7.6")).build()).build());
        bindingSubTlvs.add(new BindingSubTlvsBuilder().setBindingSubTlv(
            new UnnumberedInterfaceIdEroCaseBuilder().setLoose(Boolean.FALSE).setRouterId(16843009L).setInterfaceId(33686018L).build()).build());
        bindingSubTlvs.add(new BindingSubTlvsBuilder().setBindingSubTlv(
            new Ipv4EroBackupCaseBuilder().setLoose(Boolean.FALSE).setAddress(new Ipv4Address("3.4.5.6")).build()).build());
        bindingSubTlvs.add(new BindingSubTlvsBuilder().setBindingSubTlv(
            new UnnumberedInterfaceIdBackupEroCaseBuilder().setLoose(Boolean.FALSE).setRouterId(50529027L).setInterfaceId(67372036L).build()).build());
    }

    @Test
    public void testRangeTlvIpv6() {
        final byte[] tested = Bytes.concat(
            new byte[] {
                0, 0, 0, 5,
                4, (byte)0x88, 0, 0x34, 5, 0, 0, 0, // binding sid
                // binding sub-tlvs
                4, (byte)0x8c, 0, 0x14, 0, 0, 0, 0 }, IPV6_A_BYTES, // IPv6 ERO
                    new byte[] { 4, (byte)0x8f, 0, 0x14, 0, 0, 0, 0 }, IPV6_B_BYTES // IPv6 ERO backup
            );
        final SrRange parsedRange = RangeTlvParser.parseSrRange(Unpooled.wrappedBuffer(tested));

        final List<SubTlvs> rangeSubTlvs = new ArrayList<SubTlvs>();
        final List<BindingSubTlvs> bindingSubTlvs = new ArrayList<BindingSubTlvs>();
        bindingSubTlvs.add(new BindingSubTlvsBuilder().setBindingSubTlv(
            new Ipv6EroCaseBuilder().setLoose(Boolean.FALSE).setAddress(new Ipv6Address(IPV6_A)).build()).build());
        bindingSubTlvs.add(new BindingSubTlvsBuilder().setBindingSubTlv(
            new Ipv6EroBackupCaseBuilder().setLoose(Boolean.FALSE).setAddress(new Ipv6Address(IPV6_B)).build()).build());
        rangeSubTlvs.add(new SubTlvsBuilder().setRangeSubTlv(
            new BindingSidTlvCaseBuilder()
                .setWeight(new Weight((short) 5))
                .setFlags(new byte[] {0})
                .setBindingSubTlvs(bindingSubTlvs).build()).build());
        final SrRange expected = new SrRangeBuilder().setInterArea(Boolean.FALSE).setRangeSize(5).setSubTlvs(rangeSubTlvs).build();

        assertEquals(expected, parsedRange);

        final ByteBuf serializedRange = Unpooled.buffer();
        RangeTlvParser.serializeSrRange(parsedRange, serializedRange);
        assertArrayEquals(tested, ByteArray.getAllBytes(serializedRange));
    }

    // tools.ietf.org/html/draft-gredler-idr-bgp-ls-segment-routing-ext-00#section-2.2.1
    @Test
    public void testSrAdjId() {
        final byte[] tested = { (byte)-80, 10, 0, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 };
        final byte[] sidLabel = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 };
        final SrAdjId srAdjId = new SrAdjIdBuilder()
            .setFlags(new byte[] { (byte)-80 })
            .setWeight(new Weight((short) 10))
            .setSidLabelIndex(new Ipv6AddressCaseBuilder().setIpv6Address(Ipv6Util.addressForByteBuf(Unpooled.copiedBuffer(sidLabel))).build()).build();
        final ByteBuf serializedData = SrLinkAttributesParser.serializeAdjacencySegmentIdentifier(srAdjId);
        assertEquals(srAdjId, new SrAdjIdBuilder(SrLinkAttributesParser.parseAdjacencySegmentIdentifier(Unpooled.wrappedBuffer(tested))).build());
        assertArrayEquals(tested, ByteArray.readAllBytes(serializedData));
    }

    @Test
    public void testSrLanAdjId() {
        final byte[] tested = { (byte)-80, 10, 0, 0, 1, 2, 3, 4, 5, 6, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 };
        final byte[] sidLabel = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};
        final byte[] systemId = { 1, 2, 3, 4, 5, 6 };
        final SrLanAdjId srLanAdjId = new SrLanAdjIdBuilder()
            .setFlags(new byte[] { (byte) -80 })
            .setWeight(new Weight((short)10))
            .setIsoSystemId(new IsoSystemIdentifier(systemId))
            .setSidLabelIndex(new Ipv6AddressCaseBuilder().setIpv6Address(Ipv6Util.addressForByteBuf(Unpooled.copiedBuffer(sidLabel))).build()).build();
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
