/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.parser.spi.pojo;

import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import java.util.List;
import org.junit.Assert;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.AbstractBGPExtensionProviderActivator;
import org.opendaylight.protocol.bgp.parser.spi.AttributeParser;
import org.opendaylight.protocol.bgp.parser.spi.AttributeSerializer;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.parser.spi.CapabilityParser;
import org.opendaylight.protocol.bgp.parser.spi.CapabilitySerializer;
import org.opendaylight.protocol.bgp.parser.spi.MessageParser;
import org.opendaylight.protocol.bgp.parser.spi.MessageSerializer;
import org.opendaylight.protocol.bgp.parser.spi.NlriParser;
import org.opendaylight.protocol.bgp.parser.spi.NlriSerializer;
import org.opendaylight.protocol.bgp.parser.spi.ParameterParser;
import org.opendaylight.protocol.bgp.parser.spi.ParameterSerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.bgp.parameters.optional.capabilities.CParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.PathAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Notification;

public class BgpTestActivator extends AbstractBGPExtensionProviderActivator {

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

    @Override
    protected List<AutoCloseable> startImpl(BGPExtensionProviderContext context) {
        initMock();
        final List<AutoCloseable> regs = Lists.newArrayList();
        regs.add(context.registerAttributeParser(TYPE, this.attrParser));
        regs.add(context.registerAttributeSerializer(DataObject.class, this.attrSerializer));

        regs.add(context.registerParameterParser(TYPE, this.paramParser));
        regs.add(context.registerParameterSerializer(BgpParameters.class, this.paramSerializer));

        regs.add(context.registerCapabilityParser(TYPE, this.capaParser));
        regs.add(context.registerCapabilitySerializer(CParameters.class, this.capaSerializer));

        regs.add(context.registerMessageParser(TYPE, this.msgParser));
        regs.add(context.registerMessageSerializer(Notification.class, this.msgSerializer));

        regs.add(context.registerAddressFamily(Ipv4AddressFamily.class, 1));
        regs.add(context.registerSubsequentAddressFamily(UnicastSubsequentAddressFamily.class, 1));

        regs.add(context.registerNlriParser(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class, this.nlriParser));
        regs.add(context.registerNlriSerializer(DataObject.class, this.nlriSerializer));

        return regs;
    }

    private void initMock() {
        MockitoAnnotations.initMocks(this);
        try {
            Mockito.doNothing().when(this.attrParser).parseAttribute(Mockito.any(ByteBuf.class), Mockito.any(PathAttributesBuilder.class));
            Mockito.doReturn(EMPTY).when(this.attrParser).toString();
            Mockito.doNothing().when(this.attrSerializer).serializeAttribute(Mockito.any(DataObject.class), Mockito.any(ByteBuf.class));
            Mockito.doReturn(EMPTY).when(this.attrSerializer).toString();

            Mockito.doReturn(null).when(this.paramParser).parseParameter(Mockito.any(ByteBuf.class));
            Mockito.doReturn(EMPTY).when(this.paramParser).toString();
            Mockito.doNothing().when(this.paramSerializer).serializeParameter(Mockito.any(BgpParameters.class), Mockito.any(ByteBuf.class));
            Mockito.doReturn(EMPTY).when(this.paramSerializer).toString();

            Mockito.doReturn(null).when(this.capaParser).parseCapability(Mockito.any(ByteBuf.class));
            Mockito.doReturn(EMPTY).when(this.capaParser).toString();
            Mockito.doNothing().when(this.capaSerializer).serializeCapability(Mockito.any(CParameters.class), Mockito.any(ByteBuf.class));
            Mockito.doReturn(EMPTY).when(this.capaSerializer).toString();

            Mockito.doReturn(Mockito.mock(Notification.class)).when(this.msgParser).parseMessageBody(Mockito.any(ByteBuf.class), Mockito.anyInt());
            Mockito.doReturn(EMPTY).when(this.msgParser).toString();
            Mockito.doNothing().when(this.msgSerializer).serializeMessage(Mockito.any(Notification.class), Mockito.any(ByteBuf.class));
            Mockito.doReturn(EMPTY).when(this.msgSerializer).toString();

            Mockito.doNothing().when(this.nlriParser).parseNlri(Mockito.any(ByteBuf.class), Mockito.any(MpUnreachNlriBuilder.class));
            Mockito.doNothing().when(this.nlriParser).parseNlri(Mockito.any(ByteBuf.class), Mockito.any(MpReachNlriBuilder.class));
            Mockito.doReturn(EMPTY).when(this.nlriParser).toString();
        } catch (BGPDocumentedException | BGPParsingException e) {
            Assert.fail();
        }
    }

}
