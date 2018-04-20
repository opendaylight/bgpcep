/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChain;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIB;
import org.opendaylight.protocol.bgp.rib.impl.state.BGPPeerStateImpl;
import org.opendaylight.protocol.bgp.rib.spi.IdentifierUtils;
import org.opendaylight.protocol.bgp.rib.spi.Peer;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryImportParameters;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPAfiSafiState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPErrorHandlingState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.ClusterIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractPeer extends BGPPeerStateImpl implements BGPRouteEntryImportParameters, TransactionChainListener,
        Peer {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractPeer.class);
    protected final RIB rib;
    final String name;
    final PeerRole peerRole;
    private final ClusterIdentifier clusterId;
    private final AsNumber localAs;
    byte[] rawIdentifier;
    @GuardedBy("this")
    PeerId peerId;

    AbstractPeer(
            final RIB rib,
            final String peerName,
            final String groupId,
            final PeerRole role,
            @Nullable final ClusterIdentifier clusterId,
            @Nullable final AsNumber localAs,
            final IpAddress neighborAddress,
            final Set<TablesKey> afiSafisAdvertized,
            final Set<TablesKey> afiSafisGracefulAdvertized) {
        super(rib.getInstanceIdentifier(), groupId, neighborAddress, afiSafisAdvertized, afiSafisGracefulAdvertized);
        this.name = peerName;
        this.peerRole = role;
        this.clusterId = clusterId;
        this.localAs = localAs;
        this.rib = rib;
    }

    AbstractPeer(
            final RIB rib,
            final String peerName,
            final String groupId,
            final PeerRole role,
            final IpAddress neighborAddress,
            final Set<TablesKey> afiSafisGracefulAdvertized) {
        this(rib, peerName, groupId, role, null, null, neighborAddress,
                rib.getLocalTablesKeys(), afiSafisGracefulAdvertized);
    }

    @SuppressFBWarnings(value = "NP_NONNULL_PARAM_VIOLATION", justification = "Unrecognised NullableDecl")
    final synchronized ListenableFuture<Void> removePeer(
            @Nonnull final DOMTransactionChain chain,
            @Nullable final YangInstanceIdentifier peerPath) {
        if (peerPath != null) {
            LOG.info("AdjRibInWriter closed per Peer {} removed", peerPath);
            final DOMDataWriteTransaction tx = chain.newWriteOnlyTransaction();
            tx.delete(LogicalDatastoreType.OPERATIONAL, peerPath);
            final ListenableFuture<Void> future = tx.submit();
            Futures.addCallback(future, new FutureCallback<Void>() {
                @Override
                public void onSuccess(final Void result) {
                    LOG.debug("Peer {} removed", peerPath);
                }

                @Override
                public void onFailure(final Throwable t) {
                    LOG.error("Failed to remove Peer {}", peerPath, t);
                }
            }, MoreExecutors.directExecutor());
            return future;
        }
        return Futures.immediateFuture(null);
    }

    synchronized YangInstanceIdentifier createPeerPath() {
        return this.rib.getYangRibId().node(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib
                .rev180329.bgp.rib.rib.Peer.QNAME).node(IdentifierUtils.domPeerId(this.peerId));
    }

    @Override
    public synchronized final PeerId getPeerId() {
        return this.peerId;
    }

    @Override
    public final PeerRole getRole() {
        return this.peerRole;
    }

    @Override
    public final synchronized byte[] getRawIdentifier() {
        return Arrays.copyOf(this.rawIdentifier, this.rawIdentifier.length);
    }

    @Override
    public final PeerRole getFromPeerRole() {
        return getRole();
    }

    @Override
    public final PeerId getFromPeerId() {
        return getPeerId();
    }

    @Override
    public final ClusterIdentifier getFromClusterId() {
        return getClusterId();
    }

    @Override
    public final void onTransactionChainSuccessful(final TransactionChain<?, ?> chain) {
        LOG.debug("Transaction chain {} successful.", chain);
    }

    @Override
    public final BGPErrorHandlingState getBGPErrorHandlingState() {
        return this;
    }

    @Override
    public final BGPAfiSafiState getBGPAfiSafiState() {
        return this;
    }

    @Override
    public final AsNumber getFromPeerLocalAs() {
        return getLocalAs();
    }

    @Override
    public final String getName() {
        return this.name;
    }

    @Override
    public final ClusterIdentifier getClusterId() {
        return this.clusterId;
    }

    @Override
    public final AsNumber getLocalAs() {
        return this.localAs;
    }
}
