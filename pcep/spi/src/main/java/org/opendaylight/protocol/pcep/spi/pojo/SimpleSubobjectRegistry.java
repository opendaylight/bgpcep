/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.spi.pojo;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.concepts.HandlerRegistry;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.SubobjectParser;
import org.opendaylight.protocol.pcep.spi.SubobjectRegistry;
import org.opendaylight.protocol.pcep.spi.SubobjectSerializer;
import org.opendaylight.protocol.util.Values;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.CSubobject;
import org.opendaylight.yangtools.yang.binding.DataContainer;

public class SimpleSubobjectRegistry implements SubobjectRegistry {

    private final HandlerRegistry<DataContainer, SubobjectParser, SubobjectSerializer> handlers = new HandlerRegistry<>();

    public AutoCloseable registerSubobjectParser(final int subType, final SubobjectParser parser) {
        Preconditions.checkArgument(subType >= 0 && subType < Values.UNSIGNED_SHORT_MAX_VALUE);
        return this.handlers.registerParser(subType, parser);
    }

    public AutoCloseable registerSubobjectSerializer(final Class<? extends CSubobject> subClass, final SubobjectSerializer serializer) {
        return this.handlers.registerSerializer(subClass, serializer);
    }

    @Override
    public CSubobject parseSubobject(final int subType, final ByteBuf buffer) throws PCEPDeserializerException {
        Preconditions.checkArgument(subType >= 0 && subType <= Values.UNSIGNED_SHORT_MAX_VALUE);
        final SubobjectParser parser = this.handlers.getParser(subType);
        if (parser == null) {
            return null;
        }
        return parser.parseSubobject(buffer);
    }

    @Override
    public void serializeSubobject(final CSubobject subobject, final ByteBuf buffer) {
        final SubobjectSerializer serializer = this.handlers.getSerializer(subobject.getImplementedInterface());
        if (serializer == null) {
            return;
        }
        serializer.serializeSubobject(subobject, buffer);
    }
}
