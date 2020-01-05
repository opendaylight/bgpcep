/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.spi;

import io.netty.buffer.ByteBuf;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev180329.Identifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev180329.ProtocolId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev180329.linkstate.ObjectType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev180329.linkstate.destination.CLinkstateDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev180329.linkstate.destination.CLinkstateDestinationBuilder;
import org.opendaylight.yangtools.yang.common.netty.ByteBufUtils;

public abstract class AbstractNlriTypeCodec implements NlriTypeCaseParser, NlriTypeCaseSerializer {
    @Override
    public final CLinkstateDestination parseTypeNlri(final ByteBuf nlri) {
        return new CLinkstateDestinationBuilder()
                .setProtocolId(ProtocolId.forValue(nlri.readUnsignedByte()))
                .setIdentifier(new Identifier(ByteBufUtils.readUint64(nlri)))
                .setObjectType(parseObjectType(nlri))
                .build();
    }

    @Override
    public final void serializeTypeNlri(final CLinkstateDestination nlriType, final ByteBuf byteAggregator) {
        byteAggregator.writeByte(nlriType.getProtocolId().getIntValue());
        ByteBufUtils.write(byteAggregator, nlriType.getIdentifier().getValue());
        serializeObjectType(nlriType.getObjectType(), byteAggregator);
    }

    protected abstract ObjectType parseObjectType(ByteBuf buffer);

    protected abstract void serializeObjectType(ObjectType objectType, ByteBuf buffer);
}
