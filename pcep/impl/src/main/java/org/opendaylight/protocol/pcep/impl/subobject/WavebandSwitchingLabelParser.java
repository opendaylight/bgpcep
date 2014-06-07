/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.subobject;

import com.google.common.base.Preconditions;

import io.netty.buffer.ByteBuf;

import org.opendaylight.protocol.pcep.spi.LabelParser;
import org.opendaylight.protocol.pcep.spi.LabelSerializer;
import org.opendaylight.protocol.pcep.spi.LabelUtil;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.label.subobject.LabelType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.label.subobject.label.type.WavebandSwitchingLabelCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.label.subobject.label.type.WavebandSwitchingLabelCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.label.subobject.label.type.waveband.switching.label._case.WavebandSwitchingLabel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.label.subobject.label.type.waveband.switching.label._case.WavebandSwitchingLabelBuilder;

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
        Preconditions.checkArgument(buffer != null && buffer.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        if (buffer.readableBytes() != CONTENT_LENGTH) {
            throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + buffer.readableBytes() + "; Expected: "
                    + CONTENT_LENGTH + ".");
        }
        final WavebandSwitchingLabelBuilder builder = new WavebandSwitchingLabelBuilder();
        builder.setWavebandId(buffer.readUnsignedInt());
        builder.setStartLabel(buffer.readUnsignedInt());
        builder.setEndLabel(buffer.readUnsignedInt());
        return new WavebandSwitchingLabelCaseBuilder().setWavebandSwitchingLabel(builder.build()).build();
    }

    @Override
    public byte[] serializeLabel(final boolean unidirectional, final boolean global, final LabelType subobject) {
        if (!(subobject instanceof WavebandSwitchingLabelCase)) {
            throw new IllegalArgumentException("Unknown Label Subobject instance. Passed " + subobject.getClass()
                    + ". Needed WavebandSwitchingLabelCase.");
        }
        final byte[] retBytes = new byte[CONTENT_LENGTH];
        final WavebandSwitchingLabel obj = ((WavebandSwitchingLabelCase) subobject).getWavebandSwitchingLabel();
        System.arraycopy(ByteArray.intToBytes(obj.getWavebandId().intValue(), WAVEB_F_LENGTH), 0, retBytes, 0, WAVEB_F_LENGTH);
        System.arraycopy(ByteArray.intToBytes(obj.getStartLabel().intValue(), START_F_LENGTH), 0, retBytes, WAVEB_F_LENGTH, START_F_LENGTH);
        System.arraycopy(ByteArray.intToBytes(obj.getEndLabel().intValue(), END_F_LENGTH), 0, retBytes, WAVEB_F_LENGTH + START_F_LENGTH,
                END_F_LENGTH);
        return LabelUtil.formatLabel(CTYPE, unidirectional, global, retBytes);
    }
}
