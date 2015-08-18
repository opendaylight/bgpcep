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
import org.opendaylight.protocol.rsvp.parser.spi.LabelParser;
import org.opendaylight.protocol.rsvp.parser.spi.LabelRegistry;
import org.opendaylight.protocol.rsvp.parser.spi.LabelSerializer;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPParsingException;
import org.opendaylight.protocol.util.Values;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.label.subobject.LabelType;
import org.opendaylight.yangtools.yang.binding.DataContainer;

public final class SimpleLabelRegistry implements LabelRegistry {
    private final HandlerRegistry<DataContainer, LabelParser, LabelSerializer> handlers = new HandlerRegistry<>();

    public AutoCloseable registerLabelParser(final int cType, final LabelParser parser) {
        Preconditions.checkArgument(cType >= 0 && cType <= Values.UNSIGNED_BYTE_MAX_VALUE);
        return this.handlers.registerParser(cType, parser);
    }

    public AutoCloseable registerLabelSerializer(final Class<? extends LabelType> labelClass, final LabelSerializer serializer) {
        return this.handlers.registerSerializer(labelClass, serializer);
    }

    @Override
    public LabelType parseLabel(final int cType, final ByteBuf buffer) throws RSVPParsingException {
        Preconditions.checkArgument(cType >= 0 && cType <= Values.UNSIGNED_BYTE_MAX_VALUE);
        final LabelParser parser = this.handlers.getParser(cType);
        if (parser == null) {
            return null;
        }
        return parser.parseLabel(buffer);
    }

    @Override
    public void serializeLabel(final boolean unidirectional, final boolean global, final LabelType label, final ByteBuf buffer) {
        final LabelSerializer serializer = this.handlers.getSerializer(label.getImplementedInterface());
        if (serializer != null) {
            serializer.serializeLabel(unidirectional, global, label, buffer);
        }
    }
}
