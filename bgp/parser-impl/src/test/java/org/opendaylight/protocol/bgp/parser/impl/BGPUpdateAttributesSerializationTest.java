/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl;

import com.google.common.primitives.UnsignedInteger;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.impl.message.BGPUpdateMessageParser;
import org.opendaylight.protocol.bgp.parser.impl.message.update.AggregatorAttributeParser;
import org.opendaylight.protocol.bgp.parser.impl.message.update.AsPathAttributeParser;
import org.opendaylight.protocol.bgp.parser.impl.message.update.AtomicAggregateAttributeParser;
import org.opendaylight.protocol.bgp.parser.impl.message.update.CommunitiesAttributeParser;
import org.opendaylight.protocol.bgp.parser.impl.message.update.ExtendedCommunitiesAttributeParser;
import org.opendaylight.protocol.bgp.parser.impl.message.update.LocalPreferenceAttributeParser;
import org.opendaylight.protocol.bgp.parser.impl.message.update.MPUnreachAttributeParser;
import org.opendaylight.protocol.bgp.parser.impl.message.update.MultiExitDiscriminatorAttributeParser;
import org.opendaylight.protocol.bgp.parser.impl.message.update.NextHopAttributeParser;
import org.opendaylight.protocol.bgp.parser.impl.message.update.OriginAttributeParser;
import org.opendaylight.protocol.bgp.parser.impl.message.update.OriginatorIdAttributeParser;
import org.opendaylight.protocol.bgp.parser.spi.AttributeSerializer;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.parser.spi.MessageUtil;
import org.opendaylight.protocol.bgp.parser.spi.NlriRegistry;
import org.opendaylight.protocol.bgp.parser.spi.pojo.ServiceLoaderBGPExtensionProviderContext;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.ReferenceCache;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.AtomicAggregate;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.AtomicAggregateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.Communities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.CommunitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.ExtendedCommunities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.ExtendedCommunitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.LocalPref;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.LocalPrefBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.MultiExitDisc;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.MultiExitDiscBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.Origin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.OriginBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpOrigin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv4NextHopCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv4NextHopCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv6NextHopCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv6NextHopCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.ipv4.next.hop._case.Ipv4NextHopBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.ipv6.next.hop._case.Ipv6NextHopBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import static org.junit.Assert.assertEquals;

public class BGPUpdateAttributesSerializationTest {

    private ByteBuf byteAggregator;
    private final BGPExtensionProviderContext context = ServiceLoaderBGPExtensionProviderContext.getSingletonInstance();
    private final ReferenceCache referenceCache = context.getReferenceCache();
    private final NlriRegistry nlriReg = context.getNlriRegistry();

    static final List<byte[]> inputBytes = new ArrayList<byte[]>();
    private static BGPUpdateMessageParser updateParser = new BGPUpdateMessageParser(ServiceLoaderBGPExtensionProviderContext.getSingletonInstance().getAttributeRegistry());
    private Update message;

    private static int COUNTER = 17;

    private static int MAX_SIZE = 300;


    @Before
    public void setupUpdateMessage() throws Exception{

        for (int i = 1; i <= COUNTER; i++) {
            final String name = "/up" + i + ".bin";
            final InputStream is = BGPParserTest.class.getResourceAsStream(name);
            if (is == null) {
                throw new IOException("Failed to get resource " + name);
            }

            final ByteArrayOutputStream bis = new ByteArrayOutputStream();
            final byte[] data = new byte[MAX_SIZE];
            int nRead = 0;
            while ((nRead = is.read(data, 0, data.length)) != -1) {
                bis.write(data, 0, nRead);
            }
            bis.flush();

            inputBytes.add(bis.toByteArray());
        }


    }

    private void readUpdateMesageFromList (int listIndex) throws BGPDocumentedException {
        final byte[] body = ByteArray.cutBytes(inputBytes.get(listIndex), MessageUtil.COMMON_HEADER_LENGTH);
        final int messageLength = ByteArray.bytesToInt(ByteArray.subByte(inputBytes.get(listIndex), MessageUtil.MARKER_LENGTH,
                MessageUtil.LENGTH_FIELD_LENGTH));
        message =  BGPUpdateAttributesSerializationTest.updateParser.parseMessageBody(Unpooled.copiedBuffer(body), messageLength);
    }

    @Test
    public void testOriginatorId() throws BGPDocumentedException {
        readUpdateMesageFromList(1);
        OriginatorIdAttributeParser originatorIdAttributeParser = new OriginatorIdAttributeParser();
    }
    @Test
    public void testPathAttributesSerialization() throws BGPDocumentedException {
        readUpdateMesageFromList(0);

        AtomicAggregateAttributeParser atomicAggregateAttributeParser = new AtomicAggregateAttributeParser();
        serialize(atomicAggregateAttributeParser,message.getPathAttributes().getAtomicAggregate());
        assertEquals("",asHexDump());

        CommunitiesAttributeParser communitiesAttributeParser = new CommunitiesAttributeParser(referenceCache);
        for (int i=0;i<message.getPathAttributes().getCommunities().size();i++) {
            serialize(communitiesAttributeParser, message.getPathAttributes().getCommunities().get(i));
            assertEquals("0000ffff",asHexDump());
        }

        MultiExitDiscriminatorAttributeParser multiExitDiscriminatorAttributeParser = new MultiExitDiscriminatorAttributeParser();
        serialize(multiExitDiscriminatorAttributeParser,message.getPathAttributes().getMultiExitDisc());
        assertEquals("00000000",asHexDump());


/*
        OriginatorIdAttributeParser originatorIdAttributeParser = new OriginatorIdAttributeParser();
        serialize(originatorIdAttributeParser,message.getPathAttributes().getOriginatorId());
*/

        readUpdateMesageFromList(3);
        LocalPreferenceAttributeParser localPreferenceAttributeParser = new LocalPreferenceAttributeParser();
        serialize(localPreferenceAttributeParser, message.getPathAttributes().getLocalPref());
        assertEquals("0064",asHexDump());


    }

    @Test
    public void testAsPathAttributeSerialization() throws BGPDocumentedException{
        AsPathAttributeParser asPathAttributeParser = new AsPathAttributeParser(referenceCache);
        readUpdateMesageFromList(0);
        serialize(asPathAttributeParser,message.getPathAttributes().getAsPath());
        assertEquals("0201fdea",asHexDump());
    }
    @Test
    public void testAggregatorAttributeParser() throws BGPDocumentedException {
        AggregatorAttributeParser aggregatorAttributeParser = new AggregatorAttributeParser(referenceCache);
        readUpdateMesageFromList(2);
        serialize(aggregatorAttributeParser,message.getPathAttributes().getAggregator());
        assertEquals("001e0a000009",asHexDump());
    }
    @Test
    public void testOriginAttributeSerialization() throws BGPDocumentedException {
        readUpdateMesageFromList(0);
        OriginAttributeParser originAttributeParser = new OriginAttributeParser();

        for (int i=0;i<3;i++){
            Origin origin =  new OriginBuilder().setValue(BgpOrigin.forValue(i)).build();
            serialize(originAttributeParser,origin);
            assertEquals(String.format("%02d",i), ByteBufUtil.hexDump(byteAggregator));
        }
    }

    @Test
    public void testOriginatorIdAttributeParser(){
        OriginatorIdAttributeParser originatorIdAttributeParser = new OriginatorIdAttributeParser();
    }

    @Test
    public void testNextHopAttributeParser() throws BGPDocumentedException {
        readUpdateMesageFromList(0);
        final NextHopAttributeParser nextHopAttributeParser = new NextHopAttributeParser();
        final Ipv4NextHopCase nextHop = new Ipv4NextHopCaseBuilder().setIpv4NextHop(
                new Ipv4NextHopBuilder().setGlobal(new Ipv4Address("10.0.0.2")).build()).build();
        serialize(nextHopAttributeParser,nextHop.getIpv4NextHop());
        assertEquals("0a000002", asHexDump());

    }
    @Test
    public void testMultiExitDiscriminatorAttributeParser() throws BGPDocumentedException {
        readUpdateMesageFromList(0);
        MultiExitDiscriminatorAttributeParser multiExitDiscriminatorAttributeParser = new MultiExitDiscriminatorAttributeParser();
        MultiExitDisc multiExitDisc = new MultiExitDiscBuilder().setMed(new Long(42)).build();
        serialize(multiExitDiscriminatorAttributeParser,multiExitDisc);
        assertEquals("0000002a", asHexDump());
    }
    @Test
    public void testLocalLocalPreferenceAttributeParser() throws BGPDocumentedException {
        readUpdateMesageFromList(3);
        LocalPreferenceAttributeParser localPreferenceAttributeParser = new LocalPreferenceAttributeParser();
        LocalPref localPref = new LocalPrefBuilder().setPref(UnsignedInteger.valueOf("4294967295").longValue()).build();
        serialize(localPreferenceAttributeParser,localPref);
        assertEquals("ffff", asHexDump());
    }

    @Test
    public void testAtomicAggregateAttributeParser() throws BGPDocumentedException {
        readUpdateMesageFromList(0);
        AtomicAggregateAttributeParser atomicAggregateAttributeParser = new AtomicAggregateAttributeParser();
        AtomicAggregate atomicAggregate = new AtomicAggregateBuilder().build();
        serialize(atomicAggregateAttributeParser,atomicAggregate);
        assertEquals("",asHexDump());
    }

    @Test
    public void testExtendedCommunitiesAttributeParser() throws BGPDocumentedException {
        readUpdateMesageFromList(3);
        ExtendedCommunitiesAttributeParser extendedCommunitiesAttributeParser = new ExtendedCommunitiesAttributeParser(referenceCache);
        ExtendedCommunities extendedCommunities = new ExtendedCommunitiesBuilder().setCommType((short) 10).setCommSubType((short)20).build();
        serialize(extendedCommunitiesAttributeParser,extendedCommunities);
        assertEquals("000a0014",asHexDump());
    }
    @Test
    public void testCommunitiesAttributeParser() throws BGPDocumentedException {
        readUpdateMesageFromList(0);
        CommunitiesAttributeParser communitiesAttributeParser = new CommunitiesAttributeParser(referenceCache);
        AsNumber asNumber = new AsNumber(new Long(100));
        Communities communities = new CommunitiesBuilder().setAsNumber(asNumber).setSemantics(new Integer(100)).build();
        serialize(communitiesAttributeParser,communities);
        assertEquals("00000064",asHexDump());
    }

    //@Test
    public void testMPUnreachAttributeParser(){

        final Ipv6NextHopCase nextHop = new Ipv6NextHopCaseBuilder().setIpv6NextHop(
                new Ipv6NextHopBuilder().setGlobal(new Ipv6Address("2001:db8::1")).setLinkLocal(new Ipv6Address("fe80::c001:bff:fe7e:0")).build()).build();


        MPUnreachAttributeParser mpUnreachAttributeParser = new MPUnreachAttributeParser(nlriReg);

        final MpReachNlriBuilder mpBuilder = new MpReachNlriBuilder();
        mpBuilder.setAfi(Ipv6AddressFamily.class);
        mpBuilder.setSafi(UnicastSubsequentAddressFamily.class);
        mpBuilder.setCNextHop(nextHop);

        serialize(mpUnreachAttributeParser,mpBuilder.build());
        assertEquals("", asHexDump());
    }
    private void serialize(AttributeSerializer serializer,DataObject dataObject){
        byteAggregator = Unpooled.buffer(0);
        serializer.serializeAttribute(dataObject,byteAggregator);
    }

    private String asHexDump(){
        return ByteBufUtil.hexDump(this.byteAggregator);
    }

}
