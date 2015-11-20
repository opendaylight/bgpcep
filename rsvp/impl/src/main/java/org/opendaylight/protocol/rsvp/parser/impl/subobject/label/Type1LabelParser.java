/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.rsvp.parser.impl.subobject.label;

import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeUnsignedInt;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.rsvp.parser.spi.LabelParser;
import org.opendaylight.protocol.rsvp.parser.spi.LabelSerializer;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPParsingException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.label.subobject.LabelType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.label.subobject.label.type.Type1LabelCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.label.subobject.label.type.Type1LabelCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.label.subobject.label.type.type1.label._case.Type1Label;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.label.subobject.label.type.type1.label._case.Type1LabelBuilder;

/**
 * Parser for {@link Type1LabelCase}
 */
public class Type1LabelParser implements LabelParser, LabelSerializer {

    public static final int CTYPE = 1;

    public static final int LABEL_LENGTH = 4;

    @Override
    public LabelType parseLabel(final ByteBuf buffer) throws RSVPParsingException {
        Preconditions.checkArgument(buffer != null && buffer.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        if (buffer.readableBytes() != LABEL_LENGTH) {
            throw new RSVPParsingException("Wrong length of array of bytes. Passed: " + buffer.readableBytes() + "; Expected: "
                + LABEL_LENGTH + ".");
        }
        return new Type1LabelCaseBuilder().setType1Label(new Type1LabelBuilder().setType1Label(buffer.readUnsignedInt()).build()).build();
    }

    @Override
    public void serializeLabel(final boolean unidirectional, final boolean global, final LabelType subobject, final ByteBuf buffer) {
        Preconditions.checkArgument(subobject instanceof Type1LabelCase, "Unknown Label Subobject instance. Passed {}. Needed Type1LabelCase.", subobject.getClass());
        final ByteBuf body = Unpooled.buffer(LABEL_LENGTH);
        final Type1Label type1Label = ((Type1LabelCase) subobject).getType1Label();
        Preconditions.checkArgument(type1Label != null, "Type1Label is mandatory.");
        writeUnsignedInt(type1Label.getType1Label(), body);
        LabelUtil.formatLabel(CTYPE, unidirectional, global, body, buffer);
    }
}
