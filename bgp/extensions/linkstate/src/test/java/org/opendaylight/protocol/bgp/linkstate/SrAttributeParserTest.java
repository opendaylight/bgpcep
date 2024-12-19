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

import com.google.common.collect.ImmutableSet;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.bgp.linkstate.impl.BGPActivator;
import org.opendaylight.protocol.bgp.linkstate.impl.attribute.sr.SrFlexAlgoParser;
import org.opendaylight.protocol.bgp.linkstate.impl.attribute.sr.SrLinkAttributesParser;
import org.opendaylight.protocol.bgp.linkstate.impl.attribute.sr.SrNodeAttributesParser;
import org.opendaylight.protocol.bgp.linkstate.impl.attribute.sr.SrPrefixAttributesParser;
import org.opendaylight.protocol.bgp.linkstate.impl.attribute.sr.SrRangeParser;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.parser.spi.pojo.SimpleBGPExtensionProviderContext;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.types.rev171204.Uint24;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.ProtocolId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.node.state.FlexAlgoDefinition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.node.state.FlexAlgoDefinitionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.node.state.NodeMsd;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.node.state.NodeMsdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.node.state.SrAlgorithm;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.node.state.SrAlgorithmBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.node.state.SrCapabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.node.state.SrCapabilitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.node.state.SrLocalBlock;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.node.state.SrLocalBlockBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.prefix.state.SrPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.prefix.state.SrPrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.prefix.state.SrRange;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.prefix.state.SrRangeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.sr.attributes.SrAdjIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.sr.attributes.SrAdjIdsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.sr.attributes.SrLanAdjIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.sr.attributes.SrLanAdjIdsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.Algorithm;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.ExtendedAdminGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.FlexAlgo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.FlexAlgoDefinitionFlag;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.FlexMetric;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.MsdType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.Weight;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.adj.flags.flags.IsisAdjFlagsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.adj.flags.flags.IsisAdjFlagsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.adj.flags.flags.OspfAdjFlagsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.adj.flags.flags.OspfAdjFlagsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.adj.flags.flags.isis.adj.flags._case.IsisAdjFlagsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.adj.flags.flags.ospf.adj.flags._case.OspfAdjFlagsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.flex.algo.definition.tlv.FlexAlgoSubtlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.flex.algo.definition.tlv.FlexAlgoSubtlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.flex.algo.subtlv.UnsupportedTlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.lan.adj.sid.tlv.neighbor.type.IsisNeighborCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.lan.adj.sid.tlv.neighbor.type.OspfNeighborCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.node.msd.tlv.Msd;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.node.msd.tlv.MsdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.prefix.sid.tlv.flags.IsisPrefixFlagsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.prefix.sid.tlv.flags.IsisPrefixFlagsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.prefix.sid.tlv.flags.OspfPrefixFlagsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.prefix.sid.tlv.flags.OspfPrefixFlagsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.prefix.sid.tlv.flags.isis.prefix.flags._case.IsisPrefixFlagsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.prefix.sid.tlv.flags.ospf.prefix.flags._case.OspfPrefixFlagsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.range.tlv.PrefixSidBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.range.tlv.range.flags.IsisRangeFlagsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.range.tlv.range.flags.IsisRangeFlagsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.sid.label.index.sid.label.index.LabelCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.sid.label.index.sid.label.index.SidCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.sr.capabilities.tlv.Srgb;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.sr.capabilities.tlv.SrgbBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.sr.local.block.tlv.Srlb;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.sr.local.block.tlv.SrlbBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.IsoSystemIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.MplsLabel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.SrlgId;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint8;

public class SrAttributeParserTest {

    private static final IsisPrefixFlagsCase ISIS_PREFIX_FLAGS = new IsisPrefixFlagsCaseBuilder()
            .setIsisPrefixFlags(new IsisPrefixFlagsBuilder()
                .setReAdvertisement(Boolean.TRUE)
                .setNodeSid(Boolean.FALSE)
                .setNoPhp(Boolean.TRUE)
                .setExplicitNull(Boolean.FALSE)
                .setValue(Boolean.FALSE)
                .setLocal(Boolean.FALSE)
                .build())
            .build();
    private static final IsisPrefixFlagsCase ISIS_PREFIX_RANGE_FLAGS = new IsisPrefixFlagsCaseBuilder()
            .setIsisPrefixFlags(new IsisPrefixFlagsBuilder()
                .setReAdvertisement(Boolean.TRUE)
                .setNodeSid(Boolean.FALSE)
                .setNoPhp(Boolean.TRUE)
                .setExplicitNull(Boolean.FALSE)
                .setValue(Boolean.TRUE)
                .setLocal(Boolean.TRUE)
                .build())
            .build();
    private static final IsisRangeFlagsCase ISIS_RANGE_FLAGS = new IsisRangeFlagsCaseBuilder()
        .setAddressFamily(Boolean.FALSE)
        .setAttachedFlag(Boolean.FALSE)
        .setLeakedFromLevel2(Boolean.FALSE)
        .setMirrorContext(Boolean.FALSE)
        .setSpreadTlv(Boolean.FALSE)
        .build();
    private static final OspfPrefixFlagsCase OSPF_PREFIX_FLAGS = new OspfPrefixFlagsCaseBuilder()
            .setOspfPrefixFlags(new OspfPrefixFlagsBuilder()
                .setNoPhp(Boolean.FALSE)
                .setMappingServer(Boolean.TRUE)
                .setExplicitNull(Boolean.FALSE)
                .setValue(Boolean.FALSE)
                .setLocal(Boolean.FALSE)
                .build())
            .build();

    private static final IsisAdjFlagsCase ISIS_ADJ_FLAGS = new IsisAdjFlagsCaseBuilder()
            .setIsisAdjFlags(new IsisAdjFlagsBuilder()
                .setAddressFamily(Boolean.FALSE)
                .setBackup(Boolean.TRUE)
                .setValue(Boolean.TRUE)
                .setLocal(Boolean.TRUE)
                .setSet(Boolean.FALSE)
                .setPersistent(Boolean.TRUE)
                .build())
            .build();
    private static final OspfAdjFlagsCase OSPF_ADJ_FLAGS = new OspfAdjFlagsCaseBuilder()
            .setOspfAdjFlags(new OspfAdjFlagsBuilder()
                .setBackup(Boolean.TRUE)
                .setValue(Boolean.TRUE)
                .setLocal(Boolean.TRUE)
                .setSet(Boolean.FALSE)
                .setPersistent(Boolean.FALSE)
                .build())
            .build();
    private static final OspfAdjFlagsCase OSPF_LAN_ADJ_FLAGS = new OspfAdjFlagsCaseBuilder()
            .setOspfAdjFlags(new OspfAdjFlagsBuilder()
                .setBackup(Boolean.FALSE)
                .setValue(Boolean.TRUE)
                .setLocal(Boolean.TRUE)
                .setSet(Boolean.FALSE)
                .setPersistent(Boolean.FALSE)
                .build())
            .build();

    @Before
    public void setUp() throws Exception {
        final BGPActivator act = new BGPActivator();
        final BGPExtensionProviderContext context = new SimpleBGPExtensionProviderContext();
        act.start(context);
    }

    // tools.ietf.org/html/rfc90850#section-2.1.2
    @Test
    public void testSrCapabilities() {
        final byte[] bytesIsis = { (byte)0xC0, 0, 0, 0, 10, 4, (byte)0x89, 0, 4, 1, 2, 3, 4 };
        final byte[] bytesOspf = { 0, 0, 0, 0, 10, 4, (byte)0x89, 0, 4, 1, 2, 3, 4 };
        final List<Srgb> srgb = new ArrayList<Srgb>();
        srgb.add(new SrgbBuilder()
                .setRangeSize(new Uint24(Uint32.TEN))
                .setSidLabelIndex(new SidCaseBuilder().setSid(Uint32.valueOf(16909060L)).build())
                .build());
        final SrCapabilities capsIsis = new SrCapabilitiesBuilder().setMplsIpv4(Boolean.TRUE).setMplsIpv6(Boolean.TRUE)
                .setSrgb(srgb)
                .build();
        final SrCapabilities capsOspf = new SrCapabilitiesBuilder().setMplsIpv4(Boolean.FALSE)
                .setMplsIpv6(Boolean.FALSE)
                .setSrgb(srgb)
                .build();
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

    // tools.ietf.org/html/rfc90850#section-2.1.3
    @Test
    public void testSrAlgorithm() {
        final byte[] bytes = { 0, 1 };
        final var algos = ImmutableSet.<Algorithm>builder();
        algos.add(Algorithm.ShortestPathFirst);
        algos.add(Algorithm.StrictShortestPathFirst);
        final SrAlgorithm alg = new SrAlgorithmBuilder().setAlgorithms(algos.build()).build();
        final SrAlgorithm empty = new SrAlgorithmBuilder().setAlgorithms(Set.of()).build();
        assertEquals(alg, SrNodeAttributesParser.parseSrAlgorithms(Unpooled.wrappedBuffer(bytes)));
        final ByteBuf b = Unpooled.buffer();
        assertEquals(empty, SrNodeAttributesParser.parseSrAlgorithms(b));
        SrNodeAttributesParser.serializeSrAlgorithms(alg, b);
        assertArrayEquals(bytes, ByteArray.readAllBytes(b));

        SrNodeAttributesParser.serializeSrAlgorithms(empty, b);
        assertEquals(0, b.readableBytes());
    }

    // tools.ietf.org/html/rfc90850#section-2.1.4
    @Test
    public void testSrLocalBlock() {
        final byte[] bytesSrlb = { 0, 0, 0, 0, 10, 4, (byte)0x89, 0, 3, 0, 1, 2 };
        final List<Srlb> srlbs = new ArrayList<Srlb>();
        srlbs.add(new SrlbBuilder()
                .setRangeSize(new Uint24(Uint32.TEN))
                .setSidLabelIndex(new LabelCaseBuilder().setLabel(new MplsLabel(Uint32.valueOf(258L))).build())
                .build());
        final SrLocalBlock srlb = new SrLocalBlockBuilder().setSrlb(srlbs).build();
        assertEquals(srlb, SrNodeAttributesParser.parseSrLocalBlock(Unpooled.wrappedBuffer(bytesSrlb)));
        final ByteBuf encodedSrlb = Unpooled.buffer();
        SrNodeAttributesParser.serializeSrLocalBlock(srlb, encodedSrlb);
        assertArrayEquals(bytesSrlb, ByteArray.readAllBytes(encodedSrlb));
    }

    // tools.ietf.org/html/rfc8814#section-3
    @Test
    public void testMSD() {
        final byte[] bytesMSD = { 1, 10, 2, 8 };
        final List<Msd> msds = new ArrayList<Msd>();
        msds.add(new MsdBuilder().setType(MsdType.forValue(1)).setValue(Uint8.valueOf(10)).build());
        msds.add(new MsdBuilder().setType(MsdType.forValue(2)).setValue(Uint8.valueOf(8)).build());
        final NodeMsd nodeMsd = new NodeMsdBuilder().setMsd(msds).build();
        assertEquals(nodeMsd, SrNodeAttributesParser.parseSrNodeMsd(Unpooled.wrappedBuffer(bytesMSD)));
        final ByteBuf encodedNodeMsd = Unpooled.buffer();
        SrNodeAttributesParser.serializeSrNodeMsd(nodeMsd, encodedNodeMsd);
        assertArrayEquals(bytesMSD, ByteArray.readAllBytes(encodedNodeMsd));
    }

    // tools.ietf.org/html/rfc9085#section-2.3.1
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

    // tools.ietf.org/html/rfc9085#section-2.3.5
    @Test
    public void testRangeTlv() {
        final byte[] tested = {
            0, 0, 0, 4, // Flag + Range Size
            4, (byte)0x86, 0, 7, (byte)0xac, 1, 0, 0, 1, 2, 0, // Prefix-SID with mpls label
        };
        final SrRange parsedRange = SrRangeParser.parseSrRange(Unpooled.wrappedBuffer(tested), ProtocolId.IsisLevel1);
        final SrRange expected = new SrRangeBuilder()
                .setRangeFlags(ISIS_RANGE_FLAGS)
                .setRangeSize(Uint16.valueOf(4))
                .setPrefixSid(new PrefixSidBuilder()
                        .setFlags(ISIS_PREFIX_RANGE_FLAGS)
                        .setAlgorithm(Algorithm.StrictShortestPathFirst)
                        .setSidLabelIndex(new LabelCaseBuilder()
                                .setLabel(new MplsLabel(Uint32.valueOf(66048)))
                                .build())
                        .build())
                .build();

        assertEquals(expected, parsedRange);

        final ByteBuf serializedRange = Unpooled.buffer();
        SrRangeParser.serializeSrRange(parsedRange, serializedRange);
        assertArrayEquals(tested, ByteArray.getAllBytes(serializedRange));
    }

    // tools.ietf.org/html/rfc9085#section-2.2.1
    @Test
    public void testSrAdjId() {
        final byte[] tested = { (byte)0x74, 10, 0, 0, 0, 0x5d, (byte)0xc0 };
        final byte[] testedOspf = { (byte)0xe0, 10, 0, 0, 0, 0x5d, (byte)0xc0 };
        final SrAdjIds srAdjId = new SrAdjIdsBuilder()
            .setFlags(ISIS_ADJ_FLAGS)
            .setWeight(new Weight(Uint8.TEN))
            .setSidLabelIndex(new LabelCaseBuilder()
                    .setLabel(new MplsLabel(Uint32.valueOf(24000L)))
                    .build())
            .build();
        final SrAdjIds ospfAdj = new SrAdjIdsBuilder()
            .setFlags(OSPF_ADJ_FLAGS)
            .setWeight(new Weight(Uint8.TEN))
            .setSidLabelIndex(new LabelCaseBuilder()
                    .setLabel(new MplsLabel(Uint32.valueOf(24000L)))
                    .build())
            .build();

        assertEquals(srAdjId, new SrAdjIdsBuilder(SrLinkAttributesParser.parseAdjacencySegmentIdentifier(
            Unpooled.wrappedBuffer(tested), ProtocolId.IsisLevel1)).build());
        assertEquals(ospfAdj, new SrAdjIdsBuilder(SrLinkAttributesParser.parseAdjacencySegmentIdentifier(
            Unpooled.wrappedBuffer(testedOspf), ProtocolId.Ospf)).build());
        final ByteBuf serializedData = SrLinkAttributesParser.serializeAdjacencySegmentIdentifier(srAdjId);
        final ByteBuf serializedOspf = SrLinkAttributesParser.serializeAdjacencySegmentIdentifier(ospfAdj);
        assertArrayEquals(tested, ByteArray.readAllBytes(serializedData));
        assertArrayEquals(testedOspf, ByteArray.readAllBytes(serializedOspf));
    }

    // tools.ietf.org/html/rfc9085#section-2.2.2
    @Test
    public void testSrLanAdjIdOspf() {
        final byte[] tested = { (byte)0x60, 10, 0, 0, 1, 2, 3, 4, 0, 0x5d, (byte)0xc0 };
        final SrLanAdjIds srLanAdjId = new SrLanAdjIdsBuilder()
            .setFlags(OSPF_LAN_ADJ_FLAGS)
            .setWeight(new Weight(Uint8.TEN))
            .setNeighborType(new OspfNeighborCaseBuilder().setNeighborId(new Ipv4AddressNoZone("1.2.3.4")).build())
            .setSidLabelIndex(new LabelCaseBuilder()
                .setLabel(new MplsLabel(Uint32.valueOf(24000L)))
                .build())
            .build();
        assertEquals(srLanAdjId, SrLinkAttributesParser.parseLanAdjacencySegmentIdentifier(
            Unpooled.wrappedBuffer(tested), ProtocolId.Ospf));
        final ByteBuf serializedData = SrLinkAttributesParser.serializeLanAdjacencySegmentIdentifier(srLanAdjId);
        assertArrayEquals(tested, ByteArray.readAllBytes(serializedData));
    }

    @Test
    public void testSrLanAdjIdIsis() {
        final byte[] tested = { (byte)0x74, 10, 0, 0, 1, 2, 3, 4, 5, 6, 0,  0x5d, (byte)0xc0 };
        final byte[] systemId = { 1, 2, 3, 4, 5, 6 };
        final SrLanAdjIds srLanAdjId = new SrLanAdjIdsBuilder()
            .setFlags(ISIS_ADJ_FLAGS)
            .setWeight(new Weight(Uint8.TEN))
            .setNeighborType(new IsisNeighborCaseBuilder().setIsoSystemId(new IsoSystemIdentifier(systemId)).build())
            .setSidLabelIndex(new LabelCaseBuilder()
                    .setLabel(new MplsLabel(Uint32.valueOf(24000L)))
                    .build())
            .build();
        assertEquals(srLanAdjId, SrLinkAttributesParser.parseLanAdjacencySegmentIdentifier(
            Unpooled.wrappedBuffer(tested), ProtocolId.IsisLevel1));
        final ByteBuf serializedData = SrLinkAttributesParser.serializeLanAdjacencySegmentIdentifier(srLanAdjId);
        assertArrayEquals(tested, ByteArray.readAllBytes(serializedData));
    }

    @Test
    public void testFlexAlgo() {
        final byte[] tested = {(byte)0x80, 1, 1, 0x0a,  // Flex-Algo Definition + Flex-Algo SubTLVS bellow
            4, 0x10, 0, 4, 0, 0, 0, 1,                  // exclude-any
            4, 0x11, 0, 4, 0, 0, 0, 0,                  // include-any
            4, 0x12, 0, 4, 0, 0, 0, 2,                  // include-all
            4, 0x13, 0, 4, (byte)0x80, 0, 0, 0,         // flags
            4, 0x15, 0, 4, 0, 0, 0, 0x0a,               // exclude-SRLG
            4, 0x16, 0, 2, 1, 0x0a                      // unsupported TLVs
        };
        final FlexAlgoSubtlvs fas = new FlexAlgoSubtlvsBuilder()
            .setExcludeAny(Set.of(new ExtendedAdminGroup(Uint32.ONE)))
            .setIncludeAny(Set.of(new ExtendedAdminGroup(Uint32.ZERO)))
            .setIncludeAll(Set.of(new ExtendedAdminGroup(Uint32.TWO)))
            .setFlags(new FlexAlgoDefinitionFlag(true))
            .setExcludeSrlg(Set.of(new SrlgId(Uint32.TEN)))
            .setUnsupportedTlv(new UnsupportedTlvBuilder()
                    .setProtocolId(Uint8.valueOf(ProtocolId.IsisLevel1.getIntValue()))
                    .setProtocolType(Set.of(Uint16.TEN))
                    .build())
            .build();
        final FlexAlgoDefinition fad = new FlexAlgoDefinitionBuilder()
            .setFlexAlgo(new FlexAlgo(Uint8.valueOf(128)))
            .setMetricType(FlexMetric.Delay)
            .setCalcType(Uint8.ONE)
            .setPriority(Uint8.TEN)
            .setFlexAlgoSubtlvs(fas)
            .build();
        assertEquals(fad, SrFlexAlgoParser.parseSrFlexAlgoDefinition(Unpooled.wrappedBuffer(tested)));
        final ByteBuf serializedData = Unpooled.buffer();
        SrFlexAlgoParser.serializeSrFlexAlgoDefinition(fad, serializedData);
        assertArrayEquals(tested, ByteArray.readAllBytes(serializedData));
    }
}
