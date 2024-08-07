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
import org.opendaylight.protocol.rsvp.parser.spi.LabelParser;
import org.opendaylight.protocol.rsvp.parser.spi.LabelRegistry;
import org.opendaylight.protocol.rsvp.parser.spi.LabelSerializer;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPParsingException;
import org.opendaylight.protocol.util.Values;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.label.subobject.LabelType;
import org.opendaylight.yangtools.binding.DataContainer;
import org.opendaylight.yangtools.concepts.Registration;

public final class SimpleLabelRegistry implements LabelRegistry {
    private final HandlerRegistry<DataContainer, LabelParser, LabelSerializer> handlers = new HandlerRegistry<>();

    public Registration registerLabelParser(final int ctype, final LabelParser parser) {
        checkArgument(ctype >= 0 && ctype <= Values.UNSIGNED_BYTE_MAX_VALUE);
        return this.handlers.registerParser(ctype, parser);
    }

    public Registration registerLabelSerializer(final Class<? extends LabelType> labelClass,
        final LabelSerializer serializer) {
        return this.handlers.registerSerializer(labelClass, serializer);
    }

    @Override
    public LabelType parseLabel(final int ctype, final ByteBuf buffer) throws RSVPParsingException {
        checkArgument(ctype >= 0 && ctype <= Values.UNSIGNED_BYTE_MAX_VALUE);
        final LabelParser parser = this.handlers.getParser(ctype);
        if (parser == null) {
            return null;
        }
        return parser.parseLabel(buffer);
    }

    @Override
    public void serializeLabel(final boolean unidirectional, final boolean global, final LabelType label,
        final ByteBuf buffer) {
        final LabelSerializer serializer = this.handlers.getSerializer(label.implementedInterface());
        if (serializer != null) {
            serializer.serializeLabel(unidirectional, global, label, buffer);
        }
    }
}
