/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.spi;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.protocol.pcep.spi.pojo.AbstractPCEPExtensionProviderActivator;
import org.opendaylight.protocol.pcep.spi.pojo.ServiceLoaderPCEPExtensionProviderContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iana.rev130816.EnterpriseNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev181109.Keepalive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev181109.KeepaliveBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.exclude.route.object.xro.Subobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.exclude.route.object.xro.SubobjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.of.list.tlv.OfList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.of.list.tlv.OfListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.open.object.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.rp.object.Rp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.vendor.information.EnterpriseSpecificInformation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.vendor.information.objects.VendorInformationObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.vendor.information.tlvs.VendorInformationTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.AsNumberCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.AsNumberCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.IpPrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.label.subobject.LabelType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.label.subobject.label.type.Type1LabelCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.label.subobject.label.type.Type1LabelCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.subobjects.subobject.type.IpPrefixCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.subobjects.subobject.type.LabelCase;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class RegistryTest {

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
    VendorInformationObject esi;

    @Mock
    EnterpriseSpecificInformation es;

    @Mock
    VendorInformationTlv viTlv;

    public final List<AutoCloseable> regs = new ArrayList<>();

    final AbstractPCEPExtensionProviderActivator activator = new AbstractPCEPExtensionProviderActivator() {

        @Override
        protected List<AutoCloseable> startImpl(final PCEPExtensionProviderContext context) {
            return RegistryTest.this.regs;
        }
    };

    final PCEPExtensionProviderContext ctx = ServiceLoaderPCEPExtensionProviderContext.getSingletonInstance();

    @Before
    public void setUp() throws PCEPDeserializerException {
        doReturn(null).when(this.xroParser).parseSubobject(any(ByteBuf.class), anyBoolean());
        doNothing().when(this.xroSerializer).serializeSubobject(any(Subobject.class), any(ByteBuf.class));

        doReturn(this.viTlv).when(this.tlvParser).parseTlv(any(ByteBuf.class));
        doNothing().when(this.tlvSerializer).serializeTlv(any(Tlv.class), any(ByteBuf.class));

        doReturn(5).when(this.objectParser).getObjectClass();
        doReturn(1).when(this.objectParser).getObjectType();
        doReturn(new OpenBuilder().build()).when(this.objectParser).parseObject(any(ObjectHeader.class),
            any(ByteBuf.class));
        doNothing().when(this.objectSerializer).serializeObject(any(Object.class), any(ByteBuf.class));

        doReturn(null).when(this.msgParser).parseMessage(any(ByteBuf.class), any(List.class));
        doNothing().when(this.msgSerializer).serializeMessage(any(Message.class), any(ByteBuf.class));

        doNothing().when(this.labelSerializer).serializeLabel(anyBoolean(), anyBoolean(), any(LabelType.class),
            any(ByteBuf.class));

        doReturn(this.es).when(this.esi).getEnterpriseSpecificInformation();

        doReturn(this.es).when(this.viTlv).getEnterpriseSpecificInformation();

        doReturn(EnterpriseSpecificInformation.class).when(this.es).getImplementedInterface();
    }

    @Test
    public void testRegistry() throws PCEPDeserializerException {
        this.regs.add(this.ctx.registerXROSubobjectParser(2, this.xroParser));
        this.regs.add(this.ctx.registerXROSubobjectSerializer(AsNumberCase.class, this.xroSerializer));

        this.regs.add(this.ctx.registerRROSubobjectParser(3, this.rroParser));
        this.regs.add(this.ctx.registerRROSubobjectSerializer(LabelCase.class, this.rroSerializer));

        this.regs.add(this.ctx.registerEROSubobjectParser(4, this.eroParser));
        this.regs.add(this.ctx.registerEROSubobjectSerializer(IpPrefixCase.class, this.eroSerializer));

        this.regs.add(this.ctx.registerTlvParser(1, this.tlvParser));
        this.regs.add(this.ctx.registerTlvSerializer(OfList.class, this.tlvSerializer));

        this.regs.add(this.ctx.registerObjectParser(this.objectParser));
        this.regs.add(this.ctx.registerObjectSerializer(Rp.class, this.objectSerializer));

        this.regs.add(this.ctx.registerMessageParser(6, this.msgParser));
        this.regs.add(this.ctx.registerMessageSerializer(Keepalive.class, this.msgSerializer));

        this.regs.add(this.ctx.registerLabelParser(7, this.labelParser));
        this.regs.add(this.ctx.registerLabelSerializer(Type1LabelCase.class, this.labelSerializer));

        this.regs.add(this.ctx.registerVendorInformationObjectParser(new EnterpriseNumber(10L), this.objectParser));
        this.regs.add(this.ctx.registerVendorInformationObjectSerializer(EnterpriseSpecificInformation.class,
            this.objectSerializer));

        this.regs.add(this.ctx.registerVendorInformationTlvParser(new EnterpriseNumber(12L), this.tlvParser));
        this.regs.add(this.ctx.registerVendorInformationTlvSerializer(EnterpriseSpecificInformation.class,
            this.tlvSerializer));

        final ByteBuf buffer = Unpooled.buffer();
        this.ctx.getXROSubobjectHandlerRegistry().parseSubobject(2, buffer, false);
        this.ctx.getXROSubobjectHandlerRegistry().serializeSubobject(new SubobjectBuilder()
            .setSubobjectType(new AsNumberCaseBuilder().build()).build(), buffer);

        this.ctx.getEROSubobjectHandlerRegistry().parseSubobject(3, buffer, true);
        this.ctx.getEROSubobjectHandlerRegistry().serializeSubobject(new org.opendaylight.yang.gen.v1.urn.opendaylight
            .params.xml.ns.yang.pcep.types.rev181109.explicit.route.object.ero.SubobjectBuilder()
            .setSubobjectType(new AsNumberCaseBuilder().build()).build(), buffer);

        this.ctx.getRROSubobjectHandlerRegistry().parseSubobject(4, buffer);
        this.ctx.getRROSubobjectHandlerRegistry().serializeSubobject(new org.opendaylight.yang.gen.v1.urn.opendaylight
            .params.xml.ns.yang.pcep.types.rev181109.reported.route.object.rro.SubobjectBuilder()
            .setSubobjectType(new IpPrefixCaseBuilder().build()).build(), buffer);

        this.ctx.getLabelHandlerRegistry().parseLabel(1, buffer);
        this.ctx.getLabelHandlerRegistry().serializeLabel(true, false, new Type1LabelCaseBuilder().build(), buffer);

        this.ctx.getTlvHandlerRegistry().parseTlv(2, buffer);
        this.ctx.getTlvHandlerRegistry().serializeTlv(new OfListBuilder().build(), buffer);

        this.ctx.getObjectHandlerRegistry().parseObject(4, 1, new ObjectHeaderImpl(true, false), buffer);
        this.ctx.getObjectHandlerRegistry().serializeObject(new OpenBuilder().build(), buffer);

        this.ctx.getMessageHandlerRegistry().parseMessage(6, buffer, Collections.emptyList());
        this.ctx.getMessageHandlerRegistry().serializeMessage(new KeepaliveBuilder().build(), buffer);

        this.ctx.getVendorInformationObjectRegistry().parseVendorInformationObject(new EnterpriseNumber(10L),
            new ObjectHeaderImpl(true, false), buffer);
        this.ctx.getVendorInformationObjectRegistry().serializeVendorInformationObject(this.esi, buffer);

        this.ctx.getVendorInformationTlvRegistry().parseVendorInformationTlv(new EnterpriseNumber(12L), buffer);
        this.ctx.getVendorInformationTlvRegistry().serializeVendorInformationTlv(this.viTlv, buffer);

        this.activator.start(this.ctx);
        this.activator.close();
    }
}
