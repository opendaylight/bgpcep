/*
 * Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi.entry;

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.mdsal.binding.dom.adapter.AdapterContext;
import org.opendaylight.mdsal.binding.dom.adapter.test.AbstractConcurrentDataBrokerTest;
import org.opendaylight.mdsal.binding.dom.adapter.test.AbstractDataBrokerTestCustomizer;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupportTestImp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.test.rev180515.bgp.rib.rib.loc.rib.tables.routes.Ipv4RoutesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.test.rev180515.ipv4.routes.Ipv4Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.test.rev180515.ipv4.routes.ipv4.routes.Ipv4Route;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.spi.node.ImmutableNodes;

/**
 * Tests {@link AbstractAdvertizedRoute#sharedRouteEntry} caching. An update fanned out to many peers must build
 * the AdjRibsOut entry once per route key and reuse it while the effective attributes stay equal.
 */
public class AdvertizedRouteTest extends AbstractConcurrentDataBrokerTest {
    private static final String PREFIX = "1.2.3.4/32";
    private static final QName ATTRS_QNAME = QName.create(Ipv4Route.QNAME, "attributes").intern();
    private static final QName MARKER_QNAME = QName.create(Ipv4Route.QNAME, "marker").intern();
    private static final PeerId PEER_ID = new PeerId("bgp://127.0.0.1");

    private AdapterContext context;
    private AdvertizedRoute<Ipv4RoutesCase, Ipv4Routes> advRoute;
    private NodeIdentifierWithPredicates addPathKey;
    private NodeIdentifierWithPredicates nonAddPathKey;
    private RIBSupportTestImp ribSupport;

    @Override
    protected AbstractDataBrokerTestCustomizer createDataBrokerTestCustomizer() {
        final var customizer = super.createDataBrokerTestCustomizer();
        context = customizer.getAdapterContext();
        return customizer;
    }

    @Before
    public void createRoute() {
        ribSupport = new RIBSupportTestImp(context.currentSerializer());
        final var routeKey = ribSupport.createRouteListArgument(Uint32.ONE, PREFIX);
        final var route = ImmutableNodes.newMapEntryBuilder().withNodeIdentifier(routeKey).build();
        advRoute = new AdvertizedRoute<>(ribSupport, route, attributes(), PEER_ID, false);
        addPathKey = advRoute.getAddPathRouteKeyIdentifier();
        nonAddPathKey = advRoute.getNonAddPathRouteKeyIdentifier();
    }

    /*
     * Equal effective attributes on the same key return the cached entry instead of rebuilding it.
     */
    @Test
    public void testEqualAttributesReuseEntry() {
        final var attributes = attributes();
        final var first = advRoute.sharedRouteEntry(ribSupport, addPathKey, attributes);
        final var second = advRoute.sharedRouteEntry(ribSupport, addPathKey, attributes);
        assertSame(first, second);
    }

    /*
     * Attributes that are equal but not the same instance still hit the cache.
     */
    @Test
    public void testEqualButDistinctAttributesReuseEntry() {
        final var first = advRoute.sharedRouteEntry(ribSupport, addPathKey, attributes());
        final var second = advRoute.sharedRouteEntry(ribSupport, addPathKey, attributes());
        assertSame(first, second);
    }

    /*
     * The add-path and non-add-path keys are cached in independent slots.
     */
    @Test
    public void testKeyTypesCachedIndependently() {
        final var attributes = attributes();
        final var addPathEntry = advRoute.sharedRouteEntry(ribSupport, addPathKey, attributes);
        final var nonAddPathEntry = advRoute.sharedRouteEntry(ribSupport, nonAddPathKey, attributes);
        assertNotSame(addPathEntry, nonAddPathEntry);
        assertSame(addPathEntry, advRoute.sharedRouteEntry(ribSupport, addPathKey, attributes));
        assertSame(nonAddPathEntry, advRoute.sharedRouteEntry(ribSupport, nonAddPathKey, attributes));
    }

    /*
     * Changed effective attributes rebuild the entry and refresh the cache.
     */
    @Test
    public void testChangedAttributesRebuildEntry() {
        final var first = advRoute.sharedRouteEntry(ribSupport, addPathKey, attributes());
        final var changed = attributesWithMarker();
        final var rebuilt = advRoute.sharedRouteEntry(ribSupport, addPathKey, changed);
        assertNotSame(first, rebuilt);
        assertSame(rebuilt, advRoute.sharedRouteEntry(ribSupport, addPathKey, changed));
    }

    private static ContainerNode attributes() {
        return ImmutableNodes.newContainerBuilder().withNodeIdentifier(new NodeIdentifier(ATTRS_QNAME)).build();
    }

    private static ContainerNode attributesWithMarker() {
        return ImmutableNodes.newContainerBuilder()
            .withNodeIdentifier(new NodeIdentifier(ATTRS_QNAME))
            .withChild(ImmutableNodes.leafNode(MARKER_QNAME, "changed"))
            .build();
    }
}
