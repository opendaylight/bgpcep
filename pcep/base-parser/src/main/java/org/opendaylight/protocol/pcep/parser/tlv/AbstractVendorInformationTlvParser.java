/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.parser.tlv;

import static com.google.common.base.Preconditions.checkArgument;
import static org.opendaylight.protocol.pcep.spi.VendorInformationUtil.VENDOR_INFORMATION_TLV_TYPE;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.pcep.spi.EnterpriseSpecificInformationParser;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.TlvParser;
import org.opendaylight.protocol.pcep.spi.TlvSerializer;
import org.opendaylight.protocol.pcep.spi.TlvUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.vendor.information.EnterpriseSpecificInformation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.vendor.information.tlvs.VendorInformationTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.vendor.information.tlvs.VendorInformationTlvBuilder;
import org.opendaylight.yangtools.yang.common.netty.ByteBufUtils;

public abstract class AbstractVendorInformationTlvParser
        implements TlvSerializer, TlvParser, EnterpriseSpecificInformationParser {

    @Override
    public final void serializeTlv(final Tlv tlv, final ByteBuf buffer) {
        checkArgument(tlv instanceof VendorInformationTlv, "Vendor Specific Tlv is mandatory.");
        final VendorInformationTlv viTlv = (VendorInformationTlv) tlv;
        final ByteBuf body = Unpooled.buffer();
        ByteBufUtils.write(body, getEnterpriseNumber().getValue());
        serializeEnterpriseSpecificInformation(viTlv.getEnterpriseSpecificInformation(), body);
        TlvUtil.formatTlv(VENDOR_INFORMATION_TLV_TYPE, body, buffer);
    }

    @Override
    public final VendorInformationTlv parseTlv(final ByteBuf buffer) throws PCEPDeserializerException {
        if (buffer == null) {
            return null;
        }
        final VendorInformationTlvBuilder viTlvBuider = new VendorInformationTlvBuilder()
                .setEnterpriseNumber(getEnterpriseNumber());
        if (buffer.isReadable()) {
            final EnterpriseSpecificInformation esInformation = parseEnterpriseSpecificInformation(buffer.slice());
            if (esInformation != null) {
                viTlvBuider.setEnterpriseSpecificInformation(esInformation);
            }
        }
        return viTlvBuider.build();
    }
}
