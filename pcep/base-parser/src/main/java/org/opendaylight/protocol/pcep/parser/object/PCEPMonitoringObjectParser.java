/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
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
import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.AbstractObjectWithTlvsParser;
import org.opendaylight.protocol.pcep.spi.ObjectUtil;
import org.opendaylight.protocol.pcep.spi.TlvRegistry;
import org.opendaylight.protocol.pcep.spi.VendorInformationTlvRegistry;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.monitoring.object.Monitoring;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.monitoring.object.Monitoring.Flags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.monitoring.object.MonitoringBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.monitoring.object.monitoring.Tlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.monitoring.object.monitoring.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.vendor.information.tlvs.VendorInformationTlv;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.netty.ByteBufUtils;

/**
 * Parser for {@link Monitoring}.
 * @see <a href="https://tools.ietf.org/html/rfc5886#section-4.1">Monitoring Object</a>
 *
 */
public class PCEPMonitoringObjectParser extends AbstractObjectWithTlvsParser<TlvsBuilder> {
    private static final int CLASS = 19;
    private static final int TYPE = 1;

    private static final int FLAGS_SIZE = 24;
    private static final int RESERVED = 1;
    private static final int L_FLAG_POS = 23;
    private static final int G_FLAG_POS = 22;
    private static final int P_FLAG_POS = 21;
    private static final int C_FLAG_POS = 20;
    private static final int I_FLAG_POS = 19;

    public PCEPMonitoringObjectParser(final TlvRegistry tlvReg, final VendorInformationTlvRegistry viTlvReg) {
        super(tlvReg, viTlvReg, CLASS, TYPE);
    }

    @Override
    public Object parseObject(final ObjectHeader header, final ByteBuf buffer) throws PCEPDeserializerException {
        checkArgument(buffer != null && buffer.isReadable(), "Array of bytes is mandatory. Cannot be null or empty.");
        buffer.skipBytes(RESERVED);
        final BitArray flagBits = BitArray.valueOf(buffer, FLAGS_SIZE);
        final Flags flags = new Flags(flagBits.get(G_FLAG_POS), flagBits.get(I_FLAG_POS), flagBits.get(L_FLAG_POS),
                flagBits.get(C_FLAG_POS), flagBits.get(P_FLAG_POS));
        final Uint32 monitoring = ByteBufUtils.readUint32(buffer);
        final TlvsBuilder tbuilder = new TlvsBuilder();
        parseTlvs(tbuilder, buffer.slice());
        return new MonitoringBuilder()
                .setFlags(flags)
                .setMonitoringId(monitoring)
                .setTlvs(tbuilder.build())
                .build();
    }

    @Override
    public void serializeObject(final Object object, final ByteBuf buffer) {
        checkArgument(object instanceof Monitoring, "Wrong instance of PCEPObject. Passed %s. Needed MonitoringObject.",
            object.getClass());
        final Monitoring monitoring = (Monitoring) object;
        final ByteBuf body = Unpooled.buffer();
        body.writeZero(RESERVED);
        final Flags flags = monitoring.getFlags();
        final BitArray flagBits = new BitArray(FLAGS_SIZE);
        flagBits.set(I_FLAG_POS, flags.getIncomplete());
        flagBits.set(C_FLAG_POS, flags.getOverload());
        flagBits.set(P_FLAG_POS, flags.getProcessingTime());
        flagBits.set(G_FLAG_POS, flags.getGeneral());
        flagBits.set(L_FLAG_POS, flags.getLiveness());
        flagBits.toByteBuf(body);
        ByteBufUtils.writeOrZero(body, monitoring.getMonitoringId());
        serializeTlvs(monitoring.getTlvs(), body);
        ObjectUtil.formatSubobject(TYPE, CLASS, object.getProcessingRule(), object.getIgnore(), body, buffer);
    }

    @Override
    protected void addVendorInformationTlvs(final TlvsBuilder builder, final List<VendorInformationTlv> tlvs) {
        if (!tlvs.isEmpty()) {
            builder.setVendorInformationTlv(tlvs);
        }
    }

    public void serializeTlvs(final Tlvs tlvs, final ByteBuf body) {
        if (tlvs != null) {
            serializeVendorInformationTlvs(tlvs.getVendorInformationTlv(), body);
        }
    }
}
