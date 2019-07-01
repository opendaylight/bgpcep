/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi;

import io.netty.buffer.ByteBuf;
import java.util.Optional;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.update.attributes.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.update.attributes.MpUnreachNlri;

/**
 * The codec registry for BGP NLRI, offers services for NLRI encoding/decoding.
 */
@NonNullByDefault
public interface NlriRegistry {
    /**
     * Decode MP REACH NLRI Attribute.
     *
     * @param buffer Input buffer.
     * @param constraint Peer specific constraint.
     * @return Parsed reach NLRI.
     */
    MpReachNlri parseMpReach(ByteBuf buffer, @Nullable PeerSpecificParserConstraint constraint)
            throws BGPParsingException;

    /**
     * Decode MP REACH NLRI Attribute.
     *
     * @param buffer Input buffer.
     * @param constraint Peer specific constraint.
     * @return Parsed unreach NLRI.
     */
    MpUnreachNlri parseMpUnreach(ByteBuf buffer, @Nullable PeerSpecificParserConstraint constraint)
            throws BGPParsingException;

    /**
     * Encode BGP MP REACH NLRI Attribute.
     *
     * @param mpReachNlri Input reach NLRI.
     * @param byteAggregator Output buffer.
     */
    void serializeMpReach(MpReachNlri mpReachNlri, ByteBuf byteAggregator);

    /**
     * Encode BGP MP UNREACH NLRI Attribute.
     *
     * @param mpUnreachNlri Input unreach NLRI.
     * @param byteAggregator Output buffer.
     */
    void serializeMpUnReach(MpUnreachNlri mpUnreachNlri, ByteBuf byteAggregator);

    /**
     * Get all available NLRI encoders.
     *
     * @return Iterable of NLRI serializers.
     */
    Iterable<NlriSerializer> getSerializers();

    /**
     * Convert MP_REACH attribute to MP_UNREACH attribute and merge it with original one if it exists.
     *
     * <p>
     * The default implementation rejects the conversion.
     *
     * @param mpReachNlri MP_REACH attribute to be converted
     * @param mpUnreachNlri original MP_UNREACH attribute
     * @return resulting MP_UNREACH attribute after conversion
     */
    Optional<MpUnreachNlri> convertMpReachToMpUnReach(MpReachNlri mpReachNlri, @Nullable MpUnreachNlri mpUnreachNlri);
}
