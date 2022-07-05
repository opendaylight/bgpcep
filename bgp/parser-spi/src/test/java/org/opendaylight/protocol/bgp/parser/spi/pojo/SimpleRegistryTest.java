/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi.pojo;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.Optional;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.BGPTreatAsWithdrawException;
import org.opendaylight.protocol.bgp.parser.spi.AddressFamilyRegistry;
import org.opendaylight.protocol.bgp.parser.spi.AttributeRegistry;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionConsumerContext;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.parser.spi.BgpPrefixSidTlvRegistry;
import org.opendaylight.protocol.bgp.parser.spi.CapabilityRegistry;
import org.opendaylight.protocol.bgp.parser.spi.MessageRegistry;
import org.opendaylight.protocol.bgp.parser.spi.MultiPathSupport;
import org.opendaylight.protocol.bgp.parser.spi.NlriRegistry;
import org.opendaylight.protocol.bgp.parser.spi.ParameterRegistry;
import org.opendaylight.protocol.bgp.parser.spi.PeerSpecificParserConstraint;
import org.opendaylight.protocol.bgp.parser.spi.RevisedErrorHandling;
import org.opendaylight.protocol.bgp.parser.spi.SubsequentAddressFamilyRegistry;
import org.opendaylight.protocol.bgp.parser.spi.pojo.BgpTestActivator.MockNotification;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.bgp.prefix.sid.bgp.prefix.sid.tlvs.BgpPrefixSidTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.reach.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.reach.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.unreach.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.unreach.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.UnicastSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.next.hop.c.next.hop.Ipv4NextHopCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.next.hop.c.next.hop.ipv4.next.hop._case.Ipv4NextHopBuilder;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.Notification;

public class SimpleRegistryTest {
    private static final MultiPathSupport ADD_PATH_SUPPORT = tableType -> true;

    private static final PeerSpecificParserConstraint CONSTRAINT;

    static {
        PeerSpecificParserConstraintImpl peerConstraint = new PeerSpecificParserConstraintImpl();
        peerConstraint.addPeerConstraint(MultiPathSupport.class, ADD_PATH_SUPPORT);
        CONSTRAINT = peerConstraint;
    }

    protected BGPExtensionConsumerContext ctx;

    private final BgpTestActivator activator = new BgpTestActivator();
    private List<? extends Registration> regs;

    @Before
    public void setUp() {
        final BGPExtensionProviderContext provider = new SimpleBGPExtensionProviderContext();
        ctx = provider;
        regs = activator.start(provider);
    }

    @After
    public void tearDown() {
        regs.forEach(Registration::close);
    }

    @Test
    public void testSimpleAttribute() throws BGPDocumentedException, BGPParsingException, BGPTreatAsWithdrawException {
        final AttributeRegistry attrReg = ctx.getAttributeRegistry();
        final byte[] attributeBytes = {
            0x00, 0x00, 0x00
        };
        final ByteBuf byteAggregator = Unpooled.buffer(attributeBytes.length);
        attrReg.serializeAttribute(mock(Attributes.class), byteAggregator);
        attrReg.parseAttributes(Unpooled.wrappedBuffer(attributeBytes), CONSTRAINT);
        verify(activator.attrParser, times(1)).parseAttribute(any(ByteBuf.class), any(AttributesBuilder.class),
            any(RevisedErrorHandling.class), any(PeerSpecificParserConstraint.class));
        verify(activator.attrSerializer, times(1)).serializeAttribute(any(Attributes.class), any(ByteBuf.class));
    }

    @Test
    public void testSimpleParameter() throws Exception {
        final ParameterRegistry paramReg = ctx.getParameterRegistry();
        final BgpParameters param = mock(BgpParameters.class);
        doReturn(BgpParameters.class).when(param).implementedInterface();

        assertEquals(Optional.of(activator.paramParser), paramReg.findParser(0));
        assertEquals(Optional.of(activator.paramSerializer), paramReg.findSerializer(param));
    }

    @Test
    public void testSimpleCapability() throws Exception {
        final CapabilityRegistry capaRegistry = ctx.getCapabilityRegistry();
        final byte[] capabilityBytes = {
            0x0, 0x00
        };
        capaRegistry.parseCapability(BgpTestActivator.TYPE, Unpooled.wrappedBuffer(capabilityBytes));
        verify(activator.capaParser, times(1)).parseCapability(any(ByteBuf.class));
    }

    @Test
    public void testSimpleBgpPrefixSidTlvRegistry() {
        final BgpPrefixSidTlvRegistry sidTlvReg = ctx.getBgpPrefixSidTlvRegistry();
        final byte[] tlvBytes = {
            0x00, 0x03, 0x00, 0x00, 0x00
        };

        final BgpPrefixSidTlv tlv = mock(BgpPrefixSidTlv.class);
        doReturn(BgpPrefixSidTlv.class).when(tlv).implementedInterface();

        final ByteBuf buffer = Unpooled.buffer(tlvBytes.length);
        sidTlvReg.serializeBgpPrefixSidTlv(tlv, buffer);
        verify(activator.sidTlvSerializer, times(1)).serializeBgpPrefixSidTlv(any(BgpPrefixSidTlv.class),
            any(ByteBuf.class));

        sidTlvReg.parseBgpPrefixSidTlv(BgpTestActivator.TYPE, Unpooled.wrappedBuffer(tlvBytes));
        verify(activator.sidTlvParser, times(1)).parseBgpPrefixSidTlv(any(ByteBuf.class));
    }

    @Test
    public void testSimpleMessageRegistry() throws Exception {
        final MessageRegistry msgRegistry = ctx.getMessageRegistry();

        final byte[] msgBytes = {
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0x00, (byte) 0x13, (byte) 0x00
        };
        final MockNotification msg = mock(MockNotification.class);
        doReturn(MockNotification.class).when(msg).implementedInterface();

        final ByteBuf buffer = Unpooled.buffer(msgBytes.length);
        msgRegistry.serializeMessage(msg, buffer);
        msgRegistry.parseMessage(Unpooled.wrappedBuffer(msgBytes), CONSTRAINT);
        verify(activator.msgParser, times(1)).parseMessageBody(any(ByteBuf.class), anyInt(),
            any(PeerSpecificParserConstraint.class));
        verify(activator.msgSerializer, times(1)).serializeMessage(any(Notification.class), any(ByteBuf.class));
    }

    @Test
    public void testAfiRegistry() throws Exception {
        final AddressFamilyRegistry afiRegistry = ctx.getAddressFamilyRegistry();
        assertEquals(Ipv4AddressFamily.class, afiRegistry.classForFamily(1));
        assertEquals(1, afiRegistry.numberForClass(Ipv4AddressFamily.VALUE).intValue());
    }

    @Test
    public void testSafiRegistry() throws Exception {
        final SubsequentAddressFamilyRegistry safiRegistry = ctx.getSubsequentAddressFamilyRegistry();
        assertEquals(UnicastSubsequentAddressFamily.class, safiRegistry.classForFamily(1));
        assertEquals(1, safiRegistry.numberForClass(UnicastSubsequentAddressFamily.VALUE).intValue());
    }

    @Test
    public void testMpReachParser() throws BGPParsingException {
        final NlriRegistry nlriReg = ctx.getNlriRegistry();
        final byte[] mpReachBytes = {
            0x00, 0x01, 0x01, 0x04, 0x7f, 0x00, 0x00, 0x01, 0x00
        };
        final MpReachNlri mpReach = new MpReachNlriBuilder()
            .setAfi(Ipv4AddressFamily.VALUE)
            .setSafi(UnicastSubsequentAddressFamily.VALUE)
            .setCNextHop(new Ipv4NextHopCaseBuilder().setIpv4NextHop(new Ipv4NextHopBuilder().setGlobal(
                new Ipv4AddressNoZone("127.0.0.1")).build()).build())
            .build();
        final ByteBuf buffer = Unpooled.buffer(mpReachBytes.length);
        nlriReg.serializeMpReach(mpReach, buffer);
        assertArrayEquals(mpReachBytes, buffer.array());
        assertEquals(mpReach, nlriReg.parseMpReach(Unpooled.wrappedBuffer(mpReachBytes), CONSTRAINT));
        verify(activator.nlriParser, times(1)).parseNlri(any(ByteBuf.class), any(MpReachNlriBuilder.class), any());
    }

    @Test
    public void testMpReachWithZeroNextHop() throws BGPParsingException {
        final NlriRegistry nlriReg = ctx.getNlriRegistry();
        final byte[] mpReachBytes = {
            0x00, 0x01, 0x01, 0x00, 0x00
        };
        final MpReachNlri mpReach = new MpReachNlriBuilder()
            .setAfi(Ipv4AddressFamily.VALUE)
            .setSafi(UnicastSubsequentAddressFamily.VALUE)
            .build();
        final ByteBuf buffer = Unpooled.buffer(mpReachBytes.length);
        nlriReg.serializeMpReach(mpReach, buffer);
        assertArrayEquals(mpReachBytes, buffer.array());
        assertEquals(mpReach, nlriReg.parseMpReach(Unpooled.wrappedBuffer(mpReachBytes), CONSTRAINT));
    }

    @Test
    public void testMpReachIpv6() throws BGPParsingException {
        final NlriRegistry nlriReg = ctx.getNlriRegistry();
        final byte[] mpReachBytes = {
            0x00, 0x02, 0x01, 0x00, 0x00
        };
        final MpReachNlri mpReach = new MpReachNlriBuilder()
            .setAfi(Ipv6AddressFamily.VALUE)
            .setSafi(UnicastSubsequentAddressFamily.VALUE)
            .build();
        final ByteBuf buffer = Unpooled.buffer(mpReachBytes.length);
        nlriReg.serializeMpReach(mpReach, buffer);
        assertArrayEquals(mpReachBytes, buffer.array());
        assertEquals(mpReach, nlriReg.parseMpReach(Unpooled.wrappedBuffer(mpReachBytes), CONSTRAINT));
    }

    @Test
    public void testEOTMpUnReachParser() throws BGPParsingException {
        final NlriRegistry nlriReg = ctx.getNlriRegistry();
        final byte[] mpUnreachBytes = {
            0x00, 0x01, 0x01
        };
        final MpUnreachNlri mpUnreach = new MpUnreachNlriBuilder()
            .setAfi(Ipv4AddressFamily.VALUE)
            .setSafi(UnicastSubsequentAddressFamily.VALUE)
            .build();
        final ByteBuf buffer = Unpooled.buffer(mpUnreachBytes.length);
        nlriReg.serializeMpUnReach(mpUnreach, buffer);
        assertArrayEquals(mpUnreachBytes, buffer.array());
        assertEquals(mpUnreach, nlriReg.parseMpUnreach(Unpooled.wrappedBuffer(mpUnreachBytes), CONSTRAINT));
        verify(activator.nlriParser, never()).parseNlri(any(ByteBuf.class), any(MpUnreachNlriBuilder.class),
            any());
    }
}
