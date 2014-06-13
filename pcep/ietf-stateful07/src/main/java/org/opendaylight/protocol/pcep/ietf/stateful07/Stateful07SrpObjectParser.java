/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.ietf.stateful07;

import com.google.common.base.Preconditions;

import io.netty.buffer.ByteBuf;

import org.opendaylight.protocol.pcep.spi.AbstractObjectWithTlvsParser;
import org.opendaylight.protocol.pcep.spi.ObjectUtil;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.TlvRegistry;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.SrpIdNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.srp.object.Srp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.srp.object.SrpBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.srp.object.srp.Tlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.srp.object.srp.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.symbolic.path.name.tlv.SymbolicPathName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;

/**
 * Parser for {@link Srp}
 */
public class Stateful07SrpObjectParser extends AbstractObjectWithTlvsParser<SrpBuilder> {

    public static final int CLASS = 33;

    public static final int TYPE = 1;

    protected static final int FLAGS_SIZE = 4;

    protected static final int SRP_ID_SIZE = 4;

    protected static final int TLVS_OFFSET = FLAGS_SIZE + SRP_ID_SIZE;

    protected static final int MIN_SIZE = FLAGS_SIZE + SRP_ID_SIZE;

    public Stateful07SrpObjectParser(final TlvRegistry tlvReg) {
        super(tlvReg);
    }

    @Override
    public Srp parseObject(final ObjectHeader header, final ByteBuf bytes) throws PCEPDeserializerException {
        Preconditions.checkArgument(bytes != null && bytes.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        if (bytes.readableBytes() < MIN_SIZE) {
            throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + bytes.readableBytes() + "; Expected: >="
                    + MIN_SIZE + ".");
        }
        if (header.isProcessingRule()) {
            throw new PCEPDeserializerException("Processed flag is set");
        }
        final SrpBuilder builder = new SrpBuilder();
        builder.setIgnore(header.isIgnore());
        builder.setProcessingRule(header.isProcessingRule());
        bytes.readerIndex(bytes.readerIndex() + FLAGS_SIZE);
        builder.setOperationId(new SrpIdNumber(bytes.readUnsignedInt()));
        return builder.build();
    }

    @Override
    public void addTlv(final SrpBuilder builder, final Tlv tlv) {
        if (tlv instanceof SymbolicPathName) {
            builder.setTlvs(new TlvsBuilder().setSymbolicPathName((SymbolicPathName) tlv).build());
        }
    }

    @Override
    public void serializeObject(final Object object, final ByteBuf buffer) {
        if (!(object instanceof Srp)) {
            throw new IllegalArgumentException("Wrong instance of PCEPObject. Passed " + object.getClass() + ". Needed SrpObject.");
        }
        final Srp srp = (Srp) object;
        final byte[] tlvs = serializeTlvs(srp.getTlvs());
        final Long id = srp.getOperationId().getValue();
        final byte[] retBytes = new byte[MIN_SIZE];
        if (tlvs != null) {
            ByteArray.copyWhole(tlvs, retBytes, TLVS_OFFSET);
        }
        System.arraycopy(ByteArray.intToBytes(id.intValue(), SRP_ID_SIZE), 0, retBytes, FLAGS_SIZE, SRP_ID_SIZE);
        ByteArray.copyWhole(tlvs, retBytes, TLVS_OFFSET);
        // FIXME: switch to ByteBuf
        buffer.writeBytes(ObjectUtil.formatSubobject(TYPE, CLASS, object.isProcessingRule(), object.isIgnore(), retBytes));
    }

    public byte[] serializeTlvs(final Tlvs tlvs) {
        if (tlvs == null) {
            return new byte[0];
        } else if (tlvs.getSymbolicPathName() != null) {
            return serializeTlv(tlvs.getSymbolicPathName());
        }
        return new byte[0];
    }
}
