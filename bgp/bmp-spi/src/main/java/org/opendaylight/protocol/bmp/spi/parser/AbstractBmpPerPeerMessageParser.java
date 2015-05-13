/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bmp.spi.parser;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.parser.spi.MessageRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.PeerHeader;

public abstract class AbstractBmpPerPeerMessageParser extends AbstractBmpMessageParser {

    private final MessageRegistry bgpMssageRegistry;

    public AbstractBmpPerPeerMessageParser(final MessageRegistry bgpMssageRegistry) {
        this.bgpMssageRegistry = bgpMssageRegistry;
    }

    protected final PeerHeader parsePerPeerHeader(final ByteBuf bytes) {
        //TODO
        return null;
    }

    protected final void serializePerPeerHeader(final PeerHeader peerHeader, final ByteBuf buffer) {
        //TODO
    }

    protected final MessageRegistry getBmpMessageRegistry() {
        return this.bgpMssageRegistry;
    }
}
