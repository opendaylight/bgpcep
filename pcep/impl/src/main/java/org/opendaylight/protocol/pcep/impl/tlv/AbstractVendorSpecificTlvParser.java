/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.tlv;

import io.netty.buffer.ByteBuf;

import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.TlvParser;
import org.opendaylight.protocol.pcep.spi.TlvSerializer;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iana.rev130816.EnterpriseNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.vs.tlv.VsTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.vs.tlv.VsTlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.vs.tlv.vs.tlv.VendorPayload;

public abstract class AbstractVendorSpecificTlvParser implements TlvParser, TlvSerializer {

    public static final int TYPE = 27;

    protected static final int ENTERPRISE_NUM_LENGTH = 4;

    @Override
    public byte[] serializeTlv(final Tlv tlv) {
        if (tlv == null) {
            throw new IllegalArgumentException("Vendor Specific Tlv is mandatory.");
        }
        final VsTlv vsTlv = (VsTlv) tlv;
        if (vsTlv.getEnterpriseNumber().getValue() == getEnterpriseNumber()) {
            final byte[] payloadBytes = serializeVendorPayload(vsTlv.getVendorPayload());
            final byte[] ianaNumBytes = ByteArray.longToBytes(vsTlv.getEnterpriseNumber().getValue(), ENTERPRISE_NUM_LENGTH);

            final byte[] bytes = new byte[ianaNumBytes.length + payloadBytes.length];
            System.arraycopy(ianaNumBytes, 0, bytes, 0, ENTERPRISE_NUM_LENGTH);
            System.arraycopy(payloadBytes, 0, bytes, ENTERPRISE_NUM_LENGTH, payloadBytes.length);
            return TlvUtil.formatTlv(TYPE, bytes);
        }
        return new byte[0];
    }

    @Override
    public VsTlv parseTlv(final ByteBuf buffer) throws PCEPDeserializerException {
        if (buffer == null) {
            return null;
        }
        VsTlvBuilder vsTlvBuider = new VsTlvBuilder();
        long en = buffer.readUnsignedInt();
        if (en == getEnterpriseNumber()) {
            vsTlvBuider.setEnterpriseNumber(new EnterpriseNumber(getEnterpriseNumber()));
            VendorPayload vendorPayload = null;
            if (buffer.isReadable()) {
                ByteBuf payloadBytes = buffer.slice();
                // FIXME: change this to ByteBuf
                vendorPayload = parseVendorPayload(ByteArray.getAllBytes(payloadBytes));
                if (vendorPayload != null) {
                    vsTlvBuider.setVendorPayload(vendorPayload);
                }
            }
        }
        return vsTlvBuider.build();
    }

    protected abstract byte[] serializeVendorPayload(VendorPayload payload);

    protected abstract long getEnterpriseNumber();

    protected abstract VendorPayload parseVendorPayload(byte[] payloadBytes) throws PCEPDeserializerException;

    protected static int getPadding(final int length, final int padding) {
        return (padding - (length % padding)) % padding;
    }
}
