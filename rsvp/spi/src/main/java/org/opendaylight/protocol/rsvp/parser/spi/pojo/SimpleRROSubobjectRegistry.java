/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.rsvp.parser.spi.pojo;

import static com.google.common.base.Preconditions.checkArgument;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.concepts.HandlerRegistry;
import org.opendaylight.protocol.rsvp.parser.spi.RROSubobjectParser;
import org.opendaylight.protocol.rsvp.parser.spi.RROSubobjectRegistry;
import org.opendaylight.protocol.rsvp.parser.spi.RROSubobjectSerializer;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPParsingException;
import org.opendaylight.protocol.util.Values;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820._record.route.subobjects.SubobjectType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820._record.route.subobjects.list.SubobjectContainer;
import org.opendaylight.yangtools.binding.DataContainer;
import org.opendaylight.yangtools.concepts.Registration;

public final class SimpleRROSubobjectRegistry implements RROSubobjectRegistry {
    private final HandlerRegistry<DataContainer, RROSubobjectParser, RROSubobjectSerializer> handlers =
        new HandlerRegistry<>();

    public Registration registerSubobjectParser(final int subobjectType, final RROSubobjectParser parser) {
        checkArgument(subobjectType >= 0 && subobjectType <= Values.UNSIGNED_SHORT_MAX_VALUE);
        return this.handlers.registerParser(subobjectType, parser);
    }

    public Registration registerSubobjectSerializer(final Class<? extends SubobjectType> subobjectClass,
        final RROSubobjectSerializer serializer) {
        return this.handlers.registerSerializer(subobjectClass, serializer);
    }

    @Override
    public SubobjectContainer parseSubobject(final int type, final ByteBuf buffer) throws RSVPParsingException {
        checkArgument(type >= 0 && type <= Values.UNSIGNED_SHORT_MAX_VALUE);
        final RROSubobjectParser parser = this.handlers.getParser(type);
        if (parser == null) {
            return null;
        }
        return parser.parseSubobject(buffer);
    }

    @Override
    public void serializeSubobject(final SubobjectContainer subobject, final ByteBuf buffer) {
        final RROSubobjectSerializer serializer = this.handlers.getSerializer(subobject.getSubobjectType()
            .implementedInterface());
        if (serializer == null) {
            return;
        }
        serializer.serializeSubobject(subobject, buffer);
    }
}
