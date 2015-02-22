/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.impl.object;

import static org.opendaylight.protocol.util.ByteBufWriteUtil.INT_BYTES_LENGTH;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.BitSet;
import org.opendaylight.protocol.pcep.spi.ObjectParser;
import org.opendaylight.protocol.pcep.spi.ObjectSerializer;
import org.opendaylight.protocol.pcep.spi.ObjectUtil;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.ByteBufWriteUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.proc.time.object.ProcTime;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.proc.time.object.ProcTimeBuilder;

/**
 * Parser for {@link ProcTime}
 * @see https://tools.ietf.org/html/rfc5886#section-4.4
 */
public class PCEPProcTimeObjectParser implements ObjectParser, ObjectSerializer {

    public static final int CLASS = 26;

    public static final int TYPE = 1;

    private static final int RESERVED = 2;
    private static final int FLAGS = RESERVED;
    private static final int COUNT_FIELDS = 5;
    private static final int BODY_SIZE = RESERVED + FLAGS + COUNT_FIELDS * INT_BYTES_LENGTH;
    private static final int E_FLAG_POSITION = 15;

    @Override
    public void serializeObject(final Object object, final ByteBuf buffer) {
        Preconditions.checkArgument(object instanceof ProcTime, "Wrong instance of PCEPObject. Passed %s. Needed ProcTimeObject.", object.getClass());
        final ProcTime procTime = (ProcTime) object;
        final ByteBuf body = Unpooled.buffer(BODY_SIZE);
        body.writeZero(RESERVED);
        final BitSet flagBits = new BitSet(FLAGS * Byte.SIZE);
        flagBits.set(E_FLAG_POSITION, procTime.isEstimated());
        ByteBufWriteUtil.writeBitSet(flagBits, FLAGS, body);
        ByteBufWriteUtil.writeUnsignedInt(procTime.getCurrentProcTime(), body);
        ByteBufWriteUtil.writeUnsignedInt(procTime.getMinProcTime(), body);
        ByteBufWriteUtil.writeUnsignedInt(procTime.getMaxProcTime(), body);
        ByteBufWriteUtil.writeUnsignedInt(procTime.getAverageProcTime(), body);
        ByteBufWriteUtil.writeUnsignedInt(procTime.getVarianceProcTime(), body);
        ObjectUtil.formatSubobject(TYPE, CLASS, object.isProcessingRule(), object.isIgnore(), body, buffer);
    }

    @Override
    public Object parseObject(final ObjectHeader header, final ByteBuf buffer) throws PCEPDeserializerException {
        Preconditions.checkArgument(buffer != null && buffer.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        final ProcTimeBuilder builder = new ProcTimeBuilder();
        buffer.readBytes(RESERVED);
        final BitSet flagBits = ByteArray.bytesToBitSet(buffer.readBytes(FLAGS).array());
        builder.setEstimated(flagBits.get(E_FLAG_POSITION));
        builder.setCurrentProcTime(buffer.readUnsignedInt());
        builder.setMinProcTime(buffer.readUnsignedInt());
        builder.setMaxProcTime(buffer.readUnsignedInt());
        builder.setAverageProcTime(buffer.readUnsignedInt());
        builder.setVarianceProcTime(buffer.readUnsignedInt());
        return builder.build();
    }

}
