/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.spi;

import io.netty.buffer.ByteBuf;
import java.util.List;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.Message;

public interface MessageRegistry {
    /**
     * Finds parser for given message type in the registry. Delegates parsing to found parser.
     *
     * @param messageType message type, key in parser registry
     * @param buffer message wrapped in ByteBuf
     * @param errors list of error messages, that is filled during parsing
     * @return null if the parser for this message could not be found
     * @throws PCEPDeserializerException if the parsing did not succeed
     */
    Message parseMessage(int messageType, ByteBuf buffer, List<Message> errors) throws PCEPDeserializerException;

    /**
     * Find serializer for given message. Delegates parsing to found serializer.
     *
     * @param message to be parsed
     * @param buffer byte buffer that will be filled with serialized message
     */
    void serializeMessage(Message message, ByteBuf buffer);
}
