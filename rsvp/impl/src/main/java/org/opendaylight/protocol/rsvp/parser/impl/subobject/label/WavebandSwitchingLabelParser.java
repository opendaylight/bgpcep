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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.label.subobject.label.type.WavebandSwitchingLabelCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.label.subobject.label.type.WavebandSwitchingLabelCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.label.subobject.label.type.waveband.switching.label._case.WavebandSwitchingLabel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.label.subobject.label.type.waveband.switching.label._case.WavebandSwitchingLabelBuilder;

/**
 * Parser for {@link WavebandSwitchingLabelCase}
 */
public class WavebandSwitchingLabelParser implements LabelParser, LabelSerializer {

    public static final int CTYPE = 3;

    private static final int WAVEB_F_LENGTH = 4;
    private static final int START_F_LENGTH = 4;
    private static final int END_F_LENGTH = 4;

    private static final int CONTENT_LENGTH = WAVEB_F_LENGTH + START_F_LENGTH + END_F_LENGTH;

    @Override
    public final LabelType parseLabel(final ByteBuf buffer) throws RSVPParsingException {
        Preconditions.checkArgument(buffer != null && buffer.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        if (buffer.readableBytes() != CONTENT_LENGTH) {
            throw new RSVPParsingException("Wrong length of array of bytes. Passed: " + buffer.readableBytes() + "; Expected: "
                + CONTENT_LENGTH + ".");
        }
        final WavebandSwitchingLabelBuilder builder = new WavebandSwitchingLabelBuilder();
        builder.setWavebandId(buffer.readUnsignedInt());
        builder.setStartLabel(buffer.readUnsignedInt());
        builder.setEndLabel(buffer.readUnsignedInt());
        return new WavebandSwitchingLabelCaseBuilder().setWavebandSwitchingLabel(builder.build()).build();
    }

    @Override
    public final void serializeLabel(final boolean unidirectional, final boolean global, final LabelType subobject,
                                     final ByteBuf buffer) {
        Preconditions.checkArgument(subobject instanceof WavebandSwitchingLabelCase, "Unknown Label Subobject instance. Passed {}. Needed WavebandSwitchingLabelCase.", subobject.getClass());
        final WavebandSwitchingLabel obj = ((WavebandSwitchingLabelCase) subobject).getWavebandSwitchingLabel();
        final ByteBuf body = Unpooled.buffer(CONTENT_LENGTH);
        Preconditions.checkArgument(obj.getWavebandId() != null, "WavebandId is mandatory.");
        writeUnsignedInt(obj.getWavebandId(), body);
        Preconditions.checkArgument(obj.getStartLabel() != null, "StartLabel is mandatory.");
        writeUnsignedInt(obj.getStartLabel(), body);
        Preconditions.checkArgument(obj.getEndLabel() != null, "EndLabel is mandatory.");
        writeUnsignedInt(obj.getEndLabel(), body);
        LabelUtil.formatLabel(CTYPE, unidirectional, global, body, buffer);
    }
}
