/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.object;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import io.netty.buffer.ByteBuf;

import java.util.BitSet;
import java.util.List;

import org.opendaylight.protocol.pcep.spi.AbstractObjectWithTlvsParser;
import org.opendaylight.protocol.pcep.spi.ObjectUtil;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.TlvRegistry;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.RequestId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.svec.object.Svec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.svec.object.SvecBuilder;

/**
 * Parser for {@link Svec}
 */
public class PCEPSvecObjectParser extends AbstractObjectWithTlvsParser<SvecBuilder> {

    public static final int CLASS = 11;

    public static final int TYPE = 1;

    /*
     * field lengths in bytes
     */
    private static final int FLAGS_F_LENGTH = 3;
    private static final int REQ_LIST_ITEM_LENGTH = 4;

    /*
     * fields offsets in bytes
     */
    private static final int FLAGS_F_OFFSET = 1;
    private static final int REQ_ID_LIST_OFFSET = FLAGS_F_OFFSET + FLAGS_F_LENGTH;

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

    public PCEPSvecObjectParser(final TlvRegistry tlvReg) {
        super(tlvReg);
    }

    @Override
    public Svec parseObject(final ObjectHeader header, final ByteBuf bytes) throws PCEPDeserializerException {
        Preconditions.checkArgument(bytes != null && bytes.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        if (bytes.readableBytes() < MIN_SIZE) {
            throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + bytes.readableBytes() + "; Expected: >="
                    + MIN_SIZE + ".");
        }
        bytes.readerIndex(bytes.readerIndex() + FLAGS_F_OFFSET);
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
    public byte[] serializeObject(final Object object) {
        if (!(object instanceof Svec)) {
            throw new IllegalArgumentException("Wrong instance of PCEPObject. Passed " + object.getClass() + ". Needed SvecObject.");
        }

        final Svec svecObj = (Svec) object;
        final byte[] retBytes = new byte[svecObj.getRequestsIds().size() * REQ_LIST_ITEM_LENGTH + REQ_ID_LIST_OFFSET];
        final List<RequestId> requestIDs = svecObj.getRequestsIds();
        final BitSet flags = new BitSet(FLAGS_F_LENGTH * Byte.SIZE);
        flags.set(L_FLAG_OFFSET, svecObj.isLinkDiverse());
        flags.set(N_FLAG_OFFSET, svecObj.isNodeDiverse());
        flags.set(S_FLAG_OFFSET, svecObj.isSrlgDiverse());

        ByteArray.copyWhole(ByteArray.bitSetToBytes(flags, FLAGS_F_LENGTH), retBytes, FLAGS_F_OFFSET);

        for (int i = 0; i < requestIDs.size(); i++) {
            System.arraycopy(ByteArray.longToBytes(requestIDs.get(i).getValue(), REQ_LIST_ITEM_LENGTH), 0, retBytes, REQ_LIST_ITEM_LENGTH
                    * i + REQ_ID_LIST_OFFSET, REQ_LIST_ITEM_LENGTH);
        }
        assert !(requestIDs.isEmpty()) : "Empty Svec Object - no request ids.";
        return ObjectUtil.formatSubobject(TYPE, CLASS, object.isProcessingRule(), object.isIgnore(), retBytes);
    }
}
