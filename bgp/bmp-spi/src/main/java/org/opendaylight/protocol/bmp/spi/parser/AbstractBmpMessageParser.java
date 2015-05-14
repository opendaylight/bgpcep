/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bmp.spi.parser;

import com.google.common.base.Preconditions;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;

import org.opendaylight.yangtools.yang.binding.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractBmpMessageParser implements BmpMessageParser, BmpMessageSerializer {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractBmpMessageParser.class);

    @Override
    public void serializeMessage(final Notification message, final ByteBuf buffer) {
        // TODO Auto-generated method stub
    }

    @Override
    public Notification parseMessage(final ByteBuf bytes) {

        return null;
    }

    public void checkByteBufMotNull(final ByteBuf bytes)
    {
        Preconditions.checkArgument(bytes != null && bytes.readableBytes() != 0, "Byte buffer cannot be null.");
        LOG.trace("Started parsing of notification (Initialization) message: {}", ByteBufUtil.hexDump(bytes));
    }

}
