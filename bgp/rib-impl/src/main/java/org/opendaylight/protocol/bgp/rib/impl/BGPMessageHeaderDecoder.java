/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import io.netty.channel.FixedRecvByteBufAllocator;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

/**
 * @see <a href="http://tools.ietf.org/html/rfc4271#section-4.1">BGP Message Header</a>
 */
final class BGPMessageHeaderDecoder extends LengthFieldBasedFrameDecoder {

    private static final int MARKER_SIZE = 16;

    /*
     * the length field represents the length of the whole message including the header
     */
    private static final int LENGTH_SIZE = 2;

    private static final int MAX_FRAME_SIZE = 4096;

    private static final int EXTENDED_MAX_FRAME_SIZE = 65535;

    // Allocators
    private static final FixedRecvByteBufAllocator RECV_ALLOCATOR = new FixedRecvByteBufAllocator(MAX_FRAME_SIZE);
    private static final FixedRecvByteBufAllocator EXTENDED_RECV_ALLOCATOR =
            new FixedRecvByteBufAllocator(EXTENDED_MAX_FRAME_SIZE);

    /*

     0                   1                   2                   3
      0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
      |                                                               |
      |                                                               |
      |                                                               |
      |                                                               |
      |                           Marker                              |
      |                                                               |
      |                                                               |
      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
      |          Length               |      Type     |
      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

     */


    private BGPMessageHeaderDecoder(final int maxFrameSize) {
        super(maxFrameSize, MARKER_SIZE, LENGTH_SIZE, -MARKER_SIZE - LENGTH_SIZE, 0);
    }

    static FixedRecvByteBufAllocator getRecvAllocator() {
        return RECV_ALLOCATOR;
    }

    static FixedRecvByteBufAllocator getExtendedRecvAllocator() {
        return EXTENDED_RECV_ALLOCATOR;
    }

    static BGPMessageHeaderDecoder getBGPMessageHeaderDecoder() {
        return new BGPMessageHeaderDecoder(MAX_FRAME_SIZE);
    }

    static BGPMessageHeaderDecoder getExtendedBGPMessageHeaderDecoder() {
        return new BGPMessageHeaderDecoder(EXTENDED_MAX_FRAME_SIZE);
    }
}
