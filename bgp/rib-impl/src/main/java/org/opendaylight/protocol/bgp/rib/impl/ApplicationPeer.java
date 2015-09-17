/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import com.google.common.net.InetAddresses;
import java.util.Arrays;
import java.util.Collection;
import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChain;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.ApplicationRibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.rib.Peer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.rib.peer.AdjRibIn;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.Tables;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Application Peer is a special case of BGP peer. It serves as an interface
 * for user to advertise user routes to ODL and through ODL to other BGP peers.
 *
 * This peer has it's own RIB, where it stores all user routes. This RIB is
 * located in configurational datastore. Routes are added through RESTCONF.
 *
 * They are then processed as routes from any other peer, through AdjRib,
 * EffectiveRib,LocRib and if they are advertised further, through AdjRibOut.
 *
 * For purposed of import policies such as Best Path Selection, application
 * peer needs to have a BGP-ID that is configurable.
 */
public class ApplicationPeer implements AutoCloseable, org.opendaylight.protocol.bgp.rib.spi.Peer, DOMDataTreeChangeListener, TransactionChainListener {

    private static final Logger LOG = LoggerFactory.getLogger(ApplicationPeer.class);

    private final byte[] rawIdentifier;
    private final RIBImpl targetRib;
    private final String name;
    private final YangInstanceIdentifier adjRibsInId;
    private final DOMTransactionChain chain;
    private final DOMTransactionChain writerChain;

    private AdjRibInWriter writer;

    public ApplicationPeer(final ApplicationRibId applicationRibId, final Ipv4Address ipAddress, final RIBImpl targetRib) {
        this.name = applicationRibId.getValue().toString();
        this.targetRib = Preconditions.checkNotNull(targetRib);
        this.rawIdentifier = InetAddresses.forString(ipAddress.getValue()).getAddress();
        final NodeIdentifierWithPredicates peerId = IdentifierUtils.domPeerId(RouterIds.createPeerId(ipAddress));
        this.adjRibsInId = this.targetRib.getYangRibId().node(Peer.QNAME).node(peerId).node(AdjRibIn.QNAME).node(Tables.QNAME);
        this.chain = this.targetRib.createPeerChain(this);
        this.writerChain = this.targetRib.createPeerChain(this);
        this.writer = AdjRibInWriter.create(this.targetRib.getYangRibId(), PeerRole.Internal, this.writerChain);
        // FIXME: set to true, once it's fixed how to skip advertising routes back to AppPeer
        this.writer = this.writer.transform(RouterIds.createPeerId(ipAddress), this.targetRib.getRibSupportContext(), this.targetRib.getLocalTablesKeys(), false);
    }

    /**
     * Routes come from application RIB that is identified by (configurable) name.
     * Each route is pushed into AdjRibsInWriter with it's whole context. In this
     * method, it doesn't matter if the routes are removed or added, this will
     * be determined in LocRib.
     */
    @Override
    public void onDataTreeChanged(final Collection<DataTreeCandidate> changes) {
        final DOMDataWriteTransaction tx = this.chain.newWriteOnlyTransaction();
        LOG.debug("Received data change to ApplicationRib {}", changes);
        for (final DataTreeCandidate tc : changes) {
            LOG.debug("Modification Type {}", tc.getRootNode().getModificationType());
            final YangInstanceIdentifier path = tc.getRootPath();
            final PathArgument lastArg = path.getLastPathArgument();
            Verify.verify(lastArg instanceof NodeIdentifierWithPredicates, "Unexpected type %s in path %s", lastArg.getClass(), path);
            final NodeIdentifierWithPredicates tableKey = (NodeIdentifierWithPredicates) lastArg;
            for (final DataTreeCandidateNode child : tc.getRootNode().getChildNodes()) {
                final YangInstanceIdentifier tableId = this.adjRibsInId.node(tableKey).node(child.getIdentifier());
                if (child.getDataAfter().isPresent()) {
                    LOG.trace("App peer -> AdjRibsIn path : {}", tableId);
                    LOG.trace("App peer -> AdjRibsIn data : {}", child.getDataAfter().get());
                    tx.put(LogicalDatastoreType.OPERATIONAL, tableId, child.getDataAfter().get());
                }
            }
        }
        tx.submit();
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void close() {
        this.writer.cleanTables(this.targetRib.getLocalTablesKeys());
        this.chain.close();
        this.writerChain.close();
    }

    @Override
    public byte[] getRawIdentifier() {
        return Arrays.copyOf(this.rawIdentifier, this.rawIdentifier.length);
    }

    @Override
    public void onTransactionChainFailed(final TransactionChain<?, ?> chain, final AsyncTransaction<?, ?> transaction,
        final Throwable cause) {
        LOG.error("Transaction chain failed.", cause);
    }

    @Override
    public void onTransactionChainSuccessful(final TransactionChain<?, ?> chain) {
        LOG.debug("Transaction chain {} successfull.", chain);
    }
}
