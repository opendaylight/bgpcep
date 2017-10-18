/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi;

import io.netty.buffer.ByteBuf;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.Attributes;
import org.opendaylight.yangtools.yang.binding.DataObject;

/**
 *
 */
public interface AttributeRegistry {
    /**
     * Parse BGP Attribute from buffer.
     * @param buffer Input buffer.
     * @param constraints Peer specific constraint.
     * @return Decoded BGP Attribute.
     * @throws BGPDocumentedException
     * @throws BGPParsingException
     */
    @Nonnull Attributes parseAttributes(@Nonnull ByteBuf buffer, @Nullable PeerSpecificParserConstraint constraints)
            throws BGPDocumentedException, BGPParsingException;

    /**
     * Serialize BGP Attribute to buffer.
     * @param attribute Input BGP Attribute.
     * @param byteAggregator Output buffer.
     */
    void serializeAttribute(@Nonnull DataObject attribute, @Nonnull ByteBuf byteAggregator);
}
