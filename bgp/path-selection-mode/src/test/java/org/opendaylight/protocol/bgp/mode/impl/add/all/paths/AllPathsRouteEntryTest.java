/*
 * Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.mode.impl.add.all.paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;

import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.protocol.bgp.rib.spi.RouterId;
import org.opendaylight.protocol.bgp.rib.spi.entry.AdvertizedRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.spi.node.ImmutableNodes;

/**
 * Add-path counterpart of {@code BaseRouteEntryTest}: verifies that a content-only change of a contributing route
 * (same router-id, same remote path-id, same attributes only the BGP-LU label-stack differs) is detected by
 * {@link org.opendaylight.protocol.bgp.mode.impl.add.AddPathAbstractRouteEntry#selectBest} and re-advertised.
 */
@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class AllPathsRouteEntryTest {
    private static final long AS = 64496L;
    private static final RouterId ROUTER_ID = RouterId.forAddress("127.0.0.1");
    private static final Uint32 PATH_ID = Uint32.ONE;

    private static final QName ROUTE_QNAME = QName.create("urn:opendaylight:test:route", "route").intern();
    private static final QName ROUTE_KEY_QNAME = QName.create(ROUTE_QNAME, "route-key").intern();
    private static final QName LABEL_QNAME = QName.create(ROUTE_QNAME, "label").intern();
    private static final NodeIdentifierWithPredicates ROUTE_LIST_ARG =
        NodeIdentifierWithPredicates.of(ROUTE_QNAME, ROUTE_KEY_QNAME, "test-route");

    private static final ContainerNode ATTRIBUTES = ImmutableNodes.newContainerBuilder()
        .withNodeIdentifier(new NodeIdentifier(Attributes.QNAME))
        .build();

    private final AllPathsRouteEntry entry = new AllPathsRouteEntry();

    @Mock
    private RIBSupport ribSupport;


    @Test
    public void testSelectBestDetectsLabelChange() {
        doReturn(ATTRIBUTES).when(ribSupport).extractAttributes(any());

        entry.addRoute(ROUTER_ID, PATH_ID, route(Uint32.valueOf(1000)));
        assertTrue(entry.selectBest(ribSupport, AS));

        entry.addRoute(ROUTER_ID, PATH_ID, route(Uint32.valueOf(2000)));
        assertTrue(entry.selectBest(ribSupport, AS));
    }

    @Test
    public void testSelectBestStableWhenRouteUnchanged() {
        doReturn(ATTRIBUTES).when(ribSupport).extractAttributes(any());

        entry.addRoute(ROUTER_ID, PATH_ID, route(Uint32.valueOf(1000)));
        assertTrue(entry.selectBest(ribSupport, AS));

        entry.addRoute(ROUTER_ID, PATH_ID, route(Uint32.valueOf(1000)));
        assertFalse(entry.selectBest(ribSupport, AS));
    }

    @Test
    public void testNewBestPathCarriesUpdatedLabel() {
        doReturn(ATTRIBUTES).when(ribSupport).extractAttributes(any());
        doAnswer(inv -> inv.getArgument(0)).when(ribSupport).createRoute(any(), any(), any());
        doReturn(ROUTE_LIST_ARG).when(ribSupport).createRouteListArgument(any(), anyString());
        doAnswer(inv -> inv.getArgument(0)).when(ribSupport).toAddPathListArgument(any());
        doAnswer(inv -> inv.getArgument(0)).when(ribSupport).toNonPathListArgument(any());

        entry.addRoute(ROUTER_ID, PATH_ID, route(Uint32.valueOf(1000)));
        entry.selectBest(ribSupport, AS);

        final MapEntryNode updated = route(Uint32.valueOf(2000));
        entry.addRoute(ROUTER_ID, PATH_ID, updated);
        assertTrue(entry.selectBest(ribSupport, AS));

        final List<AdvertizedRoute> advertized = entry.newBestPaths(ribSupport, "test-route");
        assertEquals(1, advertized.size());
        assertEquals(updated, advertized.getFirst().getRoute());
    }

    private static MapEntryNode route(final Uint32 label) {
        return ImmutableNodes.newMapEntryBuilder()
            .withNodeIdentifier(NodeIdentifierWithPredicates.of(ROUTE_QNAME, ROUTE_KEY_QNAME, "test-route"))
            .withChild(ImmutableNodes.leafNode(LABEL_QNAME, label))
            .build();
    }
}
