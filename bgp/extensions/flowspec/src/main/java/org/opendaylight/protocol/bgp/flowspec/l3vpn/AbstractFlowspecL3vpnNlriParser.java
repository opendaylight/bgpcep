/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec.l3vpn;

import static java.util.Objects.requireNonNull;
import static org.opendaylight.bgp.concepts.RouteDistinguisherUtil.extractRouteDistinguisher;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.bgp.concepts.RouteDistinguisherUtil;
import org.opendaylight.protocol.bgp.flowspec.AbstractFlowspecNlriParser;
import org.opendaylight.protocol.bgp.flowspec.FlowspecTypeRegistry;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.Flowspec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.FlowspecBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.PathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.destination.DestinationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.RouteDistinguisher;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractFlowspecL3vpnNlriParser extends AbstractFlowspecNlriParser {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractFlowspecL3vpnNlriParser.class);
    public static final NodeIdentifier RD_NID =
        new NodeIdentifier(QName.create(Flowspec.QNAME, "route-distinguisher").intern());

    protected AbstractFlowspecL3vpnNlriParser(final FlowspecTypeRegistry flowspecTypeRegistry) {
        super(flowspecTypeRegistry);
    }

    @Override
    public final String stringNlri(final DataContainerNode flowspec) {
        final StringBuilder buffer = new StringBuilder();
        final RouteDistinguisher rd = extractRouteDistinguisher(flowspec, RD_NID);
        if (rd != null) {
            buffer.append("[l3vpn with route-distinguisher ").append(rd.stringValue()).append("] ");
        }
        buffer.append(super.stringNlri(flowspec));
        return buffer.toString();
    }

    /**
     * For flowspec-l3vpn, there is a route distinguisher field at the beginning of NLRI (8 bytes).
     */
    private static RouteDistinguisher readRouteDistinguisher(final ByteBuf nlri) {
        final RouteDistinguisher rd = RouteDistinguisherUtil.parseRouteDistinguisher(nlri);
        LOG.trace("Route Distinguisher read from NLRI: {}", rd);
        return rd;
    }

    @Override
    protected final void serializeNlri(final Object[] nlriFields, final ByteBuf buffer) {
        final RouteDistinguisher rd = requireNonNull((RouteDistinguisher) nlriFields[0]);
        RouteDistinguisherUtil.serializeRouteDistinquisher(rd, buffer);
        final List<Flowspec> flowspecList = (List<Flowspec>) nlriFields[1];
        serializeNlri(flowspecList, buffer);
    }

    @Override
    protected final DestinationType parseAdvertizedNlri(final ByteBuf nlri, final PathId pathId)
            throws BGPParsingException {
        readNlriLength(nlri);
        return createAdvertizedRoutesDestinationType(requireNonNull(readRouteDistinguisher(nlri)),
            parseL3vpnNlriFlowspecList(nlri), pathId);
    }

    /**
     * Create advertized destination type.
     *
     * @param rd           the RouteDistinguisher
     * @param flowspecList a list of {@link Flowspec}s
     * @param pathId       associated path id with given destination
     * @return created destination type
     */
    protected abstract @NonNull DestinationType createAdvertizedRoutesDestinationType(RouteDistinguisher rd,
        @Nullable List<Flowspec> flowspecList, @Nullable PathId pathId);

    @Override
    protected final DestinationType parseWithdrawnNlri(final ByteBuf nlri, final PathId pathId)
            throws BGPParsingException {
        readNlriLength(nlri);
        return createWithdrawnDestinationType(requireNonNull(readRouteDistinguisher(nlri)),
            parseL3vpnNlriFlowspecList(nlri), pathId);
    }

    /**
     * Create withdrawn destination type.
     *
     * @param rd           the RouteDistinguisher
     * @param flowspecList a list of {@link Flowspec}s
     * @param pathId       associated path id with given destination
     * @return created destination type
     */
    protected abstract @NonNull DestinationType createWithdrawnDestinationType(RouteDistinguisher rd,
        @Nullable List<Flowspec> flowspecList, @Nullable PathId pathId);

    private @Nullable List<Flowspec> parseL3vpnNlriFlowspecList(final ByteBuf nlri) {
        if (!nlri.isReadable()) {
            return null;
        }

        final var fss = new ArrayList<Flowspec>();
        while (nlri.isReadable()) {
            fss.add(new FlowspecBuilder().setFlowspecType(flowspecTypeRegistry.parseFlowspecType(nlri)).build());
        }
        return fss;
    }
}

