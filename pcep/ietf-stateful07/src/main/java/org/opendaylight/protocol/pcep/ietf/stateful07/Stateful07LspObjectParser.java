/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.ietf.stateful07;

import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeMedium;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.OperationalStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.PlspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.lsp.error.code.tlv.LspErrorCode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.lsp.identifiers.tlv.LspIdentifiers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.lsp.object.Lsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.lsp.object.LspBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.lsp.object.lsp.Tlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.lsp.object.lsp.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.path.binding.tlv.PathBinding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.rsvp.error.spec.tlv.RsvpErrorSpec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.symbolic.path.name.tlv.SymbolicPathName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.vendor.information.tlvs.VendorInformationTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.vs.tlv.VsTlv;

/**
 * Parser for {@link Lsp}
 */
public class Stateful07LspObjectParser extends AbstractObjectWithTlvsParser<TlvsBuilder> {

    public static final int CLASS = 32;

    public static final int TYPE = 1;

    /*
     * 12b extended to 16b so first 4b are restricted (belongs to LSP ID)
     */
    protected static final int DELEGATE = 11;
    protected static final int SYNC = 10;
    protected static final int REMOVE = 9;
    protected static final int ADMINISTRATIVE = 8;
    protected static final int OPERATIONAL = 5;

    protected static final int FOUR_BITS_SHIFT = 4;
    protected static final int FLAGS_SIZE = 12;

    public Stateful07LspObjectParser(final TlvRegistry tlvReg, final VendorInformationTlvRegistry viTlvReg) {
        super(tlvReg, viTlvReg);
    }

    @Override
    public Lsp parseObject(final ObjectHeader header, final ByteBuf bytes) throws PCEPDeserializerException {
        Preconditions.checkArgument(bytes != null && bytes.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        final LspBuilder builder = new LspBuilder();
        builder.setIgnore(header.isIgnore());
        builder.setProcessingRule(header.isProcessingRule());
        final int[] plspIdRaw = new int[] { bytes.readUnsignedByte(), bytes.readUnsignedByte(), bytes.getUnsignedByte(2), };
        builder.setPlspId(new PlspId((long) ((plspIdRaw[0] << FLAGS_SIZE) | (plspIdRaw[1] << FOUR_BITS_SHIFT) | (plspIdRaw[2] >> FOUR_BITS_SHIFT))));
        parseFlags(builder, bytes);
        final TlvsBuilder b = new TlvsBuilder();
        parseTlvs(b, bytes.slice());
        builder.setTlvs(b.build());
        return builder.build();
    }

    protected void parseFlags(final LspBuilder builder, final ByteBuf bytes) {
        final BitArray flags = BitArray.valueOf(bytes, FLAGS_SIZE);
        builder.setDelegate(flags.get(DELEGATE));
        builder.setSync(flags.get(SYNC));
        builder.setRemove(flags.get(REMOVE));
        builder.setAdministrative(flags.get(ADMINISTRATIVE));
        short s = 0;
        s |= flags.get(OPERATIONAL + 2) ? 1 : 0;
        s |= (flags.get(OPERATIONAL + 1) ? 1 : 0) << 1;
        s |= (flags.get(OPERATIONAL) ? 1 : 0) << 2;
        builder.setOperational(OperationalStatus.forValue(s));
    }

    @Override
    public void addTlv(final TlvsBuilder builder, final Tlv tlv) {
        if (tlv instanceof LspErrorCode) {
            builder.setLspErrorCode((LspErrorCode) tlv);
        } else if (tlv instanceof LspIdentifiers) {
            builder.setLspIdentifiers((LspIdentifiers) tlv);
        } else if (tlv instanceof RsvpErrorSpec) {
            builder.setRsvpErrorSpec((RsvpErrorSpec) tlv);
        } else if (tlv instanceof SymbolicPathName) {
            builder.setSymbolicPathName((SymbolicPathName) tlv);
        } else if (tlv instanceof VsTlv) {
            builder.setVsTlv((VsTlv) tlv);
        } else if (tlv instanceof PathBinding) {
            builder.setPathBinding((PathBinding) tlv);
        }
    }

    @Override
    public void serializeObject(final Object object, final ByteBuf buffer) {
        Preconditions.checkArgument(object instanceof Lsp, "Wrong instance of PCEPObject. Passed %s . Needed LspObject.", object.getClass());
        final Lsp specObj = (Lsp) object;
        final ByteBuf body = Unpooled.buffer();
        Preconditions.checkArgument(specObj.getPlspId() != null, "PLSP-ID not present");
        writeMedium(specObj.getPlspId().getValue().intValue() << FOUR_BITS_SHIFT, body);
        final BitArray flags = serializeFlags(specObj);
        byte op = 0;
        if (specObj.getOperational() != null) {
            op = UnsignedBytes.checkedCast(specObj.getOperational().getIntValue());
            op = (byte) (op << FOUR_BITS_SHIFT);
        }
        final byte[] res = flags.array();
        res[res.length -1] = (byte) (res[res.length -1] | op);
        body.writeByte(res[res.length -1]);
        serializeTlvs(specObj.getTlvs(), body);
        ObjectUtil.formatSubobject(TYPE, CLASS, object.isProcessingRule(), object.isIgnore(), body, buffer);
    }

    protected BitArray serializeFlags(final Lsp specObj) {
        final BitArray flags = new BitArray(FLAGS_SIZE);
        flags.set(DELEGATE, specObj.isDelegate());
        flags.set(REMOVE, specObj.isRemove());
        flags.set(SYNC, specObj.isSync());
        flags.set(ADMINISTRATIVE, specObj.isAdministrative());
        return flags;
    }

    public void serializeTlvs(final Tlvs tlvs, final ByteBuf body) {
        if (tlvs == null) {
            return;
        }
        if (tlvs.getLspErrorCode() != null) {
            serializeTlv(tlvs.getLspErrorCode(), body);
        }
        if (tlvs.getLspIdentifiers() != null) {
            serializeTlv(tlvs.getLspIdentifiers(), body);
        }
        if (tlvs.getRsvpErrorSpec() != null) {
            serializeTlv(tlvs.getRsvpErrorSpec(), body);
        }
        if (tlvs.getSymbolicPathName() != null) {
            serializeTlv(tlvs.getSymbolicPathName(), body);
        }
        if (tlvs.getVsTlv() != null) {
            serializeTlv(tlvs.getVsTlv(), body);
        }
        serializeVendorInformationTlvs(tlvs.getVendorInformationTlv(), body);
        if (tlvs.getPathBinding() != null) {
            serializeTlv(tlvs.getPathBinding(), body);
        }
    }

    @Override
    protected final void addVendorInformationTlvs(final TlvsBuilder builder, final List<VendorInformationTlv> tlvs) {
        if (!tlvs.isEmpty()) {
            builder.setVendorInformationTlv(tlvs);
        }
    }
}
