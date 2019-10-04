/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

/**
 * PCEPMessageHeaderDecode. Decodes PCEP messages headers.
 *
 * @see <a href="http://tools.ietf.org/html/rfc5440#section-6.1">Common Message Header</a>
 */
public class PCEPMessageHeaderDecoder extends LengthFieldBasedFrameDecoder {

    // min 4, max 65535
    private static final int MAX_FRAME_SIZE = 65535;

    private static final int VERSION_FLAGS_SIZE = 1;

    // the length field represents the length of the whole message including the header
    private static final int LENGTH_SIZE = 2;

    private static final int MESSAGE_TYPE_SIZE = 1;

    /*

    0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    | Ver |  Flags  |  Message-Type |       Message-Length          |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

     */

    public PCEPMessageHeaderDecoder() {
        super(MAX_FRAME_SIZE, VERSION_FLAGS_SIZE + MESSAGE_TYPE_SIZE, LENGTH_SIZE,
            -LENGTH_SIZE - MESSAGE_TYPE_SIZE - VERSION_FLAGS_SIZE, 0);
    }
}
