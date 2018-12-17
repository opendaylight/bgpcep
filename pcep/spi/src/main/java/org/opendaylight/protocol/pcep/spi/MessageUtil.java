/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.spi;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;

public final class MessageUtil {

    private static final int VERSION_SF_LENGTH = 3;

    private MessageUtil() {
    }

    public static void formatMessage(final int messageType, final ByteBuf body, final ByteBuf out) {
        final int msgLength = body.writerIndex();
        out.writeByte(PCEPMessageConstants.PCEP_VERSION << Byte.SIZE - VERSION_SF_LENGTH);
        out.writeByte(messageType);
        out.writeShort(msgLength + PCEPMessageConstants.COMMON_HEADER_LENGTH);
        Preconditions.checkState(out.writerIndex() == PCEPMessageConstants.COMMON_HEADER_LENGTH);
        out.writeBytes(body);
    }
}
