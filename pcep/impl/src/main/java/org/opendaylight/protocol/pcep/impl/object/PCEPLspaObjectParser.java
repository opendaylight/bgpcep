/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.object;

import com.google.common.base.Preconditions;
import com.google.common.primitives.UnsignedBytes;

import io.netty.buffer.ByteBuf;

import java.util.BitSet;

import org.opendaylight.protocol.pcep.spi.AbstractObjectWithTlvsParser;
import org.opendaylight.protocol.pcep.spi.ObjectUtil;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.TlvRegistry;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lspa.object.Lspa;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lspa.object.LspaBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lspa.object.lspa.Tlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lspa.object.lspa.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.AttributeFilter;

/**
 * Parser for {@link Lspa}
 */
public class PCEPLspaObjectParser extends AbstractObjectWithTlvsParser<TlvsBuilder> {

    public static final int CLASS = 9;

    public static final int TYPE = 1;

    /*
     * lengths of fields in bytes
     */
    private static final int EXC_ANY_F_LENGTH = 4;
    private static final int INC_ANY_F_LENGTH = 4;
    private static final int INC_ALL_F_LENGTH = 4;
    private static final int SET_PRIO_F_LENGTH = 1;
    private static final int HOLD_PRIO_F_LENGTH = 1;
    private static final int FLAGS_F_LENGTH = 1;

    /*
     * offsets of flags inside flags field in bits
     */
    private static final int L_FLAG_OFFSET = 7;

    /*
     * offsets of fields in bytes
     */
    private static final int EXC_ANY_F_OFFSET = 0;
    private static final int INC_ANY_F_OFFSET = EXC_ANY_F_OFFSET + EXC_ANY_F_LENGTH;
    private static final int INC_ALL_F_OFFSET = INC_ANY_F_OFFSET + INC_ANY_F_LENGTH;
    private static final int SET_PRIO_F_OFFSET = INC_ALL_F_OFFSET + INC_ALL_F_LENGTH;
    private static final int HOLD_PRIO_F_OFFSET = SET_PRIO_F_OFFSET + SET_PRIO_F_LENGTH;
    private static final int FLAGS_F_OFFSET = HOLD_PRIO_F_OFFSET + HOLD_PRIO_F_LENGTH;
    private static final int TLVS_F_OFFSET = FLAGS_F_OFFSET + FLAGS_F_LENGTH + 1;

    public PCEPLspaObjectParser(final TlvRegistry tlvReg) {
        super(tlvReg);
    }

    @Override
    public Lspa parseObject(final ObjectHeader header, final ByteBuf bytes) throws PCEPDeserializerException {
        Preconditions.checkArgument(bytes != null && bytes.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        final LspaBuilder builder = new LspaBuilder();
        builder.setIgnore(header.isIgnore());
        builder.setProcessingRule(header.isProcessingRule());

        builder.setExcludeAny(new AttributeFilter(bytes.readUnsignedInt()));
        builder.setIncludeAll(new AttributeFilter(bytes.readUnsignedInt()));
        builder.setIncludeAny(new AttributeFilter(bytes.readUnsignedInt()));
        builder.setSetupPriority((short) UnsignedBytes.toInt(bytes.readByte()));
        builder.setHoldPriority((short) UnsignedBytes.toInt(bytes.readByte()));

        final BitSet flags = ByteArray.bytesToBitSet(new byte[] { bytes.readByte() });
        builder.setLocalProtectionDesired(flags.get(L_FLAG_OFFSET));
        final TlvsBuilder tbuilder = new TlvsBuilder();
        bytes.readerIndex(TLVS_F_OFFSET);
        parseTlvs(tbuilder, bytes.slice());
        builder.setTlvs(tbuilder.build());
        return builder.build();
    }

    @Override
    public byte[] serializeObject(final Object object) {
        if (!(object instanceof Lspa)) {
            throw new IllegalArgumentException("Wrong instance of PCEPObject. Passed " + object.getClass() + ". Needed LspaObject.");
        }
        final Lspa lspaObj = (Lspa) object;

        final byte[] tlvs = serializeTlvs(lspaObj.getTlvs());
        final byte[] retBytes = new byte[TLVS_F_OFFSET + tlvs.length + getPadding(TLVS_F_OFFSET + tlvs.length, PADDED_TO)];

        if (lspaObj.getExcludeAny() != null) {
            System.arraycopy(ByteArray.longToBytes(lspaObj.getExcludeAny().getValue(), EXC_ANY_F_LENGTH), 0, retBytes, EXC_ANY_F_OFFSET,
                    EXC_ANY_F_LENGTH);
        }
        if (lspaObj.getIncludeAny() != null) {
            System.arraycopy(ByteArray.longToBytes(lspaObj.getIncludeAny().getValue(), INC_ANY_F_LENGTH), 0, retBytes, INC_ANY_F_OFFSET,
                    INC_ANY_F_LENGTH);
        }
        if (lspaObj.getIncludeAll() != null) {
            System.arraycopy(ByteArray.longToBytes(lspaObj.getIncludeAll().getValue(), INC_ALL_F_LENGTH), 0, retBytes, INC_ALL_F_OFFSET,
                    INC_ALL_F_LENGTH);
        }
        if (lspaObj.getSetupPriority() != null) {
            retBytes[SET_PRIO_F_OFFSET] = UnsignedBytes.checkedCast(lspaObj.getSetupPriority());
        }
        if (lspaObj.getHoldPriority() != null) {
            retBytes[HOLD_PRIO_F_OFFSET] = UnsignedBytes.checkedCast(lspaObj.getHoldPriority());
        }

        final BitSet flags = new BitSet(FLAGS_F_LENGTH * Byte.SIZE);
        if (lspaObj.isLocalProtectionDesired() != null && lspaObj.isLocalProtectionDesired()) {
            flags.set(L_FLAG_OFFSET, lspaObj.isLocalProtectionDesired());
        }
        ByteArray.copyWhole(ByteArray.bitSetToBytes(flags, FLAGS_F_LENGTH), retBytes, FLAGS_F_OFFSET);
        ByteArray.copyWhole(tlvs, retBytes, TLVS_F_OFFSET);
        return ObjectUtil.formatSubobject(TYPE, CLASS, object.isProcessingRule(), object.isIgnore(), retBytes);
    }

    public byte[] serializeTlvs(final Tlvs tlvs) {
        return new byte[0];
    }
}
