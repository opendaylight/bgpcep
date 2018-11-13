/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.parser.object;

import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeUnsignedInt;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.List;
import org.opendaylight.protocol.pcep.spi.CommonObjectParser;
import org.opendaylight.protocol.pcep.spi.ObjectSerializer;
import org.opendaylight.protocol.pcep.spi.ObjectUtil;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.RequestId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.svec.object.Svec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.svec.object.SvecBuilder;

/**
 * Parser for {@link Svec}
 */
public final class PCEPSvecObjectParser extends CommonObjectParser implements ObjectSerializer {

    private static final int CLASS = 11;
    private static final int TYPE = 1;

    /*
     * field lengths in bytes
     */
    private static final int FLAGS_SIZE = 24;

    /*
     * fields offsets in bytes
     */
    private static final int FLAGS_F_OFFSET = 1;

    /*
     * flags offsets inside flags field in bits
     */
    private static final int P_FLAG_OFFSET = 19;
    private static final int D_FLAG_OFFSET = 20;
    private static final int S_FLAG_OFFSET = 21;
    private static final int N_FLAG_OFFSET = 22;
    private static final int L_FLAG_OFFSET = 23;

    /*
     * min size in bytes
     */
    private static final int MIN_SIZE = FLAGS_SIZE / Byte.SIZE + FLAGS_F_OFFSET;

    public PCEPSvecObjectParser() {
        super(CLASS, TYPE);
    }

    @Override
    public Svec parseObject(final ObjectHeader header, final ByteBuf bytes) throws PCEPDeserializerException {
        Preconditions.checkArgument(bytes != null && bytes.isReadable(),
            "Array of bytes is mandatory. Can't be null or empty.");
        if (bytes.readableBytes() < MIN_SIZE) {
            throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: "
                + bytes.readableBytes() + "; Expected: >=" + MIN_SIZE + ".");
        }
        bytes.skipBytes(FLAGS_F_OFFSET);
        final BitArray flags = BitArray.valueOf(bytes, FLAGS_SIZE);
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
        builder.setLinkDirectionDiverse(flags.get(D_FLAG_OFFSET));
        builder.setPartialPathDiverse(flags.get(P_FLAG_OFFSET));
        builder.setRequestsIds(requestIDs);
        return builder.build();
    }

    @Override
    public void serializeObject(final Object object, final ByteBuf buffer) {
        Preconditions.checkArgument(object instanceof Svec,
            "Wrong instance of PCEPObject. Passed %s. Needed SvecObject.", object.getClass());
        final Svec svecObj = (Svec) object;
        final ByteBuf body = Unpooled.buffer();
        body.writeZero(FLAGS_F_OFFSET);
        final BitArray flags = new BitArray(FLAGS_SIZE);
        flags.set(L_FLAG_OFFSET, svecObj.isLinkDiverse());
        flags.set(N_FLAG_OFFSET, svecObj.isNodeDiverse());
        flags.set(S_FLAG_OFFSET, svecObj.isSrlgDiverse());
        flags.set(D_FLAG_OFFSET, svecObj.isLinkDirectionDiverse());
        flags.set(P_FLAG_OFFSET, svecObj.isPartialPathDiverse());
        flags.toByteBuf(body);

        final List<RequestId> requestIDs = svecObj.getRequestsIds();
        assert !(requestIDs.isEmpty()) : "Empty Svec Object - no request ids.";
        for (final RequestId requestId : requestIDs) {
            writeUnsignedInt(requestId.getValue(), body);
        }
        ObjectUtil.formatSubobject(TYPE, CLASS, object.isProcessingRule(), object.isIgnore(), body, buffer);
    }
}
