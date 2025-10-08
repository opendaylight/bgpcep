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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.protocol.pcep.spi.pojo.SimplePCEPExtensionProviderContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iana.rev130816.EnterpriseNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.Keepalive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.KeepaliveBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.exclude.route.object.xro.Subobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.exclude.route.object.xro.SubobjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.open.object.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.rp.object.Rp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.vendor.information.objects.VendorInformationObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.of.list.tlv.OfList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.of.list.tlv.OfListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.vendor.information.EnterpriseSpecificInformation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.vendor.information.tlvs.VendorInformationTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820._record.route.subobjects.subobject.type.IpPrefixCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820._record.route.subobjects.subobject.type.LabelCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.AsNumberCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.AsNumberCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.IpPrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.label.subobject.LabelType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.label.subobject.label.type.Type1LabelCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.label.subobject.label.type.Type1LabelCaseBuilder;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.common.Uint32;

@ExtendWith(MockitoExtension.class)
class RegistryTest {
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

    private final List<Registration> regs = new ArrayList<>();
    private final PCEPExtensionProviderContext ctx = new SimplePCEPExtensionProviderContext();

    @BeforeEach
    void beforeEach() throws Exception {
        doReturn(null).when(xroParser).parseSubobject(any(ByteBuf.class), anyBoolean());
        doNothing().when(xroSerializer).serializeSubobject(any(Subobject.class), any(ByteBuf.class));

        doReturn(viTlv).when(tlvParser).parseTlv(any(ByteBuf.class));
        doNothing().when(tlvSerializer).serializeTlv(any(Tlv.class), any(ByteBuf.class));

        doReturn(5).when(objectParser).getObjectClass();
        doReturn(1).when(objectParser).getObjectType();
        doReturn(new OpenBuilder().build()).when(objectParser).parseObject(any(ObjectHeader.class),
            any(ByteBuf.class));
        doNothing().when(objectSerializer).serializeObject(any(Object.class), any(ByteBuf.class));

        doReturn(null).when(msgParser).parseMessage(any(ByteBuf.class), anyList());
        doNothing().when(msgSerializer).serializeMessage(any(Message.class), any(ByteBuf.class));

        doNothing().when(labelSerializer).serializeLabel(anyBoolean(), anyBoolean(), any(LabelType.class),
            any(ByteBuf.class));

        doReturn(es).when(esi).getEnterpriseSpecificInformation();

        doReturn(es).when(viTlv).getEnterpriseSpecificInformation();

        doReturn(EnterpriseSpecificInformation.class).when(es).implementedInterface();
    }

    @Test
    void testRegistry() throws Exception {
        regs.add(ctx.registerXROSubobjectParser(2, xroParser));
        regs.add(ctx.registerXROSubobjectSerializer(AsNumberCase.class, xroSerializer));

        regs.add(ctx.registerRROSubobjectParser(3, rroParser));
        regs.add(ctx.registerRROSubobjectSerializer(LabelCase.class, rroSerializer));

        regs.add(ctx.registerEROSubobjectParser(4, eroParser));
        regs.add(ctx.registerEROSubobjectSerializer(IpPrefixCase.class, eroSerializer));

        regs.add(ctx.registerTlvParser(1, tlvParser));
        regs.add(ctx.registerTlvSerializer(OfList.class, tlvSerializer));

        regs.add(ctx.registerObjectParser(objectParser));
        regs.add(ctx.registerObjectSerializer(Rp.class, objectSerializer));

        regs.add(ctx.registerMessageParser(6, msgParser));
        regs.add(ctx.registerMessageSerializer(Keepalive.class, msgSerializer));

        regs.add(ctx.registerLabelParser(7, labelParser));
        regs.add(ctx.registerLabelSerializer(Type1LabelCase.class, labelSerializer));

        regs.add(ctx.registerVendorInformationObjectParser(new EnterpriseNumber(Uint32.TEN),
            objectParser));
        regs.add(ctx.registerVendorInformationObjectSerializer(EnterpriseSpecificInformation.class,
            objectSerializer));

        regs.add(ctx.registerVendorInformationTlvParser(new EnterpriseNumber(Uint32.valueOf(12)),
            tlvParser));
        regs.add(ctx.registerVendorInformationTlvSerializer(EnterpriseSpecificInformation.class,
            tlvSerializer));

        final var buffer = Unpooled.buffer();
        ctx.getXROSubobjectHandlerRegistry().parseSubobject(2, buffer, false);
        ctx.getXROSubobjectHandlerRegistry().serializeSubobject(new SubobjectBuilder()
            .setSubobjectType(new AsNumberCaseBuilder().build()).build(), buffer);

        ctx.getEROSubobjectHandlerRegistry().parseSubobject(3, buffer, true);
        ctx.getEROSubobjectHandlerRegistry().serializeSubobject(new org.opendaylight.yang.gen.v1.urn.opendaylight
            .params.xml.ns.yang.pcep.object.rev250930.explicit.route.object.ero.SubobjectBuilder()
            .setSubobjectType(new AsNumberCaseBuilder().build()).build(), buffer);

        ctx.getRROSubobjectHandlerRegistry().parseSubobject(4, buffer);
        ctx.getRROSubobjectHandlerRegistry().serializeSubobject(new org.opendaylight.yang.gen.v1.urn.opendaylight
            .params.xml.ns.yang.pcep.object.rev250930.reported.route.object.rro.SubobjectBuilder()
            .setSubobjectType(new IpPrefixCaseBuilder().build()).build(), buffer);

        ctx.getLabelHandlerRegistry().parseLabel(1, buffer);
        ctx.getLabelHandlerRegistry().serializeLabel(true, false, new Type1LabelCaseBuilder().build(), buffer);

        ctx.getTlvHandlerRegistry().parseTlv(2, buffer);
        ctx.getTlvHandlerRegistry().serializeTlv(new OfListBuilder().build(), buffer);

        ctx.getObjectHandlerRegistry().parseObject(4, 1, new ObjectHeaderImpl(true, false), buffer);
        ctx.getObjectHandlerRegistry().serializeObject(new OpenBuilder().build(), buffer);

        ctx.getMessageHandlerRegistry().parseMessage(6, buffer, List.of());
        ctx.getMessageHandlerRegistry().serializeMessage(new KeepaliveBuilder().build(), buffer);

        ctx.getVendorInformationObjectRegistry().parseVendorInformationObject(
            new EnterpriseNumber(Uint32.TEN), new ObjectHeaderImpl(true, false), buffer);
        ctx.getVendorInformationObjectRegistry().serializeVendorInformationObject(esi, buffer);

        ctx.getVendorInformationTlvRegistry().parseVendorInformationTlv(
            new EnterpriseNumber(Uint32.valueOf(12)), buffer);
        ctx.getVendorInformationTlvRegistry().serializeVendorInformationTlv(viTlv, buffer);
    }
}
