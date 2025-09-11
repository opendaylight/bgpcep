/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.spi;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iana.rev130816.EnterpriseNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.vendor.information.tlvs.VendorInformationTlv;
import org.opendaylight.yangtools.yang.common.netty.ByteBufUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractObjectWithTlvsParser<T> extends CommonObjectParser implements ObjectSerializer {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractObjectWithTlvsParser.class);

    private final TlvRegistry tlvReg;
    private final VendorInformationTlvRegistry viTlvReg;

    protected AbstractObjectWithTlvsParser(final TlvRegistry tlvReg, final VendorInformationTlvRegistry viTlvReg,
        final int objectClass, final int objectType) {
        super(objectClass, objectType);
        this.tlvReg = requireNonNull(tlvReg);
        this.viTlvReg = requireNonNull(viTlvReg);
    }

    protected final void parseTlvs(final T builder, final ByteBuf bytes) throws PCEPDeserializerException {
        checkArgument(bytes != null, "Array of bytes is mandatory. Can't be null.");
        if (!bytes.isReadable()) {
            return;
        }
        final var viTlvs = new ArrayList<VendorInformationTlv>();
        while (bytes.isReadable()) {
            final int type = bytes.readUnsignedShort();
            final int length = bytes.readUnsignedShort();
            if (length > bytes.readableBytes()) {
                throw new PCEPDeserializerException("Wrong length specified. Passed: " + length + "; Expected: <= "
                    + bytes.readableBytes() + ".");
            }
            final var tlvBytes = bytes.readSlice(length);
            LOG.trace("Parsing PCEP TLV {}/{}: {}", type, length, ByteBufUtil.hexDump(tlvBytes));

            if (VendorInformationUtil.isVendorInformationTlv(type)) {
                final var enterpriseNumber = new EnterpriseNumber(ByteBufUtils.readUint32(tlvBytes));
                viTlvReg.parseVendorInformationTlv(enterpriseNumber, tlvBytes).ifPresent(viTlv -> {
                    LOG.trace("Parsed VENDOR-INFORMATION TLV {}.", viTlv);
                    viTlvs.add(viTlv);
                });
            } else {
                final Tlv tlv = tlvReg.parseTlv(type, tlvBytes);
                if (tlv != null) {
                    LOG.trace("Parsed PCEP TLV {}.", tlv);
                    addTlv(builder, tlv);
                }
            }
            bytes.skipBytes(TlvUtil.getPadding(TlvUtil.HEADER_SIZE + length, TlvUtil.PADDED_TO));
        }
        addVendorInformationTlvs(builder, viTlvs);
    }

    @NonNullByDefault
    protected final void serializeTlv(final Tlv tlv, final ByteBuf buffer) {
        requireNonNull(tlv, "PCEP TLV is mandatory.");
        LOG.trace("Serializing PCEP TLV {}", tlv);
        tlvReg.serializeTlv(tlv, buffer);
        LOG.trace("Serialized PCEP TLV : {}.", ByteBufUtil.hexDump(buffer));
    }

    @NonNullByDefault
    protected final void serializeOptionalTlv(final @Nullable Tlv tlv, final ByteBuf buffer) {
        if (tlv != null) {
            serializeTlv(tlv, buffer);
        }
    }

    protected void addTlv(final T builder, final Tlv tlv) {
        // FIXME: No TLVs by default, fallback to augments
    }

    protected abstract void addVendorInformationTlvs(T builder, List<VendorInformationTlv> tlvs);

    @NonNullByDefault
    protected final void serializeVendorInformationTlvs(final @Nullable List<VendorInformationTlv> tlvs,
            final ByteBuf buffer) {
        if (tlvs != null) {
            for (var tlv : tlvs) {
                LOG.trace("Serializing VENDOR-INFORMATION TLV {}", tlv);
                viTlvReg.serializeVendorInformationTlv(tlv, buffer);
                LOG.trace("Serialized VENDOR-INFORMATION TLV : {}.", ByteBufUtil.hexDump(buffer));
            }
        }
    }
}
