/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.ComponentType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.NumericOperand;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.Flowspec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.FlowspecBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.DestinationPrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.DestinationPrefixCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.PortCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.PortCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.ProtocolIpCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.ProtocolIpCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.SourcePrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.SourcePrefixCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.destination.prefix._case.DestinationPrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.port._case.PortBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.port._case.port.Ports;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.port._case.port.PortsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.protocol.ip._case.ProtocolIpBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.protocol.ip._case.protocol.ip.ProtocolIps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.protocol.ip._case.protocol.ip.ProtocolIpsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.source.prefix._case.SourcePrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.update.path.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationFlowspecCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.update.path.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationFlowspecCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.update.path.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.flowspec._case.DestinationFlowspecBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.PathAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.PathAttributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.PathAttributes1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.mp.reach.nlri.AdvertizedRoutesBuilder;

public class FSNlriParserTest {

    private static final byte[] nlri = new byte[] { 0x13, 01, 0x18, 0x0a, 00, 01, 02, 0x08, (byte) 0xc0, 03, (byte) 0x81, 06, 04, 03, (byte) 0x89, 0x45, (byte) 0x8b, (byte) 0x91, 0x1f, (byte) 0x90 };

    @Test
    public void testParseLength() {
        // 00-00-0000 = 1
        assertEquals(1, FSNlriParser.parseLength((byte)0x00));
        // 00-01-0000 = 2
        assertEquals(2, FSNlriParser.parseLength((byte)16));
        // 00-10-0000 = 4
        assertEquals(4, FSNlriParser.parseLength((byte)32));
        // 00-11-0000 = 8
        assertEquals(8, FSNlriParser.parseLength((byte)48));
    }


    @Test
    public void testParseNlri() throws BGPParsingException {
        final List<Flowspec> fs = new ArrayList<>();
        final MpReachNlriBuilder mp = new MpReachNlriBuilder();

        final FlowspecBuilder builder = new FlowspecBuilder();
        builder.setComponentType(ComponentType.DestinationPrefix);
        final DestinationPrefixCase destinationPrefix = new DestinationPrefixCaseBuilder().setDestinationPrefix(new DestinationPrefixBuilder().setDestinationPrefix(new Ipv4Prefix("10.0.1.0/24")).build()).build();
        builder.setFlowspecType(destinationPrefix);
        fs.add(builder.build());
        builder.setComponentType(ComponentType.SourcePrefix);
        final SourcePrefixCase sourcePrefix = new SourcePrefixCaseBuilder().setSourcePrefix(new SourcePrefixBuilder().setSourcePrefix(new Ipv4Prefix("192.0.0.0/8")).build()).build();
        builder.setFlowspecType(sourcePrefix);
        fs.add(builder.build());

        builder.setComponentType(ComponentType.ProtocolIp);
        final List<ProtocolIps> protocols = Lists.newArrayList(new ProtocolIpsBuilder().setOp(new NumericOperand(false, true, true, false, false)).setValue(6).build());
        final ProtocolIpCase prots = new ProtocolIpCaseBuilder().setProtocolIp(new ProtocolIpBuilder().setProtocolIps(protocols).build()).build();
        builder.setFlowspecType(prots);
        fs.add(builder.build());

        builder.setComponentType(ComponentType.Port);
        final List<Ports> ports = Lists.newArrayList(new PortsBuilder().setOp(new NumericOperand(false, false, true, true, false)).setValue(137).build(),
            new PortsBuilder().setOp(new NumericOperand(true, false, true, false, true)).setValue(139).build(),
            new PortsBuilder().setOp(new NumericOperand(false, true, true, false, false)).setValue(8080).build());
        final PortCase ps = new PortCaseBuilder().setPort(new PortBuilder().setPorts(ports).build()).build();
        builder.setFlowspecType(ps);
        fs.add(builder.build());

        mp.setAdvertizedRoutes(new AdvertizedRoutesBuilder().setDestinationType(new DestinationFlowspecCaseBuilder().setDestinationFlowspec(new DestinationFlowspecBuilder().setFlowspec(fs).build()).build()).build());

        final FSNlriParser parser = new FSNlriParser();

        final MpReachNlriBuilder result = new MpReachNlriBuilder();
        parser.parseNlri(Unpooled.wrappedBuffer(nlri), result);

        final List<Flowspec> flows = ((DestinationFlowspecCase)(result.getAdvertizedRoutes().getDestinationType())).getDestinationFlowspec().getFlowspec();
        assertEquals(4, flows.size());
        assertEquals(destinationPrefix, flows.get(0).getFlowspecType());
        assertEquals(sourcePrefix, flows.get(1).getFlowspecType());
        assertEquals(prots, flows.get(2).getFlowspecType());
        assertEquals(ps, flows.get(3).getFlowspecType());

        final ByteBuf buffer = Unpooled.buffer();
        parser.serializeAttribute(new PathAttributesBuilder().addAugmentation(PathAttributes1.class, new PathAttributes1Builder().setMpReachNlri(mp.build()).build()).build(), buffer);
        assertArrayEquals(nlri, ByteArray.readAllBytes(buffer));
    }
}
