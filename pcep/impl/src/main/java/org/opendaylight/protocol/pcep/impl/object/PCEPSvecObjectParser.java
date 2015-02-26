/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.object;

import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeBitSet;
import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeUnsignedInt;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.BitSet;
import java.util.List;
import org.opendaylight.protocol.pcep.spi.ObjectParser;
import org.opendaylight.protocol.pcep.spi.ObjectSerializer;
import org.opendaylight.protocol.pcep.spi.ObjectUtil;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.RequestId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.svec.object.Svec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.svec.object.SvecBuilder;

/**
 * Parser for {@link Svec}
 */
public class PCEPSvecObjectParser implements ObjectParser, ObjectSerializer {

    public static final int CLASS = 11;

    public static final int TYPE = 1;

    /*
     * field lengths in bytes
     */
    private static final int FLAGS_F_LENGTH = 3;

    /*
     * fields offsets in bytes
     */
    private static final int FLAGS_F_OFFSET = 1;

    /*
     * flags offsets inside flags field in bits
     */
    private static final int S_FLAG_OFFSET = 21;
    private static final int N_FLAG_OFFSET = 22;
    private static final int L_FLAG_OFFSET = 23;

    /*
     * min size in bytes
     */
    private static final int MIN_SIZE = FLAGS_F_LENGTH + FLAGS_F_OFFSET;

    @Override
    public Svec parseObject(final ObjectHeader header, final ByteBuf bytes) throws PCEPDeserializerException {
        Preconditions.checkArgument(bytes != null && bytes.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        if (bytes.readableBytes() < MIN_SIZE) {
            throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + bytes.readableBytes() + "; Expected: >="
                    + MIN_SIZE + ".");
        }
        bytes.skipBytes(FLAGS_F_OFFSET);
        final BitSet flags = ByteArray.bytesToBitSet(ByteArray.readBytes(bytes, FLAGS_F_LENGTH));
        final List<RequestId> requestIDs = Lists.newArrayList();

        while (bytes.isReadable()) {
            requestIDs.add(new RequestId(bytes.readUnsignedInt()));
        }
        if (requestIDs.isEmpty()) {
            throw new PCEPDeserializerException("Empty Svec Object - no request ids.");
        }
        final SvecBuilder builder = new SvecBuilder();

        builder.setIgnore(header.isIgnore());
        builder.setProcessingRule(header.isProcessingRule());

        builder.setLinkDiverse(flags.get(L_FLAG_OFFSET));
        builder.setNodeDiverse(flags.get(N_FLAG_OFFSET));
        builder.setSrlgDiverse(flags.get(S_FLAG_OFFSET));
        builder.setRequestsIds(requestIDs);
        return builder.build();
    }

    @Override
    public void serializeObject(final Object object, final ByteBuf buffer) {
        Preconditions.checkArgument(object instanceof Svec, "Wrong instance of PCEPObject. Passed %s. Needed SvecObject.", object.getClass());
        final Svec svecObj = (Svec) object;
        final ByteBuf body = Unpooled.buffer();
        body.writeZero(FLAGS_F_OFFSET);
        final BitSet flags = new BitSet(FLAGS_F_LENGTH * Byte.SIZE);
        if (svecObj.isLinkDiverse() != null) {
            flags.set(L_FLAG_OFFSET, svecObj.isLinkDiverse());
        }
        if (svecObj.isNodeDiverse() != null) {
            flags.set(N_FLAG_OFFSET, svecObj.isNodeDiverse());
        }
        if (svecObj.isSrlgDiverse() != null) {
            flags.set(S_FLAG_OFFSET, svecObj.isSrlgDiverse());
        }
        writeBitSet(flags, FLAGS_F_LENGTH, body);

        final List<RequestId> requestIDs = svecObj.getRequestsIds();
        assert !(requestIDs.isEmpty()) : "Empty Svec Object - no request ids.";
        for(final RequestId requestId : requestIDs) {
            writeUnsignedInt(requestId.getValue(), body);
        }
        ObjectUtil.formatSubobject(TYPE, CLASS, object.isProcessingRule(), object.isIgnore(), body, buffer);
    }
}
