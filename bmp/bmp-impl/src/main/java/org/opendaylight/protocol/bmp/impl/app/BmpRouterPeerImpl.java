/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bmp.impl.app;

import static java.util.Objects.requireNonNull;
import static org.opendaylight.protocol.bmp.impl.app.TablesUtil.BMP_TABLES_QNAME;
import static org.opendaylight.yangtools.yang.common.QName.create;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.Locale;
import java.util.Set;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingCodecTree;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingDataObjectCodecTreeNode;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.mdsal.dom.api.DOMTransactionChain;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionConsumerContext;
import org.opendaylight.protocol.bmp.impl.spi.BmpRouterPeer;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Timestamp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.bgp.parameters.OptionalCapabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.bgp.parameters.optional.capabilities.CParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.CParameters1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.MultiprotocolCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.UnicastSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.AdjRibInType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.Mirror;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.MirrorInformationCode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.Peer.PeerDistinguisher;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.PeerDownNotification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.PeerUp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.PeerUpNotification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.RouteMirroringMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.RouteMonitoringMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.Stat;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.StatsReportsMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.peer.header.PeerHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.peer.up.ReceivedOpen;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.peer.up.SentOpen;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.stat.Tlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.rev200120.BmpMonitor;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.rev200120.bmp.monitor.Monitor;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.rev200120.peers.Peer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.rev200120.peers.peer.Mirrors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.rev200120.peers.peer.PeerSession;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.rev200120.peers.peer.PostPolicyRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.rev200120.peers.peer.PrePolicyRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.rev200120.peers.peer.Stats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.rev200120.routers.Router;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.builder.DataContainerNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BmpRouterPeerImpl implements BmpRouterPeer {

    private static final Logger LOG = LoggerFactory.getLogger(BmpRouterPeerImpl.class);
    private static final String TIMESTAMP_SEC = "timestamp-sec";

    private static final QName PEER_ID_QNAME = create(Peer.QNAME, "peer-id").intern();
    private static final QName PEER_TYPE_QNAME = create(Peer.QNAME, "type");
    private static final QName PEER_ADDRESS_QNAME = create(Peer.QNAME, "address").intern();
    private static final QName PEER_AS_QNAME = create(Peer.QNAME, "as").intern();
    private static final QName PEER_BGP_ID_QNAME = create(Peer.QNAME, "bgp-id").intern();
    private static final QName PEER_DISTINGUISHER_QNAME = create(Peer.QNAME, "router-distinguisher").intern();
    private static final QName PEER_LOCAL_ADDRESS_QNAME = create(PeerSession.QNAME, "local-address").intern();
    private static final QName PEER_LOCAL_PORT_QNAME = create(PeerSession.QNAME, "local-port").intern();
    private static final QName PEER_REMOTE_PORT_QNAME = create(PeerSession.QNAME, "remote-port").intern();
    private static final QName PEER_STATUS_QNAME = create(PeerSession.QNAME, "status").intern();
    private static final QName PEER_UP_TIMESTAMP_QNAME = create(PeerSession.QNAME, TIMESTAMP_SEC).intern();
    private static final QName PEER_STATS_TIMESTAMP_QNAME = create(Stats.QNAME, TIMESTAMP_SEC).intern();
    private static final QName PEER_MIRROR_INFORMATION_QNAME = create(Mirrors.QNAME, "information").intern();
    private static final QName PEER_MIRROR_TIMESTAMP_QNAME = create(Mirrors.QNAME, TIMESTAMP_SEC).intern();

    private static final QName STAT0_QNAME = create(Stats.QNAME, "rejected-prefixes").intern();
    private static final QName STAT1_QNAME = create(Stats.QNAME, "duplicate-prefix-advertisements").intern();
    private static final QName STAT2_QNAME = create(Stats.QNAME, "duplicate-withdraws").intern();
    private static final QName STAT3_QNAME = create(Stats.QNAME, "invalidated-cluster-list-loop").intern();
    private static final QName STAT4_QNAME = create(Stats.QNAME, "invalidated-as-path-loop").intern();
    private static final QName STAT5_QNAME = create(Stats.QNAME, "invalidated-originator-id").intern();
    private static final QName STAT6_QNAME = create(Stats.QNAME, "invalidated-as-confed-loop").intern();
    private static final QName STAT7_QNAME = create(Stats.QNAME, "adj-ribs-in-routes");
    private static final QName STAT8_QNAME = create(Stats.QNAME, "loc-rib-routes");
    private static final QName STAT9_QNAME = create(Stats.QNAME, "per-afi-safi-adj-rib-in-routes").intern();
    private static final QName AF_QNAME = create(Stats.QNAME, "afi-safi").intern();
    private static final QName COUNT_QNAME = create(Stats.QNAME, "count").intern();
    private static final QName STAT10_QNAME = create(Stats.QNAME, "per-afi-safi-loc-rib-routes").intern();
    private static final QName STAT11_QNAME = create(Stats.QNAME, "updates-treated-as-withdraw").intern();
    private static final QName STAT13_QNAME = create(Stats.QNAME, "duplicate-updates").intern();

    private static final TablesKey DEFAULT_TABLE =
            new TablesKey(Ipv4AddressFamily.VALUE, UnicastSubsequentAddressFamily.VALUE);

    private static final InstanceIdentifier<PeerSession> PEER_SESSION_ID = InstanceIdentifier.builder(BmpMonitor.class)
            .child(Monitor.class)
            .child(Router.class)
            .child(Peer.class)
            .child(PeerSession.class).build();

    private static final InstanceIdentifier<SentOpen> SENT_OPEN_IID = PEER_SESSION_ID.child(SentOpen.class);

    private static final InstanceIdentifier<ReceivedOpen> RECEIVED_OPEN_IID = PEER_SESSION_ID.child(ReceivedOpen.class);

    private final DOMTransactionChain domTxChain;
    private final PeerId peerId;
    private final YangInstanceIdentifier peerYangIId;
    private final BmpRibInWriter prePolicyWriter;
    private final BmpRibInWriter postPolicyWriter;
    private final BindingDataObjectCodecTreeNode<SentOpen> sentOpenCodec;
    private final BindingDataObjectCodecTreeNode<ReceivedOpen> receivedOpenCodec;
    private boolean up = true;

    private BmpRouterPeerImpl(final DOMTransactionChain domTxChain, final YangInstanceIdentifier peersYangIId,
        final PeerId peerId, final RIBExtensionConsumerContext extensions, final PeerUpNotification peerUp,
        final BindingCodecTree tree) {
        this.domTxChain = requireNonNull(domTxChain);
        this.peerId = peerId;
        peerYangIId = YangInstanceIdentifier.builder(peersYangIId).nodeWithKey(Peer.QNAME, PEER_ID_QNAME,
                this.peerId.getValue()).build();
        sentOpenCodec = tree.getSubtreeCodec(SENT_OPEN_IID);
        receivedOpenCodec = tree.getSubtreeCodec(RECEIVED_OPEN_IID);

        final Set<TablesKey> peerTables = setPeerTables(peerUp.getReceivedOpen());
        final DOMDataTreeWriteTransaction wTx = this.domTxChain.newWriteOnlyTransaction();
        wTx.put(LogicalDatastoreType.OPERATIONAL, peerYangIId, createPeerEntry(peerUp));
        wTx.commit().addCallback(new FutureCallback<CommitInfo>() {
            @Override
            public void onSuccess(final CommitInfo result) {
                LOG.trace("Successful commit");
            }

            @Override
            public void onFailure(final Throwable trw) {
                LOG.error("Failed commit", trw);
            }
        }, MoreExecutors.directExecutor());
        prePolicyWriter = BmpRibInWriter.create(peerYangIId.node(PrePolicyRib.QNAME).node(BMP_TABLES_QNAME),
                this.domTxChain, extensions, peerTables, tree);
        postPolicyWriter = BmpRibInWriter.create(peerYangIId.node(PostPolicyRib.QNAME).node(BMP_TABLES_QNAME),
                this.domTxChain, extensions, peerTables, tree);
    }

    static BmpRouterPeer createRouterPeer(final DOMTransactionChain domTxChain,
            final YangInstanceIdentifier peersYangIId, final PeerUpNotification peerUp,
            final RIBExtensionConsumerContext extensions, final BindingCodecTree tree, final PeerId peerId) {
        return new BmpRouterPeerImpl(domTxChain, peersYangIId, peerId, extensions,
                peerUp, tree);
    }

    @Override
    public void onPeerMessage(final Notification<?> message) {
        if (message instanceof PeerDownNotification) {
            onPeerDown();
        } else if (message instanceof RouteMonitoringMessage) {
            onRouteMonitoring((RouteMonitoringMessage) message);
        } else if (message instanceof StatsReportsMessage) {
            onStatsReports((StatsReportsMessage) message);
        } else if (message instanceof RouteMirroringMessage) {
            onRouteMirror((RouteMirroringMessage) message);
        }
    }

    private void onRouteMonitoring(final RouteMonitoringMessage routeMonitoring) {
        if (up) {
            final AdjRibInType ribType = routeMonitoring.getPeerHeader().getAdjRibInType();
            switch (ribType) {
                case PrePolicy:
                    prePolicyWriter.onMessage(routeMonitoring.getUpdate());
                    break;
                case PostPolicy:
                    postPolicyWriter.onMessage(routeMonitoring.getUpdate());
                    break;
                default:
                    break;
            }
        }
    }

    private synchronized void onStatsReports(final StatsReportsMessage statsReports) {
        if (up) {
            final DOMDataTreeWriteTransaction wTx = domTxChain.newWriteOnlyTransaction();
            wTx.merge(LogicalDatastoreType.OPERATIONAL, peerYangIId.node(Stats.QNAME),
                    createStats(statsReports, statsReports.getPeerHeader().getTimestampSec()));
            wTx.commit().addCallback(new FutureCallback<CommitInfo>() {
                @Override
                public void onSuccess(final CommitInfo result) {
                    LOG.trace("Successful commit");
                }

                @Override
                public void onFailure(final Throwable trw) {
                    LOG.error("Failed commit", trw);
                }
            }, MoreExecutors.directExecutor());
        }
    }

    private synchronized void onRouteMirror(final RouteMirroringMessage mirror) {
        final DOMDataTreeWriteTransaction wTx = domTxChain.newWriteOnlyTransaction();
        wTx.merge(LogicalDatastoreType.OPERATIONAL, peerYangIId.node(Mirrors.QNAME),
                createMirrors(mirror, mirror.getPeerHeader().getTimestampSec()));
        wTx.commit().addCallback(new FutureCallback<CommitInfo>() {
            @Override
            public void onSuccess(final CommitInfo result) {
                LOG.trace("Successful commit");
            }

            @Override
            public void onFailure(final Throwable trw) {
                LOG.error("Failed commit", trw);
            }
        }, MoreExecutors.directExecutor());
    }

    private synchronized void onPeerDown() {
        final DOMDataTreeWriteTransaction wTx = domTxChain.newWriteOnlyTransaction();
        wTx.delete(LogicalDatastoreType.OPERATIONAL, peerYangIId);
        wTx.commit().addCallback(new FutureCallback<CommitInfo>() {
            @Override
            public void onSuccess(final CommitInfo result) {
                LOG.trace("Successful commit");
            }

            @Override
            public void onFailure(final Throwable trw) {
                LOG.error("Failed commit", trw);
            }
        }, MoreExecutors.directExecutor());
        close();
    }

    @Override
    public void close() {
        Preconditions.checkState(up, "Already closed.");
        up = false;
    }

    private static Set<TablesKey> setPeerTables(final ReceivedOpen open) {
        final Set<TablesKey> tables = Sets.newHashSet(DEFAULT_TABLE);
        for (final BgpParameters param : open.nonnullBgpParameters()) {
            for (final OptionalCapabilities optCapa : param.nonnullOptionalCapabilities()) {
                final CParameters cParam = optCapa.getCParameters();
                if (cParam != null) {
                    final CParameters1 augment = cParam.augmentation(CParameters1.class);
                    if (augment != null) {
                        final MultiprotocolCapability multi = augment.getMultiprotocolCapability();
                        if (multi != null) {
                            tables.add(new TablesKey(multi.getAfi(), multi.getSafi()));
                        }
                    }
                }
            }
        }
        return tables;
    }

    private ContainerNode createPeerSessionUp(final PeerUp peerUp, final Timestamp timestamp) {
        final DataContainerNodeBuilder<NodeIdentifier, ContainerNode> builder = Builders.containerBuilder()
                .withNodeIdentifier(new NodeIdentifier(PeerSession.QNAME))
                .withChild(ImmutableNodes.leafNode(PEER_LOCAL_ADDRESS_QNAME,
                        getStringIpAddress(peerUp.getLocalAddress())))
                .withChild(ImmutableNodes.leafNode(PEER_LOCAL_PORT_QNAME, peerUp
                        .getLocalPort().getValue()))
                .withChild(ImmutableNodes.leafNode(PEER_REMOTE_PORT_QNAME, peerUp
                        .getRemotePort().getValue()))
                .withChild(ImmutableNodes.leafNode(PEER_STATUS_QNAME, "up"))
                .withChild(ImmutableNodes.leafNode(PEER_UP_TIMESTAMP_QNAME,
                        timestamp.getValue()));
        if (receivedOpenCodec != null) {
            builder.withChild((ContainerNode) receivedOpenCodec.serialize(peerUp.getReceivedOpen()));
        }
        if (sentOpenCodec != null) {
            builder.withChild((ContainerNode) sentOpenCodec.serialize(peerUp.getSentOpen()));
        }
        return builder.build();
    }

    private MapEntryNode createPeerEntry(final PeerUpNotification peerUp) {
        final PeerHeader peerHeader = peerUp.getPeerHeader();
        final DataContainerNodeBuilder<NodeIdentifierWithPredicates, MapEntryNode> mapEntryBuilder =
                Builders.mapEntryBuilder()
                        .withNodeIdentifier(NodeIdentifierWithPredicates.of(Peer.QNAME, PEER_ID_QNAME,
                                peerId.getValue()))
                        .withChild(ImmutableNodes.leafNode(PEER_ID_QNAME, peerId.getValue()))
                        .withChild(ImmutableNodes.leafNode(PEER_TYPE_QNAME,
                                peerHeader.getType().name().toLowerCase(Locale.ENGLISH)))
                        .withChild(ImmutableNodes.leafNode(PEER_ADDRESS_QNAME,
                                getStringIpAddress(peerHeader.getAddress())))
                        .withChild(ImmutableNodes.leafNode(PEER_AS_QNAME, peerHeader.getAs().getValue()))
                        .withChild(ImmutableNodes.leafNode(PEER_BGP_ID_QNAME, peerHeader.getBgpId().getValue()))
                        .withChild(createPeerSessionUp(peerUp, peerHeader.getTimestampSec()))
                        .withChild(
                                Builders.containerBuilder().withNodeIdentifier(new NodeIdentifier(PrePolicyRib.QNAME))
                                        .withChild(ImmutableNodes.mapNodeBuilder(BMP_TABLES_QNAME).build()).build())
                        .withChild(
                                Builders.containerBuilder().withNodeIdentifier(new NodeIdentifier(PostPolicyRib.QNAME))
                                        .withChild(ImmutableNodes.mapNodeBuilder(BMP_TABLES_QNAME).build()).build());
        final PeerDistinguisher pd = peerHeader.getPeerDistinguisher();
        if (pd != null) {
            mapEntryBuilder.withChild(ImmutableNodes.leafNode(PEER_DISTINGUISHER_QNAME, pd.getRouteDistinguisher()));
        }
        return mapEntryBuilder.build();
    }

    private static ContainerNode createStats(final Stat stat, final Timestamp timestamp) {
        final DataContainerNodeBuilder<NodeIdentifier, ContainerNode> builder =
                Builders.containerBuilder().withNodeIdentifier(new NodeIdentifier(Stats.QNAME));
        builder.withChild(ImmutableNodes.leafNode(PEER_STATS_TIMESTAMP_QNAME, timestamp.getValue()));
        final Tlvs tlvs = stat.getTlvs();
        if (tlvs != null) {
            statsForTlvs(tlvs, builder);
        }
        return builder.build();
    }

    private static void statsForTlvs(final Tlvs tlvs,
            final DataContainerNodeBuilder<NodeIdentifier, ContainerNode> builder) {
        if (tlvs.getRejectedPrefixesTlv() != null) {
            builder.withChild(ImmutableNodes.leafNode(STAT0_QNAME,
                    tlvs.getRejectedPrefixesTlv().getCount().getValue()));
        }
        if (tlvs.getDuplicatePrefixAdvertisementsTlv() != null) {
            builder.withChild(ImmutableNodes.leafNode(STAT1_QNAME,
                    tlvs.getDuplicatePrefixAdvertisementsTlv().getCount().getValue()));
        }
        if (tlvs.getDuplicateWithdrawsTlv() != null) {
            builder.withChild(ImmutableNodes.leafNode(STAT2_QNAME,
                    tlvs.getDuplicateWithdrawsTlv().getCount().getValue()));
        }
        if (tlvs.getInvalidatedClusterListLoopTlv() != null) {
            builder.withChild(ImmutableNodes.leafNode(STAT3_QNAME,
                    tlvs.getInvalidatedClusterListLoopTlv().getCount().getValue()));
        }
        if (tlvs.getInvalidatedAsPathLoopTlv() != null) {
            builder.withChild(ImmutableNodes.leafNode(STAT4_QNAME,
                    tlvs.getInvalidatedAsPathLoopTlv().getCount().getValue()));
        }
        if (tlvs.getInvalidatedOriginatorIdTlv() != null) {
            builder.withChild(ImmutableNodes.leafNode(STAT5_QNAME,
                    tlvs.getInvalidatedOriginatorIdTlv().getCount().getValue()));
        }
        if (tlvs.getInvalidatedAsConfedLoopTlv() != null) {
            builder.withChild(ImmutableNodes.leafNode(STAT6_QNAME,
                    tlvs.getInvalidatedAsConfedLoopTlv().getCount().getValue()));
        }
        if (tlvs.getAdjRibsInRoutesTlv() != null) {
            builder.withChild(ImmutableNodes.leafNode(STAT7_QNAME,
                    tlvs.getAdjRibsInRoutesTlv().getCount().getValue()));
        }
        if (tlvs.getLocRibRoutesTlv() != null) {
            builder.withChild(ImmutableNodes.leafNode(STAT8_QNAME,
                    tlvs.getLocRibRoutesTlv().getCount().getValue()));
        }
        if (tlvs.getPerAfiSafiAdjRibInTlv() != null) {
            builder.withChild(Builders.containerBuilder().withNodeIdentifier(new NodeIdentifier(STAT9_QNAME))
                .withChild(ImmutableNodes.mapNodeBuilder(AF_QNAME).withChild(Builders.mapEntryBuilder()
                    .withChild(ImmutableNodes.leafNode(COUNT_QNAME,
                            tlvs.getPerAfiSafiAdjRibInTlv().getCount().getValue()))
                    .withNodeIdentifier(TablesUtil.toYangTablesKey(AF_QNAME,
                            Ipv4AddressFamily.VALUE, UnicastSubsequentAddressFamily.VALUE))
                    .build()).build()).build());
        }
        if (tlvs.getPerAfiSafiLocRibTlv() != null) {
            builder.withChild(Builders.containerBuilder().withNodeIdentifier(new NodeIdentifier(STAT10_QNAME))
                    .withChild(ImmutableNodes.mapNodeBuilder(AF_QNAME)
                            .withChild(Builders.mapEntryBuilder()
                                    .withChild(ImmutableNodes.leafNode(COUNT_QNAME,
                                            tlvs.getPerAfiSafiLocRibTlv().getCount().getValue()))
                                    .withNodeIdentifier(TablesUtil.toYangTablesKey(AF_QNAME,
                                            Ipv4AddressFamily.VALUE, UnicastSubsequentAddressFamily.VALUE))
                                    .build()).build()).build());
        }
        if (tlvs.getUpdatesTreatedAsWithdrawTlv() != null) {
            builder.withChild(ImmutableNodes.leafNode(STAT11_QNAME,
                    tlvs.getUpdatesTreatedAsWithdrawTlv().getCount().getValue()));
        }
        if (tlvs.getPrefixesTreatedAsWithdrawTlv() != null) {
            builder.withChild(ImmutableNodes.leafNode(STAT11_QNAME,
                    tlvs.getPrefixesTreatedAsWithdrawTlv().getCount().getValue()));
        }
        if (tlvs.getDuplicateUpdatesTlv() != null) {
            builder.withChild(ImmutableNodes.leafNode(STAT13_QNAME,
                    tlvs.getDuplicateUpdatesTlv().getCount().getValue()));
        }
    }

    private static ContainerNode createMirrors(final Mirror mirror, final Timestamp timestamp) {
        final DataContainerNodeBuilder<NodeIdentifier, ContainerNode> builder =
                Builders.containerBuilder().withNodeIdentifier(new NodeIdentifier(Mirrors.QNAME));
        builder.withChild(ImmutableNodes.leafNode(PEER_MIRROR_INFORMATION_QNAME, toDom(MirrorInformationCode.forValue(
                mirror.getTlvs().getMirrorInformationTlv().getCode().getIntValue()))));
        builder.withChild(ImmutableNodes.leafNode(PEER_MIRROR_TIMESTAMP_QNAME, timestamp.getValue()));
        return builder.build();
    }

    private static String toDom(final MirrorInformationCode informationCode) {
        switch (informationCode) {
            case ErroredPdu:
                return "errored-pdu";
            case MessageLost:
                return "message-lost";
            default:
                return null;
        }
    }

    private static String getStringIpAddress(final IpAddressNoZone ipAddress) {
        if (ipAddress.getIpv4AddressNoZone() != null) {
            return ipAddress.getIpv4AddressNoZone().getValue();
        }
        return ipAddress.getIpv6AddressNoZone().getValue();
    }

}
