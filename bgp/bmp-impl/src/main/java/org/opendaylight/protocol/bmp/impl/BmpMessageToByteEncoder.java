/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bmp.impl;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.opendaylight.yangtools.yang.binding.Notification;

public class BmpMessageToByteEncoder extends MessageToByteEncoder<Notification> {

    @Override
    protected void encode(final ChannelHandlerContext ctx, final Notification msg, final ByteBuf out) throws Exception {
        //TODO implementation
    }

}
