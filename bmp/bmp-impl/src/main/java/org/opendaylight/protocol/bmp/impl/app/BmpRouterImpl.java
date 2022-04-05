/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bmp.impl.app;

import static java.util.Objects.requireNonNull;

import com.google.common.base.Preconditions;
import com.google.common.net.InetAddresses;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.checkerframework.checker.lock.qual.Holding;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingCodecTree;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMDataTreeTransaction;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.mdsal.dom.api.DOMTransactionChain;
import org.opendaylight.mdsal.dom.api.DOMTransactionChainListener;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionConsumerContext;
import org.opendaylight.protocol.bmp.api.BmpSession;
import org.opendaylight.protocol.bmp.impl.spi.BmpRouter;
import org.opendaylight.protocol.bmp.impl.spi.BmpRouterPeer;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.OpenMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.InitiationMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.PeerDownNotification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.PeerHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.PeerUpNotification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.string.informations.StringInformation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.rev200120.RouterId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.rev200120.peers.Peer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.rev200120.routers.Router;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BmpRouterImpl implements BmpRouter, DOMTransactionChainListener {

    private static final Logger LOG = LoggerFactory.getLogger(BmpRouterImpl.class);

    private static final QName ROUTER_ID_QNAME = QName.create(Router.QNAME, "router-id").intern();
    private static final QName ROUTER_STATUS_QNAME = QName.create(Router.QNAME, "status").intern();
    private static final QName ROUTER_NAME_QNAME = QName.create(Router.QNAME, "name").intern();
    private static final QName ROUTER_DESCRIPTION_QNAME = QName.create(Router.QNAME, "description").intern();
    private static final QName ROUTER_INFO_QNAME = QName.create(Router.QNAME, "info").intern();
    private static final String UP = "up";
    private static final String DOWN = "down";

    private final RouterSessionManager sessionManager;
    @GuardedBy("this")
    private final Map<PeerId, BmpRouterPeer> peers = new HashMap<>();
    private final DOMTransactionChain domTxChain;
    private final DOMDataBroker domDataBroker;
    private final RIBExtensionConsumerContext extensions;
    private final BindingCodecTree tree;
    private BmpSession session;
    private RouterId routerId;
    private String routerIp;
    @GuardedBy("this")
    private YangInstanceIdentifier routerYangIId;
    @GuardedBy("this")
    private YangInstanceIdentifier peersYangIId;

    public BmpRouterImpl(final RouterSessionManager sessionManager) {
        this.sessionManager = requireNonNull(sessionManager);
        domDataBroker = sessionManager.getDomDataBroker();
        domTxChain = domDataBroker.createMergingTransactionChain(this);
        extensions = sessionManager.getExtensions();
        tree = sessionManager.getCodecTree();
    }

    @Override
    public synchronized void onSessionUp(final BmpSession psession) {
        session = psession;
        routerIp = InetAddresses.toAddrString(session.getRemoteAddress());
        routerId = new RouterId(Ipv4Util.getIpAddress(session.getRemoteAddress()));
        // check if this session is redundant
        if (!sessionManager.addSessionListener(this)) {
            LOG.warn("Redundant BMP session with remote router {} ({}) detected. This BMP session will be abandoned.",
                routerIp, session);
            this.close();
        } else {
            routerYangIId = YangInstanceIdentifier.builder(sessionManager.getRoutersYangIId())
                .nodeWithKey(Router.QNAME, ROUTER_ID_QNAME, routerIp).build();
            peersYangIId = YangInstanceIdentifier.builder(routerYangIId).node(Peer.QNAME).build();
            createRouterEntry();
            LOG.info("BMP session with remote router {} ({}) is up now.", routerIp, session);
        }
    }

    @Override
    public synchronized void onSessionDown(final Exception exception) {
        // we want to tear down as we want to do clean up like closing the transaction chain, etc.
        // even when datastore is not writable (routerYangIId == null / redundant session)
        tearDown();
    }

    @Override
    public void onMessage(final Notification<?> message) {
        if (message instanceof InitiationMessage) {
            onInitiate((InitiationMessage) message);
        } else if (message instanceof PeerUpNotification) {
            onPeerUp((PeerUpNotification) message);
        } else if (message instanceof PeerHeader) {
            delegateToPeer(message);
        }
    }

    @Override
    public synchronized RouterId getRouterId() {
        return routerId;
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public synchronized void close() {
        if (session != null) {
            try {
                session.close();
            } catch (final Exception exc) {
                LOG.error("Fail to close session.", exc);
            }
        }
    }

    @Holding("this")
    @SuppressWarnings("checkstyle:IllegalCatch")
    private synchronized void tearDown() {
        // the session has been teared down before
        if (session == null) {
            return;
        }
        // we want to display remote router's IP here, as sometimes this.session.close() is already
        // invoked before tearDown(), and session channel is null in this case, which leads to unuseful
        // log information
        LOG.info("BMP Session with remote router {} ({}) went down.", routerIp, session);
        session = null;
        final Iterator<BmpRouterPeer> it = peers.values().iterator();
        try {
            while (it.hasNext()) {
                it.next().close();
                it.remove();
            }
            domTxChain.close();
        } catch (final Exception e) {
            LOG.error("Failed to properly close BMP application.", e);
        } finally {
            // remove session only when session is valid, otherwise
            // we would remove the original valid session when a redundant connection happens
            // as the routerId is the same for both connection
            if (isDatastoreWritable()) {
                try {
                    // it means the session was closed before it was written to datastore
                    final DOMDataTreeWriteTransaction wTx = domDataBroker.newWriteOnlyTransaction();
                    wTx.delete(LogicalDatastoreType.OPERATIONAL, routerYangIId);
                    wTx.commit().get();
                } catch (final InterruptedException | ExecutionException e) {
                    LOG.error("Failed to remove BMP router data from DS.", e);
                }
                sessionManager.removeSessionListener(this);
            }
        }
    }

    @Override
    public synchronized void onTransactionChainFailed(final DOMTransactionChain chain,
        final DOMDataTreeTransaction transaction, final Throwable cause) {
        LOG.error("Transaction chain failed.", cause);
    }

    @Override
    public void onTransactionChainSuccessful(final DOMTransactionChain chain) {
        LOG.debug("Transaction chain {} successfully.", chain);
    }

    private synchronized boolean isDatastoreWritable() {
        return routerYangIId != null;
    }

    private synchronized void createRouterEntry() {
        Preconditions.checkState(isDatastoreWritable());
        final DOMDataTreeWriteTransaction wTx = domTxChain.newWriteOnlyTransaction();
        wTx.put(LogicalDatastoreType.OPERATIONAL, routerYangIId,
                Builders.mapEntryBuilder()
                .withNodeIdentifier(NodeIdentifierWithPredicates.of(Router.QNAME, ROUTER_ID_QNAME, routerIp))
                .withChild(ImmutableNodes.leafNode(ROUTER_ID_QNAME, routerIp))
                .withChild(ImmutableNodes.leafNode(ROUTER_STATUS_QNAME, DOWN))
                .withChild(ImmutableNodes.mapNodeBuilder(Peer.QNAME).build()).build());
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

    private synchronized void onInitiate(final InitiationMessage initiation) {
        Preconditions.checkState(isDatastoreWritable());
        final DOMDataTreeWriteTransaction wTx = domTxChain.newWriteOnlyTransaction();
        wTx.merge(LogicalDatastoreType.OPERATIONAL, routerYangIId,
                Builders.mapEntryBuilder()
                .withNodeIdentifier(NodeIdentifierWithPredicates.of(Router.QNAME, ROUTER_ID_QNAME, routerIp))
                .withChild(ImmutableNodes.leafNode(ROUTER_NAME_QNAME, initiation.getTlvs().getNameTlv().getName()))
                .withChild(ImmutableNodes.leafNode(ROUTER_DESCRIPTION_QNAME, initiation.getTlvs().getDescriptionTlv()
                        .getDescription()))
                .withChild(ImmutableNodes.leafNode(ROUTER_INFO_QNAME, getStringInfo(initiation.getTlvs()
                        .getStringInformation())))
                .withChild(ImmutableNodes.leafNode(ROUTER_STATUS_QNAME, UP)).build());
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

    private synchronized void onPeerUp(final PeerUpNotification peerUp) {
        final PeerId peerId = getPeerIdFromOpen(peerUp.getReceivedOpen());
        if (!getPeer(peerId).isPresent()) {
            final BmpRouterPeer peer = BmpRouterPeerImpl.createRouterPeer(domTxChain, peersYangIId, peerUp,
                extensions, tree, peerId);
            peers.put(peerId, peer);
            LOG.debug("Router {}: Peer {} goes up.", routerIp, peerId.getValue());
        } else {
            LOG.debug("Peer: {} for Router: {} already exists.", peerId.getValue(), routerIp);
        }
    }

    private synchronized void delegateToPeer(final Notification<?> perPeerMessage) {
        final PeerId peerId = getPeerId((PeerHeader) perPeerMessage);
        final Optional<BmpRouterPeer> maybePeer = getPeer(peerId);
        if (maybePeer.isPresent()) {
            maybePeer.get().onPeerMessage(perPeerMessage);
            if (perPeerMessage instanceof PeerDownNotification) {
                peers.remove(peerId);
                LOG.debug("Router {}: Peer {} removed.", routerIp, peerId.getValue());
            }
        } else {
            LOG.debug("Peer: {} for Router: {} was not found.", peerId.getValue(), routerIp);
        }
    }

    private Optional<BmpRouterPeer> getPeer(final PeerId peerId) {
        return Optional.ofNullable(peers.get(peerId));
    }

    private static PeerId getPeerId(final PeerHeader peerHeader) {
        return new PeerId(peerHeader.getPeerHeader().getBgpId().getValue());
    }

    private static PeerId getPeerIdFromOpen(final OpenMessage open) {
        return new PeerId(open.getBgpIdentifier().getValue());
    }

    private static String getStringInfo(final List<StringInformation> info) {
        final StringBuilder builder = new StringBuilder();
        if (info != null) {
            for (final StringInformation string : info) {
                if (string.getStringTlv() != null) {
                    builder.append(string.getStringTlv().getStringInfo());
                    builder.append(";");
                }
            }
        }
        return builder.toString();
    }

}
