/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.ietf.stateful;

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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.SrpIdNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.srp.object.Srp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.srp.object.SrpBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.srp.object.srp.Tlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.srp.object.srp.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.symbolic.path.name.tlv.SymbolicPathName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.path.setup.type.tlv.PathSetupType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.vendor.information.tlvs.VendorInformationTlv;
import org.opendaylight.yangtools.yang.common.netty.ByteBufUtils;

/**
 * Parser for {@link Srp}.
 */
public class StatefulSrpObjectParser extends AbstractObjectWithTlvsParser<TlvsBuilder> {
    private static final int CLASS = 33;
    private static final int TYPE = 1;

    protected static final int FLAGS_SIZE = 32;
    protected static final int SRP_ID_SIZE = 4;
    protected static final int MIN_SIZE = FLAGS_SIZE / Byte.SIZE + SRP_ID_SIZE;

    protected StatefulSrpObjectParser(final TlvRegistry tlvReg, final VendorInformationTlvRegistry viTlvReg) {
        super(tlvReg, viTlvReg, CLASS, TYPE);
    }

    @Override
    public Srp parseObject(final ObjectHeader header, final ByteBuf bytes) throws PCEPDeserializerException {
        checkArgument(bytes != null && bytes.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        if (bytes.readableBytes() < MIN_SIZE) {
            throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + bytes.readableBytes()
                + "; Expected: >=" + MIN_SIZE + ".");
        }
        final SrpBuilder builder = new SrpBuilder()
                .setIgnore(header.getIgnore())
                .setProcessingRule(header.getProcessingRule());
        parseFlags(builder, bytes);
        builder.setOperationId(new SrpIdNumber(ByteBufUtils.readUint32(bytes)));
        final TlvsBuilder tlvsBuilder = new TlvsBuilder();
        parseTlvs(tlvsBuilder, bytes.slice());
        return builder.setTlvs(tlvsBuilder.build()).build();
    }

    protected void parseFlags(final SrpBuilder builder, final ByteBuf bytes) {
        bytes.skipBytes(FLAGS_SIZE / Byte.SIZE);
    }

    @Override
    public void addTlv(final TlvsBuilder builder, final Tlv tlv) {
        if (tlv instanceof SymbolicPathName symbolic) {
            builder.setSymbolicPathName(symbolic);
        }
        if (tlv instanceof PathSetupType setup) {
            builder.setPathSetupType(setup);
        }
    }

    @Override
    public void serializeObject(final Object object, final ByteBuf buffer) {
        checkArgument(object instanceof Srp, "Wrong instance of PCEPObject. Passed %s . Needed SrpObject.",
            object.getClass());
        final Srp srp = (Srp) object;
        final ByteBuf body = Unpooled.buffer();
        serializeFlags(srp, body);
        final SrpIdNumber srpId = srp.getOperationId();
        checkArgument(srpId != null, "SrpId is mandatory.");
        ByteBufUtils.write(body, srpId.getValue());
        serializeTlvs(srp.getTlvs(), body);
        ObjectUtil.formatSubobject(TYPE, CLASS, object.getProcessingRule(), object.getIgnore(), body, buffer);
    }

    protected void serializeFlags(final Srp srp, final ByteBuf body) {
        body.writeZero(FLAGS_SIZE / Byte.SIZE);
    }

    @NonNullByDefault
    public void serializeTlvs(final @Nullable Tlvs tlvs, final ByteBuf body) {
        if (tlvs != null) {
            serializeOptionalTlv(tlvs.getSymbolicPathName(), body);
            serializeOptionalTlv(tlvs.getPathSetupType(), body);
        }
    }

    @Override
    protected final void addVendorInformationTlvs(final TlvsBuilder builder, final List<VendorInformationTlv> tlvs) {
        if (!tlvs.isEmpty()) {
            builder.setVendorInformationTlv(tlvs);
        }
    }
}
