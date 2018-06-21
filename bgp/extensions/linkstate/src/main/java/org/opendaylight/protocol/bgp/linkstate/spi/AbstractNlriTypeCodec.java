/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.linkstate.spi;

import io.netty.buffer.ByteBuf;
import java.math.BigInteger;
import org.opendaylight.protocol.bgp.linkstate.spi.NlriTypeCaseParser;
import org.opendaylight.protocol.bgp.linkstate.spi.NlriTypeCaseSerializer;
import org.opendaylight.protocol.util.ByteBufWriteUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.Identifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.ProtocolId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.ObjectType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.destination.CLinkstateDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.destination.CLinkstateDestinationBuilder;

public abstract class AbstractNlriTypeCodec implements NlriTypeCaseParser, NlriTypeCaseSerializer {
    @Override
    public final CLinkstateDestination parseTypeNlri(final ByteBuf nlri) {
        final CLinkstateDestinationBuilder builder = new CLinkstateDestinationBuilder();
        builder.setProtocolId(ProtocolId.forValue(nlri.readUnsignedByte()));
        builder.setIdentifier(new Identifier(BigInteger.valueOf(nlri.readLong())));
        builder.setObjectType(parseObjectType(nlri));
        return builder.build();
    }

    @Override
    public final void serializeTypeNlri(final CLinkstateDestination nlriType, final ByteBuf byteAggregator) {
        ByteBufWriteUtil.writeUnsignedByte((short) nlriType.getProtocolId().getIntValue(), byteAggregator);
        ByteBufWriteUtil.writeUnsignedLong(nlriType.getIdentifier().getValue(), byteAggregator);
        serializeObjectType(nlriType.getObjectType(), byteAggregator);
    }

    protected abstract ObjectType parseObjectType(ByteBuf buffer);

    protected abstract void serializeObjectType(ObjectType objectType, ByteBuf buffer);
}
