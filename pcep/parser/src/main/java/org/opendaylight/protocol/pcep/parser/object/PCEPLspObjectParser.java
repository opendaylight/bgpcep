/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.parser.object;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.primitives.UnsignedBytes;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.lsp.object.Lsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.lsp.object.LspBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.lsp.object.lsp.LspFlagsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.lsp.object.lsp.Tlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.lsp.object.lsp.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.OperationalStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.PlspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.lsp.db.version.tlv.LspDbVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.lsp.error.code.tlv.LspErrorCode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.lsp.identifiers.tlv.LspIdentifiers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.path.binding.tlv.PathBinding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.rsvp.error.spec.tlv.RsvpErrorSpec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.symbolic.path.name.tlv.SymbolicPathName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.vendor.information.tlvs.VendorInformationTlv;
import org.opendaylight.yangtools.yang.common.Uint32;

/**
 * Parser for {@link Lsp}.
 */
public class PCEPLspObjectParser extends AbstractObjectWithTlvsParser<TlvsBuilder> {
    private static final int CLASS = 32;
    private static final int TYPE = 1;

    /*
     * 12b extended to 16b so first 4b are restricted (belongs to LSP ID)
     */
    protected static final int DELEGATE = 11;
    protected static final int SYNC = 10;
    protected static final int REMOVE = 9;
    protected static final int ADMINISTRATIVE = 8;
    protected static final int OPERATIONAL = 5;
    protected static final int CREATE = 4;
    protected static final int PCE_ALLOCATION = 0;

    protected static final int FOUR_BITS_SHIFT = 4;
    protected static final int FLAGS_SIZE = 12;

    public PCEPLspObjectParser(final TlvRegistry tlvReg, final VendorInformationTlvRegistry viTlvReg) {
        super(tlvReg, viTlvReg, CLASS, TYPE);
    }

    @Override
    public Lsp parseObject(final ObjectHeader header, final ByteBuf bytes) throws PCEPDeserializerException {
        checkArgument(bytes != null && bytes.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        final LspBuilder builder = new LspBuilder()
            .setIgnore(header.getIgnore())
            .setProcessingRule(header.getProcessingRule());
        final int[] plspIdRaw
            = new int[] { bytes.readUnsignedByte(), bytes.readUnsignedByte(), bytes.getUnsignedByte(2), };
        builder.setPlspId(new PlspId(Uint32.valueOf(plspIdRaw[0] << FLAGS_SIZE | plspIdRaw[1] << FOUR_BITS_SHIFT
            | plspIdRaw[2] >> FOUR_BITS_SHIFT)));
        parseFlags(builder, bytes);
        final TlvsBuilder b = new TlvsBuilder();
        parseTlvs(b, bytes.slice());
        builder.setTlvs(b.build());
        return builder.build();
    }

    private void parseFlags(final LspBuilder builder, final ByteBuf bytes) {
        final BitArray flags = BitArray.valueOf(bytes, FLAGS_SIZE);
        short oper = 0;
        oper |= flags.get(OPERATIONAL + 2) ? 1 : 0;
        oper |= (flags.get(OPERATIONAL + 1) ? 1 : 0) << 1;
        oper |= (flags.get(OPERATIONAL) ? 1 : 0) << 2;
        builder.setLspFlags(new LspFlagsBuilder()
            .setDelegate(flags.get(DELEGATE))
            .setSync(flags.get(SYNC))
            .setRemove(flags.get(REMOVE))
            .setAdministrative(flags.get(ADMINISTRATIVE))
            .setOperational(OperationalStatus.forValue(oper))
            .setCreate(flags.get(CREATE))
            .setPceAllocation(flags.get(PCE_ALLOCATION))
            .build());
    }

    @Override
    public void addTlv(final TlvsBuilder builder, final Tlv tlv) {
        switch (tlv) {
            case LspErrorCode lec -> builder.setLspErrorCode(lec);
            case LspIdentifiers li -> builder.setLspIdentifiers(li);
            case RsvpErrorSpec rec -> builder.setRsvpErrorSpec(rec);
            case SymbolicPathName spn -> builder.setSymbolicPathName(spn);
            case LspDbVersion ldv -> builder.setLspDbVersion(ldv);
            case PathBinding pb -> builder.setPathBinding(pb);
            case null, default -> {
                // No-op
            }
        }
    }

    @Override
    public void serializeObject(final Object object, final ByteBuf buffer) {
        checkArgument(object instanceof Lsp, "Wrong instance of PCEPObject. Passed %s . Needed LspObject.",
            object.getClass());
        final Lsp specObj = (Lsp) object;
        final ByteBuf body = Unpooled.buffer();
        final PlspId plspId = specObj.getPlspId();
        checkArgument(plspId != null, "PLSP-ID not present");
        body.writeMedium(plspId.getValue().intValue() << FOUR_BITS_SHIFT);
        final BitArray flags = serializeFlags(specObj);
        byte op = 0;
        if (specObj.getLspFlags().getOperational() != null) {
            op = UnsignedBytes.checkedCast(specObj.getLspFlags().getOperational().getIntValue());
            op = (byte) (op << FOUR_BITS_SHIFT);
        }
        final byte[] res = flags.array();
        res[res.length - 1] = (byte) (res[res.length - 1] | op);
        body.writeByte(res[res.length - 1]);
        serializeTlvs(specObj.getTlvs(), body);
        ObjectUtil.formatSubobject(TYPE, CLASS, object.getProcessingRule(), object.getIgnore(), body, buffer);
    }

    private BitArray serializeFlags(final Lsp specObj) {
        final BitArray flags = new BitArray(FLAGS_SIZE);
        final var lf = specObj.getLspFlags();
        if (lf != null) {
            flags.set(DELEGATE, lf.getDelegate());
            flags.set(REMOVE, lf.getRemove());
            flags.set(SYNC, lf.getSync());
            flags.set(ADMINISTRATIVE, lf.getAdministrative());
            flags.set(CREATE, lf.getCreate());
            flags.set(PCE_ALLOCATION, lf.getPceAllocation());
        }
        return flags;
    }

    @NonNullByDefault
    public void serializeTlvs(final @Nullable Tlvs tlvs, final ByteBuf body) {
        if (tlvs == null) {
            return;
        }
        serializeOptionalTlv(tlvs.getLspErrorCode(), body);
        serializeOptionalTlv(tlvs.getLspIdentifiers(), body);
        serializeOptionalTlv(tlvs.getRsvpErrorSpec(), body);
        serializeOptionalTlv(tlvs.getSymbolicPathName(), body);
        serializeOptionalTlv(tlvs.getLspDbVersion(), body);
        serializeVendorInformationTlvs(tlvs.getVendorInformationTlv(), body);
        serializeOptionalTlv(tlvs.getPathBinding(), body);
    }

    @Override
    protected final void addVendorInformationTlvs(final TlvsBuilder builder, final List<VendorInformationTlv> tlvs) {
        if (!tlvs.isEmpty()) {
            builder.setVendorInformationTlv(tlvs);
        }
    }
}
