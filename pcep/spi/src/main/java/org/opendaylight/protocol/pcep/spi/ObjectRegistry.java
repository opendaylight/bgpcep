/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.spi;

import io.netty.buffer.ByteBuf;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.ObjectHeader;

public interface ObjectRegistry extends VendorInformationObjectRegistry {
    /**
     * Finds parser for given object type and class in the registry. Delegates parsing to found parser.
     *
     * @param objectClass object class
     * @param objectType object type
     * @param header ObjectHeader
     * @param buffer object wrapped in ByteBuf
     * @return null if the parser for this object could not be found
     * @throws PCEPDeserializerException if the parsing did not succeed
     */
    Object parseObject(int objectClass, int objectType, ObjectHeader header, ByteBuf buffer)
            throws PCEPDeserializerException;

    /**
     * Find serializer for given object. Delegates parsing to found serializer.
     *
     * @param object to be parsed
     * @param buffer ByteBuf wrapped around bytes representing given object
     */
    void serializeObject(Object object, ByteBuf buffer);
}
