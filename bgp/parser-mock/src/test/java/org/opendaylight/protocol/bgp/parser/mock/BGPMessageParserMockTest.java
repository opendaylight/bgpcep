/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.mock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv6.prefixes.DestinationIpv6Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv6.prefixes.destination.ipv6.Ipv6PrefixesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationIpv6CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.ProtocolVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.UpdateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.BgpParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.bgp.parameters.OptionalCapabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.bgp.parameters.OptionalCapabilitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.bgp.parameters.optional.capabilities.CParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.bgp.parameters.optional.capabilities.CParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.AsPathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.OriginBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.as.path.Segments;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.as.path.SegmentsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.AttributesReachBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.CParameters1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.CParameters1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.reach.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.reach.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.MultiprotocolCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.BgpOrigin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.MplsLabeledVpnSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.UnicastSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.next.hop.CNextHop;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.next.hop.c.next.hop.Ipv6NextHopCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.next.hop.c.next.hop.ipv6.next.hop._case.Ipv6NextHopBuilder;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint8;

public class BGPMessageParserMockTest {

    private final byte[][] inputBytes = new byte[11][];
    private final List<Update> messages = new ArrayList<>();

    @Before
    public void init() throws Exception {
        // Creating input bytes and update messages
        for (int i = 0; i < inputBytes.length; i++) {
            inputBytes[i] = fillInputBytes(i);
            messages.add(fillMessages(i));
        }
    }

    /**
     * Test if mock implementation of parser returns correct message.
     */
    @Test
    public void testGetUpdateMessage() throws BGPParsingException, BGPDocumentedException {
        final Map<ByteBuf, Notification<?>> updateMap = new HashMap<>();
        for (int i = 0; i < inputBytes.length; i++) {
            updateMap.put(Unpooled.copiedBuffer(inputBytes[i]), messages.get(i));
        }
        final BGPMessageParserMock mockParser = new BGPMessageParserMock(updateMap);

        for (int i = 0; i < inputBytes.length; i++) {
            assertEquals(messages.get(i),
                    mockParser.parseMessage(Unpooled.copiedBuffer(inputBytes[i]), null));
        }
        assertNotSame(messages.get(3),
                mockParser.parseMessage(Unpooled.copiedBuffer(inputBytes[8]), null));
    }

    /**
     * Test if method throws IllegalArgumentException after finding no BGPUpdateMessage
     * associated with given byte[] key.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testGetUpdateMessageException() throws BGPParsingException, BGPDocumentedException {
        final Map<ByteBuf, Notification<?>> updateMap = new HashMap<>();
        for (int i = 0; i < inputBytes.length; i++) {
            updateMap.put(Unpooled.copiedBuffer(inputBytes[i]), messages.get(i));
        }
        final BGPMessageParserMock mockParser = new BGPMessageParserMock(updateMap);
        mockParser.parseMessage(Unpooled.copiedBuffer(new byte[]{7, 4, 6}), null);
    }

    /**
     * Helper method to fill inputBytes variable.
     *
     * @param fileNumber parameter to distinguish between files from which bytes are read
     */
    private byte[] fillInputBytes(final int fileNumber) throws Exception {
        final ByteArrayOutputStream bis = new ByteArrayOutputStream();
        final byte[] data = new byte[60];
        int read;
        try (InputStream is = this.getClass().getResourceAsStream("/up" + fileNumber + ".bin")) {
            while ((read = is.read(data, 0, data.length)) != -1) {
                bis.write(data, 0, read);
            }
            bis.flush();
            is.close();
        }
        return bis.toByteArray();
    }

    /**
     * Helper method to fill messages variable.
     *
     * @param asn this parameter is passed to ASNumber constructor
     */
    private static Update fillMessages(final long asn) {

        final UpdateBuilder builder = new UpdateBuilder();

        final List<Segments> asPath = new ArrayList<>();
        asPath.add(new SegmentsBuilder().setAsSequence(Lists.newArrayList(new AsNumber(Uint32.valueOf(asn)))).build());
        final CNextHop nextHop = new Ipv6NextHopCaseBuilder().setIpv6NextHop(
                new Ipv6NextHopBuilder().setGlobal(new Ipv6AddressNoZone("2001:db8::1"))
                        .setLinkLocal(new Ipv6AddressNoZone("fe80::c001:bff:fe7e:0")).build()).build();

        final Ipv6Prefix pref1 = new Ipv6Prefix("2001:db8:1:2::/64");
        final Ipv6Prefix pref2 = new Ipv6Prefix("2001:db8:1:1::/64");
        final Ipv6Prefix pref3 = new Ipv6Prefix("2001:db8:1::/64");

        final AttributesBuilder paBuilder = new AttributesBuilder();
        paBuilder.setOrigin(new OriginBuilder().setValue(BgpOrigin.Igp).build());
        paBuilder.setAsPath(new AsPathBuilder().setSegments(asPath).build());

        final MpReachNlriBuilder mpReachBuilder = new MpReachNlriBuilder()
            .setAfi(Ipv6AddressFamily.VALUE)
            .setSafi(UnicastSubsequentAddressFamily.VALUE)
            .setCNextHop(nextHop)
            .setAdvertizedRoutes(new AdvertizedRoutesBuilder().setDestinationType(
                new DestinationIpv6CaseBuilder().setDestinationIpv6(
                        new DestinationIpv6Builder().setIpv6Prefixes(Lists.newArrayList(
                                new Ipv6PrefixesBuilder().setPrefix(pref1).build(),
                                new Ipv6PrefixesBuilder().setPrefix(pref2).build(),
                                new Ipv6PrefixesBuilder().setPrefix(pref3).build())).build()).build()).build());

        paBuilder.addAugmentation(new AttributesReachBuilder().setMpReachNlri(mpReachBuilder.build()).build());

        builder.setAttributes(paBuilder.build());

        return builder.build();
    }

    @Test
    public void testGetOpenMessage() throws BGPParsingException, BGPDocumentedException {
        final Map<ByteBuf, Notification<?>> openMap = new HashMap<>();

        final Set<BgpTableType> type = new HashSet<>();
        type.add(new BgpTableTypeImpl(Ipv4AddressFamily.VALUE, MplsLabeledVpnSubsequentAddressFamily.VALUE));

        final List<BgpParameters> params = new ArrayList<>();

        final CParameters par = new CParametersBuilder().addAugmentation(new CParameters1Builder()
                .setMultiprotocolCapability(new MultiprotocolCapabilityBuilder().setAfi(Ipv4AddressFamily.VALUE)
                        .setSafi(MplsLabeledVpnSubsequentAddressFamily.VALUE).build()).build()).build();
        params.add(new BgpParametersBuilder().setOptionalCapabilities(Lists.newArrayList(
                new OptionalCapabilitiesBuilder().setCParameters(par).build())).build());

        final byte[] input = new byte[]{5, 8, 13, 21};

        openMap.put(Unpooled.copiedBuffer(input), new OpenBuilder()
            .setMyAsNumber(Uint16.valueOf(30))
            .setHoldTimer(Uint16.valueOf(30))
            .setBgpParameters(params)
            .setVersion(new ProtocolVersion(Uint8.valueOf(4)))
            .build());

        final BGPMessageParserMock mockParser = new BGPMessageParserMock(openMap);

        final Set<BgpTableType> result = new HashSet<>();
        for (final BgpParameters p : ((Open) mockParser.parseMessage(Unpooled.copiedBuffer(input), null))
                .getBgpParameters()) {
            for (final OptionalCapabilities capa : p.getOptionalCapabilities()) {
                final CParameters cp = capa.getCParameters();
                if (cp.augmentation(CParameters1.class) != null && cp.augmentation(CParameters1.class)
                        .getMultiprotocolCapability() != null) {
                    final BgpTableType t = new BgpTableTypeImpl(cp.augmentation(CParameters1.class)
                            .getMultiprotocolCapability().getAfi(),
                            cp.augmentation(CParameters1.class).getMultiprotocolCapability().getSafi());
                    result.add(t);
                }
            }
        }
        assertEquals(type, result);
    }
}
