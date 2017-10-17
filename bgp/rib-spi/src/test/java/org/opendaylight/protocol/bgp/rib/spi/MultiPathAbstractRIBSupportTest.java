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
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv4.routes.ipv4.routes.Ipv4Prefixes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv4.routes.ipv4.routes.Ipv4Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv4.routes.ipv4.routes.Ipv4Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv4.routes.ipv4.routes.Ipv4RoutesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv4.routes.ipv4.routes.MultiPathAbstractTest;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.UpdateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.destination.DestinationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.reach.nlri.AdvertizedRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.unreach.nlri.WithdrawnRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.unreach.nlri.WithdrawnRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.BgpRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.rib.LocRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv4NextHopCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv4NextHopCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.ipv4.next.hop._case.Ipv4NextHopBuilder;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeSchemaAwareBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableLeafNodeBuilder;

public class MultiPathAbstractRIBSupportTest {
    private static final long PATH_ID = 0;
    private static final String ROUTE_KEY = "prefix";
    private static final String PREFIX = "1.2.3.4/32";
    private static final QName PATH_ID_QNAME = QName.create(Ipv4Route.QNAME, "path-id").intern();
    private static final QName PREFIX_QNAME = QName.create(Ipv4Route.QNAME, ROUTE_KEY).intern();
    private static final NodeIdentifierWithPredicates PREFIX_NII = new NodeIdentifierWithPredicates(Ipv4Route.QNAME,
        ImmutableMap.of(QName.create(Ipv4Route.QNAME, ROUTE_KEY).intern(), PREFIX));
    private static final MultiPathAbstractTest MULTI_PATH_ABSTRACT_TEST = new MultiPathAbstractTest();
    private static final TablesKey TABLES_KEY = new TablesKey(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class);
    private static final YangInstanceIdentifier LOC_RIB_TARGET = YangInstanceIdentifier.create(YangInstanceIdentifier.of(BgpRib.QNAME)
        .node(LocRib.QNAME).node(Tables.QNAME).node(RibSupportUtils.toYangTablesKey(TABLES_KEY)).getPathArguments());
    private static final NodeIdentifier ROUTES_IDENTIFIER = new NodeIdentifier(Routes.QNAME);
    private static final NodeIdentifier IPV4_ROUTES_IDENTIFIER = new NodeIdentifier(Ipv4Routes.QNAME);
    private DataTreeCandidateNode emptyTree;
    private DataTreeCandidateNode emptySubTree;
    private DataTreeCandidateNode subTree;
    private DOMDataWriteTransaction tx;
    private ContainerNode nlri;
    private Map<YangInstanceIdentifier, NormalizedNode<?, ?>> routes;
    private ContainerNode attributes;
    private MapEntryNode mapEntryNode;

    @Before
    public void setUp() {
        this.routes = new HashMap<>();
        MockitoAnnotations.initMocks(this);
        this.emptyTree = Mockito.mock(DataTreeCandidateNode.class);
        this.emptySubTree = Mockito.mock(DataTreeCandidateNode.class);
        this.subTree = Mockito.mock(DataTreeCandidateNode.class);
        final DataTreeCandidateNode emptyNode = Mockito.mock(DataTreeCandidateNode.class);
        final DataTreeCandidateNode node = Mockito.mock(DataTreeCandidateNode.class);
        doReturn(null).when(this.emptyTree).getModifiedChild(IPV4_ROUTES_IDENTIFIER);

        doReturn(emptyNode).when(this.emptySubTree).getModifiedChild(IPV4_ROUTES_IDENTIFIER);
        doReturn(null).when(emptyNode).getModifiedChild(new NodeIdentifier(Ipv4Route.QNAME));

        doReturn(node).when(this.subTree).getModifiedChild(IPV4_ROUTES_IDENTIFIER);
        doReturn(node).when(node).getModifiedChild(new NodeIdentifier(Ipv4Route.QNAME));
        final Collection<DataTreeCandidateNode> emptyCollection = new HashSet<>();
        doReturn(emptyCollection).when(node).getChildNodes();

        this.tx = Mockito.mock(DOMDataWriteTransaction.class);
        this.nlri = Mockito.mock(ContainerNode.class);
        this.attributes = ImmutableContainerNodeBuilder.create().withNodeIdentifier(new NodeIdentifier(QName.create(Ipv4Routes.QNAME, Attributes.QNAME
            .getLocalName().intern()))).build();
        final ContainerNode destination = Mockito.mock(ContainerNode.class);
        final ContainerNode route = Mockito.mock(ContainerNode.class);
        final Optional<?> optional = Mockito.mock(Optional.class);
        final Optional<?> destinationOptional = Mockito.mock(Optional.class);
        final Optional<?> destinationsOptional = Mockito.mock(Optional.class);
        final ChoiceNode destinations = Mockito.mock(ChoiceNode.class);

        doReturn(optional).when(this.nlri).getChild(new NodeIdentifier(WithdrawnRoutes.QNAME));
        doReturn(optional).when(this.nlri).getChild(new NodeIdentifier(AdvertizedRoutes.QNAME));
        doReturn(true).when(optional).isPresent();
        doReturn(destination).when(optional).get();
        doReturn(destinationOptional).when(destination).getChild(new NodeIdentifier(DestinationType.QNAME));
        doReturn(true).when(destinationOptional).isPresent();
        doReturn(destinations).when(destinationOptional).get();
        doReturn(destinationsOptional).when(destinations).getChild(new NodeIdentifier(Ipv4Prefixes.QNAME));
        doReturn(true).when(destinationsOptional).isPresent();
        doReturn(route).when(destinationsOptional).get();
        doReturn(emptyCollection).when(route).getValue();

        doAnswer(invocation -> {
            final Object[] args = invocation.getArguments();
            MultiPathAbstractRIBSupportTest.this.routes.remove(args[1]);
            return args[1];
        }).when(this.tx).delete(Mockito.eq(LogicalDatastoreType.OPERATIONAL), Mockito.any(YangInstanceIdentifier.class));
        doAnswer(invocation -> {
            final Object[] args = invocation.getArguments();
            final NormalizedNode<?, ?> node1 = (NormalizedNode<?, ?>) args[2];
            MultiPathAbstractRIBSupportTest.this.routes.put((YangInstanceIdentifier) args[1], node1);
            return args[1];
        }).when(this.tx).put(Mockito.eq(LogicalDatastoreType.OPERATIONAL), Mockito.any(YangInstanceIdentifier.class), Mockito.any(NormalizedNode.class));

        this.mapEntryNode = Mockito.mock(MapEntryNode.class);
    }

    @Test
    public void pathIdQName() {
        final MultiPathAbstractTest multiPath = MULTI_PATH_ABSTRACT_TEST;
        assertEquals(PATH_ID_QNAME, multiPath.pathIdQName());
    }

    @Test
    public void extractPathId() {
        final NodeIdentifier nii = new NodeIdentifier(PATH_ID_QNAME);
        final ContainerNode cont = ImmutableContainerNodeSchemaAwareBuilder.create().withNodeIdentifier(nii).
            addChild(new ImmutableLeafNodeBuilder<>().withNodeIdentifier(nii).withValue(PATH_ID).build()).build();
        assertEquals((Long) PATH_ID, MULTI_PATH_ABSTRACT_TEST.extractPathId(cont));
    }

    @Test
    public void getRouteIdAddPath() {
        final NodeIdentifierWithPredicates routeIdPa =
            new NodeIdentifierWithPredicates(Ipv4Route.QNAME, ImmutableMap.of(PATH_ID_QNAME, PATH_ID, PREFIX_QNAME, PREFIX));
        assertEquals(routeIdPa, MULTI_PATH_ABSTRACT_TEST.getRouteIdAddPath(PATH_ID, PREFIX_NII));

    }

    @Test
    public void createRouteKeyPathArgument() {
        assertEquals(PREFIX_NII, MULTI_PATH_ABSTRACT_TEST.createRouteKeyPathArgument(PREFIX_NII));
    }

    @Test
    public void routesCaseClass() throws Exception {
        assertEquals(Ipv4RoutesCase.class, MULTI_PATH_ABSTRACT_TEST.routesCaseClass());
    }

    @Test
    public void routesContainerClass() throws Exception {
        assertEquals(Ipv4Routes.class, MULTI_PATH_ABSTRACT_TEST.routesContainerClass());
    }

    @Test
    public void routesListClass() throws Exception {
        assertEquals(Ipv4Route.class, MULTI_PATH_ABSTRACT_TEST.routesListClass());
    }

    @Test
    public void routeQName() throws Exception {
        assertEquals(Ipv4Route.QNAME, MULTI_PATH_ABSTRACT_TEST.routeQName());
    }

    @Test
    public void emptyRoutes() throws Exception {
        final ChoiceNode emptyRoutes = Builders.choiceBuilder().withNodeIdentifier(ROUTES_IDENTIFIER).addChild(Builders.containerBuilder()
            .withNodeIdentifier(IPV4_ROUTES_IDENTIFIER).withChild(ImmutableNodes.mapNodeBuilder(MULTI_PATH_ABSTRACT_TEST.routeQName()
            ).build()).build())
            .build();
        assertEquals(emptyRoutes, MULTI_PATH_ABSTRACT_TEST.emptyRoutes());
    }

    @Test
    public void routeNid() throws Exception {
        assertEquals(new NodeIdentifier(Ipv4Route.QNAME), MULTI_PATH_ABSTRACT_TEST.routeNid());
    }

    @Test
    public void getAfi() throws Exception {
        assertEquals(Ipv4AddressFamily.class, MULTI_PATH_ABSTRACT_TEST.getAfi());
    }

    @Test
    public void getSafi() throws Exception {
        assertEquals(UnicastSubsequentAddressFamily.class, MULTI_PATH_ABSTRACT_TEST.getSafi());
    }

    @Test
    public void routesContainerIdentifier() throws Exception {
        assertEquals(IPV4_ROUTES_IDENTIFIER, MULTI_PATH_ABSTRACT_TEST.routesContainerIdentifier());

    }

    @Test
    public void routeAttributesIdentifier() throws Exception {
        assertEquals(new NodeIdentifier(QName.create(Ipv4Routes.QNAME, Attributes.QNAME.getLocalName().intern())), MULTI_PATH_ABSTRACT_TEST.routeAttributesIdentifier());
    }

    @Test
    public void routePath() throws Exception {
        Assert.assertEquals(LOC_RIB_TARGET.node(ROUTES_IDENTIFIER).node(Ipv4Routes.QNAME).node(Ipv4Route.QNAME).node(PREFIX_NII),
            MULTI_PATH_ABSTRACT_TEST.routePath(LOC_RIB_TARGET.node(Routes.QNAME), PREFIX_NII));
    }

    @Test
    public void changedRoutes() throws Exception {
        Assert.assertTrue(MULTI_PATH_ABSTRACT_TEST.changedRoutes(this.emptyTree).isEmpty());
        Assert.assertTrue(MULTI_PATH_ABSTRACT_TEST.changedRoutes(this.emptySubTree).isEmpty());
        Assert.assertNotNull(MULTI_PATH_ABSTRACT_TEST.changedRoutes(this.subTree));
    }

    @Test
    public void putRoutes() throws Exception {
        MULTI_PATH_ABSTRACT_TEST.putRoutes(this.tx, LOC_RIB_TARGET, this.nlri, this.attributes);
        assertFalse(this.routes.isEmpty());
    }

    @Test
    public void deleteRoutes() throws Exception {
        MULTI_PATH_ABSTRACT_TEST.deleteRoutes(this.tx, LOC_RIB_TARGET, this.nlri);
        assertTrue(this.routes.isEmpty());
    }


    @Test
    public void buildUpdate() throws Exception {
        final Ipv4NextHopCase nextHop = new Ipv4NextHopCaseBuilder().setIpv4NextHop(new Ipv4NextHopBuilder().setGlobal(
            new Ipv4Address("10.0.0.2")).build()).build();
        final Attributes attr = new AttributesBuilder().setCNextHop(nextHop).build();
        final Collection<MapEntryNode> routes = new HashSet<>();

        assertEquals(new UpdateBuilder().setAttributes(new AttributesBuilder().build()).build(),
            MULTI_PATH_ABSTRACT_TEST.buildUpdate(routes, routes, attr));

        routes.add(this.mapEntryNode);
        final MpReachNlri mpReach = new MpReachNlriBuilder().setAfi(Ipv4AddressFamily.class).setSafi(UnicastSubsequentAddressFamily.class)
            .setCNextHop(nextHop).setAdvertizedRoutes(new AdvertizedRoutesBuilder().build()).build();

        final Attributes attMpR = new AttributesBuilder().addAugmentation(Attributes1.class, new Attributes1Builder().setMpReachNlri(mpReach).build())
            .build();
        assertEquals(new UpdateBuilder().setAttributes(attMpR).build(), MULTI_PATH_ABSTRACT_TEST.buildUpdate(routes, Collections.emptySet(), attr));

        final MpUnreachNlri mpUnreach = new MpUnreachNlriBuilder().setAfi(Ipv4AddressFamily.class).setSafi(UnicastSubsequentAddressFamily.class)
            .setWithdrawnRoutes(new WithdrawnRoutesBuilder().build()).build();

        final Attributes attMpU = new AttributesBuilder().addAugmentation(Attributes2.class, new Attributes2Builder().setMpUnreachNlri(mpUnreach)
            .build()).build();
        assertEquals(new UpdateBuilder().setAttributes(attMpU).build(), MULTI_PATH_ABSTRACT_TEST.buildUpdate(Collections.emptySet(), routes, attr));
    }
}