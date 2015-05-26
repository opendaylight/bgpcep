/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bmp.impl;

import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

public class BmpMessageHeaderDecoder extends LengthFieldBasedFrameDecoder {

    private static final int MAX_FRAME_SIZE = 65535;

    private static final int VERSION_SIZE = 1;

    private static final int LENGTH_SIZE = 4;

    /*

    0 1 2 3 4 5 6 7 8 1 2 3 4 5 6 7 8 1 2 3 4 5 6 7 8 1 2 3 4 5 6 7 8
    +-+-+-+-+-+-+-+-+
    |    Version    |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |                        Message Length                         |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |   Msg. Type   |
    +---------------+

    */

    public BmpMessageHeaderDecoder() {
        super(MAX_FRAME_SIZE, VERSION_SIZE, LENGTH_SIZE, -VERSION_SIZE - LENGTH_SIZE, 0);
    }
}
