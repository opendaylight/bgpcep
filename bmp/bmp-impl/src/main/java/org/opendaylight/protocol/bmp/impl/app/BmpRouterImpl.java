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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChain;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingCodecTree;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionConsumerContext;
import org.opendaylight.protocol.bmp.api.BmpSession;
import org.opendaylight.protocol.bmp.impl.spi.BmpRouter;
import org.opendaylight.protocol.bmp.impl.spi.BmpRouterPeer;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.OpenMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.InitiationMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.PeerDownNotification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.PeerHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.PeerUpNotification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.string.informations.StringInformation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.rev150512.RouterId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.rev150512.peers.Peer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.rev150512.routers.Router;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BmpRouterImpl implements BmpRouter, TransactionChainListener {

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
    private YangInstanceIdentifier routerYangIId;
    private YangInstanceIdentifier peersYangIId;

    public BmpRouterImpl(final RouterSessionManager sessionManager) {
        this.sessionManager = requireNonNull(sessionManager);
        this.domDataBroker = sessionManager.getDomDataBroker();
        this.domTxChain = this.domDataBroker.createTransactionChain(this);
        this.extensions = sessionManager.getExtensions();
        this.tree = sessionManager.getCodecTree();
    }

    @Override
    public void onSessionUp(final BmpSession session) {
        this.session = session;
        this.routerIp = InetAddresses.toAddrString(this.session.getRemoteAddress());
        this.routerId = new RouterId(Ipv4Util.getIpAddress(this.session.getRemoteAddress()));
        // check if this session is redundant
        if (!this.sessionManager.addSessionListener(this)) {
            LOG.warn("Redundant BMP session with remote router {} ({}) detected. This BMP session will be abandoned.",
                this.routerIp, this.session);
            this.close();
        } else {
            this.routerYangIId = YangInstanceIdentifier.builder(this.sessionManager.getRoutersYangIId())
                .nodeWithKey(Router.QNAME, ROUTER_ID_QNAME, this.routerIp).build();
            this.peersYangIId = YangInstanceIdentifier.builder(this.routerYangIId).node(Peer.QNAME).build();
            createRouterEntry();
            LOG.info("BMP session with remote router {} ({}) is up now.", this.routerIp, this.session);
        }
    }

    @Override
    public void onSessionDown(final Exception e) {
        // we want to tear down as we want to do clean up like closing the transaction chain, etc.
        // even when datastore is not writable (routerYangIId == null / redundant session)
        tearDown();
    }

    @Override
    public void onMessage(final Notification message) {
        if (message instanceof InitiationMessage) {
            onInitiate((InitiationMessage) message);
        } else if (message instanceof PeerUpNotification) {
            onPeerUp((PeerUpNotification) message);
        } else if (message instanceof PeerHeader) {
            delegateToPeer(message);
        }
    }

    @Override
    public RouterId getRouterId() {
        return this.routerId;
    }

    @Override
    public synchronized void close() {
        if (this.session != null) {
            try {
                this.session.close();
            } catch (final Exception e) {
                LOG.error("Fail to close session.", e);
            }
        }
    }

    @GuardedBy("this")
    private synchronized void tearDown() {
        // the session has been teared down before
        if (this.session == null) {
            return;
        }
        // we want to display remote router's IP here, as sometimes this.session.close() is already
        // invoked before tearDown(), and session channel is null in this case, which leads to unuseful
        // log information
        LOG.info("BMP Session with remote router {} ({}) went down.", this.routerIp, this.session);
        this.session = null;
        final Iterator<BmpRouterPeer> it = this.peers.values().iterator();
        try {
            while (it.hasNext()) {
                it.next().close();
                it.remove();
            }
            this.domTxChain.close();
        } catch(final Exception e) {
            LOG.error("Failed to properly close BMP application.", e);
        } finally {
            // remove session only when session is valid, otherwise
            // we would remove the original valid session when a redundant connection happens
            // as the routerId is the same for both connection
            if (isDatastoreWritable()) {
                try {
                    // it means the session was closed before it was written to datastore
                    final DOMDataWriteTransaction wTx = this.domDataBroker.newWriteOnlyTransaction();
                    wTx.delete(LogicalDatastoreType.OPERATIONAL, this.routerYangIId);
                    wTx.submit().checkedGet();
                } catch (final TransactionCommitFailedException e) {
                    LOG.error("Failed to remove BMP router data from DS.", e);
                }
                this.sessionManager.removeSessionListener(this);
            }
        }
    }

    @Override
    public void onTransactionChainFailed(final TransactionChain<?, ?> chain, final AsyncTransaction<?, ?> transaction,
        final Throwable cause) {
        LOG.error("Transaction chain failed.", cause);
    }

    @Override
    public void onTransactionChainSuccessful(final TransactionChain<?, ?> chain) {
        LOG.debug("Transaction chain {} successfully.", chain);
    }

    private boolean isDatastoreWritable() {
        return (this.routerYangIId != null);
    }

    private synchronized void createRouterEntry() {
        Preconditions.checkState(isDatastoreWritable());
        final DOMDataWriteTransaction wTx = this.domTxChain.newWriteOnlyTransaction();
        wTx.put(LogicalDatastoreType.OPERATIONAL, this.routerYangIId,
                Builders.mapEntryBuilder()
                .withNodeIdentifier(new NodeIdentifierWithPredicates(Router.QNAME, ROUTER_ID_QNAME, this.routerIp))
                .withChild(ImmutableNodes.leafNode(ROUTER_ID_QNAME, this.routerIp))
                .withChild(ImmutableNodes.leafNode(ROUTER_STATUS_QNAME, DOWN))
                .withChild(ImmutableNodes.mapNodeBuilder(Peer.QNAME).build()).build());
        wTx.submit();
    }

    private synchronized void onInitiate(final InitiationMessage initiation) {
        Preconditions.checkState(isDatastoreWritable());
        final DOMDataWriteTransaction wTx = this.domTxChain.newWriteOnlyTransaction();
        wTx.merge(LogicalDatastoreType.OPERATIONAL, this.routerYangIId,
                Builders.mapEntryBuilder()
                .withNodeIdentifier(new NodeIdentifierWithPredicates(Router.QNAME, ROUTER_ID_QNAME, this.routerIp))
                .withChild(ImmutableNodes.leafNode(ROUTER_NAME_QNAME, initiation.getTlvs().getNameTlv().getName()))
                .withChild(ImmutableNodes.leafNode(ROUTER_DESCRIPTION_QNAME, initiation.getTlvs().getDescriptionTlv().getDescription()))
                .withChild(ImmutableNodes.leafNode(ROUTER_INFO_QNAME, getStringInfo(initiation.getTlvs().getStringInformation())))
                .withChild(ImmutableNodes.leafNode(ROUTER_STATUS_QNAME, UP)).build());
        wTx.submit();
    }

    private void onPeerUp(final PeerUpNotification peerUp) {
        final PeerId peerId = getPeerIdFromOpen(peerUp.getReceivedOpen());
        if (!getPeer(peerId).isPresent()) {
            final BmpRouterPeer peer = BmpRouterPeerImpl.createRouterPeer(this.domTxChain, this.peersYangIId, peerUp,
                this.extensions, this.tree, peerId);
            this.peers.put(peerId, peer);
            LOG.debug("Router {}: Peer {} goes up.", this.routerIp, peerId.getValue());
        } else {
            LOG.debug("Peer: {} for Router: {} already exists.", peerId.getValue(), this.routerIp);
        }
    }

    private void delegateToPeer(final Notification perPeerMessage) {
        final PeerId peerId = getPeerId((PeerHeader) perPeerMessage);
        final Optional<BmpRouterPeer> maybePeer = getPeer(peerId);
        if (maybePeer.isPresent()) {
            maybePeer.get().onPeerMessage(perPeerMessage);
            if (perPeerMessage instanceof PeerDownNotification) {
                this.peers.remove(peerId);
                LOG.debug("Router {}: Peer {} removed.", this.routerIp, peerId.getValue());
            }
        } else {
            LOG.debug("Peer: {} for Router: {} was not found.", peerId.getValue(), this.routerIp);
        }
    }

    private Optional<BmpRouterPeer> getPeer(final PeerId peerId) {
        return Optional.ofNullable(this.peers.get(peerId));
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
