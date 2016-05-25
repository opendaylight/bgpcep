/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.parser.impl.message.update.extended.communities;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.extended.community.ExtendedCommunityParser;
import org.opendaylight.protocol.bgp.parser.spi.extended.community.ExtendedCommunitySerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.EncapsulationTunnelType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.ExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.EncapsulationCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.EncapsulationCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.encapsulation._case.EncapsulationExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.encapsulation._case.EncapsulationExtendedCommunityBuilder;

/**
 * Parser for BGP Encapsulation extended community attribute.
 *
 * @see <a href="https://tools.ietf.org/html/rfc5512#section-4.5"</a>
 */

public class EncapsulationEC implements ExtendedCommunityParser, ExtendedCommunitySerializer {
    @VisibleForTesting
    public static final int TYPE = 3;
    @VisibleForTesting
    public static final int SUBTYPE = 12;
    private static final int RESERVED_SIZE = 4;

    @Override
    public ExtendedCommunity parseExtendedCommunity(final ByteBuf buffer) throws BGPDocumentedException, BGPParsingException {
        buffer.skipBytes(RESERVED_SIZE);
        final EncapsulationExtendedCommunity encap = new EncapsulationExtendedCommunityBuilder()
            .setTunnelType(EncapsulationTunnelType.forValue(buffer.readUnsignedShort()))
            .build();
        return new EncapsulationCaseBuilder().setEncapsulationExtendedCommunity(encap).build();
    }

    @Override
    public void serializeExtendedCommunity(final ExtendedCommunity extendedCommunity, final ByteBuf body) {
        Preconditions.checkArgument(extendedCommunity instanceof EncapsulationCase,
            "The extended community %s is not EncapsulationCase type.", extendedCommunity);
        final EncapsulationExtendedCommunity encap = ((EncapsulationCase) extendedCommunity).getEncapsulationExtendedCommunity();
        body.writeZero(RESERVED_SIZE);
        body.writeShort(encap.getTunnelType().getIntValue());
    }

    @Override
    public int getType(final boolean isTransitive) {
        return TYPE;
    }

    @Override
    public int getSubType() {
        return SUBTYPE;
    }
}
