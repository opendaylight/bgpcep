/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bmp.impl.app;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.net.InetAddresses;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChain;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionConsumerContext;
import org.opendaylight.protocol.bmp.api.BmpSession;
import org.opendaylight.protocol.bmp.impl.spi.BmpRouter;
import org.opendaylight.protocol.bmp.impl.spi.BmpRouterPeer;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.OpenMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.InitiationMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.PeerDownNotification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.PeerHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.PeerUpNotification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.string.informations.StringInformation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.rev150512.RouterId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.rev150512.peers.Peer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.rev150512.routers.Router;
import org.opendaylight.yangtools.binding.data.codec.api.BindingCodecTree;
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
        this.sessionManager = Preconditions.checkNotNull(sessionManager);
        this.domDataBroker = sessionManager.getDomDataBroker();
        this.domTxChain = sessionManager.getDomDataBroker().createTransactionChain(this);
        this.extensions = sessionManager.getExtensions();
        this.tree = sessionManager.getCodecTree();
    }

    @Override
    public void onSessionUp(final BmpSession session) {
        this.session = session;
        this.routerIp = InetAddresses.toAddrString(session.getRemoteAddress());
        this.routerId = new RouterId(Ipv4Util.getIpAddress(session.getRemoteAddress()));
        this.routerYangIId = YangInstanceIdentifier.builder(this.sessionManager.getRoutersYangIId()).nodeWithKey(Router.QNAME,
                ROUTER_ID_QNAME, this.routerIp).build();
        this.peersYangIId = YangInstanceIdentifier.builder(routerYangIId).node(Peer.QNAME).build();
        createRouterEntry();
        this.sessionManager.addSessionListener(this);
    }

    @Override
    public void onSessionDown(final BmpSession session, final Exception e) {
        LOG.info("Session {} went down.", session);
        tearDown();
    }

    @Override
    public void onMessage(final BmpSession session, final Notification message) {
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
        return routerId;
    }

    @Override
    public synchronized void close() throws Exception {
        if (this.session != null) {
            this.session.close();
        }
    }

    @GuardedBy("this")
    private synchronized void tearDown() {
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
            try {
                final DOMDataWriteTransaction wTx = this.domDataBroker.newWriteOnlyTransaction();
                wTx.delete(LogicalDatastoreType.OPERATIONAL, this.routerYangIId);
                wTx.submit().checkedGet();
            } catch (final TransactionCommitFailedException e) {
                LOG.error("Failed to remove BMP router data from DS.", e);
            }
        }
        this.sessionManager.removeSessionListener(this);
    }

    @Override
    public void onTransactionChainFailed(final TransactionChain<?, ?> chain, final AsyncTransaction<?, ?> transaction, final Throwable cause) {
        LOG.error("Transaction chain failed.", cause);
    }

    @Override
    public void onTransactionChainSuccessful(final TransactionChain<?, ?> chain) {
        LOG.debug("Transaction chain {} successfull.", chain);
    }

    private void createRouterEntry() {
        final DOMDataWriteTransaction wTx = this.domTxChain.newWriteOnlyTransaction();
        wTx.put(LogicalDatastoreType.OPERATIONAL, this.routerYangIId,
                Builders.mapEntryBuilder()
                .withNodeIdentifier(new NodeIdentifierWithPredicates(Router.QNAME, ROUTER_ID_QNAME, this.routerIp))
                .withChild(ImmutableNodes.leafNode(ROUTER_ID_QNAME, routerIp))
                .withChild(ImmutableNodes.leafNode(ROUTER_STATUS_QNAME, DOWN))
                .withChild(ImmutableNodes.mapNodeBuilder(Peer.QNAME).build()).build());
        wTx.submit();
    }

    private void onInitiate(final InitiationMessage initiation) {
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
            final BmpRouterPeer peer = BmpRouterPeerImpl.createRouterPeer(this.domTxChain, this.peersYangIId, peerUp, this.extensions, this.tree, peerId);
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
        return Optional.fromNullable(this.peers.get(peerId));
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
