/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.yangtools.yang.binding.Notification;

public interface MessageRegistry {

    Notification parseMultiPathMessage(ByteBuf bytes, MultiPathSupport multiPathSupport) throws BGPDocumentedException, BGPParsingException;

    Notification parseMessage(ByteBuf bytes) throws BGPDocumentedException, BGPParsingException;

    void serializeMessage(Notification message, ByteBuf buffer);
}
