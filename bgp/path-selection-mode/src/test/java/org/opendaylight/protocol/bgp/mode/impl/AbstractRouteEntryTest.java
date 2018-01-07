/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.mode.impl;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.opendaylight.protocol.bgp.mode.impl.base.BasePathSelectorTest.ATTRS_EXTENSION_Q;
import static org.opendaylight.protocol.bgp.mode.impl.base.BasePathSelectorTest.SEGMENTS_NID;

import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.UnsignedInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.protocol.bgp.rib.spi.ExportPolicyPeerTracker;
import org.opendaylight.protocol.bgp.rib.spi.PeerExportGroup;
import org.opendaylight.protocol.bgp.rib.spi.PeerExportGroup.PeerExporTuple;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.protocol.bgp.rib.spi.RibSupportUtils;
import org.opendaylight.protocol.bgp.rib.spi.entry.RouteEntryDependenciesContainer;
import org.opendaylight.protocol.bgp.rib.spi.entry.RouteEntryInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171207.ipv4.routes.Ipv4Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171207.ipv4.routes.ipv4.routes.Ipv4Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.AsPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.AtomicAggregate;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.Origin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.BgpRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.bgp.rib.rib.LocRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.bgp.rib.rib.peer.AdjRibOut;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.rib.tables.Routes;
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

public abstract class AbstractRouteEntryTest {
    protected static final long REMOTE_PATH_ID = 1;
    protected static final PeerId PEER_ID = new PeerId("bgp://42.42.42.42");
    protected static final YangInstanceIdentifier PEER_YII2 = YangInstanceIdentifier
            .of(QName.create("urn:opendaylight:params:xml:ns:yang:bgp-inet:test",
                    "2015-03-05", "peer2"));
    protected static final long AS = 64444;
    protected static final UnsignedInteger ROUTER_ID = UnsignedInteger.ONE;
    protected static final TablesKey TABLES_KEY = new TablesKey(Ipv4AddressFamily.class,
            UnicastSubsequentAddressFamily.class);
    protected static final YangInstanceIdentifier LOC_RIB_TARGET =
            YangInstanceIdentifier.create(YangInstanceIdentifier.of(BgpRib.QNAME).node(LocRib.QNAME)
                    .node(Tables.QNAME).node(RibSupportUtils.toYangTablesKey(TABLES_KEY)).getPathArguments());
    private static final long PATH_ID = 1;
    private static final PeerId PEER_ID2 = new PeerId("bgp://43.43.43.43");
    private static final String PREFIX = "1.2.3.4/32";
    private static final String PREFIX2 = "2.2.2.2/32";
    private static final YangInstanceIdentifier PEER_YII
            = YangInstanceIdentifier.of(QName.create("urn:opendaylight:params:xml:ns:yang:bgp-inet:test",
            "2015-03-05", "peer1"));
    private static final NodeIdentifier ROUTES_IDENTIFIER = new NodeIdentifier(Routes.QNAME);
    private static final NodeIdentifier ORIGIN_NID = new NodeIdentifier(QName.create(ATTRS_EXTENSION_Q,
            Origin.QNAME.getLocalName()).intern());
    private static final NodeIdentifier ORIGIN_VALUE_NID = new NodeIdentifier(QName.create(ATTRS_EXTENSION_Q,
            "value").intern());
    private static final NodeIdentifier AS_PATH_NID = new NodeIdentifier(QName.create(ATTRS_EXTENSION_Q,
            AsPath.QNAME.getLocalName()).intern());
    private static final NodeIdentifier ATOMIC_NID = new NodeIdentifier(QName.create(ATTRS_EXTENSION_Q,
            AtomicAggregate.QNAME.getLocalName()));
    private static final QName Q_NAME = BindingReflections.findQName(Ipv4Routes.class).intern();
    private static final NodeIdentifier ROUTE_ATTRIBUTES_IDENTIFIER
            = new NodeIdentifier(QName.create(Q_NAME, Attributes.QNAME.getLocalName().intern()));
    private static final QName PREFIX_QNAME
            = QName.create(Ipv4Route.QNAME, "prefix").intern();
    protected static final NodeIdentifierWithPredicates ROUTE_ID_PA
            = new NodeIdentifierWithPredicates(Ipv4Route.QNAME, ImmutableMap.of(PREFIX_QNAME, PREFIX));
    private static final QName PATHID_QNAME = QName.create(Ipv4Route.QNAME, "path-id").intern();
    protected static final NodeIdentifierWithPredicates ROUTE_ID_PA_ADD_PATH
            = new NodeIdentifierWithPredicates(Ipv4Route.QNAME,
            ImmutableMap.of(PATHID_QNAME, PATH_ID, PREFIX_QNAME, PREFIX2));
    @Mock
    protected RIBSupport ribSupport;
    @Mock
    protected DOMDataWriteTransaction tx;
    @Mock
    protected ExportPolicyPeerTracker peerPT;
    @Mock
    protected PeerExportGroup peg;
    @Mock
    protected RouteEntryDependenciesContainer entryDep;
    @Mock
    protected RouteEntryInfo entryInfo;
    protected List<YangInstanceIdentifier> yiichanges;
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
        this.yiichanges = new ArrayList<>();
        this.attributes = createAttr();
        this.locRibTargetYii = LOC_RIB_TARGET.node(ROUTES_IDENTIFIER);
        this.locRibOutTargetYii = PEER_YII.node(AdjRibOut.QNAME).node(Tables.QNAME)
                .node(RibSupportUtils.toYangTablesKey(TABLES_KEY)).node(ROUTES_IDENTIFIER);
        this.routePaYii = this.locRibTargetYii.node(ROUTE_ID_PA);
        this.routePaAddPathYii = this.locRibTargetYii.node(ROUTE_ID_PA_ADD_PATH);
        this.routeRiboutYii = this.locRibOutTargetYii.node(ROUTE_ID_PA);
        this.routeAddRiboutYii = this.locRibOutTargetYii.node(ROUTE_ID_PA_ADD_PATH);
        this.routeRiboutAttYii = this.locRibOutTargetYii.node(ROUTE_ID_PA).node(ATTRS_EXTENSION_Q);
        this.routeAddRiboutAttYii = this.locRibOutTargetYii.node(ROUTE_ID_PA_ADD_PATH).node(ATTRS_EXTENSION_Q);
        this.locRibOutTargetYiiPeer2 = PEER_YII2.node(AdjRibOut.QNAME).node(Tables.QNAME)
                .node(RibSupportUtils.toYangTablesKey(TABLES_KEY)).node(ROUTES_IDENTIFIER);
        this.routeRiboutYiiPeer2 = this.locRibOutTargetYiiPeer2.node(ROUTE_ID_PA);
        this.routeRiboutAttYiiPeer2 = this.locRibOutTargetYiiPeer2.node(ROUTE_ID_PA).node(ATTRS_EXTENSION_Q);
        this.routeAddRiboutYiiPeer2 = this.locRibOutTargetYiiPeer2.node(ROUTE_ID_PA_ADD_PATH);
        mockRibSupport();
        mockExportPolicies();
        mockExportGroup();
        mockTransactionChain();
        mockEntryDep();
        mockEntryInfo();
    }

    private void mockEntryInfo() {
        doReturn(PEER_ID).when(this.entryInfo).getToPeerId();
        doReturn(PEER_YII2).when(this.entryInfo).getRootPath();
    }

    private void mockEntryDep() {
        doReturn(this.ribSupport).when(this.entryDep).getRibSupport();
        doReturn(this.peerPT).when(this.entryDep).getExportPolicyPeerTracker();
        doReturn(TABLES_KEY).when(this.entryDep).getLocalTablesKey();
        doReturn(LOC_RIB_TARGET).when(this.entryDep).getLocRibTableTarget();
    }

    private void mockTransactionChain() {
        doAnswer(invocation -> {
            final Object[] args = invocation.getArguments();
            this.yiichanges.add((YangInstanceIdentifier) args[1]);
            return args[1];
        }).when(this.tx)
                .put(any(LogicalDatastoreType.class), any(YangInstanceIdentifier.class), any(NormalizedNode.class));

        doAnswer(invocation -> {
            final Object[] args = invocation.getArguments();
            if (this.routePaYii.equals(args[1])) {
                this.yiichanges.remove(this.routePaYii);
            } else if (this.routePaAddPathYii.equals(args[1])) {
                this.yiichanges.remove(this.routePaAddPathYii);
            } else if (this.routeRiboutYii.equals(args[1])) {
                this.yiichanges.remove(this.routeRiboutYii);
                this.yiichanges.remove(this.routeAddRiboutAttYii);
            } else if (this.routeAddRiboutYii.equals(args[1])) {
                this.yiichanges.remove(this.routeAddRiboutYii);
                this.yiichanges.remove(this.routeAddRiboutAttYii);
            }
            return args[1];
        }).when(this.tx).delete(any(LogicalDatastoreType.class), any(YangInstanceIdentifier.class));
    }

    private void mockExportGroup() {
        doReturn(this.attributes).when(this.peg).effectiveAttributes(any(PeerRole.class), any(ContainerNode.class));
        doReturn(null).when(this.pegNot).effectiveAttributes(any(PeerRole.class), any(ContainerNode.class));

        final Map<PeerId, PeerExportGroup.PeerExporTuple> peers = new HashMap<>();
        doAnswer(invocation -> {
            final BiConsumer<PeerId, YangInstanceIdentifier> action = (BiConsumer) invocation.getArguments()[0];
            for (final Entry<PeerId, PeerExporTuple> pid : peers.entrySet()) {
                action.accept(pid.getKey(), pid.getValue().getYii());
            }
            return null;
        }).when(this.pegNot).forEach(any());
        doReturn(Boolean.TRUE).when(this.pegNot).containsPeer(any(PeerId.class));

        peers.put(PEER_ID, new PeerExportGroup.PeerExporTuple(PEER_YII, PeerRole.Ibgp));
        peers.put(PEER_ID2, new PeerExportGroup.PeerExporTuple(PEER_YII2, PeerRole.Ibgp));
        doAnswer(invocation -> {
            final BiConsumer<PeerId, YangInstanceIdentifier> action = (BiConsumer) invocation.getArguments()[0];
            for (final Entry<PeerId, PeerExporTuple> pid : peers.entrySet()) {
                action.accept(pid.getKey(), pid.getValue().getYii());
            }
            return null;
        }).when(this.peg).forEach(any());
    }

    private void mockExportPolicies() {
        doReturn(Boolean.TRUE).when(this.peerPT).isTableStructureInitialized(any(PeerId.class));
        doReturn(Boolean.TRUE).when(this.peerPT).isTableSupported(PEER_ID);
        doReturn(Boolean.FALSE).when(this.peerPT).isTableSupported(PEER_ID2);
        doAnswer(invocation -> {
            final Object[] args = invocation.getArguments();
            if (PeerRole.Ibgp.equals(args[0])) {
                return this.peg;
            } else if (PeerRole.Ebgp.equals(args[0])) {
                return this.pegNot;
            } else {
                return null;
            }
        }).when(this.peerPT).getPeerGroup(any(PeerRole.class));

        doReturn(Boolean.TRUE).when(this.peerPT).isAddPathSupportedByPeer(PEER_ID);
        doReturn(Boolean.FALSE).when(this.peerPT).isAddPathSupportedByPeer(PEER_ID2);
    }

    private void mockRibSupport() {
        doReturn(ROUTE_ATTRIBUTES_IDENTIFIER).when(this.ribSupport).routeAttributesIdentifier();
        doReturn(ROUTE_ID_PA_ADD_PATH).when(this.ribSupport)
                .getRouteIdAddPath(any(Long.class), eq(ROUTE_ID_PA_ADD_PATH));
        doReturn(null).when(this.ribSupport).getRouteIdAddPath(any(Long.class), eq(ROUTE_ID_PA));
        doAnswer(invocation -> {
            final Object[] args = invocation.getArguments();
            final YangInstanceIdentifier yii = (YangInstanceIdentifier) args[0];
            final PathArgument paa = (PathArgument) args[1];

            if (ROUTE_ID_PA.equals(paa)) {
                if (yii.equals(this.locRibTargetYii)) {
                    return this.routePaYii;
                } else if (yii.equals(this.locRibOutTargetYii)) {
                    return this.routeRiboutYii;
                } else if (yii.equals(this.locRibOutTargetYiiPeer2)) {
                    return this.routeRiboutYiiPeer2;
                }
            } else if (ROUTE_ID_PA_ADD_PATH.equals(paa)) {
                if (yii.equals(this.locRibTargetYii)) {
                    return this.routePaAddPathYii;
                } else if (yii.equals(this.locRibOutTargetYii)) {
                    return this.routeAddRiboutYii;
                } else if (yii.equals(this.locRibOutTargetYiiPeer2)) {
                    return this.routeAddRiboutYiiPeer2;
                }
            }
            return null;
        }).when(this.ribSupport).routePath(any(YangInstanceIdentifier.class), any(PathArgument.class));
    }

    private static NormalizedNode<?, ?> createAttr() {
        final ContainerNode attributes = Builders.containerBuilder()
                .withNodeIdentifier(new NodeIdentifier(ATTRS_EXTENSION_Q))
            .addChild(Builders.containerBuilder().withNodeIdentifier(ORIGIN_NID)
                .addChild(Builders.leafBuilder().withNodeIdentifier(ORIGIN_VALUE_NID)
                        .withValue("igp").build()).build())
            .addChild(Builders.containerBuilder().withNodeIdentifier(AS_PATH_NID)
                .addChild(Builders.unkeyedListBuilder().withNodeIdentifier(SEGMENTS_NID).build()).build())
            .addChild(Builders.containerBuilder().withNodeIdentifier(ATOMIC_NID).build()).build();
        return ImmutableContainerNodeBuilder.create().withNodeIdentifier(ROUTE_ATTRIBUTES_IDENTIFIER)
            .withChild(attributes).build();
    }

    protected Map<YangInstanceIdentifier, Long> collectInfo() {
        return this.yiichanges.stream().collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
    }
}
