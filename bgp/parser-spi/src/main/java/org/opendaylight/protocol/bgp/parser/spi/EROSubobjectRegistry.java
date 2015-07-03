/*
 *
 *  * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */
package org.opendaylight.protocol.bgp.parser.spi;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.explicit.route.subobjects.list.SubobjectContainer;

public interface EROSubobjectRegistry {
    /**
     * Finds parser for given subobject type in the registry. Delegates parsing to found parser.
     *
     * @param subobjectType subobject type, key in parser registry
     * @param buffer subobject wrapped in ByteBuf
     * @param loose ERO specific common field
     * @return null if the parser for this subobject could not be found
     * @throws BGPParsingException if the parsing did not succeed
     */
    SubobjectContainer parseSubobject(final int subobjectType, final ByteBuf buffer, final boolean loose) throws BGPParsingException;

    /**
     * Find serializer for given subobject. Delegates parsing to found serializer.
     *
     * @param subobject to be parsed
     * @param buffer buffer where the serialized subobject will be parsed
     */
    void serializeSubobject(final SubobjectContainer subobject, final ByteBuf buffer);
}
