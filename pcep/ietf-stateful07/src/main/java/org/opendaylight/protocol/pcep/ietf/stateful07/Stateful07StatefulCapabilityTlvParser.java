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
    protected static final int S_FLAG_OFFSET = 30;
    protected static final int I_FLAG_OFFSET = 29;
    protected static final int T_FLAG_OFFSET = 28;
    protected static final int D_FLAG_OFFSET = 27;
    protected static final int F_FLAG_OFFSET = 26;

    @Override
    public Stateful parseTlv(final ByteBuf buffer) throws PCEPDeserializerException {
        if (buffer == null) {
            return null;
        }
        if (buffer.readableBytes() < FLAGS_F_LENGTH / Byte.SIZE) {
            throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + buffer.readableBytes() + "; Expected: >= "
                    + FLAGS_F_LENGTH / Byte.SIZE + ".");
        }
        final StatefulBuilder sb = new StatefulBuilder();
        parseFlags(sb, buffer);
        return sb.build();
    }

    protected void parseFlags(final StatefulBuilder sb, final ByteBuf buffer) {
        final BitArray flags = BitArray.valueOf(buffer, FLAGS_F_LENGTH);
        sb.setLspUpdateCapability(flags.get(U_FLAG_OFFSET));
        sb.setIncludeDbVersion(flags.get(S_FLAG_OFFSET));
        sb.setLspInstantiationCapability(flags.get(I_FLAG_OFFSET));
        sb.setTriggeredResync(flags.get(T_FLAG_OFFSET));
        sb.setDeltaLspSyncCapability(flags.get(D_FLAG_OFFSET));
        sb.setTriggeredInitialResync(flags.get(F_FLAG_OFFSET));
    }

    @Override
    public void serializeTlv(final Tlv tlv, final ByteBuf buffer) {
        Preconditions.checkArgument(tlv instanceof Stateful, "StatefulCapabilityTlv is mandatory.");
        final Stateful sct = (Stateful) tlv;
        TlvUtil.formatTlv(TYPE, Unpooled.wrappedBuffer(serializeFlags(sct).array()), buffer);
    }

    protected BitArray serializeFlags(final Stateful sct) {
        final BitArray flags = new BitArray(FLAGS_F_LENGTH);
        flags.set(U_FLAG_OFFSET, sct.isLspUpdateCapability());
        flags.set(S_FLAG_OFFSET, sct.isIncludeDbVersion());
        flags.set(I_FLAG_OFFSET, sct.isLspInstantiationCapability());
        flags.set(T_FLAG_OFFSET, sct.isTriggeredResync());
        flags.set(D_FLAG_OFFSET, sct.isDeltaLspSyncCapability());
        flags.set(F_FLAG_OFFSET, sct.isTriggeredInitialResync());
        return flags;
    }
}
