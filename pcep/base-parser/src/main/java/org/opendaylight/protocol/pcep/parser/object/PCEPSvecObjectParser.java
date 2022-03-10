/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.parser.object;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableSet;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Set;
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
import org.opendaylight.yangtools.yang.common.netty.ByteBufUtils;

/**
 * Parser for {@link Svec}.
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
        checkArgument(bytes != null && bytes.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        if (bytes.readableBytes() < MIN_SIZE) {
            throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: "
                + bytes.readableBytes() + "; Expected: >=" + MIN_SIZE + ".");
        }
        bytes.skipBytes(FLAGS_F_OFFSET);
        final BitArray flags = BitArray.valueOf(bytes, FLAGS_SIZE);
        final var builder = ImmutableSet.<RequestId>builder();
        while (bytes.isReadable()) {
            builder.add(new RequestId(ByteBufUtils.readUint32(bytes)));
        }
        final Set<RequestId> requestIDs = builder.build();

        if (requestIDs.isEmpty()) {
            throw new PCEPDeserializerException("Empty Svec Object - no request ids.");
        }
        return new SvecBuilder()
                .setIgnore(header.getIgnore())
                .setProcessingRule(header.getProcessingRule())
                .setLinkDiverse(flags.get(L_FLAG_OFFSET))
                .setNodeDiverse(flags.get(N_FLAG_OFFSET))
                .setSrlgDiverse(flags.get(S_FLAG_OFFSET))
                .setLinkDirectionDiverse(flags.get(D_FLAG_OFFSET))
                .setPartialPathDiverse(flags.get(P_FLAG_OFFSET))
                .setRequestsIds(requestIDs)
                .build();
    }

    @Override
    public void serializeObject(final Object object, final ByteBuf buffer) {
        checkArgument(object instanceof Svec, "Wrong instance of PCEPObject. Passed %s. Needed SvecObject.",
            object.getClass());
        final Svec svecObj = (Svec) object;
        final Set<RequestId> requestIDs = svecObj.getRequestsIds();
        checkArgument(!requestIDs.isEmpty(), "Empty Svec Object - no request ids.");

        final ByteBuf body = Unpooled.buffer();
        body.writeZero(FLAGS_F_OFFSET);
        final BitArray flags = new BitArray(FLAGS_SIZE);
        flags.set(L_FLAG_OFFSET, svecObj.getLinkDiverse());
        flags.set(N_FLAG_OFFSET, svecObj.getNodeDiverse());
        flags.set(S_FLAG_OFFSET, svecObj.getSrlgDiverse());
        flags.set(D_FLAG_OFFSET, svecObj.getLinkDirectionDiverse());
        flags.set(P_FLAG_OFFSET, svecObj.getPartialPathDiverse());
        flags.toByteBuf(body);

        for (final RequestId requestId : requestIDs) {
            ByteBufUtils.write(body, requestId.getValue());
        }
        ObjectUtil.formatSubobject(TYPE, CLASS, object.getProcessingRule(), object.getIgnore(), body, buffer);
    }
}
