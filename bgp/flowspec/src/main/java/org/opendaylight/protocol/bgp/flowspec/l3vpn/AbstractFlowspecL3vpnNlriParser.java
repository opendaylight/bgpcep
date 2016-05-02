/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec.l3vpn;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.bgp.concepts.RouteDistinguisherUtil;
import org.opendaylight.protocol.bgp.flowspec.AbstractFlowspecNlriParser;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.PathIdUtil;
import org.opendaylight.protocol.bgp.parser.spi.PeerSpecificParserConstraint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.Flowspec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.PathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.destination.DestinationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.unreach.nlri.WithdrawnRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.RouteDistinguisher;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Kevin Wang
 */
public abstract class AbstractFlowspecL3vpnNlriParser extends AbstractFlowspecNlriParser {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractFlowspecL3vpnNlriParser.class);

    private static final NodeIdentifier RD_NID = new NodeIdentifier(QName.create(Flowspec.QNAME.getNamespace(), Flowspec.QNAME.getRevision(), "route-distinguisher"));

    protected abstract DestinationType createWithdrawnDestinationType(final List<Flowspec> dst, @Nullable final RouteDistinguisher rd, @Nullable final PathId pathId);

    protected abstract DestinationType createAdvertizedRoutesDestinationType(final List<Flowspec> dst, @Nullable final RouteDistinguisher rd, @Nullable final PathId pathId);

    // the following two methods are not used in this implementation
    @Override
    protected DestinationType createWithdrawnDestinationType(final List<Flowspec> dst, @Nullable final PathId pathId) {
        throw new UnsupportedOperationException("RouteDistinguisher must be provided for flowspec l3vpn");
    }

    @Override
    protected DestinationType createAdvertizedRoutesDestinationType(final List<Flowspec> dst, @Nullable final PathId pathId) {
        throw new UnsupportedOperationException("RouteDistinguisher must be provided for flowspec l3vpn");
    }

    @Override
    public String stringNlri(final DataContainerNode<?> flowspec) {
        final StringBuilder buffer = new StringBuilder();
        RouteDistinguisher rd = extractRouteDistinguisher(flowspec);
        if (rd != null) {
            buffer.append("[l3vpn with route-distinguisher ").append(rd.getValue()).append("] ");
        }
        buffer.append(stringNlri(extractFlowspec(flowspec)));
        return buffer.toString();
    }

    public final RouteDistinguisher extractRouteDistinguisher(final DataContainerNode<?> route) {
        RouteDistinguisher rd = null;
        final Optional<DataContainerChild<? extends PathArgument, ?>> rdNode = route.getChild(RD_NID);
        if (rdNode.isPresent()) {
            rd = RouteDistinguisherUtil.parseRouteDistinguisher(rdNode.get().getValue());
        }
        return rd;
    }

    /**
     * For flowspec-l3vpn, there is a route distinguisher field at the beginning of NLRI (8 bytes)
     *
     * @param nlri
     * @return
     */
    private RouteDistinguisher readRouteDistinguisher(final ByteBuf nlri) {
        final RouteDistinguisher rd = RouteDistinguisherUtil.parseRouteDistinguisher(nlri);
        LOG.trace("Route Distinguisher read from NLRI: {}", rd);
        return rd;
    }

    /**
     * Serializes Flowspec NLRI to ByteBuf.
     *
     * @param flows  flowspec NLRI to be serialized
     * @param pathId
     * @param buffer where flowspec NLRI will be serialized
     */
    public final void serializeNlri(final List<Flowspec> flows, @Nullable final RouteDistinguisher rd, @Nullable final PathId pathId, final ByteBuf buffer) {
        final ByteBuf nlriByteBuf = Unpooled.buffer();
        PathIdUtil.writePathId(pathId, buffer);

        if (rd != null) {
            RouteDistinguisherUtil.serializeRouteDistinquisher(rd, nlriByteBuf);
        }
        for (final Flowspec flow : flows) {
            this.flowspecTypeRegistry.serializeFlowspecType(flow.getFlowspecType(), nlriByteBuf);
        }
        Preconditions.checkState(nlriByteBuf.readableBytes() <= MAX_NLRI_LENGTH, "Maximum length of Flowspec NLRI reached.");
        if (nlriByteBuf.readableBytes() <= MAX_NLRI_LENGTH_ONE_BYTE) {
            buffer.writeByte(nlriByteBuf.readableBytes());
        } else {
            buffer.writeShort(nlriByteBuf.readableBytes() + LENGTH_MAGIC);
        }
        buffer.writeBytes(nlriByteBuf);
    }

    @Override
    public void parseNlri(@Nonnull final ByteBuf nlri, @Nonnull final MpReachNlriBuilder builder, @Nullable final PeerSpecificParserConstraint constraint) throws BGPParsingException {
        LOG.trace("Parse Nlri (MpReach) invoked. readable={}, AFI={}, SAFI={}", nlri.isReadable(), builder.getAfi(), builder.getSafi());
        if (!nlri.isReadable()) {
            return;
        }
        final PathId pathId = readPathId(nlri, builder.getAfi(), builder.getSafi(), constraint);
        verifyNlriLength(nlri);
        final RouteDistinguisher rd = readRouteDistinguisher(nlri);
        final List<Flowspec> dst = parseNlri(nlri);
        builder.setAdvertizedRoutes(
            new AdvertizedRoutesBuilder()
                .setDestinationType(
                    createAdvertizedRoutesDestinationType(dst, rd, pathId)
                ).build()
        );
    }

    @Override
    public void parseNlri(@Nonnull final ByteBuf nlri, @Nonnull final MpUnreachNlriBuilder builder, @Nullable final PeerSpecificParserConstraint constraint) throws BGPParsingException {
        LOG.trace("Parse Nlri (MpUnreach) invoked. readable={}, AFI={}, SAFI={}", nlri.isReadable(), builder.getAfi(), builder.getSafi());
        if (!nlri.isReadable()) {
            return;
        }
        final PathId pathId = readPathId(nlri, builder.getAfi(), builder.getSafi(), constraint);
        verifyNlriLength(nlri);
        final RouteDistinguisher rd = readRouteDistinguisher(nlri);
        final List<Flowspec> dst = parseNlri(nlri);
        builder.setWithdrawnRoutes(
            new WithdrawnRoutesBuilder()
                .setDestinationType(
                    createWithdrawnDestinationType(dst, rd, pathId)
                ).build()
        );
    }
}
