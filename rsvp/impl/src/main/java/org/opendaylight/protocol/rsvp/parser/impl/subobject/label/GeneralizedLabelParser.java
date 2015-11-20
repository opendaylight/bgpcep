/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.rsvp.parser.impl.subobject.label;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.rsvp.parser.spi.LabelParser;
import org.opendaylight.protocol.rsvp.parser.spi.LabelSerializer;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPParsingException;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.label.subobject.LabelType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.label.subobject.label.type.GeneralizedLabelCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.label.subobject.label.type.GeneralizedLabelCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.label.subobject.label.type.generalized.label._case.GeneralizedLabel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.label.subobject.label.type.generalized.label._case.GeneralizedLabelBuilder;

/**
 * Parser for {@link GeneralizedLabelCase}
 */
public class GeneralizedLabelParser implements LabelParser, LabelSerializer {

    public static final int CTYPE = 2;

    @Override
    public final LabelType parseLabel(final ByteBuf buffer) throws RSVPParsingException {
        Preconditions.checkArgument(buffer != null && buffer.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        return new GeneralizedLabelCaseBuilder().setGeneralizedLabel(
            new GeneralizedLabelBuilder().setGeneralizedLabel(ByteArray.readAllBytes(buffer)).build()).build();
    }

    @Override
    public final void serializeLabel(final boolean unidirectional, final boolean global, final LabelType subobject,
                                     final ByteBuf buffer) {
        Preconditions.checkArgument(subobject instanceof GeneralizedLabelCase, "Unknown Label Subobject instance. Passed {}. Needed GeneralizedLabelCase.", subobject.getClass());
        final GeneralizedLabel gl = ((GeneralizedLabelCase) subobject).getGeneralizedLabel();
        Preconditions.checkArgument(gl != null, "GeneralizedLabel is mandatory.");
        final ByteBuf body = Unpooled.wrappedBuffer(gl.getGeneralizedLabel());
        LabelUtil.formatLabel(CTYPE, unidirectional, global, body, buffer);
    }
}