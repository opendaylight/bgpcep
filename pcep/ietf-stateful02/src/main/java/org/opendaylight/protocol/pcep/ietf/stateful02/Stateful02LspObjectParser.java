/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.ietf.stateful02;

import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeMedium;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.BitSet;
import java.util.List;
import org.opendaylight.protocol.pcep.spi.AbstractObjectWithTlvsParser;
import org.opendaylight.protocol.pcep.spi.ObjectUtil;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.TlvRegistry;
import org.opendaylight.protocol.pcep.spi.VendorInformationTlvRegistry;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.PlspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.lsp.db.version.tlv.LspDbVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.lsp.object.Lsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.lsp.object.LspBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.lsp.object.lsp.Tlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.lsp.object.lsp.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.rsvp.error.spec.tlv.RsvpErrorSpec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.symbolic.path.name.tlv.SymbolicPathName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.vendor.information.tlvs.VendorInformationTlv;

/**
 * Parser for {@link Lsp}
 */
public class Stateful02LspObjectParser extends AbstractObjectWithTlvsParser<TlvsBuilder> {

    public static final int CLASS = 32;

    public static final int TYPE = 1;

    /*
     * first 4b are restricted
     */
    private static final int DELEGATE_FLAG_OFFSET = 15;
    private static final int SYNC_FLAG_OFFSET = 14;
    private static final int OPERATIONAL_FLAG_OFFSET = 13;
    private static final int REMOVE_FLAG_OFFSET = 12;

    private static final int ONE_B_OFFSET = 12;
    private static final int TWO_B_OFFSET = 4;

    private static final int FLAGS_SIZE = 2;

    public Stateful02LspObjectParser(final TlvRegistry tlvReg, final VendorInformationTlvRegistry viTlvReg) {
        super(tlvReg, viTlvReg);
    }

    @Override
    public Lsp parseObject(final ObjectHeader header, final ByteBuf bytes) throws PCEPDeserializerException {
        Preconditions.checkArgument(bytes != null && bytes.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        final LspBuilder builder = new LspBuilder();
        builder.setIgnore(header.isIgnore());
        builder.setProcessingRule(header.isProcessingRule());
        final int[] plspIdRaw = { bytes.readUnsignedByte(), bytes.readUnsignedByte(), bytes.getUnsignedByte(2) };
        builder.setPlspId(new PlspId((long) ((plspIdRaw[0] << ONE_B_OFFSET) | (plspIdRaw[1] << TWO_B_OFFSET) | (plspIdRaw[2] >> TWO_B_OFFSET))));
        final BitSet flags = ByteArray.bytesToBitSet(ByteArray.readBytes(bytes, FLAGS_SIZE));
        builder.setDelegate(flags.get(DELEGATE_FLAG_OFFSET));
        builder.setSync(flags.get(SYNC_FLAG_OFFSET));
        builder.setRemove(flags.get(REMOVE_FLAG_OFFSET));
        builder.setOperational(flags.get(OPERATIONAL_FLAG_OFFSET));
        final TlvsBuilder b = new TlvsBuilder();
        parseTlvs(b, bytes.slice());
        builder.setTlvs(b.build());
        return builder.build();
    }

    @Override
    public void addTlv(final TlvsBuilder builder, final Tlv tlv) {
        if (tlv instanceof RsvpErrorSpec) {
            builder.setRsvpErrorSpec((RsvpErrorSpec) tlv);
        } else if (tlv instanceof SymbolicPathName) {
            builder.setSymbolicPathName((SymbolicPathName) tlv);
        } else if (tlv instanceof LspDbVersion) {
            builder.setLspDbVersion((LspDbVersion) tlv);
        }
    }

    @Override
    public void serializeObject(final Object object, final ByteBuf buffer) {
        Preconditions.checkArgument(object instanceof Lsp, "Wrong instance of PCEPObject. Passed %s. Needed LspObject.", object.getClass());
        final Lsp specObj = (Lsp) object;
        final ByteBuf body = Unpooled.buffer();
        final PlspId plsp = specObj.getPlspId();
        Preconditions.checkArgument(plsp != null, "PLSP-ID not present");
        writeMedium(plsp.getValue().intValue() << TWO_B_OFFSET, body);

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
        if (specObj.isOperational() != null) {
            flags.set(OPERATIONAL_FLAG_OFFSET, specObj.isOperational());
        }
        body.writeByte(ByteArray.bitSetToBytes(flags, FLAGS_SIZE)[1]);
        serializeTlvs(specObj.getTlvs(), body);
        ObjectUtil.formatSubobject(TYPE, CLASS, object.isProcessingRule(), object.isIgnore(), body, buffer);
    }

    public void serializeTlvs(final Tlvs tlvs, final ByteBuf body) {
        if (tlvs == null) {
            return;
        }
        if (tlvs.getRsvpErrorSpec() != null) {
            serializeTlv(tlvs.getRsvpErrorSpec(), body);
        }
        if (tlvs.getSymbolicPathName() != null) {
            serializeTlv(tlvs.getSymbolicPathName(), body);
        }
        if (tlvs.getLspDbVersion() != null) {
            serializeTlv(tlvs.getLspDbVersion(), body);
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
