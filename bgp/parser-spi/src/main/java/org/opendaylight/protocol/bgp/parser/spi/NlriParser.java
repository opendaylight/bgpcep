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
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.update.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.update.attributes.MpUnreachNlriBuilder;

/**
 * Common interface for NLRI parser implementation.
 */
public interface NlriParser {

    /**
     * Parse MP UN_REACH NLRI from buffer.
     *
     * @param nlri Encoded unreachabel NLRI in ByteBuf.
     * @param builder MP UNREACH NLRI builder.
     * @throws BGPParsingException
     */
    void parseNlri(@Nonnull ByteBuf nlri, @Nonnull MpUnreachNlriBuilder builder) throws BGPParsingException;

    /**
     * Parse MP REACH NLRI from buffer.
     *
     * @param nlri Encoded reachable NLRI in ByteBuf.
     * @param builder MP REACH NLRI builder.
     * @throws BGPParsingException
     */
    void parseNlri(@Nonnull ByteBuf nlri, @Nonnull MpReachNlriBuilder builder) throws BGPParsingException;

    /**
     * Invokes {@link #parseNlri(ByteBuf, MpReachNlriBuilder)}, so the constraint is omitted. Override for specific parser behavior.
     *
     * @param nlri Encoded reachable NLRI in ByteBuf.
     * @param builder MP REACH NLRI builder.
     * @param constraint Peer specific constraints.
     * @throws BGPParsingException
     */
    default void parseNlri(@Nonnull final ByteBuf nlri, @Nonnull final MpReachNlriBuilder builder, @Nullable final PeerSpecificParserConstraint constraint)
            throws BGPParsingException {
        parseNlri(nlri, builder);
    }

    /**
     * Invokes {@link #parseNlri(ByteBuf, MpUnreachNlriBuilder)}, so the constraint is omitted. Override for specific parser behavior.
     *
     * @param nlri Encoded unreachable NLRI in ByteBuf.
     * @param builder MP UNREACH NLRI builder.
     * @param constraint Peer specific constraints.
     * @throws BGPParsingException
     */
    default void parseNlri(@Nonnull final ByteBuf nlri, @Nonnull final MpUnreachNlriBuilder builder, @Nullable final PeerSpecificParserConstraint constraint)
            throws BGPParsingException {
        parseNlri(nlri, builder);
    }
}
