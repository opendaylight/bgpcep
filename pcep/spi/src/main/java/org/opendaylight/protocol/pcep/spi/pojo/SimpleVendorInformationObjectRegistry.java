/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.spi.pojo;

import io.netty.buffer.ByteBuf;
import java.util.Optional;
import org.opendaylight.protocol.concepts.HandlerRegistry;
import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.ObjectParser;
import org.opendaylight.protocol.pcep.spi.ObjectSerializer;
import org.opendaylight.protocol.pcep.spi.PCEPErrors;
import org.opendaylight.protocol.pcep.spi.UnknownObject;
import org.opendaylight.protocol.pcep.spi.VendorInformationObjectRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iana.rev130816.EnterpriseNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.vendor.information.EnterpriseSpecificInformation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.vendor.information.objects.VendorInformationObject;
import org.opendaylight.yangtools.binding.DataContainer;
import org.opendaylight.yangtools.concepts.Registration;

public class SimpleVendorInformationObjectRegistry implements VendorInformationObjectRegistry {
    private final HandlerRegistry<DataContainer, ObjectParser, ObjectSerializer> handlers = new HandlerRegistry<>();

    public Registration registerVendorInformationObjectParser(final EnterpriseNumber enterpriseNumber,
            final ObjectParser parser) {
        return this.handlers.registerParser(enterpriseNumber.getValue().intValue(), parser);
    }

    public Registration registerVendorInformationObjectSerializer(
            final Class<? extends EnterpriseSpecificInformation> esInformationClass,
            final ObjectSerializer serializer) {
        return this.handlers.registerSerializer(esInformationClass, serializer);
    }

    @Override
    public Optional<? extends Object> parseVendorInformationObject(final EnterpriseNumber enterpriseNumber,
            final ObjectHeader header, final ByteBuf buffer)
            throws PCEPDeserializerException {
        final ObjectParser parser = this.handlers.getParser(enterpriseNumber.getValue().intValue());
        if (parser == null) {
            if (!header.getProcessingRule()) {
                return Optional.empty();
            }
            return Optional.of(new UnknownObject(PCEPErrors.UNRECOGNIZED_OBJ_CLASS));
        }
        return Optional.of(parser.parseObject(header, buffer));
    }

    @Override
    public void serializeVendorInformationObject(final VendorInformationObject viObject, final ByteBuf buffer) {
        final ObjectSerializer serializer = this.handlers.getSerializer(
            viObject.getEnterpriseSpecificInformation().implementedInterface());
        if (serializer == null) {
            return;
        }
        serializer.serializeObject(viObject, buffer);
    }
}
