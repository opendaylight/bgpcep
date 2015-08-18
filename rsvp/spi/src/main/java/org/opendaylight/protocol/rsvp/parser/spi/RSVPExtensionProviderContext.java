/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.rsvp.parser.spi;

import org.opendaylight.protocol.util.ReferenceCache;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.RsvpTeObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.SubobjectType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.label.subobject.LabelType;

public interface RSVPExtensionProviderContext extends RSVPExtensionConsumerContext {
    void registerRsvpObjectParser(int classNum, int cType, RSVPTeObjectParser parser);

    void registerRsvpObjectSerializer(Class<? extends RsvpTeObject> objectClass, RSVPTeObjectSerializer serializer);

    AutoCloseable registerXROSubobjectSerializer(Class<? extends SubobjectType> subobjectClass, XROSubobjectSerializer serializer);

    AutoCloseable registerXROSubobjectParser(int subobjectType, XROSubobjectParser parser);

    AutoCloseable registerRROSubobjectSerializer(Class<? extends org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.subobjects.SubobjectType> subobjectClass, RROSubobjectSerializer serializer);

    AutoCloseable registerRROSubobjectParser(int subobjectType, RROSubobjectParser parser);

    AutoCloseable registerEROSubobjectSerializer(Class<? extends org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.SubobjectType> subobjectClass, EROSubobjectSerializer serializer);

    AutoCloseable registerEROSubobjectParser(int subobjectType, EROSubobjectParser parser);

    AutoCloseable registerLabelSerializer(Class<? extends LabelType> labelClass, LabelSerializer serializer);

    AutoCloseable registerLabelParser(int cType, LabelParser parser);

    /**
     * Get the context-wide cache for a particular object type.
     *
     * @return An object cache instance.
     */
    ReferenceCache getReferenceCache();
}
