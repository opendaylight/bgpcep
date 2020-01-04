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
import java.util.Optional;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iana.rev130816.EnterpriseNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.vendor.information.tlvs.VendorInformationTlv;
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
        final List<VendorInformationTlv> viTlvs = new ArrayList<>();
        while (bytes.isReadable()) {
            final int type = bytes.readUnsignedShort();
            final int length = bytes.readUnsignedShort();
            if (length > bytes.readableBytes()) {
                throw new PCEPDeserializerException("Wrong length specified. Passed: " + length + "; Expected: <= "
            + bytes.readableBytes()
                    + ".");
            }
            final ByteBuf tlvBytes = bytes.readSlice(length);
            LOG.trace("Parsing PCEP TLV : {}", ByteBufUtil.hexDump(tlvBytes));

            if (VendorInformationUtil.isVendorInformationTlv(type)) {
                final EnterpriseNumber enterpriseNumber = new EnterpriseNumber(ByteBufUtils.readUint32(tlvBytes));
                final Optional<VendorInformationTlv> viTlv = this.viTlvReg.parseVendorInformationTlv(enterpriseNumber,
                    tlvBytes);
                if (viTlv.isPresent()) {
                    LOG.trace("Parsed VENDOR-INFORMATION TLV {}.", viTlv.get());
                    viTlvs.add(viTlv.get());
                }
            } else {
                final Tlv tlv = this.tlvReg.parseTlv(type, tlvBytes);
                if (tlv != null) {
                    LOG.trace("Parsed PCEP TLV {}.", tlv);
                    addTlv(builder, tlv);
                }
            }
            bytes.skipBytes(TlvUtil.getPadding(TlvUtil.HEADER_SIZE + length, TlvUtil.PADDED_TO));
        }
        addVendorInformationTlvs(builder, viTlvs);
    }

    protected final void serializeTlv(final Tlv tlv, final ByteBuf buffer) {
        requireNonNull(tlv, "PCEP TLV is mandatory.");
        LOG.trace("Serializing PCEP TLV {}", tlv);
        this.tlvReg.serializeTlv(tlv, buffer);
        LOG.trace("Serialized PCEP TLV : {}.", ByteBufUtil.hexDump(buffer));
    }

    protected void addTlv(final T builder, final Tlv tlv) {
        // FIXME: No TLVs by default, fallback to augments
    }

    protected abstract void addVendorInformationTlvs(T builder, List<VendorInformationTlv> tlvs);

    protected final void serializeVendorInformationTlvs(final List<VendorInformationTlv> tlvs, final ByteBuf buffer) {
        if (tlvs != null) {
            for (final VendorInformationTlv tlv : tlvs) {
                LOG.trace("Serializing VENDOR-INFORMATION TLV {}", tlv);
                this.viTlvReg.serializeVendorInformationTlv(tlv, buffer);
                LOG.trace("Serialized VENDOR-INFORMATION TLV : {}.", ByteBufUtil.hexDump(buffer));
            }
        }
    }
}
