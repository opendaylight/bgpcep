/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi.pojo;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.BGPTreatAsWithdrawException;
import org.opendaylight.protocol.bgp.parser.spi.AttributeParser;
import org.opendaylight.protocol.bgp.parser.spi.AttributeSerializer;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderActivator;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.parser.spi.BgpPrefixSidTlvParser;
import org.opendaylight.protocol.bgp.parser.spi.BgpPrefixSidTlvSerializer;
import org.opendaylight.protocol.bgp.parser.spi.CapabilityParser;
import org.opendaylight.protocol.bgp.parser.spi.CapabilitySerializer;
import org.opendaylight.protocol.bgp.parser.spi.MessageParser;
import org.opendaylight.protocol.bgp.parser.spi.MessageSerializer;
import org.opendaylight.protocol.bgp.parser.spi.NextHopParserSerializer;
import org.opendaylight.protocol.bgp.parser.spi.NlriParser;
import org.opendaylight.protocol.bgp.parser.spi.NlriSerializer;
import org.opendaylight.protocol.bgp.parser.spi.ParameterLengthOverflowException;
import org.opendaylight.protocol.bgp.parser.spi.ParameterParser;
import org.opendaylight.protocol.bgp.parser.spi.ParameterSerializer;
import org.opendaylight.protocol.bgp.parser.spi.PeerSpecificParserConstraint;
import org.opendaylight.protocol.bgp.parser.spi.RevisedErrorHandling;
import org.opendaylight.protocol.bgp.parser.spi.extended.community.ExtendedCommunityParser;
import org.opendaylight.protocol.bgp.parser.spi.extended.community.ExtendedCommunitySerializer;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.bgp.parameters.optional.capabilities.CParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.bgp.prefix.sid.bgp.prefix.sid.tlvs.BgpPrefixSidTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.reach.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.unreach.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.UnicastSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.extended.community.extended.community.RouteTargetIpv4Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.next.hop.CNextHop;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.next.hop.c.next.hop.Ipv4NextHopCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.next.hop.c.next.hop.Ipv4NextHopCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.next.hop.c.next.hop.Ipv6NextHopCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.next.hop.c.next.hop.ipv4.next.hop._case.Ipv4NextHopBuilder;
import org.opendaylight.yangtools.binding.DataObject;
import org.opendaylight.yangtools.binding.Notification;
import org.opendaylight.yangtools.concepts.Registration;

public class BgpTestActivator implements BGPExtensionProviderActivator {
    interface MockNotification extends DataObject, Notification<MockNotification> {
        @Override
        default Class<MockNotification> implementedInterface() {
            return MockNotification.class;
        }
    }

    protected static final int TYPE = 0;
    private static final String EMPTY = "";

    @Mock
    protected AttributeSerializer attrSerializer;
    @Mock
    protected AttributeParser attrParser;

    @Mock
    protected ParameterParser paramParser;
    @Mock
    protected ParameterSerializer paramSerializer;

    @Mock
    protected CapabilityParser capaParser;
    @Mock
    protected CapabilitySerializer capaSerializer;

    @Mock
    protected MessageParser msgParser;
    @Mock
    protected MessageSerializer msgSerializer;

    @Mock
    protected NlriParser nlriParser;
    @Mock
    protected NlriSerializer nlriSerializer;

    @Mock
    protected ExtendedCommunityParser exParser;
    @Mock
    protected ExtendedCommunitySerializer exSerializer;

    protected NextHopParserSerializer nextHopParserSerializer;

    @Mock
    protected BgpPrefixSidTlvParser sidTlvParser;
    @Mock
    protected BgpPrefixSidTlvSerializer sidTlvSerializer;

    @Override
    public List<? extends Registration> start(final BGPExtensionProviderContext context) {
        initMock();

        final List<Registration> regs = new ArrayList<>();
        regs.add(context.registerAttributeParser(TYPE, attrParser));
        regs.add(context.registerAttributeSerializer(DataObject.class, attrSerializer));

        regs.add(context.registerParameterParser(TYPE, paramParser));
        regs.add(context.registerParameterSerializer(BgpParameters.class, paramSerializer));

        regs.add(context.registerCapabilityParser(TYPE, capaParser));
        regs.add(context.registerCapabilitySerializer(CParameters.class, capaSerializer));

        regs.add(context.registerBgpPrefixSidTlvParser(TYPE, sidTlvParser));
        regs.add(context.registerBgpPrefixSidTlvSerializer(BgpPrefixSidTlv.class, sidTlvSerializer));

        regs.add(context.registerMessageParser(TYPE, msgParser));
        regs.add(context.registerMessageSerializer(MockNotification.class, msgSerializer));

        regs.add(context.registerAddressFamily(Ipv4AddressFamily.VALUE, 1));
        regs.add(context.registerAddressFamily(Ipv6AddressFamily.VALUE, 2));
        regs.add(context.registerSubsequentAddressFamily(UnicastSubsequentAddressFamily.VALUE, 1));

        nextHopParserSerializer = new NextHopParserSerializer() {
            @Override
            public CNextHop parseNextHop(final ByteBuf buffer) throws BGPParsingException {
                return new Ipv4NextHopCaseBuilder().setIpv4NextHop(new Ipv4NextHopBuilder()
                    .setGlobal(new Ipv4AddressNoZone("127.0.0.1")).build()).build();
            }

            @Override
            public void serializeNextHop(final CNextHop cnextHop, final ByteBuf byteAggregator) {
                final byte[] mpReachBytes = {
                    0x7f, 0x00, 0x00, 0x01
                };
                byteAggregator.writeBytes(mpReachBytes);
            }
        };

        regs.add(context.registerNlriParser(Ipv4AddressFamily.VALUE, UnicastSubsequentAddressFamily.VALUE, nlriParser,
            nextHopParserSerializer, Ipv4NextHopCase.class));
        regs.add(context.registerNlriParser(Ipv6AddressFamily.VALUE, UnicastSubsequentAddressFamily.VALUE, nlriParser,
            nextHopParserSerializer, Ipv6NextHopCase.class));
        regs.add(context.registerNlriSerializer(DataObject.class, nlriSerializer));

        regs.add(context.registerExtendedCommunityParser(0, 0, exParser));
        regs.add(context.registerExtendedCommunitySerializer(RouteTargetIpv4Case.class, exSerializer));

        return regs;
    }

    private void initMock() {
        MockitoAnnotations.openMocks(this);
        try {
            doNothing().when(attrParser).parseAttribute(any(ByteBuf.class), any(AttributesBuilder.class),
                any(RevisedErrorHandling.class), any(PeerSpecificParserConstraint.class));
            doReturn(EMPTY).when(attrParser).toString();
            doNothing().when(attrSerializer).serializeAttribute(any(Attributes.class), any(ByteBuf.class));
            doReturn(EMPTY).when(attrSerializer).toString();

            doReturn(null).when(paramParser).parseParameter(any(ByteBuf.class));
            doReturn(EMPTY).when(paramParser).toString();
            doNothing().when(paramSerializer).serializeParameter(any(BgpParameters.class), any(ByteBuf.class));
            doReturn(EMPTY).when(paramSerializer).toString();

            doReturn(null).when(capaParser).parseCapability(any(ByteBuf.class));
            doReturn(EMPTY).when(capaParser).toString();
            doNothing().when(capaSerializer).serializeCapability(any(CParameters.class), any(ByteBuf.class));
            doReturn(EMPTY).when(capaSerializer).toString();

            doReturn(null).when(sidTlvParser).parseBgpPrefixSidTlv(any(ByteBuf.class));
            doReturn(EMPTY).when(sidTlvParser).toString();
            doNothing().when(sidTlvSerializer).serializeBgpPrefixSidTlv(any(BgpPrefixSidTlv.class),
                any(ByteBuf.class));
            doReturn(EMPTY).when(sidTlvSerializer).toString();
            doReturn(0).when(sidTlvSerializer).getType();

            doReturn(mock(Notification.class)).when(msgParser).parseMessageBody(any(ByteBuf.class), anyInt(),
                any(PeerSpecificParserConstraint.class));
            doReturn(EMPTY).when(msgParser).toString();
            doNothing().when(msgSerializer).serializeMessage(any(Notification.class), any(ByteBuf.class));
            doReturn(EMPTY).when(msgSerializer).toString();

            doNothing().when(nlriParser).parseNlri(any(ByteBuf.class), any(MpUnreachNlriBuilder.class), any());
            doNothing().when(nlriParser).parseNlri(any(ByteBuf.class), any(MpReachNlriBuilder.class), any());
            doReturn(EMPTY).when(nlriParser).toString();

        } catch (BGPDocumentedException | BGPParsingException | BGPTreatAsWithdrawException
                | ParameterLengthOverflowException e) {
            throw new IllegalStateException("Mock setup failed", e);
        }
    }
}
