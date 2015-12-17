/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.impl.object;

import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeUnsignedInt;

import com.google.common.base.Preconditions;
import com.google.common.primitives.UnsignedBytes;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.List;
import org.opendaylight.protocol.pcep.spi.AbstractObjectWithTlvsParser;
import org.opendaylight.protocol.pcep.spi.ObjectUtil;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.TlvRegistry;
import org.opendaylight.protocol.pcep.spi.VendorInformationTlvRegistry;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.RequestId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.order.tlv.Order;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.path.setup.type.tlv.PathSetupType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.rp.object.Rp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.rp.object.RpBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.rp.object.rp.Tlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.rp.object.rp.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.vendor.information.tlvs.VendorInformationTlv;

/**
 * Parser for {@link Rp}
 */
public class PCEPRequestParameterObjectParser extends AbstractObjectWithTlvsParser<TlvsBuilder> {

    public static final int CLASS = 2;

    public static final int TYPE = 1;

    /*
     * lengths of fields in bytes
     */
    private static final int FLAGS_SIZE = 32;

    /*
     * lengths of subfields inside multi-field in bits
     */
    private static final int FLAGS_SF_LENGTH = 29;

    /*
     * offsets of subfields inside multi-field in bits
     */

    private static final int FLAGS_SF_OFFSET = 0;
    private static final int PRI_SF_OFFSET = FLAGS_SF_OFFSET + FLAGS_SF_LENGTH;

    /*
     * flags offsets inside flags sub-field in bits
     */

    private static final int O_FLAG_OFFSET = 26;
    private static final int B_FLAG_OFFSET = 27;
    private static final int R_FLAG_OFFSET = 28;

    /*
     * GCO extension flags offsets inside flags sub-field in bits
     */
    private static final int M_FLAG_OFFSET = 21;
    private static final int D_FLAG_OFFSET = 22;

    /*
     * Path-key bit (RFC5520)
     */
    private static final int P_FLAG_OFFSET = 23;
    /*
     * OF extension flags offsets inside flags sub.field in bits
     */
    private static final int S_FLAG_OFFSET = 24;
    /*
     * RFC6006 flags
     */
    private static final int F_FLAG_OFFSET = 18;

    private static final int N_FLAG_OFFSET = 19;

    private static final int E_FLAG_OFFSET = 20;

    public PCEPRequestParameterObjectParser(final TlvRegistry tlvReg, final VendorInformationTlvRegistry viTlvReg) {
        super(tlvReg, viTlvReg);
    }

    @Override
    public Object parseObject(final ObjectHeader header, final ByteBuf bytes) throws PCEPDeserializerException {
        Preconditions.checkArgument(bytes != null && bytes.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        final BitArray flags = BitArray.valueOf(bytes, FLAGS_SIZE);

        final RpBuilder builder = new RpBuilder();
        builder.setIgnore(header.isIgnore());
        builder.setProcessingRule(header.isProcessingRule());

        short priority = 0;
        priority |= flags.get(PRI_SF_OFFSET + 2) ? 1 : 0;
        priority |= (flags.get(PRI_SF_OFFSET + 1) ? 1 : 0) << 1;
        priority |= (flags.get(PRI_SF_OFFSET) ? 1 : 0) << 2;
        if (priority != 0) {
            builder.setPriority(priority);
        }
        builder.setFragmentation(flags.get(F_FLAG_OFFSET));
        builder.setP2mp(flags.get(N_FLAG_OFFSET));
        builder.setEroCompression(flags.get(E_FLAG_OFFSET));
        builder.setMakeBeforeBreak(flags.get(M_FLAG_OFFSET));
        builder.setOrder(flags.get(D_FLAG_OFFSET));
        builder.setPathKey(flags.get(P_FLAG_OFFSET));
        builder.setSupplyOf(flags.get(S_FLAG_OFFSET));
        builder.setLoose(flags.get(O_FLAG_OFFSET));
        builder.setBiDirectional(flags.get(B_FLAG_OFFSET));
        builder.setReoptimization(flags.get(R_FLAG_OFFSET));

        builder.setRequestId(new RequestId(bytes.readUnsignedInt()));
        final TlvsBuilder tlvsBuilder = new TlvsBuilder();
        parseTlvs(tlvsBuilder, bytes.slice());
        builder.setTlvs(tlvsBuilder.build());
        return builder.build();
    }

    @Override
    public void addTlv(final TlvsBuilder builder, final Tlv tlv) {
        if (tlv instanceof Order) {
            builder.setOrder((Order) tlv);
        }
        if (tlv instanceof PathSetupType) {
            builder.setPathSetupType((PathSetupType) tlv);
        }
    }

    @Override
    public void serializeObject(final Object object, final ByteBuf buffer) {
        Preconditions.checkArgument(object instanceof Rp, "Wrong instance of PCEPObject. Passed %s. Needed RPObject.", object.getClass());
        final ByteBuf body = Unpooled.buffer();
        final Rp rpObj = (Rp) object;
        final BitArray flags = new BitArray(FLAGS_SIZE);
        flags.set(R_FLAG_OFFSET, rpObj.isReoptimization());
        flags.set(B_FLAG_OFFSET, rpObj.isBiDirectional());
        flags.set(O_FLAG_OFFSET, rpObj.isLoose());
        flags.set(M_FLAG_OFFSET, rpObj.isMakeBeforeBreak());
        flags.set(D_FLAG_OFFSET, rpObj.isOrder());
        flags.set(P_FLAG_OFFSET, rpObj.isPathKey());
        flags.set(S_FLAG_OFFSET, rpObj.isSupplyOf());
        flags.set(F_FLAG_OFFSET, rpObj.isFragmentation());
        flags.set(N_FLAG_OFFSET, rpObj.isP2mp());
        flags.set(E_FLAG_OFFSET, rpObj.isEroCompression());
        final byte[] res = flags.array();
        if (rpObj.getPriority() != null) {
            final byte p = UnsignedBytes.checkedCast(rpObj.getPriority().shortValue());
            res[res.length -1] = (byte) (res[res.length -1] | p);
        }
        body.writeBytes(res);
        Preconditions.checkArgument(rpObj.getRequestId() != null, "RequestId is mandatory");
        writeUnsignedInt(rpObj.getRequestId().getValue(), body);
        serializeTlvs(rpObj.getTlvs(), body);
        ObjectUtil.formatSubobject(TYPE, CLASS, object.isProcessingRule(), object.isIgnore(), body, buffer);
    }

    public void serializeTlvs(final Tlvs tlvs, final ByteBuf body) {
        if (tlvs == null) {
            return;
        }
        if (tlvs.getOrder() != null) {
            serializeTlv(tlvs.getOrder(), body);
        }
        if (tlvs.getPathSetupType() != null) {
            serializeTlv(tlvs.getPathSetupType(), body);
        }
        serializeVendorInformationTlvs(tlvs.getVendorInformationTlv(), body);
    }

    @Override
    protected final void addVendorInformationTlvs(final TlvsBuilder builder, final List<VendorInformationTlv> tlvs) {
        if (!tlvs.isEmpty()) {
            builder.setVendorInformationTlv(tlvs);
        }
    }
}
