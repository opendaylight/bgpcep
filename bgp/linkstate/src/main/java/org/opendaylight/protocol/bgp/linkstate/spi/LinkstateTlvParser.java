/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.spi;

import io.netty.buffer.ByteBuf;
import org.opendaylight.yangtools.yang.common.QName;

public interface LinkstateTlvParser<T> {

    T parseTlvBody(ByteBuf value);

    QName getTlvQName();

    interface LinkstateTlvSerializer<T> {

        void serializeTlvBody(final T tlv, final ByteBuf body);

        int getType();

    }
}
