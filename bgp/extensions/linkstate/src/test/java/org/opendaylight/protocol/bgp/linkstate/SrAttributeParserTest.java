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

import com.google.common.primitives.Bytes;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.bgp.linkstate.impl.BGPActivator;
import org.opendaylight.protocol.bgp.linkstate.impl.attribute.sr.Ipv6SrPrefixAttributesParser;
import org.opendaylight.protocol.bgp.linkstate.impl.attribute.sr.RangeTlvParser;
import org.opendaylight.protocol.bgp.linkstate.impl.attribute.sr.SrLinkAttributesParser;
import org.opendaylight.protocol.bgp.linkstate.impl.attribute.sr.SrNodeAttributesParser;
import org.opendaylight.protocol.bgp.linkstate.impl.attribute.sr.SrPrefixAttributesParser;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.parser.spi.pojo.SimpleBGPExtensionProviderContext;
import org.opendaylight.protocol.rsvp.parser.spi.pojo.SimpleRSVPExtensionProviderContext;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.types.rev171204.Uint24;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.ProtocolId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.attribute.SrAdjIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.attribute.SrAdjIdsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.attribute.SrLanAdjIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.attribute.SrLanAdjIdsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.node.state.SrAlgorithm;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.node.state.SrAlgorithmBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.node.state.SrCapabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.node.state.SrCapabilitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.prefix.state.Ipv6SrPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.prefix.state.Ipv6SrPrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.prefix.state.SrPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.prefix.state.SrPrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.prefix.state.SrRange;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.prefix.state.SrRangeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev200120.Algorithm;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev200120.Weight;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev200120.adj.flags.flags.IsisAdjFlagsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev200120.adj.flags.flags.IsisAdjFlagsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev200120.adj.flags.flags.OspfAdjFlagsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev200120.adj.flags.flags.OspfAdjFlagsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev200120.adj.flags.flags.isis.adj.flags._case.IsisAdjFlagsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev200120.adj.flags.flags.ospf.adj.flags._case.OspfAdjFlagsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev200120.binding.sid.tlv.BindingSubTlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev200120.binding.sid.tlv.BindingSubTlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev200120.binding.sid.tlv.flags.IsisBindingFlagsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev200120.binding.sid.tlv.flags.IsisBindingFlagsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev200120.binding.sub.tlvs.binding.sub.tlv.EroMetricCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev200120.binding.sub.tlvs.binding.sub.tlv.Ipv4EroBackupCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev200120.binding.sub.tlvs.binding.sub.tlv.Ipv4EroCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev200120.binding.sub.tlvs.binding.sub.tlv.Ipv6EroBackupCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev200120.binding.sub.tlvs.binding.sub.tlv.Ipv6EroCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev200120.binding.sub.tlvs.binding.sub.tlv.PrefixSidCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev200120.binding.sub.tlvs.binding.sub.tlv.SidLabelCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev200120.binding.sub.tlvs.binding.sub.tlv.UnnumberedInterfaceIdBackupEroCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev200120.binding.sub.tlvs.binding.sub.tlv.UnnumberedInterfaceIdEroCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev200120.binding.sub.tlvs.binding.sub.tlv.ipv4.ero._case.Ipv4EroBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev200120.binding.sub.tlvs.binding.sub.tlv.ipv4.ero.backup._case.Ipv4EroBackupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev200120.binding.sub.tlvs.binding.sub.tlv.ipv6.ero._case.Ipv6EroBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev200120.binding.sub.tlvs.binding.sub.tlv.ipv6.ero.backup._case.Ipv6EroBackupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev200120.binding.sub.tlvs.binding.sub.tlv.prefix.sid._case.PrefixSidBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev200120.binding.sub.tlvs.binding.sub.tlv.unnumbered._interface.id.backup.ero._case.UnnumberedInterfaceIdBackupEroBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev200120.binding.sub.tlvs.binding.sub.tlv.unnumbered._interface.id.ero._case.UnnumberedInterfaceIdEroBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev200120.prefix.sid.tlv.flags.IsisPrefixFlagsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev200120.prefix.sid.tlv.flags.IsisPrefixFlagsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev200120.prefix.sid.tlv.flags.OspfPrefixFlagsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev200120.prefix.sid.tlv.flags.OspfPrefixFlagsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev200120.prefix.sid.tlv.flags.isis.prefix.flags._case.IsisPrefixFlagsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev200120.prefix.sid.tlv.flags.ospf.prefix.flags._case.OspfPrefixFlagsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev200120.range.sub.tlvs.range.sub.tlv.BindingSidTlvCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev200120.range.sub.tlvs.range.sub.tlv.PrefixSidTlvCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev200120.range.sub.tlvs.range.sub.tlv.SidLabelTlvCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev200120.range.sub.tlvs.range.sub.tlv.prefix.sid.tlv._case.PrefixSidTlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev200120.range.tlv.SubTlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev200120.range.tlv.SubTlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev200120.sid.label.index.sid.label.index.Ipv6AddressCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev200120.sid.label.index.sid.label.index.LocalLabelCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev200120.sid.label.index.sid.label.index.SidCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.IsoSystemIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.MplsLabel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.TeMetric;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint8;

public class SrAttributeParserTest {

    private static final byte[] IPV6_A_BYTES = { 0x20, 1, 0x0d, (byte) 0xb8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1 };
    private static final Ipv6Address IPV6_A = new Ipv6Address("2001:db8::1");
    private static final byte[] IPV6_B_BYTES = { 0x20, 1, 0x0d, (byte) 0xb8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2 };
    private static final Ipv6Address IPV6_B = new Ipv6Address("2001:db8::2");

    private static final IsisPrefixFlagsCase ISIS_PREFIX_FLAGS = new IsisPrefixFlagsCaseBuilder()
            .setIsisPrefixFlags(new IsisPrefixFlagsBuilder()
                .setReadvertisement(Boolean.TRUE)
                .setNodeSid(Boolean.FALSE)
                .setNoPhp(Boolean.TRUE)
                .setExplicitNull(Boolean.FALSE)
                .build())
            .build();
    private static final OspfPrefixFlagsCase OSPF_PREFIX_FLAGS = new OspfPrefixFlagsCaseBuilder()
            .setOspfPrefixFlags(new OspfPrefixFlagsBuilder()
                .setNoPhp(Boolean.FALSE)
                .setMappingServer(Boolean.TRUE)
                .setExplicitNull(Boolean.FALSE)
                .build())
            .build();

    private static final IsisBindingFlagsCase BINDING_FLAGS = new IsisBindingFlagsCaseBuilder()
        .setAddressFamily(Boolean.FALSE)
        .setMirrorContext(Boolean.TRUE)
        .setSpreadTlv(Boolean.FALSE)
        .setLeakedFromLevel2(Boolean.FALSE)
        .setAttachedFlag(Boolean.TRUE)
        .build();

    private static final IsisAdjFlagsCase ISIS_ADJ_FLAGS = new IsisAdjFlagsCaseBuilder()
            .setIsisAdjFlags(new IsisAdjFlagsBuilder()
                .setAddressFamily(Boolean.FALSE)
                .setBackup(Boolean.TRUE)
                .setSet(Boolean.FALSE)
                .build())
            .build();
    private static final OspfAdjFlagsCase OSPF_ADJ_FLAGS = new OspfAdjFlagsCaseBuilder()
            .setOspfAdjFlags(new OspfAdjFlagsBuilder()
                .setBackup(Boolean.TRUE)
                .setSet(Boolean.FALSE)
                .build())
            .build();
    private static final OspfAdjFlagsCase OSPF_LAN_ADJ_FLAGS = new OspfAdjFlagsCaseBuilder()
            .setOspfAdjFlags(new OspfAdjFlagsBuilder()
                .setBackup(Boolean.FALSE)
                .setSet(Boolean.FALSE)
                .build())
            .build();

    @Before
    public void setUp() throws Exception {
        final BGPActivator act = new BGPActivator(new SimpleRSVPExtensionProviderContext());
        final BGPExtensionProviderContext context = new SimpleBGPExtensionProviderContext();
        act.start(context);
    }

    @Test
    public void testSrAlgorithm() {
        final byte[] bytes = { 0 };
        final SrAlgorithm alg = new SrAlgorithmBuilder().setAlgorithms(Set.of(Algorithm.ShortestPathFirst)).build();
        final SrAlgorithm empty = new SrAlgorithmBuilder().setAlgorithms(Set.of()).build();
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
        final byte[] bytesIsis = { (byte)0xC0, 0, 0, 0, 10, 4, (byte)0x89, 0, 4, 1, 2, 3, 4 };
        final byte[] bytesOspf = { 0, 0, 0, 0, 10, 4, (byte)0x89, 0, 4, 1, 2, 3, 4 };
        final SrCapabilities capsIsis = new SrCapabilitiesBuilder().setMplsIpv4(Boolean.TRUE).setMplsIpv6(Boolean.TRUE)
                .setSrIpv6(Boolean.FALSE)
                .setSidLabelIndex(new SidCaseBuilder().setSid(Uint32.valueOf(16909060L)).build())
                .setRangeSize(new Uint24(Uint32.TEN)).build();
        final SrCapabilities capsOspf = new SrCapabilitiesBuilder().setMplsIpv4(Boolean.FALSE)
                .setMplsIpv6(Boolean.FALSE).setSrIpv6(Boolean.FALSE)
                .setSidLabelIndex(new SidCaseBuilder().setSid(Uint32.valueOf(16909060L)).build())
                .setRangeSize(new Uint24(Uint32.TEN)).build();
        assertEquals(capsIsis,
            SrNodeAttributesParser.parseSrCapabilities(Unpooled.wrappedBuffer(bytesIsis), ProtocolId.IsisLevel1));
        assertEquals(capsOspf,
            SrNodeAttributesParser.parseSrCapabilities(Unpooled.wrappedBuffer(bytesIsis), ProtocolId.Ospf));
        final ByteBuf encodedIsis = Unpooled.buffer();
        final ByteBuf encodedOspf = Unpooled.buffer();
        SrNodeAttributesParser.serializeSrCapabilities(capsIsis, encodedIsis);
        SrNodeAttributesParser.serializeSrCapabilities(capsOspf, encodedOspf);
        assertArrayEquals(bytesIsis, ByteArray.readAllBytes(encodedIsis));
        assertArrayEquals(bytesOspf, ByteArray.readAllBytes(encodedOspf));
    }

    // tools.ietf.org/html/draft-gredler-idr-bgp-ls-segment-routing-ext-00#section-2.3.1
    @Test
    public void testSrPrefix() {
        final byte[] bytes = { (byte)0xA0, 0, 0, 0, 1, 2, 3, 4 };
        final byte[] bytesOspf = { (byte)0x20, 0, 0, 0, 1, 2, 3, 4 };
        final SrPrefix prefixIsis = new SrPrefixBuilder()
            .setFlags(ISIS_PREFIX_FLAGS)
            .setAlgorithm(Algorithm.ShortestPathFirst)
            .setSidLabelIndex(new SidCaseBuilder().setSid(Uint32.valueOf(16909060L)).build())
            .build();
        final SrPrefix prefixOspf = new SrPrefixBuilder()
            .setFlags(OSPF_PREFIX_FLAGS)
            .setAlgorithm(Algorithm.ShortestPathFirst)
            .setSidLabelIndex(new SidCaseBuilder().setSid(Uint32.valueOf(16909060L)).build())
            .build();
        assertEquals(prefixIsis,
            SrPrefixAttributesParser.parseSrPrefix(Unpooled.wrappedBuffer(bytes), ProtocolId.IsisLevel1));
        assertEquals(prefixOspf,
            SrPrefixAttributesParser.parseSrPrefix(Unpooled.wrappedBuffer(bytes), ProtocolId.Ospf));
        final ByteBuf serializedPrefix = Unpooled.buffer();
        final ByteBuf serializedPrefixOspf = Unpooled.buffer();
        SrPrefixAttributesParser.serializeSrPrefix(prefixIsis, serializedPrefix);
        SrPrefixAttributesParser.serializeSrPrefix(prefixOspf, serializedPrefixOspf);
        assertArrayEquals(bytes, ByteArray.readAllBytes(serializedPrefix));
        assertArrayEquals(bytesOspf, ByteArray.readAllBytes(serializedPrefixOspf));
    }

    // https://tools.ietf.org/html/draft-gredler-idr-bgp-ls-segment-routing-ext-02#section-2.3.2
    @Test
    public void testIpv6SrPrefix() {
        final byte[] bytes = { 0, 0, 0};
        final Ipv6SrPrefix prefix = new Ipv6SrPrefixBuilder().setAlgorithm(Algorithm.ShortestPathFirst).build();
        assertEquals(prefix, Ipv6SrPrefixAttributesParser.parseSrIpv6Prefix(Unpooled.wrappedBuffer(bytes)));
        final ByteBuf serializedPrefix = Unpooled.buffer();
        Ipv6SrPrefixAttributesParser.serializeIpv6SrPrefix(prefix, serializedPrefix);
        assertArrayEquals(bytes, ByteArray.readAllBytes(serializedPrefix));
    }

    // tools.ietf.org/html/draft-gredler-idr-bgp-ls-segment-routing-ext-00#section-2.3.2
    @Test
    public void testRangeTlv() {
        final byte[] tested = {
            0, 0, 0, 5,
            4, (byte)0x89, 0, 4, 1, 2, 3, 4, // sid
            4, (byte)0x86, 0, 7, (byte)0xac, 1, 0, 0, 1, 2, 0, // prefix + mpls label
            4, (byte)0x88, 0, 0x58, 5, 0x48, 0, 0, // binding sid
            // binding sub-tlvs
            4, (byte)0x86, 0, 8, (byte)0xa0, 1, 0, 0, 1, 2, 3, 4, // prefix
            4, (byte)0x89, 0, 4, 1, 2, 3, 4, // sid
            4, (byte)0x8a, 0, 4, 0, 0, 0, 6, // ero metric
            4, (byte)0x8b, 0, 8, 0, 0, 0, 0, 9, 8, 7, 6,  // IPv4 ERO
            4, (byte)0x8d, 0, 0x0c, 0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 2, 2, // Unnumbered Interface ID ERO Sub-TLV
            4, (byte)0x8e, 0, 8, 0, 0, 0, 0, 3, 4, 5, 6, // IPv4 ERO backup
            4, (byte)0x90, 0, 0x0c, 0, 0, 0, 0, 3, 3, 3, 3, 4, 4, 4, 4, // Unnumbered Interface ID ERO Sub-TLV Backup
        };
        final SrRange parsedRange = RangeTlvParser.parseSrRange(Unpooled.wrappedBuffer(tested), ProtocolId.IsisLevel1);

        final List<SubTlvs> rangeSubTlvs = new ArrayList<>();
        addSubTlvs(rangeSubTlvs);
        final SrRange expected = new SrRangeBuilder().setInterArea(Boolean.FALSE).setRangeSize(Uint16.valueOf(5))
                .setSubTlvs(rangeSubTlvs).build();

        assertEquals(expected, parsedRange);

        final ByteBuf serializedRange = Unpooled.buffer();
        RangeTlvParser.serializeSrRange(parsedRange, serializedRange);
        assertArrayEquals(tested, ByteArray.getAllBytes(serializedRange));
    }

    private static void addSubTlvs(final List<SubTlvs> rangeSubTlvs) {
        rangeSubTlvs.add(new SubTlvsBuilder()
            .setRangeSubTlv(new SidLabelTlvCaseBuilder()
                .setSidLabelIndex(new SidCaseBuilder().setSid(Uint32.valueOf(16909060L)).build())
                .build())
            .build());
        rangeSubTlvs.add(new SubTlvsBuilder()
            .setRangeSubTlv(new PrefixSidTlvCaseBuilder()
                .setPrefixSidTlv(new PrefixSidTlvBuilder()
                    .setFlags(ISIS_PREFIX_FLAGS)
                    .setAlgorithm(Algorithm.StrictShortestPathFirst)
                    .setSidLabelIndex(new LocalLabelCaseBuilder()
                        .setLocalLabel(new MplsLabel(Uint32.valueOf(66048)))
                        .build())
                    .build())
                .build())
            .build());
        final List<BindingSubTlvs> bindingSubTlvs = new ArrayList<>();
        addBindingSubTlvs(bindingSubTlvs);
        rangeSubTlvs.add(new SubTlvsBuilder().setRangeSubTlv(
            new BindingSidTlvCaseBuilder()
                .setWeight(new Weight(Uint8.valueOf(5)))
                .setFlags(BINDING_FLAGS)
                .setBindingSubTlvs(bindingSubTlvs).build()).build());
    }

    private static void addBindingSubTlvs(final List<BindingSubTlvs> bindingSubTlvs) {
        bindingSubTlvs.add(new BindingSubTlvsBuilder()
            .setBindingSubTlv(new PrefixSidCaseBuilder()
                .setPrefixSid(new PrefixSidBuilder()
                    .setFlags(ISIS_PREFIX_FLAGS)
                    .setAlgorithm(Algorithm.StrictShortestPathFirst)
                    .setSidLabelIndex(new SidCaseBuilder().setSid(Uint32.valueOf(16909060L)).build())
                    .build())
                .build())
            .build());
        bindingSubTlvs.add(new BindingSubTlvsBuilder()
            .setBindingSubTlv(new SidLabelCaseBuilder()
                .setSidLabelIndex(new SidCaseBuilder().setSid(Uint32.valueOf(16909060L)).build())
                .build())
            .build());
        bindingSubTlvs.add(new BindingSubTlvsBuilder().setBindingSubTlv(
            new EroMetricCaseBuilder().setEroMetric(new TeMetric(Uint32.valueOf(6))).build()).build());
        bindingSubTlvs.add(new BindingSubTlvsBuilder()
            .setBindingSubTlv(new Ipv4EroCaseBuilder()
                .setIpv4Ero(new Ipv4EroBuilder()
                    .setLoose(Boolean.FALSE)
                    .setAddress(new Ipv4AddressNoZone("9.8.7.6"))
                    .build())
                .build())
            .build());
        bindingSubTlvs.add(new BindingSubTlvsBuilder()
            .setBindingSubTlv(new UnnumberedInterfaceIdEroCaseBuilder()
                .setUnnumberedInterfaceIdEro(new UnnumberedInterfaceIdEroBuilder()
                    .setLoose(Boolean.FALSE)
                    .setRouterId(Uint32.valueOf(16843009L))
                    .setInterfaceId(Uint32.valueOf(33686018L))
                    .build())
                .build())
            .build());
        bindingSubTlvs.add(new BindingSubTlvsBuilder()
            .setBindingSubTlv(new Ipv4EroBackupCaseBuilder()
                .setIpv4EroBackup(new Ipv4EroBackupBuilder()
                    .setLoose(Boolean.FALSE).setAddress(new Ipv4AddressNoZone("3.4.5.6"))
                    .build())
                .build())
            .build());
        bindingSubTlvs.add(new BindingSubTlvsBuilder()
            .setBindingSubTlv(new UnnumberedInterfaceIdBackupEroCaseBuilder()
                .setUnnumberedInterfaceIdBackupEro(new UnnumberedInterfaceIdBackupEroBuilder()
                    .setLoose(Boolean.FALSE)
                    .setRouterId(Uint32.valueOf(50529027L))
                    .setInterfaceId(Uint32.valueOf(67372036L))
                    .build())
                .build())
            .build());
    }

    @Test
    public void testRangeTlvIpv6() {
        final byte[] tested = Bytes.concat(
            new byte[] {
                0, 0, 0, 5,
                4, (byte)0x88, 0, 0x34, 5, 0x48, 0, 0, // binding sid
                // binding sub-tlvs
                4, (byte)0x8c, 0, 0x14, 0, 0, 0, 0 }, IPV6_A_BYTES, // IPv6 ERO
                    new byte[] { 4, (byte)0x8f, 0, 0x14, 0, 0, 0, 0 }, IPV6_B_BYTES // IPv6 ERO backup
            );
        final SrRange parsedRange = RangeTlvParser.parseSrRange(Unpooled.wrappedBuffer(tested), ProtocolId.IsisLevel1);

        final List<SubTlvs> rangeSubTlvs = new ArrayList<>();
        final List<BindingSubTlvs> bindingSubTlvs = new ArrayList<>();
        bindingSubTlvs.add(new BindingSubTlvsBuilder()
            .setBindingSubTlv(new Ipv6EroCaseBuilder()
                .setIpv6Ero(new Ipv6EroBuilder()
                    .setLoose(Boolean.FALSE).setAddress(new Ipv6AddressNoZone(IPV6_A))
                    .build())
                .build())
            .build());
        bindingSubTlvs.add(new BindingSubTlvsBuilder()
            .setBindingSubTlv(new Ipv6EroBackupCaseBuilder()
                .setIpv6EroBackup(new Ipv6EroBackupBuilder()
                    .setLoose(Boolean.FALSE).setAddress(new Ipv6AddressNoZone(IPV6_B))
                    .build())
                .build())
            .build());
        rangeSubTlvs.add(new SubTlvsBuilder().setRangeSubTlv(
            new BindingSidTlvCaseBuilder()
                .setWeight(new Weight(Uint8.valueOf(5)))
                .setFlags(BINDING_FLAGS)
                .setBindingSubTlvs(bindingSubTlvs).build()).build());
        final SrRange expected = new SrRangeBuilder().setInterArea(Boolean.FALSE).setRangeSize(Uint16.valueOf(5))
                .setSubTlvs(rangeSubTlvs).build();

        assertEquals(expected, parsedRange);

        final ByteBuf serializedRange = Unpooled.buffer();
        RangeTlvParser.serializeSrRange(parsedRange, serializedRange);
        assertArrayEquals(tested, ByteArray.getAllBytes(serializedRange));
    }

    // tools.ietf.org/html/draft-gredler-idr-bgp-ls-segment-routing-ext-00#section-2.2.1
    @Test
    public void testSrAdjId() {
        final byte[] tested = { (byte)0x60, 10, 0, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 };
        final byte[] testedOspf = { (byte)0xc0, 10, 0, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 };
        final byte[] sidLabel = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 };
        final SrAdjIds srAdjId = new SrAdjIdsBuilder()
            .setFlags(ISIS_ADJ_FLAGS)
            .setWeight(new Weight(Uint8.TEN))
            .setSidLabelIndex(new Ipv6AddressCaseBuilder().setIpv6Address(
                Ipv6Util.addressForByteBuf(Unpooled.copiedBuffer(sidLabel))).build()).build();
        final SrAdjIds ospfAdj = new SrAdjIdsBuilder()
            .setFlags(OSPF_ADJ_FLAGS)
            .setWeight(new Weight(Uint8.TEN))
            .setSidLabelIndex(new Ipv6AddressCaseBuilder().setIpv6Address(
                Ipv6Util.addressForByteBuf(Unpooled.copiedBuffer(sidLabel))).build()).build();

        assertEquals(srAdjId, new SrAdjIdsBuilder(SrLinkAttributesParser.parseAdjacencySegmentIdentifier(
            Unpooled.wrappedBuffer(tested), ProtocolId.IsisLevel1)).build());
        assertEquals(ospfAdj, new SrAdjIdsBuilder(SrLinkAttributesParser.parseAdjacencySegmentIdentifier(
            Unpooled.wrappedBuffer(testedOspf), ProtocolId.Ospf)).build());
        final ByteBuf serializedData = SrLinkAttributesParser.serializeAdjacencySegmentIdentifier(srAdjId);
        final ByteBuf serializedOspf = SrLinkAttributesParser.serializeAdjacencySegmentIdentifier(ospfAdj);
        assertArrayEquals(tested, ByteArray.readAllBytes(serializedData));
        assertArrayEquals(testedOspf, ByteArray.readAllBytes(serializedOspf));
    }

    @Test
    public void testSrLanAdjIdOspf() {
        final byte[] tested = { (byte)0x60, 10, 0, 0, 1, 2, 3, 4, 0,  0x5d, (byte)0xc0 };
        final SrLanAdjIds srLanAdjId = new SrLanAdjIdsBuilder()
            .setFlags(OSPF_LAN_ADJ_FLAGS)
            .setWeight(new Weight(Uint8.TEN))
            .setNeighborId(new Ipv4AddressNoZone("1.2.3.4"))
            .setSidLabelIndex(new LocalLabelCaseBuilder()
                .setLocalLabel(new MplsLabel(Uint32.valueOf(24000L)))
                .build())
            .build();
        assertEquals(srLanAdjId, SrLinkAttributesParser.parseLanAdjacencySegmentIdentifier(
            Unpooled.wrappedBuffer(tested), ProtocolId.Ospf));
        final ByteBuf serializedData = SrLinkAttributesParser.serializeLanAdjacencySegmentIdentifier(srLanAdjId);
        assertArrayEquals(tested, ByteArray.readAllBytes(serializedData));
    }

    @Test
    public void testSrLanAdjIdIsis() {
        final byte[] tested = { (byte)0x60, 10, 0, 0, 1, 2, 3, 4, 5, 6, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14,
            15, 16 };
        final byte[] sidLabel = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};
        final byte[] systemId = { 1, 2, 3, 4, 5, 6 };
        final SrLanAdjIds srLanAdjId = new SrLanAdjIdsBuilder()
            .setFlags(ISIS_ADJ_FLAGS)
            .setWeight(new Weight(Uint8.TEN))
            .setIsoSystemId(new IsoSystemIdentifier(systemId))
            .setSidLabelIndex(new Ipv6AddressCaseBuilder().setIpv6Address(Ipv6Util.addressForByteBuf(
                Unpooled.copiedBuffer(sidLabel))).build()).build();
        assertEquals(srLanAdjId, SrLinkAttributesParser.parseLanAdjacencySegmentIdentifier(
            Unpooled.wrappedBuffer(tested), ProtocolId.IsisLevel1));
        final ByteBuf serializedData = SrLinkAttributesParser.serializeLanAdjacencySegmentIdentifier(srLanAdjId);
        assertArrayEquals(tested, ByteArray.readAllBytes(serializedData));
    }
}
