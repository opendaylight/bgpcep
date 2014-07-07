/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.ietf.stateful07;

import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeUnsignedInt;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.pcep.spi.AbstractObjectWithTlvsParser;
import org.opendaylight.protocol.pcep.spi.ObjectUtil;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.TlvRegistry;
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
        Preconditions.checkArgument(object instanceof Srp, "Wrong instance of PCEPObject. Passed %s . Needed SrpObject.", object.getClass());
        final Srp srp = (Srp) object;
        final ByteBuf body = Unpooled.buffer();
        body.writerIndex(body.writerIndex() + FLAGS_SIZE);
        final SrpIdNumber srpId = srp.getOperationId();
        Preconditions.checkArgument(srpId != null, "SrpId is mandatory.");
        writeUnsignedInt(srpId.getValue(), body);
        serializeTlvs(srp.getTlvs(), body);
        ObjectUtil.formatSubobject(TYPE, CLASS, object.isProcessingRule(), object.isIgnore(), body, buffer);
    }

    public void serializeTlvs(final Tlvs tlvs, final ByteBuf body) {
        if (tlvs == null) {
            return;
        } else if (tlvs.getSymbolicPathName() != null) {
            serializeTlv(tlvs.getSymbolicPathName(), body);
        }
    }
}
