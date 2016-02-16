/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

/**
 * @see <a href="http://tools.ietf.org/html/rfc4271#section-4.1">BGP Message Header</a>
 */
public final class BGPMessageHeaderDecoder extends LengthFieldBasedFrameDecoder {

    private static final int MARKER_SIZE = 16;

    /*
     * the length field represents the length of the whole message including the header
     */
    private static final int LENGTH_SIZE = 2;

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

    public static final BGPMessageHeaderDecoder getBGPMessageHeaderDecoder(final int maxFrameSize){
        return new BGPMessageHeaderDecoder(maxFrameSize);
    }

}
