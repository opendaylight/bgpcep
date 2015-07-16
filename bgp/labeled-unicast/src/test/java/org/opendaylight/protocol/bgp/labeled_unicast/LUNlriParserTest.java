/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.labeled.unicast.LabelStack;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.labeled.unicast.LabelStackBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.labeled.unicast.destination.CLabeledUnicastDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.labeled.unicast.destination.CLabeledUnicastDestinationBuilder;
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
    private static final byte[] nlriParser = new byte[] {
        (byte) 0x30, (byte) 0x00, (byte) 0x16, (byte) 0x21, (byte) 0x22, (byte) 0x01, (byte) 0x15,
        (byte) 0x30, (byte) 0x00, (byte) 0x16, (byte) 0x31, (byte) 0x22, (byte) 0x01, (byte) 0x16,
        (byte) 0x30, (byte) 0x00, (byte) 0x16, (byte) 0x41, (byte) 0x22, (byte) 0x01, (byte) 0x17,
        (byte) 0x30, (byte) 0x00, (byte) 0x16, (byte) 0x61, (byte) 0x22, (byte) 0x01, (byte) 0x18,
        (byte) 0x30, (byte) 0x00, (byte) 0x16, (byte) 0x71, (byte) 0x22, (byte) 0x01, (byte) 0x19,
        (byte) 0x30, (byte) 0x00, (byte) 0x16, (byte) 0x81, (byte) 0x22, (byte) 0x01, (byte) 0x1a,
        (byte) 0x30, (byte) 0x00, (byte) 0x16, (byte) 0x91, (byte) 0x22, (byte) 0x01, (byte) 0x1b,
        (byte) 0x30, (byte) 0x00, (byte) 0x16, (byte) 0xb1, (byte) 0x22, (byte) 0x01, (byte) 0x1c,
        (byte) 0x30, (byte) 0x00, (byte) 0x16, (byte) 0xc1, (byte) 0x22, (byte) 0x01, (byte) 0x1d,
        (byte) 0x30, (byte) 0x00, (byte) 0x16, (byte) 0xd1, (byte) 0x22, (byte) 0x01, (byte) 0x1e };
    private static final byte[] mlnlriParser = new byte[] {(byte) 0x60,
    	(byte) 0x00, (byte) 0x16, (byte) 0x30,
    	(byte) 0x00, (byte) 0x16, (byte) 0x40,
    	(byte) 0x00, (byte) 0x16, (byte) 0x51,
    	(byte) 0x22, (byte) 0x01, (byte) 0x16,
    };
    private static final byte[] nlriSerilize = new byte[] {(byte) 0x60,
    	(byte) 0x00, (byte) 0x16, (byte) 0x20,
    	(byte) 0x00, (byte) 0x16, (byte) 0x30,
    	(byte) 0x00, (byte) 0x16, (byte) 0x41,
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
    private void mlsetUp(final byte[] data) throws BGPParsingException {
        final LUNlriParser parser = new LUNlriParser();
        final MpReachNlriBuilder mpBuilder = new MpReachNlriBuilder();
        mpBuilder.setAfi(Ipv4AddressFamily.class);
        assertEquals(true, Unpooled.copiedBuffer(data).isReadable());
        assertEquals(Ipv4AddressFamily.class, mpBuilder.getAfi());
        parser.parseNlri(Unpooled.copiedBuffer(data), mpBuilder);

        final DestinationLabeledUnicast lu = ((DestinationLabeledUnicastCase) mpBuilder.getAdvertizedRoutes().getDestinationType()).getDestinationLabeledUnicast();
        assertEquals(1, lu.getCLabeledUnicastDestination().size());
        this.dest = lu.getCLabeledUnicastDestination().get(0);
    }

	@SuppressWarnings("static-access")
	@Test
    public void testLUNlriParser() throws Exception {
        setUp(this.nlriParser);
        assertEquals(1, this.dest.getLabelStack().size());
        long labelVal = this.dest.getLabelStack().get(0).getLabelValue();
        assertEquals(354, labelVal);

        final CLabeledUnicastDestinationBuilder dest1 = new CLabeledUnicastDestinationBuilder();
        dest1.setPrefixType(new IpPrefix(new Ipv4Prefix("34.1.21.0/24")));
        assertEquals(dest1.getPrefixType(), this.dest.getPrefixType());
    }

	@SuppressWarnings("static-access")
	@Test
    public void testMLLUNlriParser() throws Exception {
    	mlsetUp(this.mlnlriParser);
    	assertEquals(3, this.dest.getLabelStack().size());
    	long labelVal1 = this.dest.getLabelStack().get(0).getLabelValue();
    	assertEquals(355, labelVal1);
    	long labelVal2 = this.dest.getLabelStack().get(1).getLabelValue();
    	assertEquals(356, labelVal2);
    	long labelVal3 = this.dest.getLabelStack().get(2).getLabelValue();
    	assertEquals(357, labelVal3);

    	final CLabeledUnicastDestinationBuilder mldest = new CLabeledUnicastDestinationBuilder();
    	mldest.setPrefixType(new IpPrefix(new Ipv4Prefix("34.1.22.0/24")));
    	assertEquals(mldest.getPrefixType(), this.dest.getPrefixType());
    }

	@SuppressWarnings("static-access")
	@Test
    public void testLUNlriSerializer() {
        LUNlriParser parser = new LUNlriParser();
        ByteBuf buffer = Unpooled.buffer();
        List<CLabeledUnicastDestination> dests = new ArrayList<>();
        CLabeledUnicastDestinationBuilder builder = new CLabeledUnicastDestinationBuilder();
        LabelStackBuilder label1 = new LabelStackBuilder();
        List<LabelStack> labels= new ArrayList<>();
        labels.add(label1.setLabelValue(354L).build());
        labels.add(label1.setLabelValue(355L).build());
        labels.add(label1.setLabelValue(356L).build());
        builder.setLabelStack(labels);
        builder.setPrefixType(new IpPrefix(new Ipv4Prefix("34.1.21.0/24")));
        dests.add(builder.build());

        final MpReachNlriBuilder mp = new MpReachNlriBuilder();
        mp.setAfi(Ipv4AddressFamily.class);
        mp.setAdvertizedRoutes(new AdvertizedRoutesBuilder().setDestinationType(new DestinationLabeledUnicastCaseBuilder().setDestinationLabeledUnicast(new DestinationLabeledUnicastBuilder().setCLabeledUnicastDestination(dests).build()).build()).build());
        parser.serializeAttribute(new AttributesBuilder().addAugmentation(Attributes1.class, new Attributes1Builder().setMpReachNlri(mp.build()).build()).build(), buffer);
        assertArrayEquals(this.nlriSerilize, ByteArray.readAllBytes(buffer));
    }
}
