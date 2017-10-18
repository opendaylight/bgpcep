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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Optional;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.AddressFamilyRegistry;
import org.opendaylight.protocol.bgp.parser.spi.AttributeRegistry;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.parser.spi.BgpPrefixSidTlvRegistry;
import org.opendaylight.protocol.bgp.parser.spi.CapabilityRegistry;
import org.opendaylight.protocol.bgp.parser.spi.MessageRegistry;
import org.opendaylight.protocol.bgp.parser.spi.MultiPathSupport;
import org.opendaylight.protocol.bgp.parser.spi.NlriRegistry;
import org.opendaylight.protocol.bgp.parser.spi.ParameterRegistry;
import org.opendaylight.protocol.bgp.parser.spi.PeerConstraint;
import org.opendaylight.protocol.bgp.parser.spi.PeerSpecificParserConstraint;
import org.opendaylight.protocol.bgp.parser.spi.SubsequentAddressFamilyRegistry;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.open.message.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.bgp.prefix.sid.bgp.prefix.sid.tlvs.BgpPrefixSidTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.update.attributes.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.update.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.update.attributes.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.update.attributes.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv4NextHopCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.ipv4.next.hop._case.Ipv4NextHopBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Notification;

public class SimpleRegistryTest {

    private static final MultiPathSupport ADD_PATH_SUPPORT = tableType -> true;

    private static final PeerSpecificParserConstraint CONSTRAINT = new PeerSpecificParserConstraint() {
        @Override
        public <T extends PeerConstraint> Optional<T> getPeerConstraint(final Class<T> peerConstraintType) {
            return (Optional<T>) Optional.of(ADD_PATH_SUPPORT);
        }
    };

    protected BGPExtensionProviderContext ctx;
    private BgpTestActivator activator;

    @Before
    public void setUp() {
        this.ctx = ServiceLoaderBGPExtensionProviderContext.getSingletonInstance();
        this.activator = new BgpTestActivator();
        this.activator.start(this.ctx);
    }

    @After
    public void tearDown() {
        this.activator.close();
    }

    @Test
    public void testSimpleAttribute() throws BGPDocumentedException, BGPParsingException {
        final AttributeRegistry attrReg = this.ctx.getAttributeRegistry();
        final byte[] attributeBytes = {
            0x00, 0x00, 0x00
        };
        final ByteBuf byteAggregator = Unpooled.buffer(attributeBytes.length);
        attrReg.serializeAttribute(Mockito.mock(DataObject.class), byteAggregator);
        attrReg.parseAttributes(Unpooled.wrappedBuffer(attributeBytes), CONSTRAINT);
        verify(this.activator.attrParser, times(1)).parseAttribute(Mockito.any(ByteBuf.class), Mockito.any(AttributesBuilder.class),
            Mockito.any(PeerSpecificParserConstraint.class));
        verify(this.activator.attrSerializer, times(1)).serializeAttribute(Mockito.any(DataObject.class), Mockito.any(ByteBuf.class));
    }

    @Test
    public void testSimpleParameter() throws Exception {
        final ParameterRegistry paramReg = this.ctx.getParameterRegistry();
        final BgpParameters param = Mockito.mock(BgpParameters.class);
        Mockito.doReturn(BgpParameters.class).when(param).getImplementedInterface();
        final byte[] paramBytes = {
            0x00, 0x00
        };
        final ByteBuf buffer = Unpooled.buffer(paramBytes.length);
        paramReg.serializeParameter(param, buffer);
        paramReg.parseParameter(0, Unpooled.wrappedBuffer(paramBytes));
        verify(this.activator.paramParser, times(1)).parseParameter(Mockito.any(ByteBuf.class));
        verify(this.activator.paramSerializer, times(1)).serializeParameter(Mockito.any(BgpParameters.class), Mockito.any(ByteBuf.class));
    }

    @Test
    public void testSimpleCapability() throws Exception {
        final CapabilityRegistry capaRegistry = this.ctx.getCapabilityRegistry();
        final byte[] capabilityBytes = {
            0x0, 0x00
        };
        capaRegistry.parseCapability(BgpTestActivator.TYPE, Unpooled.wrappedBuffer(capabilityBytes));
        verify(this.activator.capaParser, times(1)).parseCapability(Mockito.any(ByteBuf.class));
    }

    @Test
    public void testSimpleBgpPrefixSidTlvRegistry() {
        final BgpPrefixSidTlvRegistry sidTlvReg = this.ctx.getBgpPrefixSidTlvRegistry();
        final byte[] tlvBytes = {
            0x00, 0x03, 0x00, 0x00, 0x00
        };

        final BgpPrefixSidTlv tlv = mock(BgpPrefixSidTlv.class);
        doReturn(BgpPrefixSidTlv.class).when(tlv).getImplementedInterface();

        final ByteBuf buffer = Unpooled.buffer(tlvBytes.length);
        sidTlvReg.serializeBgpPrefixSidTlv(tlv, buffer);
        verify(this.activator.sidTlvSerializer, times(1)).serializeBgpPrefixSidTlv(Mockito.any(BgpPrefixSidTlv.class), Mockito.any(ByteBuf.class));

        sidTlvReg.parseBgpPrefixSidTlv(BgpTestActivator.TYPE, Unpooled.wrappedBuffer(tlvBytes));
        verify(this.activator.sidTlvParser, times(1)).parseBgpPrefixSidTlv(Mockito.any(ByteBuf.class));
    }

    @Test
    public void testSimpleMessageRegistry() throws Exception {
        final MessageRegistry msgRegistry = this.ctx.getMessageRegistry();

        final byte[] msgBytes = {
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0x00, (byte) 0x13, (byte) 0x00
        };
        final Notification msg = mock(Notification.class);
        doReturn(Notification.class).when(msg).getImplementedInterface();

        final ByteBuf buffer = Unpooled.buffer(msgBytes.length);
        msgRegistry.serializeMessage(msg, buffer);
        msgRegistry.parseMessage(Unpooled.wrappedBuffer(msgBytes), CONSTRAINT);
        verify(this.activator.msgParser, times(1)).parseMessageBody(Mockito.any(ByteBuf.class), Mockito.anyInt(),
            Mockito.any(PeerSpecificParserConstraint.class));
        verify(this.activator.msgSerializer, times(1)).serializeMessage(Mockito.any(Notification.class), Mockito.any(ByteBuf.class));
    }

    @Test
    public void testAfiRegistry() throws Exception {
        final AddressFamilyRegistry afiRegistry = this.ctx.getAddressFamilyRegistry();
        assertEquals(Ipv4AddressFamily.class, afiRegistry.classForFamily(1));
        assertEquals(1, afiRegistry.numberForClass(Ipv4AddressFamily.class).intValue());
    }

    @Test
    public void testSafiRegistry() throws Exception {
        final SubsequentAddressFamilyRegistry safiRegistry = this.ctx.getSubsequentAddressFamilyRegistry();
        assertEquals(UnicastSubsequentAddressFamily.class, safiRegistry.classForFamily(1));
        assertEquals(1, safiRegistry.numberForClass(UnicastSubsequentAddressFamily.class).intValue());
    }

    @Test
    public void testMpReachParser() throws BGPParsingException {
        final NlriRegistry nlriReg = this.ctx.getNlriRegistry();
        final byte[] mpReachBytes = {
            0x00, 0x01, 0x01, 0x04, 0x7f, 0x00, 0x00, 0x01, 0x00
        };
        final MpReachNlri mpReach = new MpReachNlriBuilder()
            .setAfi(Ipv4AddressFamily.class)
            .setSafi(UnicastSubsequentAddressFamily.class)
            .setCNextHop(new Ipv4NextHopCaseBuilder().setIpv4NextHop(new Ipv4NextHopBuilder().setGlobal(new Ipv4Address("127.0.0.1")).build()).build())
            .build();
        final ByteBuf buffer = Unpooled.buffer(mpReachBytes.length);
        nlriReg.serializeMpReach(mpReach, buffer);
        assertArrayEquals(mpReachBytes, buffer.array());
        assertEquals(mpReach, nlriReg.parseMpReach(Unpooled.wrappedBuffer(mpReachBytes), CONSTRAINT));
        verify(this.activator.nlriParser, times(1)).parseNlri(Mockito.any(ByteBuf.class), Mockito.any(MpReachNlriBuilder.class), Mockito.any());
    }

    @Test
    public void testMpReachWithZeroNextHop() throws BGPParsingException {
        final NlriRegistry nlriReg = this.ctx.getNlriRegistry();
        final byte[] mpReachBytes = {
            0x00, 0x01, 0x01, 0x00, 0x00
        };
        final MpReachNlri mpReach = new MpReachNlriBuilder()
            .setAfi(Ipv4AddressFamily.class)
            .setSafi(UnicastSubsequentAddressFamily.class)
            .build();
        final ByteBuf buffer = Unpooled.buffer(mpReachBytes.length);
        nlriReg.serializeMpReach(mpReach, buffer);
        assertArrayEquals(mpReachBytes, buffer.array());
        assertEquals(mpReach, nlriReg.parseMpReach(Unpooled.wrappedBuffer(mpReachBytes), CONSTRAINT));
    }

    @Test
    public void testMpReachIpv6() throws BGPParsingException {
        final NlriRegistry nlriReg = this.ctx.getNlriRegistry();
        final byte[] mpReachBytes = {
            0x00, 0x02, 0x01, 0x00, 0x00
        };
        final MpReachNlri mpReach = new MpReachNlriBuilder()
            .setAfi(Ipv6AddressFamily.class)
            .setSafi(UnicastSubsequentAddressFamily.class)
            .build();
        final ByteBuf buffer = Unpooled.buffer(mpReachBytes.length);
        nlriReg.serializeMpReach(mpReach, buffer);
        assertArrayEquals(mpReachBytes, buffer.array());
        assertEquals(mpReach, nlriReg.parseMpReach(Unpooled.wrappedBuffer(mpReachBytes), CONSTRAINT));
    }

    @Test
    public void testMpUnReachParser() throws BGPParsingException {
        final NlriRegistry nlriReg = this.ctx.getNlriRegistry();
        final byte[] mpUnreachBytes = {
            0x00, 0x01, 0x01
        };
        final MpUnreachNlri mpUnreach = new MpUnreachNlriBuilder().setAfi(Ipv4AddressFamily.class).setSafi(UnicastSubsequentAddressFamily.class).build();
        final ByteBuf buffer = Unpooled.buffer(mpUnreachBytes.length);
        nlriReg.serializeMpUnReach(mpUnreach, buffer);
        assertArrayEquals(mpUnreachBytes, buffer.array());
        assertEquals(mpUnreach, nlriReg.parseMpUnreach(Unpooled.wrappedBuffer(mpUnreachBytes), CONSTRAINT));
        verify(this.activator.nlriParser, times(1)).parseNlri(Mockito.any(ByteBuf.class), Mockito.any(MpUnreachNlriBuilder.class), Mockito.any());
    }
}
