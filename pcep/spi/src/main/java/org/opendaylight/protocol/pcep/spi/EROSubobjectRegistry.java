/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.spi;

import io.netty.buffer.ByteBuf;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.explicit.route.object.ero.Subobject;

/**
 * Explicit Route Object Registry.
 */
public interface EROSubobjectRegistry {
    /**
     * Finds parser for given subobject type in the registry. Delegates parsing to found parser.
     *
     * @param subobjectType subobject type, key in parser registry
     * @param buffer        subobject wrapped in ByteBuf
     * @param loose         ERO specific common field
     * @return null if the parser for this subobject could not be found
     * @throws PCEPDeserializerException if the parsing did not succeed
     */
    Subobject parseSubobject(int subobjectType, ByteBuf buffer, boolean loose) throws PCEPDeserializerException;

    /**
     * Find serializer for given subobject. Delegates parsing to found serializer.
     *
     * @param subobject to be parsed
     * @param buffer    buffer where the serialized subobject will be parsed
     */
    void serializeSubobject(Subobject subobject, ByteBuf buffer);
}
