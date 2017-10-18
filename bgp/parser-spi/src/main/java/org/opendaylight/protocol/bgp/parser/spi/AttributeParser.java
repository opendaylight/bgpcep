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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.path.attributes.AttributesBuilder;

/**
 * Common interface for attribute parser implementation.
 */
public interface AttributeParser {
    /**
     * Parses attribute from ByteBuf buffer.
     *
     * @param buffer Encoded attribute body in ByteBuf.
     * @param builder Path attributes builder. Guaranteed to contain all valid attributes whose type is numerically
     *        lower than this attribute's type.
     */
    void parseAttribute(@Nonnull ByteBuf buffer, @Nonnull AttributesBuilder builder) throws BGPDocumentedException, BGPParsingException;

    /**
     * Invokes {@link #parseAttribute(ByteBuf, AttributesBuilder)}, so the constraint is omitted. Override for specific parser behavior.
     *
     * @param buffer Encoded attribute body in ByteBuf.
     * @param builder Path attributes builder. Guaranteed to contain all valid attributes whose type is numerically
     *        lower than this attribute's type.
     * @param constraint Peer specific constraints.
     * @throws BGPDocumentedException
     * @throws BGPParsingException
     */
    default void parseAttribute(@Nonnull final ByteBuf buffer, @Nonnull final AttributesBuilder builder, @Nullable final PeerSpecificParserConstraint constraint)
            throws BGPDocumentedException, BGPParsingException {
        parseAttribute(buffer, builder);
    }
}
