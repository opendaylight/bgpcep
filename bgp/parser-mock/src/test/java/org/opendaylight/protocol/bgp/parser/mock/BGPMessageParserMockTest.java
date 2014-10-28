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
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.ProtocolVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.UpdateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.BgpParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.bgp.parameters.OptionalCapabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.bgp.parameters.OptionalCapabilitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.bgp.parameters.optional.capabilities.CParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.AsPathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.OriginBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.as.path.Segments;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.as.path.SegmentsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.PathAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.PathAttributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.PathAttributes1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.destination.destination.type.DestinationIpv6CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.destination.destination.type.destination.ipv6._case.DestinationIpv6Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.open.bgp.parameters.optional.capabilities.c.parameters.MultiprotocolCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.open.bgp.parameters.optional.capabilities.c.parameters.MultiprotocolCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.open.bgp.parameters.optional.capabilities.c.parameters.multiprotocol._case.MultiprotocolCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpOrigin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.MplsLabeledVpnSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.c.segment.AListCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.c.segment.a.list._case.AListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.c.segment.a.list._case.a.list.AsSequence;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.c.segment.a.list._case.a.list.AsSequenceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.CNextHop;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv6NextHopCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.ipv6.next.hop._case.Ipv6NextHopBuilder;
import org.opendaylight.yangtools.yang.binding.Notification;

public class BGPMessageParserMockTest {

    private final byte[][] inputBytes = new byte[11][];
    private final List<Update> messages = Lists.newArrayList();

    @Before
    public void init() throws Exception {
        // Creating input bytes and update messages
        for (int i = 0; i < this.inputBytes.length; i++) {
            this.inputBytes[i] = this.fillInputBytes(i);
            this.messages.add(this.fillMessages(i));
        }
    }

    /**
     * Test if mock implementation of parser returns correct message
     *
     * @throws BGPParsingException
     * @throws BGPDocumentedException
     * @throws IOException
     */
    @Test
    public void testGetUpdateMessage() throws BGPParsingException, BGPDocumentedException, IOException {
        final Map<ByteBuf, Notification> updateMap = Maps.newHashMap();
        for (int i = 0; i < this.inputBytes.length; i++) {
            updateMap.put(Unpooled.copiedBuffer(this.inputBytes[i]), this.messages.get(i));
        }
        final BGPMessageParserMock mockParser = new BGPMessageParserMock(updateMap);

        for (int i = 0; i < this.inputBytes.length; i++) {
            assertEquals(this.messages.get(i), mockParser.parseMessage(Unpooled.copiedBuffer(this.inputBytes[i])));
        }
        assertNotSame(this.messages.get(3), mockParser.parseMessage(Unpooled.copiedBuffer(this.inputBytes[8])));
    }

    /**
     * Test if method throws IllegalArgumentException after finding no BGPUpdateMessage associated with given byte[] key
     *
     * @throws BGPDocumentedException
     * @throws BGPParsingException
     * @throws IOException
     */
    @Test(expected = IllegalArgumentException.class)
    public void testGetUpdateMessageException() throws BGPParsingException, BGPDocumentedException, IOException {
        final Map<ByteBuf, Notification> updateMap = Maps.newHashMap();
        for (int i = 0; i < this.inputBytes.length; i++) {
            updateMap.put(Unpooled.copiedBuffer(this.inputBytes[i]), this.messages.get(i));
        }
        final BGPMessageParserMock mockParser = new BGPMessageParserMock(updateMap);
        mockParser.parseMessage(Unpooled.copiedBuffer(new byte[] { 7, 4, 6 }));
    }

    /**
     * Helper method to fill inputBytes variable
     *
     * @param fileNumber parameter to distinguish between files from which bytes are read
     */
    private byte[] fillInputBytes(final int fileNumber) throws Exception {
        final ByteArrayOutputStream bis = new ByteArrayOutputStream();
        final byte[] data = new byte[60];
        int nRead = 0;
        try (final InputStream is = this.getClass().getResourceAsStream("/up" + fileNumber + ".bin")) {
            while ((nRead = is.read(data, 0, data.length)) != -1) {
                bis.write(data, 0, nRead);
            }
            bis.flush();
            is.close();
        }
        return bis.toByteArray();
    }

    /**
     * Helper method to fill messages variable
     *
     * @param asn this parameter is passed to ASNumber constructor
     */
    private Update fillMessages(final long asn) throws UnknownHostException {

        final UpdateBuilder builder = new UpdateBuilder();

        final List<AsSequence> asnums = Lists.newArrayList(new AsSequenceBuilder().setAs(new AsNumber(asn)).build());
        final List<Segments> asPath = Lists.newArrayList();
        asPath.add(new SegmentsBuilder().setCSegment(
            new AListCaseBuilder().setAList(new AListBuilder().setAsSequence(asnums).build()).build()).build());
        final CNextHop nextHop = new Ipv6NextHopCaseBuilder().setIpv6NextHop(
            new Ipv6NextHopBuilder().setGlobal(new Ipv6Address("2001:db8::1")).setLinkLocal(new Ipv6Address("fe80::c001:bff:fe7e:0")).build()).build();

        final Ipv6Prefix pref1 = new Ipv6Prefix("2001:db8:1:2::/64");
        final Ipv6Prefix pref2 = new Ipv6Prefix("2001:db8:1:1::/64");
        final Ipv6Prefix pref3 = new Ipv6Prefix("2001:db8:1::/64");

        final PathAttributesBuilder paBuilder = new PathAttributesBuilder();
        paBuilder.setOrigin(new OriginBuilder().setValue(BgpOrigin.Igp).build());
        paBuilder.setAsPath(new AsPathBuilder().setSegments(asPath).build());

        final MpReachNlriBuilder mpReachBuilder = new MpReachNlriBuilder();
        mpReachBuilder.setAfi(Ipv6AddressFamily.class);
        mpReachBuilder.setSafi(UnicastSubsequentAddressFamily.class);
        mpReachBuilder.setCNextHop(nextHop);
        mpReachBuilder.setAdvertizedRoutes(new AdvertizedRoutesBuilder().setDestinationType(
            new DestinationIpv6CaseBuilder().setDestinationIpv6(
                new DestinationIpv6Builder().setIpv6Prefixes(Lists.newArrayList(pref1, pref2, pref3)).build()).build()).build());

        paBuilder.addAugmentation(PathAttributes1.class, new PathAttributes1Builder().setMpReachNlri(mpReachBuilder.build()).build());

        builder.setPathAttributes(paBuilder.build());

        return builder.build();
    }

    @Test
    public void testGetOpenMessage() throws BGPParsingException, BGPDocumentedException, IOException {
        final Map<ByteBuf, Notification> openMap = Maps.newHashMap();

        final Set<BgpTableType> type = Sets.newHashSet();
        type.add(new BgpTableTypeImpl(Ipv4AddressFamily.class, MplsLabeledVpnSubsequentAddressFamily.class));

        final List<BgpParameters> params = Lists.newArrayList();

        final CParameters par = new MultiprotocolCaseBuilder().setMultiprotocolCapability(
            new MultiprotocolCapabilityBuilder().setAfi(Ipv4AddressFamily.class).setSafi(MplsLabeledVpnSubsequentAddressFamily.class).build()).build();
        params.add(new BgpParametersBuilder().setOptionalCapabilities(Lists.newArrayList(new OptionalCapabilitiesBuilder().setCParameters(par).build())).build());

        final byte[] input = new byte[] { 5, 8, 13, 21 };

        openMap.put(Unpooled.copiedBuffer(input), new OpenBuilder().setMyAsNumber(30).setHoldTimer(30).setBgpParameters(params).setVersion(
            new ProtocolVersion((short) 4)).build());

        final BGPMessageParserMock mockParser = new BGPMessageParserMock(openMap);

        final Set<BgpTableType> result = Sets.newHashSet();
        for (final BgpParameters p : ((Open) mockParser.parseMessage(Unpooled.copiedBuffer(input))).getBgpParameters()) {
            for (final OptionalCapabilities capa : p.getOptionalCapabilities()) {
                final CParameters cp = capa.getCParameters();
                final BgpTableType t = new BgpTableTypeImpl(((MultiprotocolCase) cp).getMultiprotocolCapability().getAfi(), ((MultiprotocolCase) cp).getMultiprotocolCapability().getSafi());
                result.add(t);
            }
        }
        assertEquals(type, result);
    }
}
