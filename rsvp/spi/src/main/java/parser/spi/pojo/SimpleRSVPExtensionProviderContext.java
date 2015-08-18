/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package parser.spi.pojo;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.RsvpTeObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.SubobjectType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.label.subobject.LabelType;
import parser.spi.EROSubobjectParser;
import parser.spi.EROSubobjectSerializer;
import parser.spi.LabelParser;
import parser.spi.LabelSerializer;
import parser.spi.RROSubobjectParser;
import parser.spi.RROSubobjectSerializer;
import parser.spi.RSVPExtensionProviderContext;
import parser.spi.RSVPTeObjectParser;
import parser.spi.RSVPTeObjectSerializer;
import parser.spi.XROSubobjectParser;
import parser.spi.XROSubobjectSerializer;

public class SimpleRSVPExtensionProviderContext extends SimpleRSVPExtensionConsumerContext implements RSVPExtensionProviderContext {

    @Override
    public void registerRsvpObjectParser(final int classNum, final int cType, final RSVPTeObjectParser parser) {
        this.getRsvpRegistry().registerRsvpObjectParser(classNum, cType, parser);
    }

    @Override
    public void registerRsvpObjectSerializer(final Class<? extends RsvpTeObject> objectClass, final RSVPTeObjectSerializer serializer) {
        this.getRsvpRegistry().registerRsvpObjectSerializer(objectClass, serializer);
    }

    @Override
    public AutoCloseable registerXROSubobjectSerializer(final Class<? extends SubobjectType> subobjectClass, final XROSubobjectSerializer serializer) {
        return this.getXROSubobjectHandlerRegistry().registerSubobjectSerializer(subobjectClass, serializer);
    }

    @Override
    public AutoCloseable registerXROSubobjectParser(final int subobjectType, final XROSubobjectParser parser) {
        return this.getXROSubobjectHandlerRegistry().registerSubobjectParser(subobjectType, parser);
    }

    @Override
    public AutoCloseable registerRROSubobjectSerializer(final Class<? extends org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.subobjects.SubobjectType> subobjectClass, final RROSubobjectSerializer serializer) {
        return this.getRROSubobjectHandlerRegistry().registerSubobjectSerializer(subobjectClass, serializer);
    }

    @Override
    public AutoCloseable registerRROSubobjectParser(final int subobjectType, final RROSubobjectParser parser) {
        return this.getRROSubobjectHandlerRegistry().registerSubobjectParser(subobjectType, parser);
    }

    @Override
    public AutoCloseable registerEROSubobjectSerializer(final Class<? extends org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.SubobjectType> subobjectClass, final EROSubobjectSerializer serializer) {
        return this.getEROSubobjectHandlerRegistry().registerSubobjectSerializer(subobjectClass, serializer);
    }

    @Override
    public AutoCloseable registerEROSubobjectParser(final int subobjectType, final EROSubobjectParser parser) {
        return this.getEROSubobjectHandlerRegistry().registerSubobjectParser(subobjectType, parser);
    }

    @Override
    public AutoCloseable registerLabelSerializer(final Class<? extends LabelType> labelClass, final LabelSerializer serializer) {
        return this.getLabelHandlerRegistry().registerLabelSerializer(labelClass, serializer);
    }

    @Override
    public AutoCloseable registerLabelParser(final int cType, final LabelParser parser) {
        return this.getLabelHandlerRegistry().registerLabelParser(cType, parser);
    }
}
