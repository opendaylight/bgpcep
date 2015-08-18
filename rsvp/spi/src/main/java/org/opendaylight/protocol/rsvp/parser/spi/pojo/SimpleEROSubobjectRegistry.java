/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.rsvp.parser.spi.pojo;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.concepts.HandlerRegistry;
import org.opendaylight.protocol.rsvp.parser.spi.EROSubobjectParser;
import org.opendaylight.protocol.rsvp.parser.spi.EROSubobjectRegistry;
import org.opendaylight.protocol.rsvp.parser.spi.EROSubobjectSerializer;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPParsingException;
import org.opendaylight.protocol.util.Values;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.SubobjectType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.explicit.route.subobjects.list.SubobjectContainer;
import org.opendaylight.yangtools.yang.binding.DataContainer;

public final class SimpleEROSubobjectRegistry implements EROSubobjectRegistry {
    private final HandlerRegistry<DataContainer, EROSubobjectParser, EROSubobjectSerializer> handlers = new HandlerRegistry<>();

    public AutoCloseable registerSubobjectParser(final int subobjectType, final EROSubobjectParser parser) {
        Preconditions.checkArgument(subobjectType >= 0 && subobjectType <= Values.UNSIGNED_SHORT_MAX_VALUE);
        return this.handlers.registerParser(subobjectType, parser);
    }

    public AutoCloseable registerSubobjectSerializer(final Class<? extends SubobjectType> subobjectClass,
                                                     final EROSubobjectSerializer serializer) {
        return this.handlers.registerSerializer(subobjectClass, serializer);
    }

    @Override
    public SubobjectContainer parseSubobject(final int type, final ByteBuf buffer, final boolean loose) throws RSVPParsingException {
        Preconditions.checkArgument(type >= 0 && type <= Values.UNSIGNED_SHORT_MAX_VALUE);
        final EROSubobjectParser parser = this.handlers.getParser(type);
        if (parser == null) {
            return null;
        }
        return parser.parseSubobject(buffer, loose);
    }

    @Override
    public void serializeSubobject(final SubobjectContainer subobject, final ByteBuf buffer) {
        final EROSubobjectSerializer serializer = this.handlers.getSerializer(subobject.getSubobjectType().getImplementedInterface());
        if (serializer == null) {
            return;
        }
        serializer.serializeSubobject(subobject, buffer);
    }
}
