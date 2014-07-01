/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.base.Charsets;
import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.JdkFutureAdapters;

import io.netty.util.concurrent.Future;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.concurrent.GuardedBy;

import org.opendaylight.bgpcep.tcpmd5.KeyMapping;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.protocol.bgp.parser.BGPSession;
import org.opendaylight.protocol.bgp.parser.BGPSessionListener;
import org.opendaylight.protocol.bgp.parser.BGPTerminationReason;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.protocol.bgp.rib.RibReference;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionPreferences;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIB;
import org.opendaylight.protocol.bgp.rib.spi.AdjRIBIn;
import org.opendaylight.protocol.bgp.rib.spi.AdjRIBInFactory;
import org.opendaylight.protocol.bgp.rib.spi.Peer;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.UpdateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.Nlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.PathAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.WithdrawnRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.PathAttributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.PathAttributes2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.destination.destination.type.DestinationIpv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.destination.destination.type.destination.ipv4._case.DestinationIpv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.open.bgp.parameters.c.parameters.multiprotocol._case.MultiprotocolCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.mp.unreach.nlri.WithdrawnRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.Rib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class representing a peer. We have a single instance for each peer, which provides translation from BGP events into
 * RIB actions.
 */
public final class BGPPeer implements BGPSessionListener, Peer, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(BGPPeer.class);

    private static final Update EOR = new UpdateBuilder().build();
    private static final TablesKey IPV4_UNICAST_TABLE = new TablesKey(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class);

    @GuardedBy("this")
    private final String name;
    private final RIB rib;

    private Comparator<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.PathAttributes> comparator;
    private Future<Void> cf;
    private BGPSession session;
    private final List<BgpTableType> local_tables;
    private final Map<TablesKey, AdjRIBIn> tables = new HashMap<>();

    public BGPPeer(final String name, final InetSocketAddress address, final String password, final BGPSessionPreferences local_prefs,
            final AsNumber remoteAs, final RIB rib) {
        this.rib = Preconditions.checkNotNull(rib);
        this.local_tables = new ArrayList<>();
        for (BgpParameters p : local_prefs.getParams()) {
            if (p.getCParameters() instanceof MultiprotocolCapability) {
                this.local_tables.add(new BgpTableTypeImpl(((MultiprotocolCapability)p.getCParameters()).getAfi(), ((MultiprotocolCapability)p.getCParameters()).getSafi()));
            }
        }
        this.name = Preconditions.checkNotNull(name);

        final KeyMapping keys;
        if (password != null) {
            keys = new KeyMapping();
            keys.put(address.getAddress(), password.getBytes(Charsets.US_ASCII));
        } else {
            keys = null;
        }

        this.cf = rib.getDispatcher().createReconnectingClient(address, local_prefs, remoteAs, this, rib.getTcpStrategyFactory(),
                rib.getSessionStrategyFactory(), keys);
    }

    @Override
    public synchronized void close() {
        if (this.cf != null) {
            this.cf.cancel(true);
            if (this.session != null) {
                this.session.close();
                this.session = null;
            }
            this.cf = null;
        }
    }

    @Override
    public void onMessage(final BGPSession session, final Notification m) {
        if (!(m instanceof Update)) {
            LOG.info("Ignoring unhandled message class {}", m.getClass());
        }
        final DataModificationTransaction trans = this.rib.getTransaction();

        Update message = (Update) m;
        if (!EOR.equals(message)) {
            final WithdrawnRoutes wr = message.getWithdrawnRoutes();
            if (wr != null) {
                final AdjRIBIn ari = this.tables.get(IPV4_UNICAST_TABLE);
                if (ari != null) {
                    ari.removeRoutes(
                            trans,
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

                    final AdjRIBIn ari = this.tables.get(new TablesKey(nlri.getAfi(), nlri.getSafi()));
                    if (ari != null) {
                        ari.removeRoutes(trans, nlri);
                    } else {
                        LOG.debug("Not removing objects from unhandled NLRI {}", nlri);
                    }
                }
            }

            final Nlri ar = message.getNlri();
            if (ar != null) {
                final AdjRIBIn ari = this.tables.get(IPV4_UNICAST_TABLE);
                if (ari != null) {
                    final MpReachNlriBuilder b = new MpReachNlriBuilder().setAfi(Ipv4AddressFamily.class).setSafi(
                            UnicastSubsequentAddressFamily.class).setAdvertizedRoutes(
                                    new AdvertizedRoutesBuilder().setDestinationType(
                                            new DestinationIpv4CaseBuilder().setDestinationIpv4(
                                                    new DestinationIpv4Builder().setIpv4Prefixes(ar.getNlri()).build()).build()).build());
                    if (attrs != null) {
                        b.setCNextHop(attrs.getCNextHop());
                    }

                    ari.addRoutes(trans, b.build(), attrs);
                } else {
                    LOG.debug("Not adding objects from unhandled IPv4 Unicast");
                }
            }

            if (attrs != null) {
                final PathAttributes1 mpr = attrs.getAugmentation(PathAttributes1.class);
                if (mpr != null) {
                    final MpReachNlri nlri = mpr.getMpReachNlri();

                    final AdjRIBIn ari = this.tables.get(new TablesKey(nlri.getAfi(), nlri.getSafi()));
                    if (ari != null) {
                        if (message.equals(ari.endOfRib())) {
                            ari.markUptodate(trans);
                        } else {
                            ari.addRoutes(trans, nlri, attrs);
                        }
                    } else {
                        LOG.debug("Not adding objects from unhandled NLRI {}", nlri);
                    }
                }
            }
        } else {
            final AdjRIBIn ari = this.tables.get(IPV4_UNICAST_TABLE);
            if (ari != null) {
                ari.markUptodate(trans);
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
    public synchronized void onSessionUp(final BGPSession session) {
        LOG.info("Session with peer {} went up with tables: {}", this.name, session.getAdvertisedTableTypes());

        this.session = session;
        // this.comparator = new BGPObjectComparator(this.rib.getLocalAs(), this.rib.getBgpIdentifier(), session.getBgpId());

        final DataModificationTransaction trans = this.rib.getTransaction();
        final Object o = trans.readOperationalData(this.rib.getInstanceIdentifier());
        Preconditions.checkState(o == null, "Data provider conflict detected on object %s", this.rib.getInstanceIdentifier());

        for (BgpTableType t : this.session.getAdvertisedTableTypes()) {
            final TablesKey key = new TablesKey(t.getAfi(), t.getSafi());
            if (this.create(trans, this.rib, key) == null) {
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
                LOG.error("Failed to initiate RIB {}", BGPPeer.this.rib.getInstanceIdentifier());
            }
        });
    }

    public synchronized AdjRIBIn create(final DataModificationTransaction trans, final RibReference rib, final TablesKey key) {

        final AdjRIBInFactory f = this.rib.getAdjRIBInFactory(key.getAfi(), key.getSafi());
        if (f == null) {
            LOG.debug("RIBsInFactory not found for key {}, returning null", key);
            return null;
        }

        final AdjRIBIn table = Preconditions.checkNotNull(f.createAdjRIBIn(trans, this, key));
        LOG.debug("Table {} created for key {}", table, key);
        this.tables.put(key,table);
        return table;
    }

    private synchronized void cleanup() {
        // FIXME: BUG-196: support graceful restart
        for (final Entry<TablesKey, AdjRIBIn> in : this.tables.entrySet()) {
            this.rib.clearTable(this, in.getValue());
        }

        this.tables.clear();
        this.session = null;
        this.comparator = null;
    }

    @Override
    public void onSessionDown(final BGPSession session, final Exception e) {
        LOG.info("Session with peer {} went down", this.name, e);
        cleanup();
    }

    @Override
    public void onSessionTerminated(final BGPSession session, final BGPTerminationReason cause) {
        LOG.info("Session with peer {} terminated: {}", this.name, cause);
        cleanup();
    }

    @Override
    public String toString() {
        return addToStringAttributes(Objects.toStringHelper(this)).toString();
    }

    protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
        toStringHelper.add("name", this.name);
        toStringHelper.add("tables", this.tables);
        return toStringHelper;
    }

    @Override
    public InstanceIdentifier<Rib> getRibInstanceIdentifier() {
        return this.rib.getInstanceIdentifier();
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Comparator<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.PathAttributes> getComparator() {
        return this.comparator;
    }
}
