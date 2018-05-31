/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.Arrays;
import java.util.Set;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChain;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.protocol.bgp.rib.impl.spi.PeerTransactionChain;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIB;
import org.opendaylight.protocol.bgp.rib.impl.state.BGPPeerStateImpl;
import org.opendaylight.protocol.bgp.rib.spi.IdentifierUtils;
import org.opendaylight.protocol.bgp.rib.spi.Peer;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryImportParameters;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPAfiSafiState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPErrorHandlingState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.ClusterIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractPeer extends BGPPeerStateImpl implements BGPRouteEntryImportParameters, TransactionChainListener,
        Peer, PeerTransactionChain {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractPeer.class);
    protected final RIB rib;
    final String name;
    final PeerRole peerRole;
    private final ClusterIdentifier clusterId;
    private final AsNumber localAs;
    @GuardedBy("this")
    DOMTransactionChain domChain;
    @GuardedBy("this")
    BindingTransactionChain bindingChain;
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
        this.domChain = this.rib.createPeerDOMChain(this);
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

    @Override
    public synchronized DOMTransactionChain getDomChain() {
        return this.domChain;
    }


    @Override
    public final synchronized void update(final InstanceIdentifier ribOutTarget, final Route route,
            final Attributes attributes) {
        if (this.bindingChain == null) {
            LOG.debug("Session closed, skip changes route {} to peer AdjRibsOut {}", ribOutTarget, getPeerId());
            return;
        }
        final WriteTransaction tx = this.bindingChain.newWriteOnlyTransaction();
        LOG.debug("Write route {} to peer AdjRibsOut {}", route, getPeerId());
        tx.put(LogicalDatastoreType.OPERATIONAL, ribOutTarget, route);
        tx.put(LogicalDatastoreType.OPERATIONAL, ribOutTarget.child(Attributes.class), attributes);
        tx.commit().addCallback(new FutureCallback<CommitInfo>() {
            @Override
            public void onSuccess(final CommitInfo result) {
                LOG.trace("Successful update commit");
            }

            @Override
            public void onFailure(final Throwable trw) {
                LOG.error("Failed update commit", trw);
            }
        }, MoreExecutors.directExecutor());

    }

    @Override
    public final synchronized FluentFuture<? extends CommitInfo> delete(final InstanceIdentifier ribOutTarget) {
        if (this.bindingChain == null) {
            LOG.debug("Session closed, skip changes route {} to peer AdjRibsOut {}", ribOutTarget, getPeerId());
            return CommitInfo.emptyFluentFuture();
        }
        final WriteTransaction tx = this.bindingChain.newWriteOnlyTransaction();
        LOG.trace("Removing {} from transaction for peer {}", ribOutTarget, getPeerId());
        tx.delete(LogicalDatastoreType.OPERATIONAL, ribOutTarget);
        final FluentFuture<? extends CommitInfo> future = tx.commit();
        future.addCallback(new FutureCallback<CommitInfo>() {
            @Override
            public void onSuccess(final CommitInfo result) {
                LOG.trace("Successful delete commit");
            }

            @Override
            public void onFailure(final Throwable trw) {
                LOG.error("Failed delete commit", trw);
            }
        }, MoreExecutors.directExecutor());
        return future;
    }
}
