/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChain;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.protocol.bgp.rib.DefaultRibReference;
import org.opendaylight.protocol.bgp.rib.impl.spi.AdjRIBsOut;
import org.opendaylight.protocol.bgp.rib.impl.spi.AdjRIBsOutRegistration;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPDispatcher;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIB;
import org.opendaylight.protocol.bgp.rib.spi.AbstractAdjRIBs;
import org.opendaylight.protocol.bgp.rib.spi.AdjRIBsIn;
import org.opendaylight.protocol.bgp.rib.spi.BGPObjectComparator;
import org.opendaylight.protocol.bgp.rib.spi.Peer;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionConsumerContext;
import org.opendaylight.protocol.framework.ReconnectStrategyFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.UpdateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.Nlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.PathAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.PathAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.WithdrawnRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.PathAttributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.PathAttributes2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.destination.destination.type.DestinationIpv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.destination.destination.type.destination.ipv4._case.DestinationIpv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.mp.unreach.nlri.WithdrawnRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.BgpRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.BgpRibBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.RibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.Rib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.RibBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.RibKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.rib.LocRibBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ThreadSafe
public final class RIBImpl extends DefaultRibReference implements AutoCloseable, RIB, TransactionChainListener {
    private static final Logger LOG = LoggerFactory.getLogger(RIBImpl.class);
    private static final Update EOR = new UpdateBuilder().build();
    private static final TablesKey IPV4_UNICAST_TABLE = new TablesKey(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class);
    private final ConcurrentMap<Peer, AdjRIBsOut> ribOuts = new ConcurrentHashMap<>();
    private final ReconnectStrategyFactory tcpStrategyFactory;
    private final ReconnectStrategyFactory sessionStrategyFactory;
    private final BGPObjectComparator comparator;
    private final BGPDispatcher dispatcher;
    private final BindingTransactionChain chain;
    private final AsNumber localAs;
    private final Ipv4Address bgpIdentifier;
    private final List<BgpTableType> localTables;
    private final RIBTables tables;
    private final BlockingQueue<Peer> peers;
    private final Thread scheduler = new Thread(new Runnable() {

        @Override
        public void run() {
            try {
                final Peer peer = RIBImpl.this.peers.take();
                LOG.debug("Advertizing loc-rib to new peer {}.", peer);
                for (final BgpTableType key : RIBImpl.this.localTables) {
                    synchronized (RIBImpl.this) {
                        final AdjRIBsTransactionImpl trans = new AdjRIBsTransactionImpl(RIBImpl.this.ribOuts, RIBImpl.this.comparator, RIBImpl.this.chain.newWriteOnlyTransaction());
                        final AbstractAdjRIBs<?, ?, ?> adj = (AbstractAdjRIBs<?, ?, ?>) RIBImpl.this.tables.get(new TablesKey(key.getAfi(), key.getSafi()));
                        adj.addAllEntries(trans);
                        Futures.addCallback(trans.commit(), new FutureCallback<Void>() {
                            @Override
                            public void onSuccess(final Void result) {
                                LOG.trace("Advertizing {} to peer {} committed successfully", key.getAfi(), peer);
                            }
                            @Override
                            public void onFailure(final Throwable t) {
                                LOG.error("Failed to update peer {} with RIB {}", peer, t);
                            }
                        });
                    }
                }
            } catch (final InterruptedException e) {

            }
        }
    });

    public RIBImpl(final RibId ribId, final AsNumber localAs, final Ipv4Address localBgpId, final RIBExtensionConsumerContext extensions,
        final BGPDispatcher dispatcher, final ReconnectStrategyFactory tcpStrategyFactory,
        final ReconnectStrategyFactory sessionStrategyFactory, final DataBroker dps, final List<BgpTableType> localTables) {
        super(InstanceIdentifier.builder(BgpRib.class).child(Rib.class, new RibKey(Preconditions.checkNotNull(ribId))).toInstance());
        this.chain = dps.createTransactionChain(this);
        this.localAs = Preconditions.checkNotNull(localAs);
        this.comparator = new BGPObjectComparator(localAs);
        this.bgpIdentifier = Preconditions.checkNotNull(localBgpId);
        this.dispatcher = Preconditions.checkNotNull(dispatcher);
        this.sessionStrategyFactory = Preconditions.checkNotNull(sessionStrategyFactory);
        this.tcpStrategyFactory = Preconditions.checkNotNull(tcpStrategyFactory);
        this.localTables = ImmutableList.copyOf(localTables);
        this.tables = new RIBTables(extensions);
        this.peers = new LinkedBlockingQueue<>();

        LOG.debug("Instantiating RIB table {} at {}", ribId, getInstanceIdentifier());

        final ReadWriteTransaction trans = this.chain.newReadWriteTransaction();
        Optional<Rib> o;
        try {
            o = trans.read(LogicalDatastoreType.OPERATIONAL, getInstanceIdentifier()).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException("Failed to read topology", e);
        }
        Preconditions.checkState(!o.isPresent(), "Data provider conflict detected on object {}", getInstanceIdentifier());

        // put empty BgpRib if not exists
        trans.merge(LogicalDatastoreType.OPERATIONAL, InstanceIdentifier.builder(BgpRib.class).build(), new BgpRibBuilder().build());
        trans.put(LogicalDatastoreType.OPERATIONAL, getInstanceIdentifier(), new RibBuilder().setKey(new RibKey(ribId)).setId(ribId).setLocRib(
            new LocRibBuilder().setTables(Collections.<Tables> emptyList()).build()).build());

        for (final BgpTableType t : localTables) {
            final TablesKey key = new TablesKey(t.getAfi(), t.getSafi());
            if (this.tables.create(trans, this, key) == null) {
                LOG.debug("Did not create local table for unhandled table type {}", t);
            }
        }

        Futures.addCallback(trans.submit(), new FutureCallback<Void>() {
            @Override
            public void onSuccess(final Void result) {
                LOG.trace("Change committed successfully");
            }

            @Override
            public void onFailure(final Throwable t) {
                LOG.error("Failed to initiate RIB {}", getInstanceIdentifier(), t);
            }
        });
    }

    @Override
    public synchronized void updateTables(final Peer peer, final Update message) {
        final AdjRIBsTransactionImpl trans = new AdjRIBsTransactionImpl(this.ribOuts, this.comparator, this.chain.newWriteOnlyTransaction());

        if (!EOR.equals(message)) {
            updateTablesMpReach(message, trans, peer);
            updateTablesMpUnreach(message, trans, peer);
        } else {
            final AdjRIBsIn<?, ?> ari = this.tables.get(IPV4_UNICAST_TABLE);
            if (ari != null) {
                ari.markUptodate(trans, peer);
            } else {
                LOG.debug("End-of-RIB for IPv4 Unicast ignored");
            }
        }

        Futures.addCallback(trans.commit(), new FutureCallback<Void>() {
            @Override
            public void onSuccess(final Void result) {
                LOG.debug("RIB modification successfully committed.");
            }

            @Override
            public void onFailure(final Throwable t) {
                LOG.error("Failed to commit RIB modification", t);
            }
        });
    }

    private void updateTablesMpUnreach(final Update message, final AdjRIBsTransactionImpl trans, final Peer peer) {
        final WithdrawnRoutes withdrawnRoutes = message.getWithdrawnRoutes();
        if (withdrawnRoutes != null) {
            final AdjRIBsIn<?, ?> ari = this.tables.get(IPV4_UNICAST_TABLE);
            if (ari == null) {
                LOG.debug("Not removing objects from unhandled IPv4 Unicast");
                return;
            }
            final MpUnreachNlri mpUnreach = prefixesToMpUnreach(withdrawnRoutes);
            ari.removeRoutes(trans, peer, mpUnreach);
            return;
        }
        final PathAttributes attributes = message.getPathAttributes();
        if (attributes != null) {
            final PathAttributes2 mpu = attributes.getAugmentation(PathAttributes2.class);
            if (mpu != null) {
                final MpUnreachNlri nlri = mpu.getMpUnreachNlri();
                final AdjRIBsIn<?, ?> ari = this.tables.get(new TablesKey(nlri.getAfi(), nlri.getSafi()));
                if (ari == null) {
                    LOG.debug("Not removing objects from unhandled NLRI {}", nlri);
                    return;
                }
                // EOR messages do not contain withdrawn routes
                if (nlri.getWithdrawnRoutes() != null) {
                    ari.removeRoutes(trans, peer, nlri);
                } else {
                    ari.markUptodate(trans, peer);
                }
            }
        }
    }

    /***
     * create MPUnreach for the routes to be handled in the same way as linkstate routes
     * @param withdrawnRoutes
     * @return
     */
    private MpUnreachNlri prefixesToMpUnreach(final WithdrawnRoutes withdrawnRoutes) {
        return new MpUnreachNlriBuilder().setAfi(Ipv4AddressFamily.class).setSafi(UnicastSubsequentAddressFamily.class).setWithdrawnRoutes(
            new WithdrawnRoutesBuilder().setDestinationType(
                new DestinationIpv4CaseBuilder().setDestinationIpv4(
                    new DestinationIpv4Builder().setIpv4Prefixes(withdrawnRoutes.getWithdrawnRoutes()).build()).build()).build()).build();
    }

    private void updateTablesMpReach(final Update message, final AdjRIBsTransactionImpl trans, final Peer peer) {
        final PathAttributes attributes = message.getPathAttributes();
        final Nlri nlri = message.getNlri();
        if (nlri != null) {
            final AdjRIBsIn<?, ?> ari = this.tables.get(IPV4_UNICAST_TABLE);
            if (ari == null) {
                LOG.debug("Not adding objects from unhandled IPv4 Unicast");
                return;
            }
            MpReachNlri mpReachNlri = prefixesToMpReach(nlri, attributes);
            ari.addRoutes(trans, peer, mpReachNlri, nextHopToAttribute(mpReachNlri, attributes));
        } else if (attributes != null) {
            final PathAttributes1 mpr = attributes.getAugmentation(PathAttributes1.class);
            if (mpr != null) {
                final MpReachNlri mpReachNlri = mpr.getMpReachNlri();
                final AdjRIBsIn<?, ?> ari = this.tables.get(new TablesKey(mpReachNlri.getAfi(), mpReachNlri.getSafi()));
                if (ari == null) {
                    LOG.debug("Not adding objects from unhandled NLRI {}", mpReachNlri);
                    return;
                }
                if (message.equals(ari.endOfRib())) {
                    ari.markUptodate(trans, peer);
                } else {
                    ari.addRoutes(trans, peer, mpReachNlri, nextHopToAttribute(mpReachNlri, attributes));
                }
            }
        }
    }

    private PathAttributes nextHopToAttribute(final MpReachNlri mpReach, final PathAttributes attributes) {
        if (attributes.getCNextHop() == null && mpReach.getCNextHop() != null) {
            final PathAttributesBuilder attributesBuilder = new PathAttributesBuilder(attributes);
            attributesBuilder.setCNextHop(mpReach.getCNextHop());
            return attributesBuilder.build();
        }
        return attributes;
    }

    /***
     * create MPReach for the routes to be handled in the same way as linkstate routes
     */
    private MpReachNlri prefixesToMpReach(@Nonnull final Nlri nlri, final PathAttributes attributes) {
        final MpReachNlriBuilder b = new MpReachNlriBuilder().setAfi(Ipv4AddressFamily.class).setSafi(
            UnicastSubsequentAddressFamily.class).setAdvertizedRoutes(
            new AdvertizedRoutesBuilder().setDestinationType(
                new DestinationIpv4CaseBuilder().setDestinationIpv4(
                    new DestinationIpv4Builder().setIpv4Prefixes(nlri.getNlri()).build()).build()).build());
        if (attributes != null) {
            b.setCNextHop(attributes.getCNextHop());
        }
        return b.build();
    }

    @Override
    public synchronized void clearTable(final Peer peer, final TablesKey key) {
        final AdjRIBsIn<?, ?> ari = this.tables.get(key);
        if (ari != null) {
            final AdjRIBsTransactionImpl trans = new AdjRIBsTransactionImpl(this.ribOuts, this.comparator, this.chain.newWriteOnlyTransaction());
            ari.clear(trans, peer);

            Futures.addCallback(trans.commit(), new FutureCallback<Void>() {
                @Override
                public void onSuccess(final Void result) {
                    LOG.trace("Table {} cleared successfully", key);
                }

                @Override
                public void onFailure(final Throwable t) {
                    LOG.error("Failed to clear table {}", key, t);
                }
            });
        }
    }

    @Override
    public String toString() {
        return addToStringAttributes(Objects.toStringHelper(this)).toString();
    }

    protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
        return toStringHelper;
    }

    @SuppressWarnings("unchecked")
    protected <K, V extends Route> AdjRIBsIn<K, V> getTable(final TablesKey key) {
        return (AdjRIBsIn<K, V>) this.tables.get(key);
    }

    @Override
    public synchronized void close() throws InterruptedException, ExecutionException {
        final WriteTransaction t = this.chain.newWriteOnlyTransaction();
        t.delete(LogicalDatastoreType.OPERATIONAL, getInstanceIdentifier());
        t.submit().get();
        this.chain.close();
    }

    @Override
    public AsNumber getLocalAs() {
        return this.localAs;
    }

    @Override
    public Ipv4Address getBgpIdentifier() {
        return this.bgpIdentifier;
    }

    @Override
    public List<? extends BgpTableType> getLocalTables() {
        return this.localTables;
    }

    @Override
    public ReconnectStrategyFactory getTcpStrategyFactory() {
        return this.tcpStrategyFactory;
    }

    @Override
    public ReconnectStrategyFactory getSessionStrategyFactory() {
        return this.sessionStrategyFactory;
    }

    @Override
    public BGPDispatcher getDispatcher() {
        return this.dispatcher;
    }

    @Override
    public void initTable(final Peer bgpPeer, final TablesKey key) {
        // FIXME: BUG-196: support graceful restart
    }

    @Override
    public AdjRIBsOutRegistration registerRIBsOut(final Peer peer, final AdjRIBsOut aro) {
        final AdjRIBsOutRegistration reg = new AdjRIBsOutRegistration(aro) {
            @Override
            protected void removeRegistration() {
                RIBImpl.this.ribOuts.remove(peer, aro);
            }
        };

        this.ribOuts.put(peer, aro);
        LOG.debug("Registering this peer {} to RIB-Out {}", peer, this.ribOuts);
        try {
            this.peers.put(peer);
            this.scheduler.run();
        } catch (final InterruptedException e) {
            //
        }
        return reg;
    }

    @Override
    public void onTransactionChainFailed(final TransactionChain<?, ?> chain, final AsyncTransaction<?, ?> transaction, final Throwable cause) {
        LOG.error("Broken chain in RIB {} transaction {}", getInstanceIdentifier(), transaction.getIdentifier(), cause);
    }

    @Override
    public void onTransactionChainSuccessful(final TransactionChain<?, ?> chain) {
        LOG.info("RIB {} closed successfully", getInstanceIdentifier());
    }
}
