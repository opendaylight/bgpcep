/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.spi.pojo;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import org.opendaylight.protocol.concepts.HandlerRegistry;
import org.opendaylight.protocol.pcep.spi.ObjectParser;
import org.opendaylight.protocol.pcep.spi.ObjectRegistry;
import org.opendaylight.protocol.pcep.spi.ObjectSerializer;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.PCEPErrors;
import org.opendaylight.protocol.pcep.spi.UnknownObject;
import org.opendaylight.protocol.pcep.spi.VendorInformationObjectRegistry;
import org.opendaylight.protocol.util.Values;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iana.rev130816.EnterpriseNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.vendor.information.objects.VendorInformationObject;
import org.opendaylight.yangtools.yang.binding.DataContainer;

public final class SimpleObjectRegistry implements ObjectRegistry {
    private final HandlerRegistry<DataContainer, ObjectParser, ObjectSerializer> handlers = new HandlerRegistry<>();

    private static final int MAX_OBJECT_TYPE = 15;
    private static final int MAX_OBJECT_CLASS = 4;

    private final VendorInformationObjectRegistry viRegistry;

    public SimpleObjectRegistry(final VendorInformationObjectRegistry viRegistry) {
        this.viRegistry = viRegistry;
    }

    private static int createKey(final int objectClass, final int objectType) {
        Preconditions.checkArgument(objectClass >= 0 && objectClass <= Values.UNSIGNED_BYTE_MAX_VALUE);
        Preconditions.checkArgument(objectType >= 0 && objectType <= MAX_OBJECT_TYPE);
        return objectClass << MAX_OBJECT_CLASS | objectType;
    }

    public AutoCloseable registerObjectParser(final int objectClass, final int objectType, final ObjectParser parser) {
        checkArgument(objectClass >= 0 && objectClass <= Values.UNSIGNED_BYTE_MAX_VALUE, "Illegal object class %s",
                objectClass);
        checkArgument(objectType >= 0 && objectType <= MAX_OBJECT_TYPE, "Illegal object type %s", objectType);
        return this.handlers.registerParser(createKey(objectClass, objectType), parser);
    }

    public AutoCloseable registerObjectSerializer(final Class<? extends Object> objClass,
            final ObjectSerializer serializer) {
        return this.handlers.registerSerializer(objClass, serializer);
    }

    @Override
    public Object parseObject(final int objectClass, final int objectType, final ObjectHeader header,
            final ByteBuf buffer) throws PCEPDeserializerException {
        checkArgument(objectType >= 0 && objectType <= Values.UNSIGNED_SHORT_MAX_VALUE);
        final ObjectParser parser = this.handlers.getParser(createKey(objectClass, objectType));

        if (parser == null) {
            if (!header.isProcessingRule()) {
                return null;
            }
            for (int type = 1; type <= MAX_OBJECT_TYPE; type++) {
                final ObjectParser objParser = this.handlers.getParser(createKey(objectClass, type));
                if (objParser != null) {
                    return new UnknownObject(PCEPErrors.UNRECOGNIZED_OBJ_TYPE);
                }
            }
            return new UnknownObject(PCEPErrors.UNRECOGNIZED_OBJ_CLASS);
        }
        return parser.parseObject(header, buffer);
    }

    @Override
    public void serializeObject(final Object object, final ByteBuf buffer) {
        final ObjectSerializer serializer = this.handlers.getSerializer(object.getImplementedInterface());
        if (serializer == null) {
            return;
        }
        serializer.serializeObject(object, buffer);
    }

    @Override
    public Optional<? extends Object> parseVendorInformationObject(final EnterpriseNumber enterpriseNumber,
            final ObjectHeader header, final ByteBuf buffer) throws PCEPDeserializerException {
        return this.viRegistry.parseVendorInformationObject(enterpriseNumber, header, buffer);
    }

    @Override
    public void serializeVendorInformationObject(final VendorInformationObject viObject, final ByteBuf buffer) {
        this.viRegistry.serializeVendorInformationObject(viObject, buffer);
    }
}
