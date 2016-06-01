/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static org.opendaylight.protocol.bgp.rib.impl.AdjRibInWriter.isAnnounceNone;
import static org.opendaylight.protocol.bgp.rib.impl.AdjRibInWriter.isLearnNone;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.net.InetAddresses;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
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
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RoutedRpcRegistration;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPError;
import org.opendaylight.protocol.bgp.parser.impl.message.update.LocalPreferenceAttributeParser;
import org.opendaylight.protocol.bgp.parser.spi.MessageUtil;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionStatistics;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIB;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIBSupportContext;
import org.opendaylight.protocol.bgp.rib.spi.BGPSession;
import org.opendaylight.protocol.bgp.rib.spi.BGPSessionListener;
import org.opendaylight.protocol.bgp.rib.spi.BGPTerminationReason;
import org.opendaylight.protocol.bgp.rib.spi.IdentifierUtils;
import org.opendaylight.protocol.bgp.rib.spi.Peer;
import org.opendaylight.protocol.bgp.rib.spi.RouterIds;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv4.prefixes.DestinationIpv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv4.prefixes.destination.ipv4.Ipv4Prefixes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv4.prefixes.destination.ipv4.Ipv4PrefixesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationIpv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.RouteRefresh;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.mp.capabilities.add.path.capability.AddressFamilies;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.unreach.nlri.WithdrawnRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.peer.rpc.rev160322.BgpPeerRpcService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.peer.rpc.rev160322.PeerContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.SimpleRoutingPolicy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.rib.PeerKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.SubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class representing a peer. We have a single instance for each peer, which provides translation from BGP events into
 * RIB actions.
 */
public class BGPPeer implements BGPSessionListener, Peer, AutoCloseable, BGPPeerRuntimeMXBean, TransactionChainListener {

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
    @GuardedBy("this")
    private EffectiveRibInWriter effRibInWriter;

    private final RIB rib;
    private final String name;
    private BGPPeerRuntimeRegistrator registrator;
    private BGPPeerRuntimeRegistration runtimeReg;
    private long sessionEstablishedCounter = 0L;
    private final Map<TablesKey, AdjRibOutListener> adjRibOutListenerSet = new HashMap();
    private final RpcProviderRegistry rpcRegistry;
    private RoutedRpcRegistration<BgpPeerRpcService> rpcRegistration;
    private final PeerRole peerRole;
    private final Optional<SimpleRoutingPolicy> simpleRoutingPolicy;

    public BGPPeer(final String name, final RIB rib, final PeerRole role, final SimpleRoutingPolicy peerStatus, final RpcProviderRegistry rpcRegistry) {
        this.peerRole = role;
        this.simpleRoutingPolicy = Optional.ofNullable(peerStatus);
        this.rib = Preconditions.checkNotNull(rib);
        this.name = name;
        this.chain = rib.createPeerChain(this);
        this.ribWriter = AdjRibInWriter.create(rib.getYangRibId(), this.peerRole, this.simpleRoutingPolicy, this.chain);
        this.rpcRegistry = rpcRegistry;
    }

    public BGPPeer(final String name, final RIB rib, final PeerRole role, final RpcProviderRegistry rpcRegistry) {
        this(name, rib, role, null, rpcRegistry);
    }

    @Override
    public synchronized void close() {
        releaseConnection();
        this.chain.close();
    }

    @Override
    public void onMessage(final BGPSession session, final Notification msg) throws BGPDocumentedException {
        if (!(msg instanceof Update) && !(msg instanceof RouteRefresh)) {
            LOG.info("Ignoring unhandled message class {}", msg.getClass());
            return;
        }
        if (msg instanceof Update) {
            onUpdateMessage((Update) msg);
        } else {
            onRouteRefreshMessage((RouteRefresh) msg, session);
        }
    }

    private void onRouteRefreshMessage(final RouteRefresh message, final BGPSession session) {
        final Class<? extends AddressFamily> rrAfi = message.getAfi();
        final Class<? extends SubsequentAddressFamily> rrSafi = message.getSafi();

        final TablesKey key = new TablesKey(rrAfi, rrSafi);
        final AdjRibOutListener listener = this.adjRibOutListenerSet.get(key);
        if (listener != null) {
            listener.close();
            this.adjRibOutListenerSet.remove(listener);
            createAdjRibOutListener(RouterIds.createPeerId(session.getBgpId()), key, listener.isMpSupported());
        } else {
            LOG.info("Ignoring RouteRefresh message. Afi/Safi is not supported: {}, {}.", rrAfi, rrSafi);
        }
    }

    /**
     * Check for presence of well known mandatory attribute LOCAL_PREF in Update message
     *
     * @param message Update message
     * @throws BGPDocumentedException
     */
    private void checkMandatoryAttributesPresence(final Update message) throws BGPDocumentedException {
        if (MessageUtil.isAnyNlriPresent(message)) {
            final Attributes attrs = message.getAttributes();
            if (this.peerRole == PeerRole.Ibgp && (attrs == null || attrs.getLocalPref() == null)) {
                throw new BGPDocumentedException(BGPError.MANDATORY_ATTR_MISSING_MSG + "LOCAL_PREF",
                        BGPError.WELL_KNOWN_ATTR_MISSING,
                        new byte[] { LocalPreferenceAttributeParser.TYPE });
            }
        }
    }

    /**
     * Process Update message received.
     * Calls {@link #checkMandatoryAttributesPresence(Update)} to check for presence of mandatory attributes.
     *
     * @param message Update message
     * @throws BGPDocumentedException
     */
    private void onUpdateMessage(final Update message) throws BGPDocumentedException {
        checkMandatoryAttributesPresence(message);

        // update AdjRibs
        final Attributes attrs = message.getAttributes();
        MpReachNlri mpReach = null;
        final boolean isAnyNlriAnnounced = message.getNlri() != null;
        if (isAnyNlriAnnounced) {
            mpReach = prefixesToMpReach(message);
        } else {
            mpReach = MessageUtil.getMpReachNlri(attrs);
        }
        if (mpReach != null) {
            this.ribWriter.updateRoutes(mpReach, nextHopToAttribute(attrs, mpReach));
        }
        MpUnreachNlri mpUnreach = null;
        if (message.getWithdrawnRoutes() != null) {
            mpUnreach = prefixesToMpUnreach(message, isAnyNlriAnnounced);
        } else {
            mpUnreach = MessageUtil.getMpUnreachNlri(attrs);
        }
        if (mpUnreach != null) {
            this.ribWriter.removeRoutes(mpUnreach);
        }
    }

    private static Attributes nextHopToAttribute(final Attributes attrs, final MpReachNlri mpReach) {
        if (attrs.getCNextHop() == null && mpReach.getCNextHop() != null) {
            final AttributesBuilder attributesBuilder = new AttributesBuilder(attrs);
            attributesBuilder.setCNextHop(mpReach.getCNextHop());
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
        final MpReachNlriBuilder b = new MpReachNlriBuilder().setAfi(Ipv4AddressFamily.class).setSafi(
            UnicastSubsequentAddressFamily.class).setAdvertizedRoutes(
                new AdvertizedRoutesBuilder().setDestinationType(
                    new DestinationIpv4CaseBuilder().setDestinationIpv4(
                        new DestinationIpv4Builder().setIpv4Prefixes(prefixes).build()).build()).build());
        if (message.getAttributes() != null) {
            b.setCNextHop(message.getAttributes().getCNextHop());
        }
        return b.build();
    }

    /**
     * Create MPUnreach for the prefixes to be handled in the same way as linkstate routes
     *
     * @param message Update message containing withdrawn routes
     * @param isAnyNlriAnnounced
     * @return MpUnreachNlri with prefixes from the withdrawn routes field
     */
    private static MpUnreachNlri prefixesToMpUnreach(final Update message, final boolean isAnyNlriAnnounced) {
        final List<Ipv4Prefixes> prefixes = new ArrayList<>();
        for (final Ipv4Prefix p : message.getWithdrawnRoutes().getWithdrawnRoutes()) {
            boolean nlriAnounced = false;
            if(isAnyNlriAnnounced) {
                nlriAnounced = message.getNlri().getNlri().contains(p);
            }

            if(!nlriAnounced) {
                prefixes.add(new Ipv4PrefixesBuilder().setPrefix(p).build());
            }
        }
        return new MpUnreachNlriBuilder().setAfi(Ipv4AddressFamily.class).setSafi(UnicastSubsequentAddressFamily.class).setWithdrawnRoutes(
                new WithdrawnRoutesBuilder().setDestinationType(
                    new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationIpv4CaseBuilder().setDestinationIpv4(
                        new DestinationIpv4Builder().setIpv4Prefixes(prefixes).build()).build()).build()).build();
    }

    @Override
    public synchronized void onSessionUp(final BGPSession session) {
        final List<AddressFamilies> addPathTablesType = session.getAdvertisedAddPathTableTypes();
        LOG.info("Session with peer {} went up with tables {} and Add Path tables {}", this.name, addPathTablesType,
            session.getAdvertisedAddPathTableTypes());
        this.session = session;

        this.rawIdentifier = InetAddresses.forString(session.getBgpId().getValue()).getAddress();
        final PeerId peerId = RouterIds.createPeerId(session.getBgpId());

        this.tables.addAll(this.session.getAdvertisedTableTypes().stream().map(t -> new TablesKey(t.getAfi(), t.getSafi())).collect(Collectors.toList()));
        final boolean announceNone = isAnnounceNone(this.simpleRoutingPolicy);
        if(!announceNone) {
            createAdjRibOutListener(peerId);
        }
        addBgp4Support(peerId, announceNone);

        if(!isLearnNone(this.simpleRoutingPolicy)) {
            final YangInstanceIdentifier peerIId = this.rib.getYangRibId().node(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.rib.Peer.QNAME).node(IdentifierUtils.domPeerId(peerId));
            this.effRibInWriter = EffectiveRibInWriter.create(this.rib.getService(), this.rib.createPeerChain(this), peerIId, ((RIBImpl) this.rib).getImportPolicyPeerTracker(),
                this.rib.getRibSupportContext(), this.peerRole);
        }
        this.ribWriter = this.ribWriter.transform(peerId, this.rib.getRibSupportContext(), this.tables, addPathTablesType);
        this.sessionEstablishedCounter++;
        if (this.registrator != null) {
            this.runtimeReg = this.registrator.register(this);
        }

        if (this.rpcRegistry != null) {
            this.rpcRegistration = this.rpcRegistry.addRoutedRpcImplementation(BgpPeerRpcService.class, new BgpPeerRpc(session, this.tables));
            final KeyedInstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.rib.Peer, PeerKey> path =
                this.rib.getInstanceIdentifier().child(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.rib.Peer.class, new PeerKey(peerId));
            this.rpcRegistration.registerPath(PeerContext.class, path);
        }
    }

    private void createAdjRibOutListener(final PeerId peerId) {
        this.tables.forEach(key->createAdjRibOutListener(peerId, key, true));
    }

    //try to add a support for old-school BGP-4, if peer did not advertise IPv4-Unicast MP capability
    private void addBgp4Support(final PeerId peerId, final boolean announceNone) {
        final TablesKey key = new TablesKey(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class);
        if (this.tables.add(key) && !announceNone) {
            createAdjRibOutListener(peerId, key, false);
        }
    }

    private void createAdjRibOutListener(final PeerId peerId, final TablesKey key, final boolean mpSupport) {
        final RIBSupportContext context = this.rib.getRibSupportContext().getRIBSupportContext(key);

        // not particularly nice
        if (context != null && this.session instanceof BGPSessionImpl) {
            this.adjRibOutListenerSet.put(key, AdjRibOutListener.create(peerId, key, this.rib.getYangRibId(), this.rib.getCodecsRegistry(),
                context.getRibSupport(), ((RIBImpl) this.rib).getService(), ((BGPSessionImpl) this.session).getLimiter(), mpSupport));
        }
    }

    private synchronized void cleanup() {
        // FIXME: BUG-196: support graceful
        for (final AdjRibOutListener adjRibOutListener : this.adjRibOutListenerSet.values()) {
            adjRibOutListener.close();
        }
        this.adjRibOutListenerSet.clear();
        if (this.effRibInWriter != null) {
            this.effRibInWriter.close();
        }
        this.ribWriter.removePeer();
        this.tables.clear();
    }

    @Override
    public void onSessionDown(final BGPSession session, final Exception e) {
        if(e.getMessage().equals(BGPSessionImpl.END_OF_INPUT)) {
            LOG.info("Session with peer {} went down", this.name);
        } else {
            LOG.info("Session with peer {} went down", this.name, e);
        }
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
        if (this.rpcRegistration != null) {
            this.rpcRegistration.close();
        }
        addPeerToDisconnectedSharedList();
        cleanup();
        dropConnection();
    }

    private void addPeerToDisconnectedSharedList() {
        if(this.session != null) {
            this.rib.getCacheDisconnectedPeers().insertDesconectedPeer(this.session.getBgpId());
        }
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
        this.chain.close();
        this.chain = this.rib.createPeerChain(this);
        this.ribWriter = AdjRibInWriter.create(this.rib.getYangRibId(), this.peerRole, this.simpleRoutingPolicy, this.chain);
        releaseConnection();
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
