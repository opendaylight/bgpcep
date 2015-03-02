/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.ietf.stateful07;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.TlvParser;
import org.opendaylight.protocol.pcep.spi.TlvSerializer;
import org.opendaylight.protocol.pcep.spi.TlvUtil;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.stateful.capability.tlv.Stateful;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.stateful.capability.tlv.StatefulBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;

/**
 * Parser for {@link Stateful}
 */
public class Stateful07StatefulCapabilityTlvParser implements TlvParser, TlvSerializer {

    public static final int TYPE = 16;

    protected static final int FLAGS_F_LENGTH = 32;

    protected static final int U_FLAG_OFFSET = 31;

    @Override
    public Stateful parseTlv(final ByteBuf buffer) throws PCEPDeserializerException {
        if (buffer == null) {
            return null;
        }
        if (buffer.readableBytes() < FLAGS_F_LENGTH / Byte.SIZE) {
            throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + buffer.readableBytes() + "; Expected: >= "
                    + FLAGS_F_LENGTH / Byte.SIZE + ".");
        }
        final BitArray flags = BitArray.valueOf(buffer, FLAGS_F_LENGTH);
        final StatefulBuilder sb = new StatefulBuilder();
        sb.setLspUpdateCapability(flags.get(U_FLAG_OFFSET));
        return sb.build();
    }

    @Override
    public void serializeTlv(final Tlv tlv, final ByteBuf buffer) {
        Preconditions.checkArgument(tlv instanceof Stateful, "StatefulCapabilityTlv is mandatory.");
        final Stateful sct = (Stateful) tlv;
        final BitArray flags = new BitArray(FLAGS_F_LENGTH);
        flags.set(U_FLAG_OFFSET, sct.isLspUpdateCapability());
        TlvUtil.formatTlv(TYPE, Unpooled.wrappedBuffer(flags.array()), buffer);
    }
}
