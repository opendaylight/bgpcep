/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi;

import java.util.Collection;
import java.util.Collections;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.destination.DestinationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.test.rev180515.Ipv4Prefixes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.test.rev180515.bgp.rib.rib.loc.rib.tables.routes.Ipv4RoutesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.test.rev180515.ipv4.routes.Ipv4Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.test.rev180515.ipv4.routes.ipv4.routes.Ipv4Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;

public final class RIBSupportTestImp extends AbstractRIBSupport<Ipv4RoutesCase, Ipv4Routes, Ipv4Route> {
    private static final String ROUTE_KEY = "prefix";
    private static final String PREFIX = "1.2.3.4/32";

    private static final NodeIdentifierWithPredicates PREFIX_NII = NodeIdentifierWithPredicates.of(Ipv4Route.QNAME,
            QName.create(Ipv4Route.QNAME, ROUTE_KEY).intern(), PREFIX);

    public RIBSupportTestImp(final BindingNormalizedNodeSerializer mappingService) {
        super(mappingService, Ipv4RoutesCase.class, Ipv4Routes.class, Ipv4Route.class, Ipv4AddressFamily.VALUE,
            UnicastSubsequentAddressFamily.VALUE, Ipv4Prefixes.QNAME);
    }

    @Override
    protected DestinationType buildDestination(final Collection<MapEntryNode> routes) {
        return null;
    }

    @Override
    protected DestinationType buildWithdrawnDestination(final Collection<MapEntryNode> routes) {
        return null;
    }

    @Override
    protected Collection<NodeIdentifierWithPredicates> processDestination(final DOMDataTreeWriteTransaction tx,
                                                                          final YangInstanceIdentifier routesPath,
                                                                          final ContainerNode destination,
                                                                          final ContainerNode attributes,
                                                                          final ApplyRoute applyFunction) {
        applyFunction.apply(tx, routesPath.node(Ipv4Route.QNAME), PREFIX_NII, destination, attributes);
        return Collections.emptySet();
    }
}