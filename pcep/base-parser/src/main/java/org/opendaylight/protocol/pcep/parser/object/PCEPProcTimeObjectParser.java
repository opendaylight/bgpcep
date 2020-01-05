/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.parser.object;

import static com.google.common.base.Preconditions.checkArgument;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.pcep.spi.CommonObjectParser;
import org.opendaylight.protocol.pcep.spi.ObjectSerializer;
import org.opendaylight.protocol.pcep.spi.ObjectUtil;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.proc.time.object.ProcTime;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.proc.time.object.ProcTimeBuilder;
import org.opendaylight.yangtools.yang.common.netty.ByteBufUtils;

/**
 * Parser for {@link ProcTime}.
 * @see <a href="https://tools.ietf.org/html/rfc5886#section-4.4">PROC-TIME Object</a>
 */
public class PCEPProcTimeObjectParser extends CommonObjectParser implements ObjectSerializer {
    private static final int CLASS = 26;
    private static final int TYPE = 1;
    private static final int RESERVED = 2;
    private static final int FLAGS = 16;
    private static final int COUNT_FIELDS = 5;
    private static final int BODY_SIZE = RESERVED + FLAGS + COUNT_FIELDS * Integer.BYTES;
    private static final int E_FLAG_POSITION = 15;

    public PCEPProcTimeObjectParser() {
        super(CLASS, TYPE);
    }

    @Override
    public void serializeObject(final Object object, final ByteBuf buffer) {
        checkArgument(object instanceof ProcTime,
            "Wrong instance of PCEPObject. Passed %s. Needed ProcTimeObject.", object.getClass());
        final ProcTime procTime = (ProcTime) object;
        final ByteBuf body = Unpooled.buffer(BODY_SIZE);
        body.writeZero(RESERVED);
        final BitArray flagBits = new BitArray(FLAGS);
        flagBits.set(E_FLAG_POSITION, procTime.isEstimated());
        flagBits.toByteBuf(body);
        ByteBufUtils.writeOrZero(body, procTime.getCurrentProcTime());
        ByteBufUtils.writeOrZero(body, procTime.getMinProcTime());
        ByteBufUtils.writeOrZero(body, procTime.getMaxProcTime());
        ByteBufUtils.writeOrZero(body, procTime.getAverageProcTime());
        ByteBufUtils.writeOrZero(body, procTime.getVarianceProcTime());
        ObjectUtil.formatSubobject(TYPE, CLASS, object.isProcessingRule(), object.isIgnore(), body, buffer);
    }

    @Override
    public Object parseObject(final ObjectHeader header, final ByteBuf buffer) throws PCEPDeserializerException {
        checkArgument(buffer != null && buffer.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        buffer.skipBytes(RESERVED);
        final BitArray flagBits = BitArray.valueOf(buffer, FLAGS);

        return new ProcTimeBuilder()
                .setEstimated(flagBits.get(E_FLAG_POSITION))
                .setCurrentProcTime(ByteBufUtils.readUint32(buffer))
                .setMinProcTime(ByteBufUtils.readUint32(buffer))
                .setMaxProcTime(ByteBufUtils.readUint32(buffer))
                .setAverageProcTime(ByteBufUtils.readUint32(buffer))
                .setVarianceProcTime(ByteBufUtils.readUint32(buffer))
                .build();
    }
}
