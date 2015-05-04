/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.ietf.stateful02;

import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeBitSet;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.BitSet;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.TlvParser;
import org.opendaylight.protocol.pcep.spi.TlvSerializer;
import org.opendaylight.protocol.pcep.spi.TlvUtil;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.stateful.capability.tlv.Stateful;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.stateful.capability.tlv.StatefulBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;

@Deprecated
public class Stateful02StatefulCapabilityTlvParser implements TlvParser, TlvSerializer {

    public static final int TYPE = 16;

    protected static final int FLAGS_F_LENGTH = 4;

    protected static final int S_FLAG_OFFSET = 30;
    protected static final int U_FLAG_OFFSET = 31;

    @Override
    public Stateful parseTlv(final ByteBuf buffer) throws PCEPDeserializerException {
        if (buffer == null) {
            return null;
        }
        if (buffer.readableBytes() < FLAGS_F_LENGTH) {
            throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + buffer.readableBytes() + "; Expected: >= "
                    + FLAGS_F_LENGTH + ".");
        }
        final BitSet flags = ByteArray.bytesToBitSet(ByteArray.readBytes(buffer, FLAGS_F_LENGTH));
        final StatefulBuilder sb = new StatefulBuilder();
        sb.setIncludeDbVersion(flags.get(S_FLAG_OFFSET));
        sb.setLspUpdateCapability(flags.get(U_FLAG_OFFSET));
        return sb.build();
    }

    @Override
    public void serializeTlv(final Tlv tlv, final ByteBuf buffer) {
        Preconditions.checkArgument(tlv instanceof Stateful, "StatefulCapabilityTlv is mandatory.");
        final Stateful sct = (Stateful) tlv;
        final ByteBuf body = Unpooled.buffer();
        final BitSet flags = new BitSet(FLAGS_F_LENGTH * Byte.SIZE);
        if (sct.isLspUpdateCapability() != null) {
            flags.set(U_FLAG_OFFSET, sct.isLspUpdateCapability());
        }
        if (sct.isIncludeDbVersion() != null) {
            flags.set(S_FLAG_OFFSET, sct.isIncludeDbVersion());
        }
        writeBitSet(flags, FLAGS_F_LENGTH, body);
        TlvUtil.formatTlv(TYPE, body, buffer);
    }
}
