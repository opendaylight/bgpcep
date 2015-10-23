/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.tlv;

import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeUnsignedInt;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.TlvParser;
import org.opendaylight.protocol.pcep.spi.TlvSerializer;
import org.opendaylight.protocol.pcep.spi.TlvUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iana.rev130816.EnterpriseNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.vs.tlv.VsTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.vs.tlv.VsTlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.vs.tlv.vs.tlv.VendorPayload;

public abstract class AbstractVendorSpecificTlvParser implements TlvParser, TlvSerializer {

    public static final int TYPE = 27;

    protected static final int ENTERPRISE_NUM_LENGTH = 4;

    @Override
    @Deprecated
    public void serializeTlv(final Tlv tlv, final ByteBuf buffer) {
        Preconditions.checkArgument(tlv instanceof VsTlv, "Vendor Specific Tlv is mandatory.");
        final VsTlv vsTlv = (VsTlv) tlv;
        final ByteBuf body = Unpooled.buffer();
        if (vsTlv.getEnterpriseNumber().getValue() == getEnterpriseNumber()) {
            Preconditions.checkArgument(vsTlv.getEnterpriseNumber() != null, "EnterpriseNumber is mandatory.");
            writeUnsignedInt(vsTlv.getEnterpriseNumber().getValue(), body);
            serializeVendorPayload(vsTlv.getVendorPayload(), body);
            TlvUtil.formatTlv(TYPE, body, buffer);
        }
    }

    @Override
    @Deprecated
    public VsTlv parseTlv(final ByteBuf buffer) throws PCEPDeserializerException {
        if (buffer == null) {
            return null;
        }
        final VsTlvBuilder vsTlvBuider = new VsTlvBuilder();
        final long en = buffer.readUnsignedInt();
        if (en == getEnterpriseNumber()) {
            vsTlvBuider.setEnterpriseNumber(new EnterpriseNumber(getEnterpriseNumber()));
            VendorPayload vendorPayload = null;
            if (buffer.isReadable()) {
                final ByteBuf payloadBytes = buffer.slice();
                vendorPayload = parseVendorPayload(payloadBytes);
                if (vendorPayload != null) {
                    vsTlvBuider.setVendorPayload(vendorPayload);
                }
            }
        }
        return vsTlvBuider.build();
    }

    @Deprecated
    protected abstract void serializeVendorPayload(final VendorPayload payload, final ByteBuf buffer);

    @Deprecated
    protected abstract long getEnterpriseNumber();

    @Deprecated
    protected abstract VendorPayload parseVendorPayload(final ByteBuf payloadBytes) throws PCEPDeserializerException;
}
