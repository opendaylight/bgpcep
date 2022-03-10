/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.mdsal.binding.dom.adapter.AdapterContext;
import org.opendaylight.mdsal.binding.dom.adapter.test.AbstractConcurrentDataBrokerTest;
import org.opendaylight.mdsal.binding.dom.adapter.test.AbstractDataBrokerTestCustomizer;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.UpdateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.AttributesReachBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.AttributesUnreachBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.reach.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.reach.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.reach.mp.reach.nlri.AdvertizedRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.reach.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.unreach.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.unreach.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.unreach.mp.unreach.nlri.WithdrawnRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.unreach.mp.unreach.nlri.WithdrawnRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.destination.DestinationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.BgpRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.rib.LocRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.tables.Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.test.rev180515.Ipv4Prefixes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.test.rev180515.bgp.rib.rib.loc.rib.tables.routes.Ipv4RoutesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.test.rev180515.ipv4.routes.Ipv4Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.test.rev180515.ipv4.routes.ipv4.routes.Ipv4Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.UnicastSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.next.hop.c.next.hop.Ipv4NextHopCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.next.hop.c.next.hop.Ipv4NextHopCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.next.hop.c.next.hop.ipv4.next.hop._case.Ipv4NextHopBuilder;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeCandidateNode;

public class RIBSupportTest extends AbstractConcurrentDataBrokerTest {
    private static final String ROUTE_KEY = "prefix";
    private static final String PREFIX = "1.2.3.4/32";
    private static final QName PATH_ID_QNAME = QName.create(Ipv4Route.QNAME, "path-id").intern();
    private static final NodeIdentifierWithPredicates PREFIX_NII = NodeIdentifierWithPredicates.of(Ipv4Route.QNAME,
        QName.create(Ipv4Route.QNAME, ROUTE_KEY).intern(), PREFIX);
    private RIBSupportTestImp ribSupportTestImp;
    private static final TablesKey TABLES_KEY = new TablesKey(Ipv4AddressFamily.class,
            UnicastSubsequentAddressFamily.class);
    private static final YangInstanceIdentifier LOC_RIB_TARGET = YangInstanceIdentifier
            .create(YangInstanceIdentifier.of(BgpRib.QNAME)
        .node(LocRib.QNAME).node(Tables.QNAME).node(RibSupportUtils.toYangTablesKey(TABLES_KEY)).getPathArguments());
    private static final NodeIdentifier ROUTES_IDENTIFIER = new NodeIdentifier(Routes.QNAME);
    private static final NodeIdentifier IPV4_ROUTES_IDENTIFIER = new NodeIdentifier(Ipv4Routes.QNAME);
    private DataTreeCandidateNode emptyTree;
    private DataTreeCandidateNode emptySubTree;
    private DataTreeCandidateNode subTree;
    private DOMDataTreeWriteTransaction tx;
    private ContainerNode nlri;
    private final Map<YangInstanceIdentifier, NormalizedNode> routesMap = new HashMap<>();
    private ContainerNode attributes;
    private MapEntryNode mapEntryNode;
    private AdapterContext context;

    @Before
    public void setUp() throws Exception {
        super.setup();
        MockitoAnnotations.initMocks(this);
        ribSupportTestImp = new RIBSupportTestImp(context.currentSerializer());
        emptyTree = Mockito.mock(DataTreeCandidateNode.class);
        emptySubTree = Mockito.mock(DataTreeCandidateNode.class);
        subTree = Mockito.mock(DataTreeCandidateNode.class);
        final DataTreeCandidateNode emptyNode = Mockito.mock(DataTreeCandidateNode.class);
        final DataTreeCandidateNode node = Mockito.mock(DataTreeCandidateNode.class);
        doReturn(Optional.empty()).when(emptyTree).getModifiedChild(IPV4_ROUTES_IDENTIFIER);

        doReturn(Optional.of(emptyNode)).when(emptySubTree).getModifiedChild(IPV4_ROUTES_IDENTIFIER);
        doReturn(Optional.empty()).when(emptyNode).getModifiedChild(new NodeIdentifier(Ipv4Route.QNAME));

        doReturn(Optional.of(node)).when(subTree).getModifiedChild(IPV4_ROUTES_IDENTIFIER);
        doReturn(Optional.of(node)).when(node).getModifiedChild(new NodeIdentifier(Ipv4Route.QNAME));
        final Collection<DataTreeCandidateNode> emptyCollection = new HashSet<>();
        doReturn(emptyCollection).when(node).getChildNodes();

        tx = Mockito.mock(DOMDataTreeWriteTransaction.class);
        nlri = Mockito.mock(ContainerNode.class);
        attributes = ImmutableContainerNodeBuilder.create()
                .withNodeIdentifier(new NodeIdentifier(QName.create(Ipv4Routes.QNAME, Attributes.QNAME
            .getLocalName().intern()))).build();
        final ContainerNode destination = Mockito.mock(ContainerNode.class);
        final ChoiceNode destinations = Mockito.mock(ChoiceNode.class);
        final ContainerNode route = Mockito.mock(ContainerNode.class);

        doReturn(destination).when(nlri).childByArg(new NodeIdentifier(WithdrawnRoutes.QNAME));
        doReturn(destination).when(nlri).childByArg(new NodeIdentifier(AdvertizedRoutes.QNAME));
        doReturn(destinations).when(destination).childByArg(new NodeIdentifier(DestinationType.QNAME));
        doReturn(route).when(destinations).childByArg(new NodeIdentifier(Ipv4Prefixes.QNAME));
        doReturn(emptyCollection).when(route).body();

        doAnswer(invocation -> {
            final Object[] args = invocation.getArguments();
            routesMap.remove(args[1]);
            return args[1];
        }).when(tx).delete(Mockito.eq(LogicalDatastoreType.OPERATIONAL), any(YangInstanceIdentifier.class));
        doAnswer(invocation -> {
            final Object[] args = invocation.getArguments();
            final NormalizedNode node1 = (NormalizedNode) args[2];
            routesMap.put((YangInstanceIdentifier) args[1], node1);
            return args[1];
        }).when(tx).put(Mockito.eq(LogicalDatastoreType.OPERATIONAL), any(YangInstanceIdentifier.class),
                any(NormalizedNode.class));

        mapEntryNode = Mockito.mock(MapEntryNode.class);
    }

    @Override
    protected final AbstractDataBrokerTestCustomizer createDataBrokerTestCustomizer() {
        final AbstractDataBrokerTestCustomizer customizer = super.createDataBrokerTestCustomizer();
        context = customizer.getAdapterContext();
        return customizer;
    }

    @Test
    public void routesCaseClass() {
        assertEquals(Ipv4RoutesCase.class, ribSupportTestImp.routesCaseClass());
    }

    @Test
    public void routesContainerClass() {
        assertEquals(Ipv4Routes.class, ribSupportTestImp.routesContainerClass());
    }

    @Test
    public void routesListClass() {
        assertEquals(Ipv4Route.class, ribSupportTestImp.routesListClass());
    }

    @Test
    public void routeQName() {
        assertEquals(Ipv4Route.QNAME, ribSupportTestImp.routeQName());
    }

    @Test
    public void routeNid() {
        assertEquals(new NodeIdentifier(Ipv4Route.QNAME),ribSupportTestImp.routeNid());
    }

    @Test
    public void getAfi() {
        assertEquals(Ipv4AddressFamily.class,ribSupportTestImp.getAfi());
    }

    @Test
    public void getSafi() {
        assertEquals(UnicastSubsequentAddressFamily.class,ribSupportTestImp.getSafi());
    }

    @Test
    public void routesContainerIdentifier() {
        assertEquals(IPV4_ROUTES_IDENTIFIER,ribSupportTestImp.routesContainerIdentifier());

    }

    @Test
    public void routeAttributesIdentifier() {
        assertEquals(new NodeIdentifier(QName.create(Ipv4Routes.QNAME,
                Attributes.QNAME.getLocalName().intern())),ribSupportTestImp.routeAttributesIdentifier());
    }

    @Test
    public void routePath() {
        assertEquals(LOC_RIB_TARGET.node(ROUTES_IDENTIFIER)
                        .node(Ipv4Routes.QNAME).node(Ipv4Route.QNAME).node(PREFIX_NII),
                ribSupportTestImp.routePath(LOC_RIB_TARGET, PREFIX_NII));
    }

    @Test
    public void changedRoutes() {
        assertTrue(ribSupportTestImp.changedRoutes(emptyTree).isEmpty());
        assertTrue(ribSupportTestImp.changedRoutes(emptySubTree).isEmpty());
        assertNotNull(ribSupportTestImp.changedRoutes(subTree));
    }

    @Test
    public void putRoutes() {
        ribSupportTestImp.putRoutes(tx, LOC_RIB_TARGET, nlri, attributes);
        assertFalse(routesMap.isEmpty());
    }

    @Test
    public void deleteRoutes() {
        ribSupportTestImp.deleteRoutes(tx, LOC_RIB_TARGET, nlri);
        assertTrue(routesMap.isEmpty());
    }


    @Test
    public void buildUpdate() {
        final Ipv4NextHopCase nextHop = new Ipv4NextHopCaseBuilder().setIpv4NextHop(new Ipv4NextHopBuilder()
                .setGlobal(new Ipv4AddressNoZone("10.0.0.2")).build()).build();
        final Attributes attr = new AttributesBuilder().setCNextHop(nextHop).build();
        final Collection<MapEntryNode> routes = new HashSet<>();

        assertEquals(new UpdateBuilder().setAttributes(new AttributesBuilder().build()).build(),
               ribSupportTestImp.buildUpdate(routes, routes, attr));

        routes.add(mapEntryNode);
        final MpReachNlri mpReach = new MpReachNlriBuilder().setAfi(Ipv4AddressFamily.class)
                .setSafi(UnicastSubsequentAddressFamily.class)
                .setCNextHop(nextHop).setAdvertizedRoutes(new AdvertizedRoutesBuilder().build()).build();

        final Attributes attMpR = new AttributesBuilder().addAugmentation(
            new AttributesReachBuilder().setMpReachNlri(mpReach).build()).build();
        assertEquals(new UpdateBuilder().setAttributes(attMpR).build(),
               ribSupportTestImp.buildUpdate(routes, Set.of(), attr));

        final MpUnreachNlri mpUnreach = new MpUnreachNlriBuilder().setAfi(Ipv4AddressFamily.class)
                .setSafi(UnicastSubsequentAddressFamily.class)
                .setWithdrawnRoutes(new WithdrawnRoutesBuilder().build()).build();

        final Attributes attMpU = new AttributesBuilder().addAugmentation(
                new AttributesUnreachBuilder().setMpUnreachNlri(mpUnreach).build()).build();
        assertEquals(new UpdateBuilder().setAttributes(attMpU).build(),
               ribSupportTestImp.buildUpdate(Set.of(), routes, attr));
    }
}
