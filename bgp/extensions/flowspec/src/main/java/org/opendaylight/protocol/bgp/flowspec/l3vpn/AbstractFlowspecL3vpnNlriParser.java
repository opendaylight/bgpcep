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
import org.opendaylight.bgp.concepts.RouteDistinguisherUtil;
import org.opendaylight.protocol.bgp.flowspec.AbstractFlowspecNlriParser;
import org.opendaylight.protocol.bgp.flowspec.SimpleFlowspecTypeRegistry;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.Flowspec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.FlowspecBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.RouteDistinguisher;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractFlowspecL3vpnNlriParser extends AbstractFlowspecNlriParser {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractFlowspecL3vpnNlriParser.class);
    public static final NodeIdentifier RD_NID = new NodeIdentifier(QName.create(Flowspec.QNAME.getNamespace(), Flowspec.QNAME.getRevision(), "route-distinguisher"));

    protected AbstractFlowspecL3vpnNlriParser(final SimpleFlowspecTypeRegistry flowspecTypeRegistry) {
        super(flowspecTypeRegistry);
    }

    @Override
    public String stringNlri(final DataContainerNode<?> flowspec) {
        final StringBuilder buffer = new StringBuilder();
        final RouteDistinguisher rd = extractRouteDistinguisher(flowspec, RD_NID);
        if (rd != null) {
            buffer.append("[l3vpn with route-distinguisher ").append(rd.getValue()).append("] ");
        }
        buffer.append(super.stringNlri(flowspec));
        return buffer.toString();
    }

    /**
     * For flowspec-l3vpn, there is a route distinguisher field at the beginning of NLRI (8 bytes)
     *
     * @param nlri
     * @return
     */
    private static RouteDistinguisher readRouteDistinguisher(final ByteBuf nlri) {
        final RouteDistinguisher rd = RouteDistinguisherUtil.parseRouteDistinguisher(nlri);
        LOG.trace("Route Distinguisher read from NLRI: {}", rd);
        return rd;
    }

    @Override
    protected void serializeNlri(final Object[] nlriFields, final ByteBuf buffer) {
        final RouteDistinguisher rd = requireNonNull((RouteDistinguisher) nlriFields[0]);
        RouteDistinguisherUtil.serializeRouteDistinquisher(rd, buffer);
        final List<Flowspec> flowspecList = (List<Flowspec>) nlriFields[1];
        serializeNlri(flowspecList, buffer);
    }

    @Override
    protected Object[] parseNlri(final ByteBuf nlri) throws BGPParsingException {
        readNlriLength(nlri);
        return new Object[] {
            requireNonNull(readRouteDistinguisher(nlri)),
            parseL3vpnNlriFlowspecList(nlri)
        };
    }

    protected final List<Flowspec> parseL3vpnNlriFlowspecList(final ByteBuf nlri) throws BGPParsingException {
        if (!nlri.isReadable()) {
            return null;
        }
        final List<Flowspec> fss = new ArrayList<>();

        while (nlri.isReadable()) {
            final FlowspecBuilder builder = new FlowspecBuilder();
            builder.setFlowspecType(this.flowspecTypeRegistry.parseFlowspecType(nlri));
            fss.add(builder.build());
        }

        return fss;
    }
}

