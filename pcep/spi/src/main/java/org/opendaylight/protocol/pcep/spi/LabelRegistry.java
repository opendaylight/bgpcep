/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.spi;

import io.netty.buffer.ByteBuf;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.label.subobject.LabelType;

public interface LabelRegistry {
    /**
     * Finds parser for given label C-type in the registry. Delegates parsing to found parser.
     *
     * @param cType label type, key in parser registry
     * @param buffer label wrapped in ByteBuf
     * @return null if the parser for this label could not be found
     * @throws PCEPDeserializerException if the parsing did not succeed
     */
    LabelType parseLabel(final int cType, final ByteBuf buffer) throws PCEPDeserializerException;

    /**
     * Find serializer for given label. Delegates parsing to found serializer.
     *
     * @param unidirectional label common header flag
     * @param global label commom header flag
     * @param label to be parsed
     * @param buffer buffer where the serialized label will be parsed
     */
    void serializeLabel(final boolean unidirectional, final boolean global, final LabelType label, final ByteBuf buffer);
}
