/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.pcep.spi;

import static org.junit.Assert.assertEquals;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.protocol.pcep.spi.EROSubobjectParser;
import org.opendaylight.protocol.pcep.spi.EROSubobjectSerializer;
import org.opendaylight.protocol.pcep.spi.LabelParser;
import org.opendaylight.protocol.pcep.spi.LabelSerializer;
import org.opendaylight.protocol.pcep.spi.MessageParser;
import org.opendaylight.protocol.pcep.spi.MessageSerializer;
import org.opendaylight.protocol.pcep.spi.ObjectHeaderImpl;
import org.opendaylight.protocol.pcep.spi.ObjectParser;
import org.opendaylight.protocol.pcep.spi.ObjectSerializer;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.PCEPExtensionProviderActivator;
import org.opendaylight.protocol.pcep.spi.PCEPExtensionProviderContext;
import org.opendaylight.protocol.pcep.spi.RROSubobjectParser;
import org.opendaylight.protocol.pcep.spi.RROSubobjectSerializer;
import org.opendaylight.protocol.pcep.spi.TlvParser;
import org.opendaylight.protocol.pcep.spi.TlvSerializer;
import org.opendaylight.protocol.pcep.spi.XROSubobjectParser;
import org.opendaylight.protocol.pcep.spi.XROSubobjectSerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iana.rev130816.EnterpriseNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.Keepalive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.KeepaliveBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.exclude.route.object.xro.Subobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.exclude.route.object.xro.SubobjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.of.list.tlv.OfList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.of.list.tlv.OfListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.rp.object.Rp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.vendor.information.EnterpriseSpecificInformation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.vendor.information.objects.VendorInformationObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.vendor.information.tlvs.VendorInformationTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.AsNumberCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.AsNumberCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.IpPrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.label.subobject.LabelType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.label.subobject.label.type.Type1LabelCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.label.subobject.label.type.Type1LabelCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.subobjects.subobject.type.IpPrefixCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.subobjects.subobject.type.LabelCase;

/**
 * Created by cgasparini on 17.6.2015.
 */
public class ReusablePCEPExtensionProviderContextTest {

    List<PCEPExtensionProviderContext> AuxList = new ArrayList<>();

    @Mock
    XROSubobjectParser xroParser;
    @Mock
    XROSubobjectSerializer xroSerializer;

    @Mock
    RROSubobjectParser rroParser;
    @Mock
    RROSubobjectSerializer rroSerializer;

    @Mock
    EROSubobjectParser eroParser;
    @Mock
    EROSubobjectSerializer eroSerializer;

    @Mock
    TlvParser tlvParser;
    @Mock
    TlvSerializer tlvSerializer;

    @Mock
    ObjectParser objectParser;
    @Mock
    ObjectSerializer objectSerializer;

    @Mock
    MessageParser msgParser;
    @Mock
    MessageSerializer msgSerializer;

    @Mock
    LabelParser labelParser;
    @Mock
    LabelSerializer labelSerializer;
    @Mock
    VendorInformationTlv viTlv;
    @Mock
    EnterpriseSpecificInformation es;
    @Mock
    VendorInformationObject esi;

    @Before
    public void setUp() throws PCEPDeserializerException {
        MockitoAnnotations.initMocks(this);

        Mockito.doReturn(null).when(this.xroParser).parseSubobject(Mockito.any(ByteBuf.class), Mockito.anyBoolean());
        Mockito.doNothing().when(this.xroSerializer).serializeSubobject(Mockito.any(Subobject.class), Mockito.any(ByteBuf.class));

        Mockito.doReturn(null).when(this.rroParser).parseSubobject(Mockito.any(ByteBuf.class));
        Mockito.doNothing().when(this.rroSerializer).serializeSubobject(Mockito.any(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.reported.route.object.rro.Subobject.class), Mockito.any(ByteBuf.class));

        Mockito.doReturn(null).when(this.eroParser).parseSubobject(Mockito.any(ByteBuf.class), Mockito.anyBoolean());
        Mockito.doNothing().when(this.eroSerializer).serializeSubobject(Mockito.any(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.Subobject.class), Mockito.any(ByteBuf.class));

        Mockito.doReturn(this.viTlv).when(this.tlvParser).parseTlv(Mockito.any(ByteBuf.class));
        Mockito.doNothing().when(this.tlvSerializer).serializeTlv(Mockito.any(Tlv.class), Mockito.any(ByteBuf.class));

        Mockito.doReturn(new OpenBuilder().build()).when(this.objectParser).parseObject(Mockito.any(ObjectHeader.class), Mockito.any(ByteBuf.class));
        Mockito.doNothing().when(this.objectSerializer).serializeObject(Mockito.any(org.opendaylight.yang.gen.v1.urn
            .opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object.class), Mockito.any(ByteBuf.class));

        Mockito.doReturn(null).when(this.msgParser).parseMessage(Mockito.any(ByteBuf.class), Mockito.any(List.class));
        Mockito.doNothing().when(this.msgSerializer).serializeMessage(Mockito.any(Message.class), Mockito.any(ByteBuf.class));

        Mockito.doReturn(null).when(this.labelParser).parseLabel(Mockito.any(ByteBuf.class));
        Mockito.doNothing().when(this.labelSerializer).serializeLabel(Mockito.anyBoolean(), Mockito.anyBoolean(), Mockito.any(LabelType.class), Mockito.any(ByteBuf.class));

        Mockito.doReturn(this.es).when(this.esi).getEnterpriseSpecificInformation();

        Mockito.doReturn(EnterpriseSpecificInformation.class).when(this.es).getImplementedInterface();

        Mockito.doReturn(this.es).when(this.viTlv).getEnterpriseSpecificInformation();

        Mockito.doReturn(EnterpriseSpecificInformation.class).when(this.es).getImplementedInterface();
    }

    @Test
    public void testReusablePCEPExtensionProviderContext() throws PCEPDeserializerException {
        List<PCEPExtensionProviderActivator> list = new ArrayList<>();
        list.add(new simplePCEPExtensionProviderActivator());
        list.add(new simplePCEPExtensionProviderActivator());


        ReusablePCEPExtensionProviderContext ctx = new ReusablePCEPExtensionProviderContext();
        ctx.start(list);

        assertEquals(2, AuxList.size());

        final List<PCEPExtensionProviderActivator> listReconf = new ArrayList<>();
        listReconf.add(new simplePCEPExtensionProviderActivator());
        listReconf.add(new simplePCEPExtensionProviderActivator());
        listReconf.add(new simplePCEPExtensionProviderActivator());
        listReconf.add(new simplePCEPExtensionProviderActivator());
        ctx.reconfigure(listReconf);
        assertEquals(4, AuxList.size());

        ctx.registerLabelParser(1, this.labelParser);
        ctx.registerLabelSerializer(Type1LabelCase.class, this.labelSerializer);

        ctx.registerEROSubobjectParser(2, this.eroParser);
        ctx.registerEROSubobjectSerializer(IpPrefixCase.class, this.eroSerializer);

        ctx.registerMessageParser(6, this.msgParser);
        ctx.registerMessageSerializer(Keepalive.class, this.msgSerializer);

        ctx.registerObjectParser(5, 1, this.objectParser);
        ctx.registerObjectSerializer(Rp.class, this.objectSerializer);

        ctx.registerRROSubobjectParser(3, this.rroParser);
        ctx.registerRROSubobjectSerializer(LabelCase.class, this.rroSerializer);

        ctx.registerTlvParser(1, this.tlvParser);
        ctx.registerTlvSerializer(OfList.class, this.tlvSerializer);

        ctx.registerXROSubobjectParser(2, this.xroParser);
        ctx.registerXROSubobjectSerializer(AsNumberCase.class, this.xroSerializer);

        ctx.registerVendorInformationTlvSerializer(EnterpriseSpecificInformation.class, this.tlvSerializer);
        ctx.registerVendorInformationTlvParser(new EnterpriseNumber(12L), this.tlvParser);

        ctx.registerVendorInformationObjectParser(new EnterpriseNumber(10L), this.objectParser);
        ctx.registerVendorInformationObjectSerializer(EnterpriseSpecificInformation.class, this.objectSerializer);

        final ByteBuf buffer = Unpooled.buffer();
        ctx.getEROSubobjectHandlerRegistry().parseSubobject(3, buffer, true);

        ctx.getEROSubobjectHandlerRegistry().serializeSubobject(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.
            SubobjectBuilder().setSubobjectType(new AsNumberCaseBuilder().build()).build(), buffer);

        ctx.getLabelHandlerRegistry().parseLabel(1, buffer);
        ctx.getLabelHandlerRegistry().serializeLabel(true, false, new Type1LabelCaseBuilder().build(), buffer);

        ctx.getMessageHandlerRegistry().parseMessage(6, buffer, Collections.<Message>emptyList());
        ctx.getMessageHandlerRegistry().serializeMessage(new KeepaliveBuilder().build(), buffer);

        ctx.getObjectHandlerRegistry().parseObject(4, 1, new ObjectHeaderImpl(true, false), buffer);
        ctx.getObjectHandlerRegistry().serializeObject(new OpenBuilder().build(), buffer);

        ctx.getRROSubobjectHandlerRegistry().parseSubobject(4, buffer);
        ctx.getRROSubobjectHandlerRegistry().serializeSubobject(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.reported.route.object.rro.SubobjectBuilder().setSubobjectType(new IpPrefixCaseBuilder().build()).build(), buffer);

        ctx.getTlvHandlerRegistry().parseTlv(2, buffer);
        ctx.getTlvHandlerRegistry().serializeTlv(new OfListBuilder().build(), buffer);

        ctx.getVendorInformationObjectRegistry().parseVendorInformationObject(new EnterpriseNumber(10L), new ObjectHeaderImpl(true, false), buffer);
        ctx.getVendorInformationObjectRegistry().serializeVendorInformationObject(this.esi, buffer);

        ctx.getVendorInformationTlvRegistry().parseVendorInformationTlv(new EnterpriseNumber(12L), buffer);
        ctx.getVendorInformationTlvRegistry().serializeVendorInformationTlv(this.viTlv, buffer);

        ctx.getXROSubobjectHandlerRegistry().parseSubobject(2, buffer, false);
        ctx.getXROSubobjectHandlerRegistry().serializeSubobject(new SubobjectBuilder().setSubobjectType(new AsNumberCaseBuilder().build()).build(), buffer);

        ctx.close();
        assertEquals(0, AuxList.size());
    }

    class simplePCEPExtensionProviderActivator implements PCEPExtensionProviderActivator {
        @Override
        public void start(final PCEPExtensionProviderContext context) {
            AuxList.add(context);
        }

        @Override
        public void stop() {
            AuxList.remove(AuxList.size() - 1);
        }
    }
}