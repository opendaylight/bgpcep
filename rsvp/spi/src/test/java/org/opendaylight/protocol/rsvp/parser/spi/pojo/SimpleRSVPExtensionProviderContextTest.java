/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.rsvp.parser.spi.pojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opendaylight.protocol.rsvp.parser.spi.EROSubobjectParser;
import org.opendaylight.protocol.rsvp.parser.spi.EROSubobjectSerializer;
import org.opendaylight.protocol.rsvp.parser.spi.LabelParser;
import org.opendaylight.protocol.rsvp.parser.spi.LabelSerializer;
import org.opendaylight.protocol.rsvp.parser.spi.RROSubobjectParser;
import org.opendaylight.protocol.rsvp.parser.spi.RROSubobjectSerializer;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPParsingException;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPTeObjectParser;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPTeObjectSerializer;
import org.opendaylight.protocol.rsvp.parser.spi.XROSubobjectParser;
import org.opendaylight.protocol.rsvp.parser.spi.XROSubobjectSerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.RsvpTeObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.SubobjectType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.exclude.route.object.exclude.route.object.SubobjectContainer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.label.subobject.LabelType;

class SimpleRSVPExtensionProviderContextTest {

    private final SimpleRSVPExtensionProviderContext context = new SimpleRSVPExtensionProviderContext();

    // FIXME: @Mock
    private final RSVPTeObjectParser rsvpTeParser = mock(RSVPTeObjectParser.class);
    private final RSVPTeObjectSerializer rsvpTeSerializer = mock(RSVPTeObjectSerializer.class);
    private final RsvpTeObject parsedRsvpTeObj = mock(RsvpTeObject.class);

    private final XROSubobjectParser xroObjParser = mock(XROSubobjectParser.class);
    private final XROSubobjectSerializer xroObjSerializer = mock(XROSubobjectSerializer.class);
    private final SubobjectContainer subObj = mock(SubobjectContainer.class);
    private final SubobjectType subObjType = mock(SubobjectType.class);

    private final RROSubobjectParser rroParser = mock(RROSubobjectParser.class);
    private final RROSubobjectSerializer rroSerializer = mock(RROSubobjectSerializer.class);
    private final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820._record.route
        .subobjects.list.SubobjectContainer rroSubObj = mock(org.opendaylight.yang.gen.v1.urn.opendaylight
        .params.xml.ns.yang.rsvp.rev150820._record.route.subobjects.list.SubobjectContainer.class);
    private final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820._record.route
        .subobjects.SubobjectType rroSubObjType = mock(org.opendaylight.yang.gen.v1.urn.opendaylight.params
        .xml.ns.yang.rsvp.rev150820._record.route.subobjects.SubobjectType.class);

    private final EROSubobjectParser eroParser = mock(EROSubobjectParser.class);
    private final EROSubobjectSerializer eroSerializer = mock(EROSubobjectSerializer.class);
    private final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.explicit.route
        .subobjects.list.SubobjectContainer eroSubObj = mock(org.opendaylight.yang.gen.v1.urn.opendaylight
        .params.xml.ns.yang.rsvp.rev150820.explicit.route.subobjects.list.SubobjectContainer.class);

    private final LabelParser labelParser = mock(LabelParser.class);
    private final LabelSerializer labelSerializer = mock(LabelSerializer.class);
    private final LabelType labelType = mock(LabelType.class);

    @BeforeEach
    void setUp() throws RSVPParsingException {
        doReturn(parsedRsvpTeObj).when(rsvpTeParser).parseObject(any(ByteBuf.class));
        doReturn(RsvpTeObject.class).when(parsedRsvpTeObj).implementedInterface();
        doReturn("parsedRsvpTeObj").when(parsedRsvpTeObj).toString();
        doNothing().when(rsvpTeSerializer).serializeObject(any(RsvpTeObject.class), any(ByteBuf.class));

        doReturn(subObj).when(xroObjParser).parseSubobject(any(ByteBuf.class), any(Boolean.class));
        doReturn(subObjType).when(subObj).getSubobjectType();
        doReturn("SubobjectContainer").when(subObj).toString();
        doReturn(SubobjectType.class).when(subObjType).implementedInterface();
        doNothing().when(xroObjSerializer).serializeSubobject(any(SubobjectContainer.class),
            any(ByteBuf.class));

        doReturn(rroSubObj).when(rroParser).parseSubobject(any(ByteBuf.class));
        doReturn(rroSubObjType).when(rroSubObj).getSubobjectType();
        doReturn("SubobjectContainer").when(rroSubObj).toString();
        doReturn(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820._record.route
            .subobjects.SubobjectType.class).when(rroSubObjType).implementedInterface();
        doNothing().when(rroSerializer).serializeSubobject(any(org.opendaylight.yang.gen.v1.urn
                .opendaylight.params.xml.ns.yang.rsvp.rev150820._record.route.subobjects.list.SubobjectContainer.class),
            any(ByteBuf.class));

        doReturn(eroSubObj).when(eroParser).parseSubobject(any(ByteBuf.class),
            any(Boolean.class));
        doReturn(subObjType).when(eroSubObj).getSubobjectType();
        doReturn("EROSubobjectContainer").when(eroSubObj).toString();
        doReturn(SubobjectType.class).when(subObjType).implementedInterface();
        doNothing().when(eroSerializer).serializeSubobject(any(org.opendaylight.yang.gen.v1.urn
            .opendaylight.params.xml.ns.yang.rsvp.rev150820.explicit.route.subobjects.list.SubobjectContainer.class),
            any(ByteBuf.class));

        doReturn(labelType).when(labelParser).parseLabel(any(ByteBuf.class));
        doReturn(LabelType.class).when(labelType).implementedInterface();
        doReturn("LabelType").when(labelType).toString();
        doNothing().when(labelSerializer).serializeLabel(anyBoolean(), anyBoolean(), any(LabelType.class),
            any(ByteBuf.class));
    }

    @Test
    void testReferenceCache() {
        assertNotNull(context.getReferenceCache());
    }

    @Test
    void testServiceForRsvpObject() throws RSVPParsingException {
        context.registerRsvpObjectParser(1, 1, rsvpTeParser);
        final ByteBuf buffer = Unpooled.buffer();
        assertEquals(parsedRsvpTeObj, context.getRsvpRegistry().parseRSPVTe(1, 1, buffer));
        context.registerRsvpObjectSerializer(RsvpTeObject.class, rsvpTeSerializer);
        context.getRsvpRegistry().serializeRSPVTe(parsedRsvpTeObj, buffer);
        verify(rsvpTeSerializer).serializeObject(any(RsvpTeObject.class), any(ByteBuf.class));
    }

    @Test
    void testServiceForXROSubobject() throws RSVPParsingException {
        context.registerXROSubobjectParser(2, xroObjParser);
        final ByteBuf buffer = Unpooled.buffer();
        assertEquals(subObj, context.getXROSubobjectHandlerRegistry().parseSubobject(2, buffer, false));
        context.registerXROSubobjectSerializer(SubobjectType.class, xroObjSerializer);
        context.getXROSubobjectHandlerRegistry().serializeSubobject(subObj, buffer);
        verify(xroObjSerializer).serializeSubobject(subObj, buffer);
    }

    @Test
    void testServiceForRROSubobject() throws RSVPParsingException {
        context.registerRROSubobjectParser(3, rroParser);
        final ByteBuf buffer = Unpooled.buffer();
        assertEquals(rroSubObj, context.getRROSubobjectHandlerRegistry().parseSubobject(3, buffer));
        context.registerRROSubobjectSerializer(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
            .rsvp.rev150820._record.route.subobjects.SubobjectType.class, rroSerializer);
        context.getRROSubobjectHandlerRegistry().serializeSubobject(rroSubObj, buffer);
        verify(rroSerializer).serializeSubobject(rroSubObj, buffer);
    }

    @Test
    void testServiceForEROSubobject() throws RSVPParsingException {
        context.registerEROSubobjectParser(4, eroParser);
        final ByteBuf buffer = Unpooled.buffer();
        assertEquals(eroSubObj, context.getEROSubobjectHandlerRegistry().parseSubobject(4, buffer, false));
        context.registerEROSubobjectSerializer(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
            .rsvp.rev150820.basic.explicit.route.subobjects.SubobjectType.class, eroSerializer);
        context.getEROSubobjectHandlerRegistry().serializeSubobject(eroSubObj, buffer);
        verify(eroSerializer).serializeSubobject(eroSubObj, buffer);
    }

    @Test
    void testServiceForLabel() throws RSVPParsingException {
        context.registerLabelParser(5, labelParser);
        final ByteBuf buffer = Unpooled.buffer();
        assertEquals(labelType, context.getLabelHandlerRegistry().parseLabel(5, buffer));
        context.registerLabelSerializer(LabelType.class, labelSerializer);
        context.getLabelHandlerRegistry().serializeLabel(false, false, labelType, buffer);
        verify(labelSerializer).serializeLabel(false, false, labelType, buffer);
    }
}
