/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.net.InetAddresses;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.controller.config.yang.bgp.rib.impl.BGPPeerRuntimeMXBean;
import org.opendaylight.controller.config.yang.bgp.rib.impl.BGPPeerRuntimeRegistration;
import org.opendaylight.controller.config.yang.bgp.rib.impl.BGPPeerRuntimeRegistrator;
import org.opendaylight.controller.config.yang.bgp.rib.impl.BgpPeerState;
import org.opendaylight.controller.config.yang.bgp.rib.impl.BgpSessionState;
import org.opendaylight.controller.config.yang.bgp.rib.impl.RouteTable;
import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChain;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionStatistics;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIB;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIBSupportContext;
import org.opendaylight.protocol.bgp.rib.impl.spi.ReusableBGPPeer;
import org.opendaylight.protocol.bgp.rib.spi.BGPSession;
import org.opendaylight.protocol.bgp.rib.spi.BGPTerminationReason;
import org.opendaylight.protocol.bgp.rib.spi.Peer;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv4.prefixes.DestinationIpv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv4.prefixes.destination.ipv4.Ipv4Prefixes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv4.prefixes.destination.ipv4.Ipv4PrefixesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationIpv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.reach.nlri.c.next.hop.Ipv4NextHopCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.reach.nlri.c.next.hop.Ipv6NextHopCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.reach.nlri.c.next.hop.ipv4.next.hop._case.Ipv4NextHopBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.reach.nlri.c.next.hop.ipv6.next.hop._case.Ipv6NextHopBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.unreach.nlri.WithdrawnRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.CNextHop;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv4NextHopCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv6NextHopCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.ipv4.next.hop._case.Ipv4NextHop;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.ipv6.next.hop._case.Ipv6NextHop;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class representing a peer. We have a single instance for each peer, which provides translation from BGP events into
 * RIB actions.
 */
public class BGPPeer implements ReusableBGPPeer, Peer, AutoCloseable, BGPPeerRuntimeMXBean, TransactionChainListener {

    private static final Logger LOG = LoggerFactory.getLogger(BGPPeer.class);

    @GuardedBy("this")
    private final Set<TablesKey> tables = new HashSet<>();
    @GuardedBy("this")
    private BGPSession session;
    @GuardedBy("this")
    private byte[] rawIdentifier;
    @GuardedBy("this")
    private DOMTransactionChain chain;
    @GuardedBy("this")
    private AdjRibInWriter ribWriter;

    private final RIB rib;
    private final String name;
    private BGPPeerRuntimeRegistrator registrator;
    private BGPPeerRuntimeRegistration runtimeReg;
    private long sessionEstablishedCounter = 0L;

    public BGPPeer(final String name, final RIB rib) {
        this(name, rib, PeerRole.Ibgp);
    }

    public BGPPeer(final String name, final RIB rib, final PeerRole role) {
        this.rib = Preconditions.checkNotNull(rib);
        this.name = name;
        this.chain = rib.createPeerChain(this);
        this.ribWriter = AdjRibInWriter.create(rib.getYangRibId(), role, this.chain);
    }

    @Override
    public synchronized void close() {
        dropConnection();
        this.chain.close();
        // TODO should this perform cleanup ?
    }

    @Override
    public void onMessage(final BGPSession session, final Notification msg) {
        if (!(msg instanceof Update)) {
            LOG.info("Ignoring unhandled message class {}", msg.getClass());
            return;
        }
        final Update message = (Update) msg;

        // update AdjRibs
        final Attributes attrs = message.getAttributes();
        MpReachNlri mpReach = null;
        if (message.getNlri() != null) {
            mpReach = prefixesToMpReach(message);
        } else if (attrs != null && attrs.getAugmentation(Attributes1.class) != null) {
            mpReach = attrs.getAugmentation(Attributes1.class).getMpReachNlri();
        }
        if (mpReach != null) {
            this.ribWriter.updateRoutes(mpReach, nextHopToAttribute(attrs, mpReach));
        }
        MpUnreachNlri mpUnreach = null;
        if (message.getWithdrawnRoutes() != null) {
            mpUnreach = prefixesToMpUnreach(message);
        } else if (attrs != null && attrs.getAugmentation(Attributes2.class) != null) {
            mpUnreach = attrs.getAugmentation(Attributes2.class).getMpUnreachNlri();
        }
        if (mpUnreach != null) {
            this.ribWriter.removeRoutes(mpUnreach);
        }
    }

    private static Attributes nextHopToAttribute(final Attributes attrs, final MpReachNlri mpReach) {
        if (attrs.getCNextHop() == null && mpReach.getCNextHop() != null) {
            final AttributesBuilder attributesBuilder = new AttributesBuilder(attrs);
            final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update
                .attributes.mp.reach.nlri.CNextHop cNextHop = mpReach.getCNextHop();

            // Implemented for UnicastFamily, What about others AFI --SAFI ??
            if (cNextHop instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol
                .rev130919.update.attributes.mp.reach.nlri.c.next.hop.Ipv4NextHopCase) {
                final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919
                    .update.attributes.mp.reach.nlri.c.next.hop.ipv4.next.hop._case.Ipv4NextHop nextHop =
                    ((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919
                        .update.attributes.mp.reach.nlri.c.next.hop.Ipv4NextHopCase) cNextHop).getIpv4NextHop();

                attributesBuilder.setCNextHop(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
                    .bgp.types.rev130919.next.hop.c.next.hop.Ipv4NextHopCaseBuilder().setIpv4NextHop(new
                    org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c
                        .next.hop.ipv4.next.hop._case.Ipv4NextHopBuilder().setGlobal(nextHop.getGlobal()).build()).build());
            } else if (cNextHop instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol
                .rev130919.update.attributes.mp.reach.nlri.c.next.hop.Ipv6NextHopCase) {
                final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919
                    .update.attributes.mp.reach.nlri.c.next.hop.ipv6.next.hop._case.Ipv6NextHop nextHop =
                    ((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919
                        .update.attributes.mp.reach.nlri.c.next.hop.Ipv6NextHopCase) cNextHop).getIpv6NextHop();

                final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c
                    .next.hop.ipv6.next.hop._case.Ipv6NextHopBuilder nhBuilder = new org.opendaylight.yang.gen.v1.urn
                    .opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.ipv6.next.hop._case.Ipv6NextHopBuilder().setGlobal(nextHop.getGlobal());

                if (nextHop.getLinkLocal() != null) {
                    nhBuilder.setLinkLocal(nextHop.getLinkLocal());
                }

                attributesBuilder.setCNextHop(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
                    .bgp.types.rev130919.next.hop.c.next.hop.Ipv6NextHopCaseBuilder().setIpv6NextHop(nhBuilder.build()).build());
            } else {
                LOG.warn("Not supported NextHopeCase", cNextHop);
            }

            return attributesBuilder.build();
        }
        return attrs;
    }

    /**
     * Creates MPReach for the prefixes to be handled in the same way as linkstate routes
     *
     * @param message Update message containing prefixes in NLRI
     * @return MpReachNlri with prefixes from the nlri field
     */
    private static MpReachNlri prefixesToMpReach(final Update message) {
        final List<Ipv4Prefixes> prefixes = new ArrayList<>();
        for (final Ipv4Prefix p : message.getNlri().getNlri()) {
            prefixes.add(new Ipv4PrefixesBuilder().setPrefix(p).build());
        }

        //Why UnicastSubsequentAddressFamily?
        final MpReachNlriBuilder b = new MpReachNlriBuilder().setAfi(Ipv4AddressFamily.class).setSafi(
            UnicastSubsequentAddressFamily.class).setAdvertizedRoutes(
                new AdvertizedRoutesBuilder().setDestinationType(
                    new DestinationIpv4CaseBuilder().setDestinationIpv4(
                        new DestinationIpv4Builder().setIpv4Prefixes(prefixes).build()).build()).build());
        if (message.getAttributes() != null) {
            final CNextHop cNextHop = message.getAttributes().getCNextHop();
            if (cNextHop instanceof Ipv6NextHopCase) {
                //Since we support only Ipv4 (parser/serializer) for path attributes, does this make sense?
                Ipv6NextHop ipv6NextHop = ((Ipv6NextHopCase)cNextHop).getIpv6NextHop();
                final Ipv6NextHopBuilder ipv6NhBuilder = new Ipv6NextHopBuilder().setGlobal(ipv6NextHop.getGlobal());
                if(ipv6NextHop.getLinkLocal() != null ) {
                    ipv6NhBuilder.setLinkLocal(ipv6NextHop.getLinkLocal());
                }
                b.setCNextHop(new Ipv6NextHopCaseBuilder().setIpv6NextHop(ipv6NhBuilder.build()).build());
            } else if (cNextHop instanceof Ipv4NextHopCase) {
                Ipv4NextHop ipv4NextHop = ((Ipv4NextHopCase)cNextHop).getIpv4NextHop();
                b.setCNextHop(new Ipv4NextHopCaseBuilder().setIpv4NextHop(new Ipv4NextHopBuilder().setGlobal
                    (ipv4NextHop.getGlobal()).build()).build());
            }

        }
        return b.build();
    }

    /**
     * Create MPUnreach for the prefixes to be handled in the same way as linkstate routes
     *
     * @param message Update message containing withdrawn routes
     * @return MpUnreachNlri with prefixes from the withdrawn routes field
     */
    private static MpUnreachNlri prefixesToMpUnreach(final Update message) {
        final List<Ipv4Prefixes> prefixes = new ArrayList<>();
        for (final Ipv4Prefix p : message.getWithdrawnRoutes().getWithdrawnRoutes()) {
            prefixes.add(new Ipv4PrefixesBuilder().setPrefix(p).build());
        }
        return new MpUnreachNlriBuilder().setAfi(Ipv4AddressFamily.class).setSafi(UnicastSubsequentAddressFamily.class).setWithdrawnRoutes(
                new WithdrawnRoutesBuilder().setDestinationType(
                    new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationIpv4CaseBuilder().setDestinationIpv4(
                        new DestinationIpv4Builder().setIpv4Prefixes(prefixes).build()).build()).build()).build();
    }

    @Override
    public synchronized void onSessionUp(final BGPSession session) {
        LOG.info("Session with peer {} went up with tables: {}", this.name, session.getAdvertisedTableTypes());
        this.session = session;
        this.rawIdentifier = InetAddresses.forString(session.getBgpId().getValue()).getAddress();
        final PeerId peerId = RouterIds.createPeerId(session.getBgpId());
        TablesKey key = new TablesKey(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class);
        this.tables.add(key);
        createAdjRibOutListener(peerId, key, false);

        for (final BgpTableType t : session.getAdvertisedTableTypes()) {
            key = new TablesKey(t.getAfi(), t.getSafi());
            if (this.tables.add(key)) {
                createAdjRibOutListener(peerId, key, true);
            }
        }

        this.ribWriter = this.ribWriter.transform(peerId, this.rib.getRibSupportContext(), this.tables, false);
        this.sessionEstablishedCounter++;
        if (this.registrator != null) {
            this.runtimeReg = this.registrator.register(this);
        }
    }

    private void createAdjRibOutListener(final PeerId peerId, final TablesKey key, final boolean mpSupport) {
        final RIBSupportContext context = this.rib.getRibSupportContext().getRIBSupportContext(key);

        // not particularly nice
        if (context != null && this.session instanceof BGPSessionImpl) {
            AdjRibOutListener.create(peerId, key, this.rib.getYangRibId(), this.rib.getCodecsRegistry(), context.getRibSupport(), ((RIBImpl) this.rib).getService(),
                ((BGPSessionImpl) this.session).getLimiter(), mpSupport);
        }
    }

    private synchronized void cleanup() {
        // FIXME: BUG-196: support graceful restart
        this.ribWriter.cleanTables(this.tables);
        this.tables.clear();
    }

    @Override
    public void onSessionDown(final BGPSession session, final Exception e) {
        LOG.info("Session with peer {} went down", this.name, e);
        releaseConnection();
    }

    @Override
    public void onSessionTerminated(final BGPSession session, final BGPTerminationReason cause) {
        LOG.info("Session with peer {} terminated: {}", this.name, cause);
        releaseConnection();
    }

    @Override
    public String toString() {
        return addToStringAttributes(MoreObjects.toStringHelper(this)).toString();
    }

    protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
        toStringHelper.add("name", this.name);
        toStringHelper.add("tables", this.tables);
        return toStringHelper;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void releaseConnection() {
        dropConnection();
        cleanup();
    }

    @GuardedBy("this")
    private void dropConnection() {
        if (this.runtimeReg != null) {
            this.runtimeReg.close();
            this.runtimeReg = null;
        }
        if (this.session != null) {
            try {
                this.session.close();
            } catch (final Exception e) {
                LOG.warn("Error closing session with peer", e);
            }
            this.session = null;
        }
    }

    @Override
    public boolean isSessionActive() {
        return this.session != null;
    }

    @Override
    public synchronized byte[] getRawIdentifier() {
        return Arrays.copyOf(this.rawIdentifier, this.rawIdentifier.length);
    }

    @Override
    public void resetSession() {
        releaseConnection();
    }

    @Override
    public void resetStats() {
        if (this.session instanceof BGPSessionStatistics) {
            ((BGPSessionStatistics) this.session).resetSessionStats();
        }
    }

    public synchronized void registerRootRuntimeBean(final BGPPeerRuntimeRegistrator registrator) {
        this.registrator = registrator;
    }

    @Override
    public BgpSessionState getBgpSessionState() {
        if (this.session instanceof BGPSessionStatistics) {
            return ((BGPSessionStatistics) this.session).getBgpSesionState();
        }
        return new BgpSessionState();
    }

    @Override
    public synchronized BgpPeerState getBgpPeerState() {
        final BgpPeerState peerState = new BgpPeerState();
        final List<RouteTable> routes = Lists.newArrayList();
        for (final TablesKey tablesKey : this.tables) {
            final RouteTable routeTable = new RouteTable();
            routeTable.setTableType("afi=" + tablesKey.getAfi().getSimpleName() + ",safi=" + tablesKey.getSafi().getSimpleName());
            routeTable.setRoutesCount(this.rib.getRoutesCount(tablesKey));
            routes.add(routeTable);
        }
        peerState.setRouteTable(routes);
        peerState.setSessionEstablishedCount(this.sessionEstablishedCounter);
        return peerState;
    }

    @Override
    public void onTransactionChainFailed(final TransactionChain<?, ?> chain, final AsyncTransaction<?, ?> transaction, final Throwable cause) {
        LOG.error("Transaction chain failed.", cause);
        dropConnection();
        this.chain.close();
        this.chain = this.rib.createPeerChain(this);
    }

    @Override
    public void onTransactionChainSuccessful(final TransactionChain<?, ?> chain) {
        LOG.debug("Transaction chain {} successfull.", chain);
    }

    @Override
    public void markUptodate(final TablesKey tablesKey) {
        this.ribWriter.markTableUptodate(tablesKey);
    }
}
