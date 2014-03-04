/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.spi;

import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iana.rev130816.EnterpriseNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.vs.tlv.VsTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.vs.tlv.VsTlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.vs.tlv.vs.tlv.VendorPayload;

abstract public class AbstractVendorSpecificTlvParser implements TlvParser, TlvSerializer {

    public static final int TYPE = 27;

    private static final int ENTERPRISE_NUM_LENGTH = 4;

    @Override
    public int getType() {
        return TYPE;
    }

    @Override
    public byte[] serializeTlv(Tlv tlv) {
        if (tlv == null) {
            throw new IllegalArgumentException("Vendor Specific Tlv is mandatory.");
        }
        final VsTlv vsTlv = (VsTlv) tlv;
        if (vsTlv.getEnterpriseNumber().getValue() == getEnterpriseNumber()) {
            final byte[] payloadBytes = serializeVendorPayload(vsTlv.getVendorPayload());
            final byte[] ianaNumBytes = ByteArray.longToBytes(vsTlv.getEnterpriseNumber().getValue(),
                    ENTERPRISE_NUM_LENGTH);

            final byte[] bytes = new byte[ianaNumBytes.length + payloadBytes.length];
            System.arraycopy(ianaNumBytes, 0, bytes, 0, ENTERPRISE_NUM_LENGTH);
            System.arraycopy(payloadBytes, 0, bytes, ENTERPRISE_NUM_LENGTH, payloadBytes.length);
            return bytes;
        }
        return new byte[0];
    }

    @Override
    public VsTlv parseTlv(byte[] valueBytes) throws PCEPDeserializerException {
        VsTlvBuilder vsTlvBuider = new VsTlvBuilder();
        if (valueBytes == null || valueBytes.length == 0) {
            throw new IllegalArgumentException("Array of bytes is mandatory. Can't be null or empty.");
        }

        byte[] enBytes = ByteArray.subByte(valueBytes, 0, ENTERPRISE_NUM_LENGTH);
        long en = ByteArray.bytesToLong(enBytes);
        if (en == getEnterpriseNumber()) {
            vsTlvBuider.setEnterpriseNumber(new EnterpriseNumber(getEnterpriseNumber()));
            int byteOffset = ENTERPRISE_NUM_LENGTH;
            int payloadLength = valueBytes.length - byteOffset;
            VendorPayload vendorPayload = null;
            if (payloadLength > 0) {
                byte[] payloadBytes = ByteArray.subByte(valueBytes, byteOffset, payloadLength);
                vendorPayload = parseVendorPayload(payloadBytes);
                if (vendorPayload != null) {
                    vsTlvBuider.setVendorPayload(vendorPayload);
                }
            }
        }
        return vsTlvBuider.build();
    }

    abstract protected byte[] serializeVendorPayload(VendorPayload payload);

    abstract protected long getEnterpriseNumber();

    abstract protected VendorPayload parseVendorPayload(byte[] payloadBytes) throws PCEPDeserializerException;

    protected static int getPadding(final int length, final int padding) {
        return (padding - (length % padding)) % padding;
    }
}
