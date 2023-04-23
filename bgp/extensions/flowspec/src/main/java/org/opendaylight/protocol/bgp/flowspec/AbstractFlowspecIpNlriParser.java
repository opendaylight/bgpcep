/*
 * Copyright (c) 2023 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.List;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.Flowspec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.PathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.destination.DestinationType;

abstract class AbstractFlowspecIpNlriParser extends AbstractFlowspecNlriParser {
    AbstractFlowspecIpNlriParser(final FlowspecTypeRegistry flowspecTypeRegistry) {
        super(flowspecTypeRegistry);
    }

    @Override
    protected final DestinationType parseAdvertizedNlri(final ByteBuf nlri, final PathId pathId)
            throws BGPParsingException {
        return createAdvertizedRoutesDestinationType(parseNlriFlowspecList(nlri), pathId);
    }

    abstract @NonNull DestinationType createAdvertizedRoutesDestinationType(List<Flowspec> flowspecList,
        @Nullable PathId pathId);

    @Override
    protected final DestinationType parseWithdrawnNlri(final ByteBuf nlri, final PathId pathId)
            throws BGPParsingException {
        return createWithdrawnDestinationType(parseNlriFlowspecList(nlri), pathId);
    }

    abstract @NonNull DestinationType createWithdrawnDestinationType(List<Flowspec> flowspecList,
        @Nullable PathId pathId);

    protected final void serializeNlri(final List<Flowspec> flowspecList, final @Nullable PathId pathId,
            final @NonNull ByteBuf buffer) {
        final var nlri = Unpooled.buffer();
        serializeNlri(flowspecList, nlri);
        appendNlri(pathId, nlri, buffer);
    }
}
