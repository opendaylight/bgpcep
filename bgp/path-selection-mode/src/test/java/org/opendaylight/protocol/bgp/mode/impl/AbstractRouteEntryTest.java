/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.mode.impl;

import static org.opendaylight.protocol.bgp.mode.impl.base.BasePathSelectorTest.ATTRS_EXTENSION_Q;
import static org.opendaylight.protocol.bgp.mode.impl.base.BasePathSelectorTest.SEGMENTS_NID;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.UnsignedInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.protocol.bgp.rib.spi.ExportPolicyPeerTracker;
import org.opendaylight.protocol.bgp.rib.spi.PeerExportGroup;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.protocol.bgp.rib.spi.RibSupportUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv4.routes.Ipv4Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv4.routes.ipv4.routes.Ipv4Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.AsPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.AtomicAggregate;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.Origin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.BgpRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.rib.LocRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.rib.peer.AdjRibOut;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;

public class AbstractRouteEntryTest {
    protected static final long REMOTE_PATH_ID = 1;
    protected static final PeerId PEER_ID = new PeerId("bgp://42.42.42.42");
    protected static final YangInstanceIdentifier PEER_YII2 = YangInstanceIdentifier.of(QName.create("urn:opendaylight:params:xml:ns:yang:bgp-inet:test", "2015-03-05", "peer2"));
    protected static final long AS = 64444;
    protected static final UnsignedInteger ROUTER_ID = UnsignedInteger.ONE;
    protected static final TablesKey TABLES_KEY = new TablesKey(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class);
    protected static final YangInstanceIdentifier LOC_RIB_TARGET = YangInstanceIdentifier.create(YangInstanceIdentifier.of(BgpRib.QNAME)
        .node(LocRib.QNAME).node(Tables.QNAME).node(RibSupportUtils.toYangTablesKey(TABLES_KEY)).getPathArguments());
    private static final long PATH_ID = 1;
    private static final PeerId PEER_ID2 = new PeerId("bgp://43.43.43.43");
    private static final PeerId PEER_DISCONNECTED = new PeerId("bgp://44.44.44.44");
    private static final String PREFIX = "1.2.3.4/32";
    private static final String PREFIX2 = "2.2.2.2/32";
    private static final YangInstanceIdentifier PEER_YII = YangInstanceIdentifier.of(QName.create("urn:opendaylight:params:xml:ns:yang:bgp-inet:test", "2015-03-05", "peer1"));
    private static final NodeIdentifier ROUTES_IDENTIFIER = new NodeIdentifier(Routes.QNAME);
    private static final NodeIdentifier ORIGIN_NID = new NodeIdentifier(QName.create(ATTRS_EXTENSION_Q, Origin.QNAME.getLocalName()).intern());
    private static final NodeIdentifier ORIGIN_VALUE_NID = new NodeIdentifier(QName.create(ATTRS_EXTENSION_Q, "value").intern());
    private static final NodeIdentifier AS_PATH_NID = new NodeIdentifier(QName.create(ATTRS_EXTENSION_Q, AsPath.QNAME.getLocalName()).intern());
    private static final NodeIdentifier ATOMIC_NID = new NodeIdentifier(QName.create(ATTRS_EXTENSION_Q, AtomicAggregate.QNAME.getLocalName()));
    private static final QName Q_NAME = BindingReflections.findQName(Ipv4Routes.class).intern();
    private static final NodeIdentifier ROUTE_ATTRIBUTES_IDENTIFIER = new NodeIdentifier(QName.create(Q_NAME, Attributes.QNAME.getLocalName().intern()));
    private static final QName PREFIX_QNAME = QName.create(Ipv4Route.QNAME, "prefix").intern();
    protected static final NodeIdentifierWithPredicates ROUTE_ID_PA = new NodeIdentifierWithPredicates(Ipv4Route.QNAME, ImmutableMap.of(PREFIX_QNAME, PREFIX));
    private static final QName PATHID_QNAME = QName.create(Ipv4Route.QNAME, "path-id").intern();
    protected static final NodeIdentifierWithPredicates ROUTE_ID_PA_ADD_PATH = new NodeIdentifierWithPredicates(Ipv4Route.QNAME, ImmutableMap.of(PATHID_QNAME, PATH_ID, PREFIX_QNAME, PREFIX2));
    @Mock
    protected RIBSupport ribSupport;
    @Mock
    protected DOMDataWriteTransaction tx;
    @Mock
    protected ExportPolicyPeerTracker peerPT;
    @Mock
    protected PeerExportGroup peg;
    protected List<YangInstanceIdentifier> yIIChanges;
    protected NormalizedNode<?, ?> attributes;
    protected YangInstanceIdentifier routePaYii;
    protected YangInstanceIdentifier routePaAddPathYii;
    protected YangInstanceIdentifier routeRiboutYii;
    protected YangInstanceIdentifier routeAddRiboutYii;
    protected YangInstanceIdentifier routeRiboutAttYii;
    protected YangInstanceIdentifier routeAddRiboutAttYii;
    protected YangInstanceIdentifier routeRiboutAttYiiPeer2;
    protected YangInstanceIdentifier routeRiboutYiiPeer2;
    protected YangInstanceIdentifier routeAddRiboutYiiPeer2;
    @Mock
    private PeerExportGroup pegNot;
    private YangInstanceIdentifier locRibTargetYii;
    private YangInstanceIdentifier locRibOutTargetYii;
    private YangInstanceIdentifier locRibOutTargetYiiPeer2;

    protected void setUp() {
        MockitoAnnotations.initMocks(this);
        this.yIIChanges = new ArrayList<>();
        this.peerPT = Mockito.mock(ExportPolicyPeerTracker.class);
        this.attributes = createAttr();
        this.locRibTargetYii = LOC_RIB_TARGET.node(ROUTES_IDENTIFIER);
        this.locRibOutTargetYii = PEER_YII.node(AdjRibOut.QNAME).node(Tables.QNAME).node(RibSupportUtils.toYangTablesKey(TABLES_KEY)).node(ROUTES_IDENTIFIER);
        this.routePaYii = this.locRibTargetYii.node(ROUTE_ID_PA);
        this.routePaAddPathYii = this.locRibTargetYii.node(ROUTE_ID_PA_ADD_PATH);
        this.routeRiboutYii = this.locRibOutTargetYii.node(ROUTE_ID_PA);
        this.routeAddRiboutYii = this.locRibOutTargetYii.node(ROUTE_ID_PA_ADD_PATH);
        this.routeRiboutAttYii = this.locRibOutTargetYii.node(ROUTE_ID_PA).node(ATTRS_EXTENSION_Q);
        this.routeAddRiboutAttYii = this.locRibOutTargetYii.node(ROUTE_ID_PA_ADD_PATH).node(ATTRS_EXTENSION_Q);
        this.locRibOutTargetYiiPeer2 = PEER_YII2.node(AdjRibOut.QNAME).node(Tables.QNAME).node(RibSupportUtils.toYangTablesKey(TABLES_KEY)).node(ROUTES_IDENTIFIER);
        this.routeRiboutYiiPeer2 = this.locRibOutTargetYiiPeer2.node(ROUTE_ID_PA);
        this.routeRiboutAttYiiPeer2 = this.locRibOutTargetYiiPeer2.node(ROUTE_ID_PA).node(ATTRS_EXTENSION_Q);
        this.routeAddRiboutYiiPeer2 = this.locRibOutTargetYiiPeer2.node(ROUTE_ID_PA_ADD_PATH);
        mockRibSupport();
        mockExportPolicies();
        mockExportGroup();
        mockTransactionChain();
    }

    private void mockTransactionChain() {
        Mockito.doAnswer(invocation -> {
            final Object[] args = invocation.getArguments();
            yIIChanges.add((YangInstanceIdentifier) args[1]);
            return args[1];
        }).when(this.tx).put(Mockito.any(LogicalDatastoreType.class), Mockito.any(YangInstanceIdentifier.class), Mockito.any(NormalizedNode.class));

        Mockito.doAnswer(invocation -> {
            final Object[] args = invocation.getArguments();
            if (routePaYii.equals(args[1])) {
                yIIChanges.remove(routePaYii);
            } else if (routePaAddPathYii.equals(args[1])) {
                yIIChanges.remove(routePaAddPathYii);
            } else if (routeRiboutYii.equals(args[1])) {
                yIIChanges.remove(routeRiboutYii);
                yIIChanges.remove(routeAddRiboutAttYii);
            } else if (routeAddRiboutYii.equals(args[1])) {
                yIIChanges.remove(routeAddRiboutYii);
                yIIChanges.remove(routeAddRiboutAttYii);
            }
            return args[1];
        }).when(this.tx).delete(Mockito.any(LogicalDatastoreType.class), Mockito.any(YangInstanceIdentifier.class));
    }

    private void mockExportGroup() {
        Mockito.doReturn(this.attributes).when(this.peg).effectiveAttributes(Mockito.any(PeerRole.class), Mockito.any(ContainerNode.class));
        Mockito.doReturn(null).when(this.pegNot).effectiveAttributes(Mockito.any(PeerRole.class), Mockito.any(ContainerNode.class));

        Map<PeerId, PeerExportGroup.PeerExporTuple> peers = new HashMap<>();
        Mockito.doReturn(ImmutableList.copyOf(peers.entrySet())).when(this.pegNot).getPeers();
        Mockito.doReturn(true).when(this.pegNot).containsPeer(Mockito.any(PeerId.class));

        peers.put(PEER_ID, new PeerExportGroup.PeerExporTuple(PEER_YII, PeerRole.Ibgp));
        peers.put(PEER_ID2, new PeerExportGroup.PeerExporTuple(PEER_YII2, PeerRole.Ibgp));

        Mockito.doReturn(ImmutableList.copyOf(peers.entrySet())).when(this.peg).getPeers();
    }

    private void mockExportPolicies() {
        Mockito.doReturn(true).when(this.peerPT).isTableSupported(PEER_ID);
        Mockito.doReturn(false).when(this.peerPT).isTableSupported(PEER_ID2);
        Mockito.doAnswer(invocation -> {
            final Object[] args = invocation.getArguments();
            if (PeerRole.Ibgp.equals(args[0])) {
                return peg;
            } else if (PeerRole.Ebgp.equals(args[0])) {
                return pegNot;
            } else {
                return null;
            }
        }).when(this.peerPT).getPeerGroup(Mockito.any(PeerRole.class));

        Mockito.doReturn(true).when(this.peerPT).isAddPathSupportedByPeer(PEER_ID);
        Mockito.doReturn(false).when(this.peerPT).isAddPathSupportedByPeer(PEER_ID2);
    }

    private void mockRibSupport() {
        Mockito.doReturn(ROUTE_ATTRIBUTES_IDENTIFIER).when(this.ribSupport).routeAttributesIdentifier();
        Mockito.doReturn(ROUTE_ID_PA_ADD_PATH).when(this.ribSupport).getRouteIdAddPath(Mockito.any(Long.class), Mockito.eq(ROUTE_ID_PA_ADD_PATH));
        Mockito.doReturn(null).when(this.ribSupport).getRouteIdAddPath(Mockito.any(Long.class), Mockito.eq(ROUTE_ID_PA));
        Mockito.doAnswer(invocation -> {
            final Object[] args = invocation.getArguments();
            final YangInstanceIdentifier yii = (YangInstanceIdentifier) args[0];
            final PathArgument paa = (PathArgument) args[1];

            if (ROUTE_ID_PA.equals(paa)) {
                if (yii.equals(locRibTargetYii)) {
                    return routePaYii;
                } else if (yii.equals(locRibOutTargetYii)) {
                    return routeRiboutYii;
                } else if (yii.equals(locRibOutTargetYiiPeer2)) {
                    return routeRiboutYiiPeer2;
                }
            } else if (ROUTE_ID_PA_ADD_PATH.equals(paa)) {
                if (yii.equals(locRibTargetYii)) {
                    return routePaAddPathYii;
                } else if (yii.equals(locRibOutTargetYii)) {
                    return routeAddRiboutYii;
                } else if (yii.equals(locRibOutTargetYiiPeer2)) {
                    return routeAddRiboutYiiPeer2;
                }
            }
            return null;
        }).when(this.ribSupport).routePath(Mockito.any(YangInstanceIdentifier.class), Mockito.any(PathArgument.class));
    }

    private NormalizedNode<?, ?> createAttr() {
        final ContainerNode attributes = Builders.containerBuilder().withNodeIdentifier(new NodeIdentifier(ATTRS_EXTENSION_Q))
            .addChild(Builders.containerBuilder().withNodeIdentifier(ORIGIN_NID)
                .addChild(Builders.leafBuilder().withNodeIdentifier(ORIGIN_VALUE_NID).withValue("igp").build()).build())
            .addChild(Builders.containerBuilder().withNodeIdentifier(AS_PATH_NID)
                .addChild(Builders.unkeyedListBuilder().withNodeIdentifier(SEGMENTS_NID).build()).build())
            .addChild(Builders.containerBuilder().withNodeIdentifier(ATOMIC_NID).build()).build();
        return ImmutableContainerNodeBuilder.create().withNodeIdentifier(ROUTE_ATTRIBUTES_IDENTIFIER)
            .withChild(attributes).build();
    }
}
