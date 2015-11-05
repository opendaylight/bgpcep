/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.parser.spi.extended.community;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.ExtendedCommunity;

/**
 * The Extended Community value parser (ByteBuf -> ExtendedCommunity)
 *
 */
public interface ExtendedCommunityParser {

    /**
     * Parses the Extended Community value encoded in the buffer.
     * @param buffer ByteBuf
     * @return Decoded Extended Community value.
     * @throws BGPDocumentedException
     * @throws BGPParsingException
     */
    ExtendedCommunity parseExtendedCommunity(ByteBuf buffer) throws BGPDocumentedException, BGPParsingException;

}
