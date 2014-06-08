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
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.JdkFutureAdapters;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.annotation.concurrent.ThreadSafe;

import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.protocol.bgp.rib.DefaultRibReference;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPDispatcher;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIB;
import org.opendaylight.protocol.bgp.rib.spi.AdjRIBsIn;
import org.opendaylight.protocol.bgp.rib.spi.Peer;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionConsumerContext;
import org.opendaylight.protocol.framework.ReconnectStrategyFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.UpdateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.Nlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.PathAttributes;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.RibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.Rib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.RibBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.RibKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.rib.LocRibBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ThreadSafe
public final class RIBImpl extends DefaultRibReference implements AutoCloseable, RIB {
    private static final Logger LOG = LoggerFactory.getLogger(RIBImpl.class);
    private static final Update EOR = new UpdateBuilder().build();
    private static final TablesKey IPV4_UNICAST_TABLE = new TablesKey(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class);
    private final ReconnectStrategyFactory tcpStrategyFactory;
    private final ReconnectStrategyFactory sessionStrategyFactory;
    private final BGPDispatcher dispatcher;
    private final DataProviderService dps;
    private final AsNumber localAs;
    private final Ipv4Address bgpIdentifier;
    private final List<BgpTableType> localTables;
    private final RIBTables tables;

    public RIBImpl(final RibId ribId, final AsNumber localAs, final Ipv4Address localBgpId, final RIBExtensionConsumerContext extensions,
            final BGPDispatcher dispatcher, final ReconnectStrategyFactory tcpStrategyFactory,
            final ReconnectStrategyFactory sessionStrategyFactory, final DataProviderService dps, final List<BgpTableType> localTables) {
        super(InstanceIdentifier.builder(BgpRib.class).child(Rib.class, new RibKey(Preconditions.checkNotNull(ribId))).toInstance());
        this.dps = Preconditions.checkNotNull(dps);
        this.localAs = Preconditions.checkNotNull(localAs);
        this.bgpIdentifier = Preconditions.checkNotNull(localBgpId);
        this.dispatcher = Preconditions.checkNotNull(dispatcher);
        this.sessionStrategyFactory = Preconditions.checkNotNull(sessionStrategyFactory);
        this.tcpStrategyFactory = Preconditions.checkNotNull(tcpStrategyFactory);
        this.localTables = ImmutableList.copyOf(localTables);
        this.tables = new RIBTables(extensions);

        LOG.debug("Instantiating RIB table {} at {}", ribId, getInstanceIdentifier());

        final DataModificationTransaction trans = dps.beginTransaction();
        final Object o = trans.readOperationalData(getInstanceIdentifier());
        Preconditions.checkState(o == null, "Data provider conflict detected on object {}", getInstanceIdentifier());

        trans.putOperationalData(getInstanceIdentifier(), new RibBuilder().setKey(new RibKey(ribId)).setId(ribId).setLocRib(
                new LocRibBuilder().setTables(Collections.<Tables> emptyList()).build()).build());

        for (BgpTableType t : localTables) {
            final TablesKey key = new TablesKey(t.getAfi(), t.getSafi());
            if (this.tables.create(trans, this, key) == null) {
                LOG.debug("Did not create local table for unhandled table type {}", t);
            }
        }

        Futures.addCallback(JdkFutureAdapters.listenInPoolThread(trans.commit()), new FutureCallback<RpcResult<TransactionStatus>>() {
            @Override
            public void onSuccess(final RpcResult<TransactionStatus> result) {
                LOG.trace("Change committed successfully");
            }

            @Override
            public void onFailure(final Throwable t) {
                LOG.error("Failed to initiate RIB {}", getInstanceIdentifier());
            }
        });
    }

    synchronized void initTables(final byte[] remoteBgpId) {
    }

    @Override
    public synchronized void updateTables(final Peer peer, final Update message) {
        final DataModificationTransaction trans = this.dps.beginTransaction();

        if (!EOR.equals(message)) {
            final WithdrawnRoutes wr = message.getWithdrawnRoutes();
            if (wr != null) {
                final AdjRIBsIn ari = this.tables.get(IPV4_UNICAST_TABLE);
                if (ari != null) {
                    ari.removeRoutes(
                            trans,
                            peer,
                            new MpUnreachNlriBuilder().setAfi(Ipv4AddressFamily.class).setSafi(UnicastSubsequentAddressFamily.class).setWithdrawnRoutes(
                                    new WithdrawnRoutesBuilder().setDestinationType(
                                            new DestinationIpv4CaseBuilder().setDestinationIpv4(
                                                    new DestinationIpv4Builder().setIpv4Prefixes(wr.getWithdrawnRoutes()).build()).build()).build()).build());
                } else {
                    LOG.debug("Not removing objects from unhandled IPv4 Unicast");
                }
            }

            final PathAttributes attrs = message.getPathAttributes();
            if (attrs != null) {
                final PathAttributes2 mpu = attrs.getAugmentation(PathAttributes2.class);
                if (mpu != null) {
                    final MpUnreachNlri nlri = mpu.getMpUnreachNlri();

                    final AdjRIBsIn ari = this.tables.get(new TablesKey(nlri.getAfi(), nlri.getSafi()));
                    if (ari != null) {
                        ari.removeRoutes(trans, peer, nlri);
                    } else {
                        LOG.debug("Not removing objects from unhandled NLRI {}", nlri);
                    }
                }
            }

            final Nlri ar = message.getNlri();
            if (ar != null) {
                final AdjRIBsIn ari = this.tables.get(IPV4_UNICAST_TABLE);
                if (ari != null) {
                    final MpReachNlriBuilder b = new MpReachNlriBuilder().setAfi(Ipv4AddressFamily.class).setSafi(
                            UnicastSubsequentAddressFamily.class).setAdvertizedRoutes(
                            new AdvertizedRoutesBuilder().setDestinationType(
                                    new DestinationIpv4CaseBuilder().setDestinationIpv4(
                                            new DestinationIpv4Builder().setIpv4Prefixes(ar.getNlri()).build()).build()).build());
                    if (attrs != null) {
                        b.setCNextHop(attrs.getCNextHop());
                    }

                    ari.addRoutes(trans, peer, b.build(), attrs);
                } else {
                    LOG.debug("Not adding objects from unhandled IPv4 Unicast");
                }
            }

            if (attrs != null) {
                final PathAttributes1 mpr = attrs.getAugmentation(PathAttributes1.class);
                if (mpr != null) {
                    final MpReachNlri nlri = mpr.getMpReachNlri();

                    final AdjRIBsIn ari = this.tables.get(new TablesKey(nlri.getAfi(), nlri.getSafi()));
                    if (ari != null) {
                        if (message.equals(ari.endOfRib())) {
                            ari.markUptodate(trans, peer);
                        } else {
                            ari.addRoutes(trans, peer, nlri, attrs);
                        }
                    } else {
                        LOG.debug("Not adding objects from unhandled NLRI {}", nlri);
                    }
                }
            }
        } else {
            final AdjRIBsIn ari = this.tables.get(IPV4_UNICAST_TABLE);
            if (ari != null) {
                ari.markUptodate(trans, peer);
            } else {
                LOG.debug("End-of-RIB for IPv4 Unicast ignored");
            }
        }

        Futures.addCallback(JdkFutureAdapters.listenInPoolThread(trans.commit()), new FutureCallback<RpcResult<TransactionStatus>>() {
            @Override
            public void onSuccess(final RpcResult<TransactionStatus> result) {
                LOG.debug("RIB modification successfully committed.");
            }

            @Override
            public void onFailure(final Throwable t) {
                LOG.error("Failed to commit RIB modification", t);
            }
        });
    }

    @Override
    public synchronized void clearTable(final Peer peer, final TablesKey key) {
        final AdjRIBsIn ari = this.tables.get(key);
        if (ari != null) {
            final DataModificationTransaction trans = this.dps.beginTransaction();
            ari.clear(trans, peer);

            Futures.addCallback(JdkFutureAdapters.listenInPoolThread(trans.commit()), new FutureCallback<RpcResult<TransactionStatus>>() {
                @Override
                public void onSuccess(final RpcResult<TransactionStatus> result) {
                    // Nothing to do
                }

                @Override
                public void onFailure(final Throwable t) {
                    LOG.error("Failed to commit RIB modification", t);
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

    @Override
    public void close() throws InterruptedException, ExecutionException {
        final DataModificationTransaction t = this.dps.beginTransaction();
        t.removeOperationalData(getInstanceIdentifier());
        t.commit().get();
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
}
