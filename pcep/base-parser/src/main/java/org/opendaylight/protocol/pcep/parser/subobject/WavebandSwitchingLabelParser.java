/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.parser.subobject;

import static com.google.common.base.Preconditions.checkArgument;
import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeUnsignedInt;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.pcep.spi.LabelParser;
import org.opendaylight.protocol.pcep.spi.LabelSerializer;
import org.opendaylight.protocol.pcep.spi.LabelUtil;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.util.ByteBufUtils;
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
    public LabelType parseLabel(final ByteBuf buffer) throws PCEPDeserializerException {
        checkArgument(buffer != null && buffer.isReadable(), "Array of bytes is mandatory. Cannot be null or empty.");
        if (buffer.readableBytes() != CONTENT_LENGTH) {
            throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + buffer.readableBytes()
                + "; Expected: " + CONTENT_LENGTH + ".");
        }
        return new WavebandSwitchingLabelCaseBuilder()
                .setWavebandSwitchingLabel(new WavebandSwitchingLabelBuilder()
                    .setWavebandId(ByteBufUtils.readUint32(buffer))
                    .setStartLabel(ByteBufUtils.readUint32(buffer))
                    .setEndLabel(ByteBufUtils.readUint32(buffer))
                    .build())
                .build();
    }

    @Override
    public void serializeLabel(final boolean unidirectional, final boolean global, final LabelType subobject, final ByteBuf buffer) {
        checkArgument(subobject instanceof WavebandSwitchingLabelCase,
            "Unknown Label Subobject instance. Passed {}. Needed WavebandSwitchingLabelCase.", subobject.getClass());
        final WavebandSwitchingLabel obj = ((WavebandSwitchingLabelCase) subobject).getWavebandSwitchingLabel();
        final ByteBuf body = Unpooled.buffer(CONTENT_LENGTH);
        checkArgument(obj.getWavebandId() != null, "WavebandId is mandatory.");
        writeUnsignedInt(obj.getWavebandId(), body);
        checkArgument(obj.getStartLabel() != null, "StartLabel is mandatory.");
        writeUnsignedInt(obj.getStartLabel(), body);
        checkArgument(obj.getEndLabel() != null, "EndLabel is mandatory.");
        writeUnsignedInt(obj.getEndLabel(), body);
        LabelUtil.formatLabel(CTYPE, unidirectional, global, body, buffer);
    }
}
