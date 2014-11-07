/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.ietf.initiated00;

import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeMedium;

import com.google.common.base.Preconditions;
import com.google.common.primitives.UnsignedBytes;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.BitSet;
import org.opendaylight.protocol.pcep.ietf.stateful07.Stateful07LspObjectParser;
import org.opendaylight.protocol.pcep.spi.ObjectUtil;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.TlvRegistry;
import org.opendaylight.protocol.pcep.spi.VendorInformationTlvRegistry;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev131126.Lsp1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev131126.Lsp1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.OperationalStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.PlspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.object.Lsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.object.LspBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.object.lsp.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ObjectHeader;

/**
 * Parser for {@link Lsp}
 */
public class CInitiated00LspObjectParser extends Stateful07LspObjectParser {

    private static final int CREATE_FLAG_OFFSET = 8;

    public CInitiated00LspObjectParser(final TlvRegistry tlvReg, final VendorInformationTlvRegistry viTlvReg) {
        super(tlvReg, viTlvReg);
    }

    @Override
    public Lsp parseObject(final ObjectHeader header, final ByteBuf bytes) throws PCEPDeserializerException {
        Preconditions.checkArgument(bytes != null && bytes.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        final LspBuilder builder = new LspBuilder();
        builder.setIgnore(header.isIgnore());
        builder.setProcessingRule(header.isProcessingRule());
        final int[] plspIdRaw = new int[] { bytes.readUnsignedByte(), bytes.readUnsignedByte(), bytes.getUnsignedByte(2), };
        builder.setPlspId(new PlspId((long) ((plspIdRaw[0] << TWELVE_BITS_SHIFT) | (plspIdRaw[1] << FOUR_BITS_SHIFT) | (plspIdRaw[2] >> FOUR_BITS_SHIFT))));
        final BitSet flags = ByteArray.bytesToBitSet(ByteArray.readBytes(bytes, 2));
        builder.setDelegate(flags.get(DELEGATE_FLAG_OFFSET));
        builder.setSync(flags.get(SYNC_FLAG_OFFSET));
        builder.setRemove(flags.get(REMOVE_FLAG_OFFSET));
        builder.setAdministrative(flags.get(ADMINISTRATIVE_FLAG_OFFSET));
        builder.addAugmentation(Lsp1.class, new Lsp1Builder().setCreate(flags.get(CREATE_FLAG_OFFSET)).build());
        short s = 0;
        s |= flags.get(OPERATIONAL_OFFSET + 2) ? 1 : 0;
        s |= (flags.get(OPERATIONAL_OFFSET + 1) ? 1 : 0) << 1;
        s |= (flags.get(OPERATIONAL_OFFSET) ? 1 : 0) << 2;
        builder.setOperational(OperationalStatus.forValue(s));
        final TlvsBuilder b = new TlvsBuilder();
        parseTlvs(b, bytes.slice());
        builder.setTlvs(b.build());
        return builder.build();
    }

    @Override
    public void serializeObject(final Object object, final ByteBuf buffer) {
        Preconditions.checkArgument(object instanceof Lsp, "Wrong instance of PCEPObject. Passed %s. Needed LspObject.", object.getClass());
        final Lsp specObj = (Lsp) object;
        final ByteBuf body = Unpooled.buffer();
        Preconditions.checkArgument(specObj.getPlspId() != null, "PLSP-ID not present");
        writeMedium(specObj.getPlspId().getValue().intValue() << FOUR_BITS_SHIFT, body);
        final BitSet flags = new BitSet(2 * Byte.SIZE);
        if (specObj.isDelegate() != null) {
            flags.set(DELEGATE_FLAG_OFFSET, specObj.isDelegate());
        }
        if (specObj.isRemove() != null) {
            flags.set(REMOVE_FLAG_OFFSET, specObj.isRemove());
        }
        if (specObj.isSync() != null) {
            flags.set(SYNC_FLAG_OFFSET, specObj.isSync());
        }
        if (specObj.isAdministrative() != null) {
            flags.set(ADMINISTRATIVE_FLAG_OFFSET, specObj.isAdministrative());
        }
        if (specObj.getAugmentation(Lsp1.class) != null && specObj.getAugmentation(Lsp1.class).isCreate() != null) {
            flags.set(CREATE_FLAG_OFFSET, specObj.getAugmentation(Lsp1.class).isCreate());
        }
        byte op = 0;
        if (specObj.getOperational() != null) {
            op = UnsignedBytes.checkedCast(specObj.getOperational().getIntValue());
            op = (byte) (op << FOUR_BITS_SHIFT);
        }
        body.writeByte(ByteArray.bitSetToBytes(flags, 2)[1] | op);
        serializeTlvs(specObj.getTlvs(), body);
        ObjectUtil.formatSubobject(TYPE, CLASS, object.isProcessingRule(), object.isIgnore(), body, buffer);
    }
}
