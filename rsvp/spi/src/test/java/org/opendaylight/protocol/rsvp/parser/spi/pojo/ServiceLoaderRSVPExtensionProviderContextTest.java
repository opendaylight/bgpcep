/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.rsvp.parser.spi.pojo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
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

public class ServiceLoaderRSVPExtensionProviderContextTest {

    private final SimpleRSVPExtensionProviderContext context = (SimpleRSVPExtensionProviderContext)
        ServiceLoaderRSVPExtensionProviderContext.getSingletonInstance();

    private final RSVPTeObjectParser rsvpTeParser = Mockito.mock(RSVPTeObjectParser.class);
    private final RSVPTeObjectSerializer rsvpTeSerializer = Mockito.mock(RSVPTeObjectSerializer.class);
    private final RsvpTeObject parsedRsvpTeObj = Mockito.mock(RsvpTeObject.class);

    private final XROSubobjectParser xroObjParser = Mockito.mock(XROSubobjectParser.class);
    private final XROSubobjectSerializer xroObjSerializer = Mockito.mock(XROSubobjectSerializer.class);
    private final SubobjectContainer subObj = Mockito.mock(SubobjectContainer.class);
    private final SubobjectType subObjType = Mockito.mock(SubobjectType.class);

    private final RROSubobjectParser rroParser = Mockito.mock(RROSubobjectParser.class);
    private final RROSubobjectSerializer rroSerializer = Mockito.mock(RROSubobjectSerializer.class);
    private final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route
        .subobjects.list.SubobjectContainer rroSubObj = Mockito.mock(org.opendaylight.yang.gen.v1.urn.opendaylight
        .params.xml.ns.yang.rsvp.rev150820.record.route.subobjects.list.SubobjectContainer.class);
    private final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route
        .subobjects.SubobjectType rroSubObjType = Mockito.mock(org.opendaylight.yang.gen.v1.urn.opendaylight.params
        .xml.ns.yang.rsvp.rev150820.record.route.subobjects.SubobjectType.class);

    private final EROSubobjectParser eroParser = Mockito.mock(EROSubobjectParser.class);
    private final EROSubobjectSerializer eroSerializer = Mockito.mock(EROSubobjectSerializer.class);
    private final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.explicit.route
        .subobjects.list.SubobjectContainer eroSubObj = Mockito.mock(org.opendaylight.yang.gen.v1.urn.opendaylight
        .params.xml.ns.yang.rsvp.rev150820.explicit.route.subobjects.list.SubobjectContainer.class);

    private final LabelParser labelParser = Mockito.mock(LabelParser.class);
    private final LabelSerializer labelSerializer = Mockito.mock(LabelSerializer.class);
    private final LabelType labelType = Mockito.mock(LabelType.class);

    @Before
    public void setUp() throws RSVPParsingException {
        Mockito.doReturn(this.parsedRsvpTeObj).when(this.rsvpTeParser).parseObject(Mockito.any(ByteBuf.class));
        Mockito.doReturn(RsvpTeObject.class).when(this.parsedRsvpTeObj).getImplementedInterface();
        Mockito.doReturn("parsedRsvpTeObj").when(this.parsedRsvpTeObj).toString();
        Mockito.doNothing().when(this.rsvpTeSerializer).serializeObject(Mockito.any(RsvpTeObject.class),
            Mockito.any(ByteBuf.class));

        Mockito.doReturn(this.subObj).when(this.xroObjParser).parseSubobject(Mockito.any(ByteBuf.class),
            Mockito.any(Boolean.class));
        Mockito.doReturn(this.subObjType).when(this.subObj).getSubobjectType();
        Mockito.doReturn("SubobjectContainer").when(this.subObj).toString();
        Mockito.doReturn(SubobjectType.class).when(this.subObjType).getImplementedInterface();
        Mockito.doNothing().when(this.xroObjSerializer).serializeSubobject(Mockito.any(SubobjectContainer.class),
            Mockito.any(ByteBuf.class));

        Mockito.doReturn(this.rroSubObj).when(this.rroParser).parseSubobject(Mockito.any(ByteBuf.class));
        Mockito.doReturn(this.rroSubObjType).when(this.rroSubObj).getSubobjectType();
        Mockito.doReturn("SubobjectContainer").when(this.rroSubObj).toString();
        Mockito.doReturn(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route
            .subobjects.SubobjectType.class).when(this.rroSubObjType).getImplementedInterface();
        Mockito.doNothing().when(this.rroSerializer).serializeSubobject(Mockito.any(org.opendaylight.yang.gen.v1.urn
                .opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.subobjects.list.SubobjectContainer.class),
            Mockito.any(ByteBuf.class));

        Mockito.doReturn(this.eroSubObj).when(this.eroParser).parseSubobject(Mockito.any(ByteBuf.class),
            Mockito.any(Boolean.class));
        Mockito.doReturn(this.subObjType).when(this.eroSubObj).getSubobjectType();
        Mockito.doReturn("EROSubobjectContainer").when(this.eroSubObj).toString();
        Mockito.doReturn(SubobjectType.class).when(this.subObjType).getImplementedInterface();
        Mockito.doNothing().when(this.eroSerializer).serializeSubobject(Mockito.any(org.opendaylight.yang.gen.v1.urn
            .opendaylight.params.xml.ns.yang.rsvp.rev150820.explicit.route.subobjects.list.SubobjectContainer.class),
            Mockito.any(ByteBuf.class));

        Mockito.doReturn(this.labelType).when(this.labelParser).parseLabel(Mockito.any(ByteBuf.class));
        Mockito.doReturn(LabelType.class).when(this.labelType).getImplementedInterface();
        Mockito.doReturn("LabelType").when(this.labelType).toString();
        Mockito.doNothing().when(this.labelSerializer).serializeLabel(Mockito.anyBoolean(), Mockito.anyBoolean(),
            Mockito.any(LabelType.class), Mockito.any(ByteBuf.class));
    }

    @Test
    public void testReferenceCache() {
        assertNotNull(this.context.getReferenceCache());
    }

    @Test
    public void testServiceForRsvpObject() throws RSVPParsingException {
        this.context.registerRsvpObjectParser(1, 1, this.rsvpTeParser);
        final ByteBuf buffer = Unpooled.buffer();
        assertEquals(this.parsedRsvpTeObj, this.context.getRsvpRegistry().parseRSPVTe(1, 1, buffer));
        this.context.registerRsvpObjectSerializer(RsvpTeObject.class, this.rsvpTeSerializer);
        this.context.getRsvpRegistry().serializeRSPVTe(this.parsedRsvpTeObj, buffer);
        Mockito.verify(this.rsvpTeSerializer).serializeObject(Mockito.any(RsvpTeObject.class),
            Mockito.any(ByteBuf.class));
    }

    @Test
    public void testServiceForXROSubobject() throws RSVPParsingException {
        this.context.registerXROSubobjectParser(2, this.xroObjParser);
        final ByteBuf buffer = Unpooled.buffer();
        assertEquals(this.subObj, this.context.getXROSubobjectHandlerRegistry().parseSubobject(2, buffer, false));
        this.context.registerXROSubobjectSerializer(SubobjectType.class, this.xroObjSerializer);
        this.context.getXROSubobjectHandlerRegistry().serializeSubobject(this.subObj, buffer);
        Mockito.verify(this.xroObjSerializer).serializeSubobject(this.subObj, buffer);
    }

    @Test
    public void testServiceForRROSubobject() throws RSVPParsingException {
        this.context.registerRROSubobjectParser(3, this.rroParser);
        final ByteBuf buffer = Unpooled.buffer();
        assertEquals(this.rroSubObj, this.context.getRROSubobjectHandlerRegistry().parseSubobject(3, buffer));
        this.context.registerRROSubobjectSerializer(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
            .rsvp.rev150820.record.route.subobjects.SubobjectType.class, this.rroSerializer);
        this.context.getRROSubobjectHandlerRegistry().serializeSubobject(this.rroSubObj, buffer);
        Mockito.verify(this.rroSerializer).serializeSubobject(this.rroSubObj, buffer);
    }

    @Test
    public void testServiceForEROSubobject() throws RSVPParsingException {
        this.context.registerEROSubobjectParser(4, this.eroParser);
        final ByteBuf buffer = Unpooled.buffer();
        assertEquals(this.eroSubObj, this.context.getEROSubobjectHandlerRegistry().parseSubobject(4, buffer, false));
        this.context.registerEROSubobjectSerializer(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
            .rsvp.rev150820.basic.explicit.route.subobjects.SubobjectType.class, this.eroSerializer);
        this.context.getEROSubobjectHandlerRegistry().serializeSubobject(this.eroSubObj, buffer);
        Mockito.verify(this.eroSerializer).serializeSubobject(this.eroSubObj, buffer);
    }

    @Test
    public void testServiceForLabel() throws RSVPParsingException {
        this.context.registerLabelParser(5, this.labelParser);
        final ByteBuf buffer = Unpooled.buffer();
        assertEquals(this.labelType, this.context.getLabelHandlerRegistry().parseLabel(5, buffer));
        this.context.registerLabelSerializer(LabelType.class, this.labelSerializer);
        this.context.getLabelHandlerRegistry().serializeLabel(false, false, this.labelType, buffer);
        Mockito.verify(this.labelSerializer).serializeLabel(false, false, this.labelType, buffer);
    }

    @Test(expected = UnsupportedOperationException.class)
    @SuppressWarnings({ "checkstyle:IllegalThrows", "checkstyle:avoidHidingCauseException" })
    public void testPrivateConstructor() throws Throwable {
        final Constructor<ServiceLoaderRSVPExtensionProviderContext> c =
            ServiceLoaderRSVPExtensionProviderContext.class.getDeclaredConstructor();
        c.setAccessible(true);
        try {
            c.newInstance();
        } catch (final InvocationTargetException e) {
            throw e.getCause();
        }
    }

}