/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.parser.object;

import static com.google.common.base.Preconditions.checkArgument;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.List;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.AbstractObjectWithTlvsParser;
import org.opendaylight.protocol.pcep.spi.ObjectUtil;
import org.opendaylight.protocol.pcep.spi.TlvRegistry;
import org.opendaylight.protocol.pcep.spi.VendorInformationTlvRegistry;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.pcrep.message.pcrep.message.replies.result.failure._case.NoPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.pcrep.message.pcrep.message.replies.result.failure._case.NoPathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.pcrep.message.pcrep.message.replies.result.failure._case.no.path.Tlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.pcrep.message.pcrep.message.replies.result.failure._case.no.path.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.pcrep.message.pcrep.message.replies.result.failure._case.no.path.tlvs.NoPathVector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.vendor.information.tlvs.VendorInformationTlv;
import org.opendaylight.yangtools.yang.common.Uint8;
import org.opendaylight.yangtools.yang.common.netty.ByteBufUtils;

/**
 * Parser for {@link NoPath}.
 */
public class PCEPNoPathObjectParser extends AbstractObjectWithTlvsParser<TlvsBuilder> {
    private static final int CLASS = 3;
    private static final int TYPE = 1;

    /*
     * lengths of fields in bytes
     */
    private static final int FLAGS_SIZE = 16;
    private static final int RESERVED_F_LENGTH = 1;

    /*
     * defined flags
     */
    private static final int C_FLAG_OFFSET = 0;

    public PCEPNoPathObjectParser(final TlvRegistry tlvReg, final VendorInformationTlvRegistry viTlvReg) {
        super(tlvReg, viTlvReg, CLASS, TYPE);
    }

    @Override
    public NoPath parseObject(final ObjectHeader header, final ByteBuf bytes) throws PCEPDeserializerException {
        checkArgument(bytes != null && bytes.isReadable(), "Array of bytes is mandatory. Cannot be null or empty.");

        final Uint8 issue = ByteBufUtils.readUint8(bytes);
        final BitArray flags = BitArray.valueOf(bytes, FLAGS_SIZE);
        bytes.skipBytes(RESERVED_F_LENGTH);
        final TlvsBuilder tlvsBuilder = new TlvsBuilder();
        parseTlvs(tlvsBuilder, bytes.slice());
        return new NoPathBuilder()
                .setIgnore(header.getIgnore())
                .setProcessingRule(header.getProcessingRule())
                .setNatureOfIssue(issue)
                .setUnsatisfiedConstraints(flags.get(C_FLAG_OFFSET))
                .setTlvs(tlvsBuilder.build())
                .build();
    }

    @Override
    public void addTlv(final TlvsBuilder builder, final Tlv tlv) {
        if (tlv instanceof NoPathVector) {
            builder.setNoPathVector((NoPathVector) tlv);
        }
    }

    @Override
    public void serializeObject(final Object object, final ByteBuf buffer) {
        checkArgument(object instanceof NoPath, "Wrong instance of PCEPObject. Passed %s. Needed NoPathObject.",
            object.getClass());
        final NoPath nPObj = (NoPath) object;
        final ByteBuf body = Unpooled.buffer();
        ByteBufUtils.writeMandatory(body, nPObj.getNatureOfIssue(), "NatureOfIssue");
        final BitArray flags = new BitArray(FLAGS_SIZE);
        flags.set(C_FLAG_OFFSET, nPObj.getUnsatisfiedConstraints());
        flags.toByteBuf(body);
        body.writeZero(RESERVED_F_LENGTH);
        serializeTlvs(nPObj.getTlvs(), body);
        ObjectUtil.formatSubobject(TYPE, CLASS, object.getProcessingRule(), object.getIgnore(), body, buffer);
    }

    @NonNullByDefault
    public void serializeTlvs(final @Nullable Tlvs tlvs, final ByteBuf body) {
        if (tlvs != null) {
            serializeOptionalTlv(tlvs.getNoPathVector(), body);
            serializeVendorInformationTlvs(tlvs.getVendorInformationTlv(), body);
        }
    }

    @Override
    protected final void addVendorInformationTlvs(final TlvsBuilder builder, final List<VendorInformationTlv> tlvs) {
        if (!tlvs.isEmpty()) {
            builder.setVendorInformationTlv(tlvs);
        }
    }
}
