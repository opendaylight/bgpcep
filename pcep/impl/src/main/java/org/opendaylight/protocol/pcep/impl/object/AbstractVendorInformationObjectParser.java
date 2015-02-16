/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.impl.object;

import static org.opendaylight.protocol.pcep.spi.VendorInformationUtil.VENDOR_INFORMATION_OBJECT_CLASS;
import static org.opendaylight.protocol.pcep.spi.VendorInformationUtil.VENDOR_INFORMATION_OBJECT_TYPE;
import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeUnsignedInt;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.pcep.spi.EnterpriseSpecificInformationParser;
import org.opendaylight.protocol.pcep.spi.ObjectParser;
import org.opendaylight.protocol.pcep.spi.ObjectSerializer;
import org.opendaylight.protocol.pcep.spi.ObjectUtil;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iana.rev130816.EnterpriseNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.vendor.information.objects.VendorInformationObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.vendor.information.objects.VendorInformationObjectBuilder;

public abstract class AbstractVendorInformationObjectParser implements ObjectSerializer, ObjectParser, EnterpriseSpecificInformationParser {

    @Override
    public final Object parseObject(final ObjectHeader header, final ByteBuf buffer) throws PCEPDeserializerException {
        Preconditions.checkArgument(buffer != null && buffer.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        final VendorInformationObjectBuilder builder = new VendorInformationObjectBuilder();
        builder.setEnterpriseNumber(new EnterpriseNumber(getEnterpriseNumber()));
        builder.setEnterpriseSpecificInformation(parseEnterpriseSpecificInformation(buffer));
        return builder.build();
    }

    @Override
    public final void serializeObject(final Object object, final ByteBuf buffer) {
        Preconditions.checkArgument(object instanceof VendorInformationObject, "Wrong instance of PCEPObject. Passed %s. Needed VendorInformationObject.", object.getClass());
        final ByteBuf body = Unpooled.buffer();
        writeUnsignedInt(getEnterpriseNumber().getValue(), body);
        serializeEnterpriseSpecificInformation(((VendorInformationObject) object).getEnterpriseSpecificInformation(), body);
        ObjectUtil.formatSubobject(VENDOR_INFORMATION_OBJECT_TYPE, VENDOR_INFORMATION_OBJECT_CLASS, object.isProcessingRule(), object.isIgnore(), body, buffer);
    }

}
