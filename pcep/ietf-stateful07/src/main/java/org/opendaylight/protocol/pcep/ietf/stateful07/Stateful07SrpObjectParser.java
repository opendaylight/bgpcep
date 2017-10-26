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
import java.util.List;
import org.opendaylight.protocol.pcep.spi.AbstractObjectWithTlvsParser;
import org.opendaylight.protocol.pcep.spi.ObjectUtil;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.TlvRegistry;
import org.opendaylight.protocol.pcep.spi.VendorInformationTlvRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.SrpIdNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.srp.object.Srp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.srp.object.SrpBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.srp.object.srp.Tlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.srp.object.srp.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.symbolic.path.name.tlv.SymbolicPathName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.path.setup.type.tlv.PathSetupType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.vendor.information.tlvs.VendorInformationTlv;

/**
 * Parser for {@link Srp}
 */
public class Stateful07SrpObjectParser extends AbstractObjectWithTlvsParser<TlvsBuilder> {

    public static final int CLASS = 33;

    public static final int TYPE = 1;

    protected static final int FLAGS_SIZE = 32;

    protected static final int SRP_ID_SIZE = 4;

    protected static final int MIN_SIZE = FLAGS_SIZE / Byte.SIZE + SRP_ID_SIZE;

    protected Stateful07SrpObjectParser(final TlvRegistry tlvReg, final VendorInformationTlvRegistry viTlvReg) {
        super(tlvReg, viTlvReg);
    }

    @Override
    public Srp parseObject(final ObjectHeader header, final ByteBuf bytes) throws PCEPDeserializerException {
        Preconditions.checkArgument(bytes != null && bytes.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        if (bytes.readableBytes() < MIN_SIZE) {
            throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + bytes.readableBytes() + "; Expected: >="
                    + MIN_SIZE + ".");
        }
        final SrpBuilder builder = new SrpBuilder();
        builder.setIgnore(header.isIgnore());
        builder.setProcessingRule(header.isProcessingRule());
        parseFlags(builder, bytes);
        builder.setOperationId(new SrpIdNumber(bytes.readUnsignedInt()));
        final TlvsBuilder tlvsBuilder = new TlvsBuilder();
        parseTlvs(tlvsBuilder, bytes.slice());
        builder.setTlvs(tlvsBuilder.build());
        return builder.build();
    }

    protected void parseFlags(final SrpBuilder builder, final ByteBuf bytes) {
        bytes.skipBytes(FLAGS_SIZE / Byte.SIZE);
    }

    @Override
    public void addTlv(final TlvsBuilder builder, final Tlv tlv) {
        if (tlv instanceof SymbolicPathName) {
            builder.setSymbolicPathName((SymbolicPathName) tlv);
        }
        if (tlv instanceof PathSetupType) {
            builder.setPathSetupType((PathSetupType) tlv);
        }
    }

    @Override
    public void serializeObject(final Object object, final ByteBuf buffer) {
        Preconditions.checkArgument(object instanceof Srp, "Wrong instance of PCEPObject. Passed %s . Needed SrpObject.", object.getClass());
        final Srp srp = (Srp) object;
        final ByteBuf body = Unpooled.buffer();
        serializeFlags(srp, body);
        final SrpIdNumber srpId = srp.getOperationId();
        Preconditions.checkArgument(srpId != null, "SrpId is mandatory.");
        writeUnsignedInt(srpId.getValue(), body);
        serializeTlvs(srp.getTlvs(), body);
        ObjectUtil.formatSubobject(TYPE, CLASS, object.isProcessingRule(), object.isIgnore(), body, buffer);
    }

    protected void serializeFlags(final Srp srp, final ByteBuf body) {
        body.writeZero(FLAGS_SIZE / Byte.SIZE);
    }

    public void serializeTlvs(final Tlvs tlvs, final ByteBuf body) {
        if (tlvs == null) {
            return;
        }
        if (tlvs.getSymbolicPathName() != null) {
            serializeTlv(tlvs.getSymbolicPathName(), body);
        }
        if (tlvs.getPathSetupType() != null) {
            serializeTlv(tlvs.getPathSetupType(), body);
        }
    }

    @Override
    protected final void addVendorInformationTlvs(final TlvsBuilder builder, final List<VendorInformationTlv> tlvs) {
        if (!tlvs.isEmpty()) {
            builder.setVendorInformationTlv(tlvs);
        }
    }
}
