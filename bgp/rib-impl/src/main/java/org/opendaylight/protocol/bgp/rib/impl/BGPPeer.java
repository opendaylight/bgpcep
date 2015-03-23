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
import org.opendaylight.protocol.bgp.rib.RibReference;
import org.opendaylight.protocol.bgp.rib.impl.spi.AdjRIBsOutRegistration;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionStatistics;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIB;
import org.opendaylight.protocol.bgp.rib.impl.spi.ReusableBGPPeer;
import org.opendaylight.protocol.bgp.rib.spi.BGPSession;
import org.opendaylight.protocol.bgp.rib.spi.BGPTerminationReason;
import org.opendaylight.protocol.bgp.rib.spi.Peer;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv4.prefixes.DestinationIpv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv4.prefixes.destination.ipv4.Ipv4Prefixes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv4.prefixes.destination.ipv4.Ipv4PrefixesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.update.path.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationIpv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.PathAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.PathAttributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.PathAttributes2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.mp.unreach.nlri.WithdrawnRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;
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
    private final RIB rib;
    private final String name;

    @GuardedBy("this")
    private BGPSession session;
    @GuardedBy("this")
    private byte[] rawIdentifier;
    @GuardedBy("this")
    private AdjRIBsOutRegistration reg;

    private BGPPeerRuntimeRegistrator registrator;
    private BGPPeerRuntimeRegistration runtimeReg;
    private long sessionEstablishedCounter = 0L;

    @GuardedBy("this")
    private AdjRibInWriter ribWriter;

    public BGPPeer(final String name, final RIB rib) {
        this.rib = Preconditions.checkNotNull(rib);
        this.name = name;

        final DOMTransactionChain chain = rib.createPeerChain(this);
        // FIXME: make this configurable
        final PeerRole role = PeerRole.Ibgp;

        this.ribWriter = AdjRibInWriter.create(((RibReference)rib).getInstanceIdentifier().getKey(), role, chain);
    }

    @Override
    public synchronized void close() {
        dropConnection();
        // TODO should this perform cleanup ?
    }

    @Override
    public void onMessage(final BGPSession session, final Notification msg) {
        if (!(msg instanceof Update)) {
            LOG.info("Ignoring unhandled message class {}", msg.getClass());
            return;
        }
        final Update message = (Update) msg;
        this.rib.updateTables(this, message);
        // update AdjRibs
        final PathAttributes attrs = message.getPathAttributes();
        MpReachNlri mpReach = null;
        if (message.getNlri() != null) {
            mpReach = prefixesToMpReach(message);
        } else if (attrs != null && attrs.getAugmentation(PathAttributes1.class) != null) {
            mpReach = attrs.getAugmentation(PathAttributes1.class).getMpReachNlri();
        }
        if (mpReach != null) {
            this.ribWriter.updateRoutes(mpReach, attrs);
            return;
        }
        MpUnreachNlri mpUnreach = null;
        if (message.getWithdrawnRoutes() != null) {
            mpUnreach = prefixesToMpUnreach(message);
        } else if (attrs != null && attrs.getAugmentation(PathAttributes2.class) != null) {
            mpUnreach = attrs.getAugmentation(PathAttributes2.class).getMpUnreachNlri();
        }
        if (mpUnreach != null) {
            this.ribWriter.removeRoutes(mpUnreach);
        }
    }

    /**
     * Creates MPReach for the prefixes to be handled in the same way as linkstate routes
     *
     * @param message Update message containing prefixes in NLRI
     * @return MpReachNlri with prefixes from the nlri field
     */
    private MpReachNlri prefixesToMpReach(final Update message) {
        final List<Ipv4Prefixes> prefixes = new ArrayList<>();
        for (final Ipv4Prefix p : message.getNlri().getNlri()) {
            prefixes.add(new Ipv4PrefixesBuilder().setPrefix(p).build());
        }
        final MpReachNlriBuilder b = new MpReachNlriBuilder().setAfi(Ipv4AddressFamily.class).setSafi(
            UnicastSubsequentAddressFamily.class).setAdvertizedRoutes(
                new AdvertizedRoutesBuilder().setDestinationType(
                    new DestinationIpv4CaseBuilder().setDestinationIpv4(
                        new DestinationIpv4Builder().setIpv4Prefixes(prefixes).build()).build()).build());
        if (message.getPathAttributes() != null) {
            b.setCNextHop(message.getPathAttributes().getCNextHop());
        }
        return b.build();
    }

    /**
     * Create MPUnreach for the prefixes to be handled in the same way as linkstate routes
     *
     * @param message Update message containing withdrawn routes
     * @return MpUnreachNlri with prefixes from the withdrawn routes field
     */
    private MpUnreachNlri prefixesToMpUnreach(final Update message) {
        final List<Ipv4Prefixes> prefixes = new ArrayList<>();
        for (final Ipv4Prefix p : message.getWithdrawnRoutes().getWithdrawnRoutes()) {
            prefixes.add(new Ipv4PrefixesBuilder().setPrefix(p).build());
        }
        return new MpUnreachNlriBuilder().setAfi(Ipv4AddressFamily.class).setSafi(UnicastSubsequentAddressFamily.class).setWithdrawnRoutes(
                new WithdrawnRoutesBuilder().setDestinationType(
                    new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.update.path.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationIpv4CaseBuilder().setDestinationIpv4(
                        new DestinationIpv4Builder().setIpv4Prefixes(prefixes).build()).build()).build()).build();
    }

    @Override
    public synchronized void onSessionUp(final BGPSession session) {
        LOG.info("Session with peer {} went up with tables: {}", this.name, session.getAdvertisedTableTypes());

        this.session = session;
        this.rawIdentifier = InetAddresses.forString(session.getBgpId().getValue()).getAddress();

        for (final BgpTableType t : session.getAdvertisedTableTypes()) {
            final TablesKey key = new TablesKey(t.getAfi(), t.getSafi());

            this.tables.add(key);
            this.rib.initTable(this, key);
        }

        this.ribWriter = this.ribWriter.transform(session.getBgpId(), this.rib.getRibExtensions(), this.tables);

        // Not particularly nice, but what can
        if (session instanceof BGPSessionImpl) {
            this.reg = this.rib.registerRIBsOut(this, new SessionRIBsOut((BGPSessionImpl) session));
        }
        this.sessionEstablishedCounter++;
        if (this.registrator != null) {
            this.runtimeReg = this.registrator.register(this);
        }
    }

    private synchronized void cleanup() {
        // FIXME: BUG-196: support graceful restart
        this.ribWriter.cleanTables(this.tables);
        for (final TablesKey key : this.tables) {
            this.rib.clearTable(this, key);
        }

        if (this.reg != null) {
            this.reg.close();
            this.reg = null;
        }

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

    protected RIB getRib() {
        return this.rib;
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
            this.session.close();
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
        // TODO Auto-generated method stub

    }

    @Override
    public void onTransactionChainSuccessful(final TransactionChain<?, ?> chain) {
        // TODO Auto-generated method stub
    }
}
