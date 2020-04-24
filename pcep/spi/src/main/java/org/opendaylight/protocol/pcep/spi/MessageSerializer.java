/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.spi;

import io.netty.buffer.ByteBuf;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.Message;

public interface MessageSerializer {
    /**
     * Serializes given message to bytes wrapped in given ByteBuf.
     * @param message PCEP message to be serialized
     * @param buffer ByteBuf wrapper around serialized message
     */
    void serializeMessage(Message message, ByteBuf buffer);
}
