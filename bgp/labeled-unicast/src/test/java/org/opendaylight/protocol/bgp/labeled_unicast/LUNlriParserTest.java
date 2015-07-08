package org.opendaylight.protocol.bgp.labeled_unicast;

import static org.junit.Assert.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.opendaylight.protocol.bgp.labeled_unicast.LUNlriParser;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.labeled.unicast.destination.CLabeledUnicastDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.labeled.unicast.destination.CLabeledUnicastDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.labeled.unicast.destination.c.labeled.unicast.destination.LabelStack;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.labeled.unicast.destination.c.labeled.unicast.destination.LabelStackBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.labeled.unicast.destination.c.labeled.unicast.destination.prefix.type.Ipv4PrefixCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationLabeledUnicastCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationLabeledUnicastCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.labeled.unicast._case.DestinationLabeledUnicast;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.labeled.unicast._case.DestinationLabeledUnicastBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;

public class LUNlriParserTest {
    //NLRI packet info
    //Label Stack = 354(bottom) IPV4 = 34.1.21.0/24
    //Label Stack = 355(bottom) IPV4 = 34.1.22.0/24
    //Label Stack = 356(bottom) IPV4 = 34.1.23.0/24
    //Label Stack = 358(bottom) IPV4 = 34.1.24.0/24
    //Label Stack = 359(bottom) IPV4 = 34.1.25.0/24
    //Label Stack = 360(bottom) IPV4 = 34.1.26.0/24
    //Label Stack = 361(bottom) IPV4 = 34.1.27.0/24
    //Label Stack = 363(bottom) IPV4 = 34.1.28.0/24
    //Label Stack = 364(bottom) IPV4 = 34.1.29.0/24
    //Label Stack = 365(bottom) IPV4 = 34.1.30.0/24
    private static final byte[] nlriParser = new byte[] {(byte) 0x30, (byte) 0x00, (byte) 0x16, (byte) 0x21,
        (byte) 0x22, (byte) 0x01, (byte) 0x15, (byte) 0x30, (byte) 0x00, (byte) 0x16, (byte) 0x31, (byte) 0x22, (byte) 0x01, (byte) 0x16, (byte) 0x30, (byte) 0x00, (byte) 0x16, (byte) 0x41, (byte) 0x22, (byte) 0x01,
        (byte) 0x17, (byte) 0x30, (byte) 0x00, (byte) 0x16, (byte) 0x61, (byte) 0x22, (byte) 0x01, (byte) 0x18, (byte) 0x30, (byte) 0x00, (byte) 0x16, (byte) 0x71, (byte) 0x22, (byte) 0x01, (byte) 0x19, (byte) 0x30,
        (byte) 0x00, (byte) 0x16, (byte) 0x81, (byte) 0x22, (byte) 0x01, (byte) 0x1a, (byte) 0x30, (byte) 0x00, (byte) 0x16, (byte) 0x91, (byte) 0x22, (byte) 0x01, (byte) 0x1b, (byte) 0x30, (byte) 0x00, (byte) 0x16,
        (byte) 0xb1, (byte) 0x22, (byte) 0x01, (byte) 0x1c, (byte) 0x30, (byte) 0x00, (byte) 0x16, (byte) 0xc1, (byte) 0x22, (byte) 0x01, (byte) 0x1d, (byte) 0x30, (byte) 0x00, (byte) 0x16, (byte) 0xd1, (byte) 0x22,
        (byte) 0x01, (byte) 0x1e };
    private static final byte[] nlriSerilize = new byte[] {(byte) 0x30, (byte) 0x00, (byte) 0x16, (byte) 0x21,
        (byte) 0x22, (byte) 0x01, (byte) 0x15};
    private CLabeledUnicastDestination dest;
    private void setUp(final byte[] data) throws BGPParsingException {
        final LUNlriParser parser = new LUNlriParser();
        final MpReachNlriBuilder mpBuilder = new MpReachNlriBuilder();
        mpBuilder.setAfi(Ipv4AddressFamily.class);
        assertEquals(true, Unpooled.copiedBuffer(data).isReadable());
        assertEquals(Ipv4AddressFamily.class, mpBuilder.getAfi());
        parser.parseNlri(Unpooled.copiedBuffer(data), mpBuilder);

        final DestinationLabeledUnicast lu = ((DestinationLabeledUnicastCase) mpBuilder.getAdvertizedRoutes().getDestinationType()).getDestinationLabeledUnicast();
        assertEquals(10, lu.getCLabeledUnicastDestination().size());
        this.dest = lu.getCLabeledUnicastDestination().get(0);
    }
    @Test
    public void testLUNlriParser() throws Exception {
        setUp(this.nlriParser);
        int a = this.dest.getLength();
        assertEquals(48, a);
        assertEquals(1, this.dest.getLabelStack().size());
        int labelVal = this.dest.getLabelStack().get(0).getLabelValue();
        assertEquals(354, labelVal);
        final CLabeledUnicastDestinationBuilder dest1 = new CLabeledUnicastDestinationBuilder();
        dest1.setPrefixType(new Ipv4PrefixCaseBuilder().setIpv4Prefix(new Ipv4Prefix("34.1.21.0/24")).build());
        assertEquals(dest1.getPrefixType(), this.dest.getPrefixType());
    }
//    @Test
//    public void testExtractLabeledUnicast() {
//    Collection<UnkeyedListNode> labels = new ArrayList<>();
//        UnkeyedListNode e;
//        labels.add(e);
//
//
//        final DataContainerNodeAttrBuilder<NodeIdentifierWithPredicates, MapEntryNode> entry = Builders.mapEntryBuilder();
//        entry.withNodeIdentifier(new NodeIdentifierWithPredicates(CLabeledUnicastDestination.QNAME, CLabeledUnicastDestination.QNAME, entry));
//        entry.withChild(Builders.leafBuilder().withNodeIdentifier(LUNlriParser.LENGTH_NID).withValue(48).build());
////        entry.withChild(Builders.unkeyedListBuilder().withNodeIdentifier(LUNlriParser.LABEL_STACK_NID).withValue(labels));
////        entry.withChild(Builders.unkeyedListBuilder().withNodeIdentifier(new NodeIdentifier(LabelStack.QNAME))
////            .withChild(Builders.leafBuilder().withNodeIdentifier(LUNlriParser.LABEL_NID).withValue(354).build()).build());
//        entry.withChild(Builders.choiceBuilder().withNodeIdentifier(new NodeIdentifier(PrefixType.QNAME))
//            .withChild(Builders.leafBuilder().withNodeIdentifier(new NodeIdentifier(Ipv4PrefixCase.QNAME)).withValue("34.1.21.0/24").build()).build());
//
//
//    }
    @Test
    public void testLUNlriSerializer() {
        LUNlriParser parser = new LUNlriParser();
        ByteBuf buffer = Unpooled.buffer();
        List<CLabeledUnicastDestination> dests = new ArrayList<>();
        CLabeledUnicastDestinationBuilder builder = new CLabeledUnicastDestinationBuilder();
        LabelStackBuilder label1 = new LabelStackBuilder();
        List<LabelStack> labels= new ArrayList<>();
        labels.add(label1.setLabelValue(354).build());

        builder.setLength((short) 48);
        builder.setLabelStack(labels);
        builder.setPrefixType(new Ipv4PrefixCaseBuilder().setIpv4Prefix(new Ipv4Prefix("34.1.21.0/24")).build());
        dests.add(builder.build());

        MpReachNlriBuilder mp = new MpReachNlriBuilder();
        mp.setAfi(Ipv4AddressFamily.class);
        mp.setAdvertizedRoutes(new AdvertizedRoutesBuilder().setDestinationType(new DestinationLabeledUnicastCaseBuilder().setDestinationLabeledUnicast(new DestinationLabeledUnicastBuilder().setCLabeledUnicastDestination(dests).build()).build()).build());
        parser.serializeAttribute(new AttributesBuilder().addAugmentation(Attributes1.class, new Attributes1Builder().setMpReachNlri(mp.build()).build()).build(), buffer);

        assertArrayEquals(this.nlriSerilize, ByteArray.readAllBytes(buffer));
    }

}
