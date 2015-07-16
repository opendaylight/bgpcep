/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.labeled.unicast;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
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
    /*Two label stacks with multiple labels.
     *
     * label stack1:
     * 60       <- length 96
     * 00 16 3  <- labelValue 355
     * 0        <- etc&bottomBit 0
     * 00 16 4  <- labelValue 356
     * 0        <- etc&bottomBit 0
     * 00 16 6  <- labelValue 357
     * 1        <- bottomBit 1
     * 22 01 16 <- prefixType IPV4=34.1.22.0/24
     *
     * label stack2:
     * 30       <- length 48
     * 00 16 6  <- labelValue 358
     * 1        <- etc&bottomBit 1
     * 22 01 17 <- preifxType Ipv4=34.1.23.0/24
     */
    private static final byte[] nlri = new byte[] {
        (byte) 0x60,
        (byte) 0x00, (byte) 0x16, (byte) 0x30,
        (byte) 0x00, (byte) 0x16, (byte) 0x40,
        (byte) 0x00, (byte) 0x16, (byte) 0x51,
        (byte) 0x22, (byte) 0x01, (byte) 0x16,
        (byte) 0x30,
        (byte) 0x00, (byte) 0x16, (byte) 0x61,
        (byte) 0x22, (byte) 0x01, (byte) 0x17,
    };
    private CLabeledUnicastDestination dest1;
    private CLabeledUnicastDestination dest2;

    private void setUp(final byte[] data) throws BGPParsingException {
        final LUNlriParser parser = new LUNlriParser();
        final MpReachNlriBuilder mpBuilder = new MpReachNlriBuilder();
        mpBuilder.setAfi(Ipv4AddressFamily.class);
        assertEquals(true, Unpooled.copiedBuffer(data).isReadable());
        assertEquals(Ipv4AddressFamily.class, mpBuilder.getAfi());
        parser.parseNlri(Unpooled.copiedBuffer(data), mpBuilder);

        final DestinationLabeledUnicast lu = ((DestinationLabeledUnicastCase) mpBuilder.getAdvertizedRoutes().getDestinationType()).getDestinationLabeledUnicast();
        assertEquals(2, lu.getCLabeledUnicastDestination().size());
        this.dest1 = lu.getCLabeledUnicastDestination().get(0);
        this.dest2 = lu.getCLabeledUnicastDestination().get(1);
    }

    @Test
    public void testLUNlriParser() throws Exception {
        setUp(this.nlri);
        assertEquals(3, this.dest1.getLabelStack().size());
        long labelVal1 = this.dest1.getLabelStack().get(0).getLabelValue();
        assertEquals(355, labelVal1);
        long labelVal2 = this.dest1.getLabelStack().get(1).getLabelValue();
        assertEquals(356, labelVal2);
        long labelVal3 = this.dest1.getLabelStack().get(2).getLabelValue();
        assertEquals(357, labelVal3);
        final CLabeledUnicastDestinationBuilder dest = new CLabeledUnicastDestinationBuilder();
        dest.setPrefixType(new IpPrefix(new Ipv4Prefix("34.1.22.0/24")));
        assertEquals(dest.getPrefixType(), this.dest1.getPrefixType());

        assertEquals(1, this.dest2.getLabelStack().size());
        long labelVal4 = this.dest2.getLabelStack().get(0).getLabelValue();
        assertEquals(358, labelVal4);
        dest.setPrefixType(new IpPrefix(new Ipv4Prefix("34.1.23.0/24")));
        assertEquals(dest.getPrefixType(), this.dest2.getPrefixType());
    }

    @Test
    public void testLUNlriSerializer(){
        final LUNlriParser parser = new LUNlriParser();
        final ByteBuf buffer = Unpooled.buffer();
        final List<CLabeledUnicastDestination> dests = new ArrayList<>();
        final CLabeledUnicastDestinationBuilder builder = new CLabeledUnicastDestinationBuilder();
        final LabelStackBuilder label = new LabelStackBuilder();
        final List<LabelStack> labels1= new ArrayList<>();
        final List<LabelStack> labels2= new ArrayList<>();

        labels1.add(label.setLabelValue(355L).build());
        labels1.add(label.setLabelValue(356L).build());
        labels1.add(label.setLabelValue(357L).build());
        builder.setLabelStack(labels1);
        builder.setPrefixType(new IpPrefix(new Ipv4Prefix("34.1.22.0/24")));
        dests.add(builder.build());

        labels2.add(label.setLabelValue(358L).build());
        builder.setLabelStack(labels2);
        builder.setPrefixType(new IpPrefix(new Ipv4Prefix("34.1.23.0/24")));
        dests.add(builder.build());

        final MpReachNlriBuilder mp = new MpReachNlriBuilder();
        mp.setAfi(Ipv4AddressFamily.class);
        mp.setAdvertizedRoutes(new AdvertizedRoutesBuilder().setDestinationType(new DestinationLabeledUnicastCaseBuilder().setDestinationLabeledUnicast(new DestinationLabeledUnicastBuilder().setCLabeledUnicastDestination(dests).build()).build()).build());
        parser.serializeAttribute(new AttributesBuilder().addAugmentation(Attributes1.class, new Attributes1Builder().setMpReachNlri(mp.build()).build()).build(), buffer);
        assertArrayEquals(this.nlri, ByteArray.readAllBytes(buffer));
    }
}
