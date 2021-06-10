/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi;

import io.netty.buffer.ByteBuf;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.reach.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.reach.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.unreach.MpUnreachNlriBuilder;

/**
 * Common interface for NLRI parser implementation.
 */
@NonNullByDefault
public interface NlriParser {
    /**
     * Parse MP UN_REACH NLRI from buffer.
     *
     * @param nlri Encoded reachable NLRI in ByteBuf.
     * @param builder MP REACH NLRI builder.
     * @param constraint Peer specific constraints.
     * @throws BGPParsingException exception.
     */
    void parseNlri(ByteBuf nlri, MpReachNlriBuilder builder, @Nullable PeerSpecificParserConstraint constraint)
            throws BGPParsingException;

    /**
     * Parse MP UN_REACH NLRI from buffer.
     *
     * @param nlri Encoded unreachable NLRI in ByteBuf.
     * @param builder MP UNREACH NLRI builder.
     * @param constraint Peer specific constraints.
     * @throws BGPParsingException exception.
     */
    void parseNlri(ByteBuf nlri, MpUnreachNlriBuilder builder, @Nullable PeerSpecificParserConstraint constraint)
            throws BGPParsingException;

    /**
     * Convert MP_REACH attribute and merge it to existing MpUnreachNlriBuilder.
     *
     * @param mpReachNlri MP_REACH attribute to be converted
     * @param builder to which converted routing information should be added
     * @return True if the conversion was successful, false otherwise
     */
    default boolean convertMpReachToMpUnReach(final MpReachNlri mpReachNlri, final MpUnreachNlriBuilder builder) {
        return false;
    }
}
