/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.spi.pojo;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.primitives.Ints;
import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.concepts.HandlerRegistry;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.TlvParser;
import org.opendaylight.protocol.pcep.spi.TlvSerializer;
import org.opendaylight.protocol.pcep.spi.VendorInformationTlvRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.vendor.information.EnterpriseSpecificInformation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.vendor.information.tlvs.VendorInformationTlv;
import org.opendaylight.yangtools.yang.binding.DataContainer;

public class SimpleVendorInformationTlvRegistry implements VendorInformationTlvRegistry {

    private final HandlerRegistry<DataContainer, TlvParser, TlvSerializer> handlers = new HandlerRegistry<>();

    public AutoCloseable registerVendorInformationTlvParser(final long enterpriseNumber, final TlvParser parser) {
        Preconditions.checkArgument(enterpriseNumber >= 0);
        return this.handlers.registerParser(Ints.checkedCast(enterpriseNumber), parser);
    }

    public AutoCloseable registerVendorInformationTlvSerializer(final Class<? extends EnterpriseSpecificInformation> esInformationClass, final TlvSerializer serializer) {
        return this.handlers.registerSerializer(esInformationClass, serializer);
    }

    @Override
    public Optional<VendorInformationTlv> parseVendorInformationTlv(final long enterpriseNumber, final ByteBuf buffer) throws PCEPDeserializerException {
        Preconditions.checkArgument(enterpriseNumber >= 0);
        final TlvParser parser = this.handlers.getParser(Ints.checkedCast(enterpriseNumber));
        if (parser == null) {
            return Optional.absent();
        }
        return Optional.of((VendorInformationTlv) parser.parseTlv(buffer));
    }

    @Override
    public void serializeVendorInformationTlv(final VendorInformationTlv viTlv, final ByteBuf buffer) {
        final TlvSerializer serializer = this.handlers.getSerializer(viTlv.getEnterpriseSpecificInformation().getImplementedInterface());
        if (serializer == null) {
            return;
        }
        serializer.serializeTlv(viTlv, buffer);
    }


}
