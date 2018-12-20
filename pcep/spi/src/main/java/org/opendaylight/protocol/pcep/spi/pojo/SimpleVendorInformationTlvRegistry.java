/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.spi.pojo;

import com.google.common.primitives.Ints;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import org.opendaylight.protocol.concepts.HandlerRegistry;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.TlvParser;
import org.opendaylight.protocol.pcep.spi.TlvSerializer;
import org.opendaylight.protocol.pcep.spi.VendorInformationTlvRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iana.rev130816.EnterpriseNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.vendor.information.EnterpriseSpecificInformation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.vendor.information.tlvs.VendorInformationTlv;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.DataContainer;

public class SimpleVendorInformationTlvRegistry implements VendorInformationTlvRegistry {

    private final HandlerRegistry<DataContainer, TlvParser, TlvSerializer> handlers = new HandlerRegistry<>();

    public Registration registerVendorInformationTlvParser(final EnterpriseNumber enterpriseNumber,
            final TlvParser parser) {
        return this.handlers.registerParser(Ints.checkedCast(enterpriseNumber.getValue()), parser);
    }

    public Registration registerVendorInformationTlvSerializer(
            final Class<? extends EnterpriseSpecificInformation> esInformationClass, final TlvSerializer serializer) {
        return this.handlers.registerSerializer(esInformationClass, serializer);
    }

    @Override
    public Optional<VendorInformationTlv> parseVendorInformationTlv(final EnterpriseNumber enterpriseNumber,
            final ByteBuf buffer) throws PCEPDeserializerException {
        final TlvParser parser = this.handlers.getParser(Ints.checkedCast(enterpriseNumber.getValue()));
        if (parser == null) {
            return Optional.empty();
        }
        return Optional.of((VendorInformationTlv) parser.parseTlv(buffer));
    }

    @Override
    public void serializeVendorInformationTlv(final VendorInformationTlv viTlv, final ByteBuf buffer) {
        final TlvSerializer serializer = this.handlers.getSerializer(
            viTlv.getEnterpriseSpecificInformation().getImplementedInterface());
        if (serializer == null) {
            return;
        }
        serializer.serializeTlv(viTlv, buffer);
    }
}
