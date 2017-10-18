/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static java.util.Objects.requireNonNull;
import static org.opendaylight.protocol.bgp.rib.impl.AdjRibInWriter.isAnnounceNone;
import static org.opendaylight.protocol.bgp.rib.impl.AdjRibInWriter.isLearnNone;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.google.common.net.InetAddresses;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;
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
import org.opendaylight.protocol.bgp.rib.impl.spi.RIB;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIBSupportContext;
import org.opendaylight.protocol.bgp.rib.impl.state.BGPPeerStateImpl;
import org.opendaylight.protocol.bgp.rib.impl.state.BGPSessionStateProvider;
import org.opendaylight.protocol.bgp.rib.spi.BGPSession;
import org.opendaylight.protocol.bgp.rib.spi.BGPSessionListener;
import org.opendaylight.protocol.bgp.rib.spi.BGPTerminationReason;
import org.opendaylight.protocol.bgp.rib.spi.ExportPolicyPeerTracker;
import org.opendaylight.protocol.bgp.rib.spi.IdentifierUtils;
import org.opendaylight.protocol.bgp.rib.spi.Peer;
import org.opendaylight.protocol.bgp.rib.spi.RouterIds;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPAfiSafiState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPErrorHandlingState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPSessionState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPTimersState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPTransportState;
import org.opendaylight.protocol.concepts.AbstractRegistration;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171122.ipv4.prefixes.DestinationIpv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171122.ipv4.prefixes.destination.ipv4.Ipv4Prefixes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171122.ipv4.prefixes.destination.ipv4.Ipv4PrefixesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171122.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationIpv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.update.message.Nlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpAddPathTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.RouteRefresh;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.SendReceive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.mp.capabilities.add.path.capability.AddressFamilies;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.unreach.nlri.WithdrawnRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.peer.rpc.rev171027.BgpPeerRpcService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.peer.rpc.rev171027.PeerContext;
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
public class BGPPeer extends BGPPeerStateImpl implements BGPSessionListener, Peer, TransactionChainListener {
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
    private final Map<TablesKey, AdjRibOutListener> adjRibOutListenerSet = new HashMap<>();
    private final RpcProviderRegistry rpcRegistry;
    private RoutedRpcRegistration<BgpPeerRpcService> rpcRegistration;
    private final PeerRole peerRole;
    private final Optional<SimpleRoutingPolicy> simpleRoutingPolicy;
    private YangInstanceIdentifier peerIId;
    private final Set<AbstractRegistration> tableRegistration = new HashSet<>();

    public BGPPeer(final String name, final RIB rib, final PeerRole role, final SimpleRoutingPolicy peerStatus,
            final RpcProviderRegistry rpcRegistry,
            @Nonnull final Set<TablesKey> afiSafisAdvertized,
            @Nonnull final Set<TablesKey> afiSafisGracefulAdvertized) {
        //FIXME BUG-6971 Once Peer Group is implemented, pass it
        super(rib.getInstanceIdentifier(), null, new IpAddress(new Ipv4Address(name)), afiSafisAdvertized,
                afiSafisGracefulAdvertized);
        this.peerRole = role;
        this.simpleRoutingPolicy = Optional.ofNullable(peerStatus);
        this.rib = requireNonNull(rib);
        this.name = name;
        this.rpcRegistry = rpcRegistry;
        this.chain = rib.createPeerChain(this);
    }

    public BGPPeer(final String name, final RIB rib, final PeerRole role,
            final RpcProviderRegistry rpcRegistry, @Nonnull final Set<TablesKey> afiSafisAdvertized,
            @Nonnull final Set<TablesKey> afiSafisGracefulAdvertized) {
        this(name, rib, role, null, rpcRegistry, afiSafisAdvertized, afiSafisGracefulAdvertized);
    }

    public void instantiateServiceInstance() {
        this.ribWriter = AdjRibInWriter.create(this.rib.getYangRibId(), this.peerRole, this.simpleRoutingPolicy, this.chain);
        setActive(true);
    }

    // FIXME ListenableFuture<?> should be used once closeServiceInstance uses wildcard too
    @Override
    public synchronized ListenableFuture<Void> close() {
        final ListenableFuture<Void> future = releaseConnection();
        this.chain.close();
        setActive(false);
        return future;
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
            this.adjRibOutListenerSet.remove(key);
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
                        new byte[]{LocalPreferenceAttributeParser.TYPE});
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
        final List<Ipv4Prefixes> prefixes = message.getNlri().stream()
                .map(n-> new Ipv4PrefixesBuilder().setPrefix(n.getPrefix()).setPathId(n.getPathId()).build())
                .collect(Collectors.toList());
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
     * @param message            Update message containing withdrawn routes
     * @param isAnyNlriAnnounced
     * @return MpUnreachNlri with prefixes from the withdrawn routes field
     */
    private static MpUnreachNlri prefixesToMpUnreach(final Update message, final boolean isAnyNlriAnnounced) {
        final List<Ipv4Prefixes> prefixes = new ArrayList<>();
        message.getWithdrawnRoutes().forEach(w -> {
            boolean nlriAnounced = false;
            if (isAnyNlriAnnounced) {
                for (final Nlri n : message.getNlri()) {
                    if (Objects.equal(n.getPrefix(), w.getPrefix()) && Objects.equal(n.getPathId(), w.getPathId())) {
                        nlriAnounced = true;
                        break;
                    }
                }
            }
            if (!nlriAnounced) {
                prefixes.add(new Ipv4PrefixesBuilder().setPrefix(w.getPrefix()).setPathId(w.getPathId()).build());
            }
        });
        return new MpUnreachNlriBuilder().setAfi(Ipv4AddressFamily.class).setSafi(UnicastSubsequentAddressFamily.class).setWithdrawnRoutes(
                new WithdrawnRoutesBuilder().setDestinationType(
                        new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171122.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationIpv4CaseBuilder().setDestinationIpv4(
                                new DestinationIpv4Builder().setIpv4Prefixes(prefixes).build()).build()).build()).build();
    }

    @Override
    public synchronized void onSessionUp(final BGPSession session) {
        this.session = session;
        if (this.session instanceof BGPSessionStateProvider) {
            ((BGPSessionStateProvider) this.session).registerMessagesCounter(this);
        }

        final List<AddressFamilies> addPathTablesType = session.getAdvertisedAddPathTableTypes();
        final Set<BgpTableType> advertizedTableTypes = session.getAdvertisedTableTypes();
        final List<BgpTableType> advertizedGracefulRestartTableTypes = session.getAdvertisedGracefulRestartTableTypes();
        LOG.info("Session with peer {} went up with tables {} and Add Path tables {}", this.name, advertizedTableTypes, addPathTablesType);
        this.rawIdentifier = InetAddresses.forString(session.getBgpId().getValue()).getAddress();
        final PeerId peerId = RouterIds.createPeerId(session.getBgpId());

        this.tables.addAll(advertizedTableTypes.stream().map(t -> new TablesKey(t.getAfi(), t.getSafi())).collect(Collectors.toList()));

        setAdvertizedGracefulRestartTableTypes(advertizedGracefulRestartTableTypes.stream()
                .map(t -> new TablesKey(t.getAfi(), t.getSafi())).collect(Collectors.toList()));
        final boolean announceNone = isAnnounceNone(this.simpleRoutingPolicy);
        final Map<TablesKey, SendReceive> addPathTableMaps = mapTableTypesFamilies(addPathTablesType);
        this.peerIId = this.rib.getYangRibId().node(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.rib.Peer.QNAME)
                .node(IdentifierUtils.domPeerId(peerId));

        if (!announceNone) {
            createAdjRibOutListener(peerId);
        }
        this.tables.forEach(tablesKey -> {
            final ExportPolicyPeerTracker exportTracker = this.rib.getExportPolicyPeerTracker(tablesKey);
            if (exportTracker != null) {
                this.tableRegistration.add(exportTracker.registerPeer(peerId, addPathTableMaps.get(tablesKey), this.peerIId, this.peerRole,
                        this.simpleRoutingPolicy));
            }
        });
        addBgp4Support(peerId, announceNone);

        if (!isLearnNone(this.simpleRoutingPolicy)) {
            this.effRibInWriter = EffectiveRibInWriter.create(this.rib.getService(),
                    this.rib.createPeerChain(this),
                    this.peerIId, this.rib.getImportPolicyPeerTracker(),
                    this.rib.getRibSupportContext(),
                    this.peerRole,
                    this.tables);
            registerPrefixesCounters(this.effRibInWriter, this.effRibInWriter);
        }
        this.ribWriter = this.ribWriter.transform(peerId, this.rib.getRibSupportContext(), this.tables, addPathTableMaps);

        if (this.rpcRegistry != null) {
            this.rpcRegistration = this.rpcRegistry.addRoutedRpcImplementation(BgpPeerRpcService.class,
                    new BgpPeerRpc(this, session, this.tables));
            final KeyedInstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.rib.Peer, PeerKey> path =
                    this.rib.getInstanceIdentifier().child(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.rib.Peer.class, new PeerKey(peerId));
            this.rpcRegistration.registerPath(PeerContext.class, path);
        }
    }

    private void createAdjRibOutListener(final PeerId peerId) {
        this.tables.forEach(key -> createAdjRibOutListener(peerId, key, true));
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
            final ChannelOutputLimiter limiter = ((BGPSessionImpl) this.session).getLimiter();
            final AdjRibOutListener adjRibOut = AdjRibOutListener.create(peerId, key,
                    this.rib.getYangRibId(), this.rib.getCodecsRegistry(), context.getRibSupport(),
                    this.rib.getService(), limiter, mpSupport);
            this.adjRibOutListenerSet.put(key, adjRibOut);
            registerPrefixesSentCounter(key, adjRibOut);
        }
    }

    private ListenableFuture<Void> cleanup() {
        // FIXME: BUG-196: support graceful
        this.adjRibOutListenerSet.values().forEach(AdjRibOutListener::close);
        this.adjRibOutListenerSet.clear();
        if (this.effRibInWriter != null) {
            this.effRibInWriter.close();
        }
        this.tables.clear();
        if (this.ribWriter != null) {
            return this.ribWriter.removePeer();
        }
        return Futures.immediateFuture(null);
    }

    @Override
    public void onSessionDown(final BGPSession session, final Exception e) {
        if (e.getMessage().equals(BGPSessionImpl.END_OF_INPUT)) {
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
    public synchronized ListenableFuture<Void> releaseConnection() {
        if (this.rpcRegistration != null) {
            this.rpcRegistration.close();
        }
        closeRegistration();
        final ListenableFuture<Void> future = cleanup();

        if (this.session != null) {
            try {
                this.session.close();
            } catch (final Exception e) {
                LOG.warn("Error closing session with peer", e);
            }
            this.session = null;
        }

        resetState();
        return future;
    }

    private void closeRegistration() {
        this.tableRegistration.iterator().forEachRemaining(AbstractRegistration::close);
        this.tableRegistration.clear();
    }

    @Override
    public synchronized byte[] getRawIdentifier() {
        return Arrays.copyOf(this.rawIdentifier, this.rawIdentifier.length);
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

    private static Map<TablesKey, SendReceive> mapTableTypesFamilies(final List<AddressFamilies> addPathTablesType) {
        return ImmutableMap.copyOf(addPathTablesType.stream().collect(Collectors.toMap(af -> new TablesKey(af.getAfi(), af.getSafi()),
                BgpAddPathTableType::getSendReceive)));
    }

    @Override
    public BGPErrorHandlingState getBGPErrorHandlingState() {
        return this;
    }

    @Override
    public BGPAfiSafiState getBGPAfiSafiState() {
        return this;
    }

    @Override
    public BGPSessionState getBGPSessionState() {
        if (this.session instanceof BGPSessionStateProvider) {
            return ((BGPSessionStateProvider) this.session).getBGPSessionState();
        }
        return null;
    }

    @Override
    public BGPTimersState getBGPTimersState() {
        if (this.session instanceof BGPSessionStateProvider) {
            return ((BGPSessionStateProvider) this.session).getBGPTimersState();
        }
        return null;
    }

    @Override
    public BGPTransportState getBGPTransportState() {
        if (this.session instanceof BGPSessionStateProvider) {
            return ((BGPSessionStateProvider) this.session).getBGPTransportState();
        }
        return null;
    }
}
