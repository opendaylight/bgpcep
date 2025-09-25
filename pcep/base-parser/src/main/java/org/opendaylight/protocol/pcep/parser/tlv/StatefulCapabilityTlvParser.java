/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.parser.tlv;

import static com.google.common.base.Preconditions.checkArgument;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.TlvParser;
import org.opendaylight.protocol.pcep.spi.TlvSerializer;
import org.opendaylight.protocol.pcep.spi.TlvUtil;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.pcerr.message.pcerr.message.error.type.stateful._case.Stateful;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.stateful.capability.tlv.StatefulCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.stateful.capability.tlv.StatefulCapabilityBuilder;

/**
 * Parser for {@link Stateful}.
 */
public class StatefulCapabilityTlvParser implements TlvParser, TlvSerializer {

    public static final int TYPE = 16;

    protected static final int FLAGS_F_LENGTH = 32;

    protected static final int U_FLAG_OFFSET = 31;
    protected static final int S_FLAG_OFFSET = 30;
    protected static final int I_FLAG_OFFSET = 29;
    protected static final int T_FLAG_OFFSET = 28;
    protected static final int D_FLAG_OFFSET = 27;
    protected static final int F_FLAG_OFFSET = 26;

    @Override
    public StatefulCapability parseTlv(final ByteBuf buffer) throws PCEPDeserializerException {
        if (buffer == null) {
            return null;
        }
        if (buffer.readableBytes() < FLAGS_F_LENGTH / Byte.SIZE) {
            throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + buffer.readableBytes()
                + "; Expected: >= " + FLAGS_F_LENGTH / Byte.SIZE + ".");
        }
        final StatefulCapabilityBuilder scb = new StatefulCapabilityBuilder();
        parseFlags(scb, buffer);
        return scb.build();
    }

    protected void parseFlags(final StatefulCapabilityBuilder scb, final ByteBuf buffer) {
        final BitArray flags = BitArray.valueOf(buffer, FLAGS_F_LENGTH);
        scb.setLspUpdateCapability(flags.get(U_FLAG_OFFSET));
        scb.setIncludeDbVersion(flags.get(S_FLAG_OFFSET));
        scb.setInitiation(flags.get(I_FLAG_OFFSET));
        scb.setTriggeredResync(flags.get(T_FLAG_OFFSET));
        scb.setDeltaLspSyncCapability(flags.get(D_FLAG_OFFSET));
        scb.setTriggeredInitialSync(flags.get(F_FLAG_OFFSET));
    }

    @Override
    public void serializeTlv(final Tlv tlv, final ByteBuf buffer) {
        checkArgument(tlv instanceof StatefulCapability, "StatefulCapabilityTlv is mandatory.");
        final StatefulCapability sct = (StatefulCapability) tlv;
        TlvUtil.formatTlv(TYPE, Unpooled.wrappedBuffer(serializeFlags(sct).array()), buffer);
    }

    protected BitArray serializeFlags(final StatefulCapability sct) {
        final BitArray flags = new BitArray(FLAGS_F_LENGTH);
        flags.set(U_FLAG_OFFSET, sct.getLspUpdateCapability());
        flags.set(S_FLAG_OFFSET, sct.getIncludeDbVersion());
        flags.set(I_FLAG_OFFSET, sct.getInitiation());
        flags.set(T_FLAG_OFFSET, sct.getTriggeredResync());
        flags.set(D_FLAG_OFFSET, sct.getDeltaLspSyncCapability());
        flags.set(F_FLAG_OFFSET, sct.getTriggeredInitialSync());
        return flags;
    }
}
