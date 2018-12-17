/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.spi.pojo;

import static com.google.common.base.Preconditions.checkArgument;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.concepts.HandlerRegistry;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.RROSubobjectParser;
import org.opendaylight.protocol.pcep.spi.RROSubobjectRegistry;
import org.opendaylight.protocol.pcep.spi.RROSubobjectSerializer;
import org.opendaylight.protocol.util.Values;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.reported.route.object.rro.Subobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.subobjects.SubobjectType;
import org.opendaylight.yangtools.yang.binding.DataContainer;

public final class SimpleRROSubobjectRegistry implements RROSubobjectRegistry {
    private final HandlerRegistry<DataContainer, RROSubobjectParser, RROSubobjectSerializer> handlers =
            new HandlerRegistry<>();

    public AutoCloseable registerSubobjectParser(final int subobjectType, final RROSubobjectParser parser) {
        checkArgument(subobjectType >= 0 && subobjectType <= Values.UNSIGNED_SHORT_MAX_VALUE);
        return this.handlers.registerParser(subobjectType, parser);
    }

    public AutoCloseable registerSubobjectSerializer(final Class<? extends SubobjectType> subobjectClass,
            final RROSubobjectSerializer serializer) {
        return this.handlers.registerSerializer(subobjectClass, serializer);
    }

    @Override
    public Subobject parseSubobject(final int type, final ByteBuf buffer) throws PCEPDeserializerException {
        checkArgument(type >= 0 && type <= Values.UNSIGNED_SHORT_MAX_VALUE);
        final RROSubobjectParser parser = this.handlers.getParser(type);
        return parser == null ? null : parser.parseSubobject(buffer);
    }

    @Override
    public void serializeSubobject(final Subobject subobject, final ByteBuf buffer) {
        final RROSubobjectSerializer serializer = this.handlers.getSerializer(
            subobject.getSubobjectType().getImplementedInterface());
        if (serializer != null) {
            serializer.serializeSubobject(subobject, buffer);
        }
    }
}
