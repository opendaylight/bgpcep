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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.ExtendedCommunities;

/**
 * The Extended Communities registry.
 *
 */
public interface ExtendedCommunityRegistry {

    /**
     * Parses the Extended Community.
     * @param buffer ByteBuf
     * @return Decoded ExtendedCommunity or null if the parser for such Extended Community type/sub-type is not present.
     * @throws BGPDocumentedException
     * @throws BGPParsingException
     */
    ExtendedCommunities parseExtendedCommunity(ByteBuf buffer) throws BGPDocumentedException, BGPParsingException;

    /**
     * Serializes the Extended Community.
     * @param extendedCommunity ExtendedCommunity to be encoded.
     * @param byteAggregator ByteBuf, where the Extended Community is serialized,
     *  if a serialized is not present for such Extended Community type, no bytes are written into output buffer.
     */
    void serializeExtendedCommunity(ExtendedCommunities extendedCommunity, ByteBuf byteAggregator);

}
