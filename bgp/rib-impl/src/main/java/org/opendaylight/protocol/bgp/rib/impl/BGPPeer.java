/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static java.util.Objects.requireNonNull;
import static org.opendaylight.mdsal.common.api.LogicalDatastoreType.OPERATIONAL;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.ADJRIBOUT_NID;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.TABLES_NID;

import com.google.common.base.MoreObjects;
import com.google.common.base.Stopwatch;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.checkerframework.checker.lock.qual.Holding;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPTableTypeRegistryConsumer;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPError;
import org.opendaylight.protocol.bgp.parser.impl.message.update.LocalPreferenceAttributeParser;
import org.opendaylight.protocol.bgp.parser.spi.MessageUtil;
import org.opendaylight.protocol.bgp.parser.spi.RevisedErrorHandlingSupport;
import org.opendaylight.protocol.bgp.parser.spi.pojo.RevisedErrorHandlingSupportImpl;
import org.opendaylight.protocol.bgp.rib.impl.config.BgpPeerBean;
import org.opendaylight.protocol.bgp.rib.impl.config.GracefulRestartUtil;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionPreferences;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIB;
import org.opendaylight.protocol.bgp.rib.impl.state.BGPSessionStateProvider;
import org.opendaylight.protocol.bgp.rib.spi.BGPSession;
import org.opendaylight.protocol.bgp.rib.spi.BGPSessionListener;
import org.opendaylight.protocol.bgp.rib.spi.BGPTerminationReason;
import org.opendaylight.protocol.bgp.rib.spi.RouterIds;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPSessionState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPTimersState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPTransportState;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.types.rev171204.Uint24;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv4.prefixes.DestinationIpv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv4.prefixes.destination.ipv4.Ipv4Prefixes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv4.prefixes.destination.ipv4.Ipv4PrefixesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationIpv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.BgpAddPathTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.RouteRefresh;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.SendReceive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.reach.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.reach.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.reach.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.unreach.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.unreach.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.unreach.mp.unreach.nlri.WithdrawnRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.add.path.capability.AddressFamilies;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.peer.rpc.rev180329.ResetSession;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.peer.rpc.rev180329.RestartGracefully;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.peer.rpc.rev180329.RouteRefreshRequest;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.rib.PeerKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.ClusterIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.RouteTarget;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.binding.Notification;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.util.concurrent.SpecialExecutors;
import org.opendaylight.yangtools.yang.common.Empty;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.spi.node.ImmutableNodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class representing a peer. We have a single instance for each peer, which provides translation from BGP events into
 * RIB actions.
 */
public final class BGPPeer extends AbstractPeer implements BGPSessionListener {
    private static final Logger LOG = LoggerFactory.getLogger(BGPPeer.class);
    private static final TablesKey IPV4_UCAST_TABLE_KEY =
        new TablesKey(Ipv4AddressFamily.VALUE, UnicastSubsequentAddressFamily.VALUE);

    private final RIB rib;

    // FIXME: Alright, this right here is a ton of state which has intertwined initialization and dependencies Split
    //        these out into separate behavior objects. This also has relationship with state in AbstractPeer -- which
    //        hints at an obvious layer of indirection. Yeah, yeah, we can always add one of those, but the point
    //        is that this class is a mutable meeting point, whereas the behaviour has captured invariants.
    private final LoadingCache<NodeIdentifierWithPredicates, YangInstanceIdentifier> tablesIId =
        CacheBuilder.newBuilder().build(new CacheLoader<>() {
            @Override
            public YangInstanceIdentifier load(final NodeIdentifierWithPredicates key) {
                return peerRibOutIId.node(TABLES_NID).node(key).toOptimized();
            }
        });

    private ImmutableSet<TablesKey> tables = ImmutableSet.of();
    private final Map<TablesKey, AdjRibOutListener> adjRibOutListenerSet = new HashMap<>();
    private final List<RouteTarget> rtMemberships = new ArrayList<>();
    private final RpcProviderService rpcRegistry;
    private final BGPTableTypeRegistryConsumer tableTypeRegistry;
    // Runs the peer tracker registration once both initialization commits complete. That registration takes the
    // LocRibWriter lock (via onPeerAdded) and can wait for it while an update is being processed, so it gets a thread
    // of its own instead of borrowing a shared one.
    private final ExecutorService registrationExecutor;
    // Enable RFC7606 treat-as-withdraw UPDATE handling
    private final boolean treatAsWithdraw;

    // FIXME: this is a reference to configuration: why do we actually need it?
    private final BgpPeerBean bean;

    // FIXME: This should be a constant co-located with ApplicationPeer.peerId
    private YangInstanceIdentifier peerRibOutIId;
    @GuardedBy("this")
    private Registration trackerRegistration;

    @GuardedBy("this")
    private BGPSession currentSession;
    @GuardedBy("this")
    private AdjRibInWriter ribWriter;
    @GuardedBy("this")
    private EffectiveRibInWriter effRibInWriter;
    private Registration rpcRegistration;
    private ImmutableMap<TablesKey, SendReceive> addPathTableMaps = ImmutableMap.of();
    // FIXME: This should be a constant co-located with ApplicationPeer.peerId
    private YangInstanceIdentifier peerPath;
    // FIXME: This is for supportsTable() and peer registration -- a trivial behavior thing, where 'peer-down'
    //        type states always return false
    private volatile boolean sessionUp;
    private boolean llgrSupport;
    private Stopwatch peerRestartStopwatch;
    private long currentSelectionDeferralTimerSeconds;
    private final List<TablesKey> missingEOT = new ArrayList<>();

    public BGPPeer(
            final BGPTableTypeRegistryConsumer tableTypeRegistry,
            final IpAddressNoZone neighborAddress,
            final String peerGroupName,
            final RIB rib,
            final PeerRole role,
            final ClusterIdentifier clusterId,
            final AsNumber localAs,
            final RpcProviderService rpcRegistry,
            final Set<TablesKey> afiSafisAdvertized,
            final Set<TablesKey> afiSafisGracefulAdvertized,
            final Map<TablesKey, Uint24> llGracefulTablesAdvertised,
            final boolean treatAsWithdraw,
            final BgpPeerBean bean) {
        super(rib, Ipv4Util.toStringIP(neighborAddress), peerGroupName, role, clusterId, localAs, neighborAddress,
            afiSafisAdvertized, afiSafisGracefulAdvertized, llGracefulTablesAdvertised);
        this.tableTypeRegistry = requireNonNull(tableTypeRegistry);
        this.rib = requireNonNull(rib);
        this.rpcRegistry = rpcRegistry;
        this.treatAsWithdraw = treatAsWithdraw;
        this.bean = requireNonNull(bean);
        // One thread is enough, a peer registers once per session, so the queue of 10 effectively never fills. Use
        // an abort policy so a rejected task fails initialization instead of running under the submitting thread's
        // BGPPeer lock and reversing the lock order described in registerPeer().
        registrationExecutor = SpecialExecutors.newBoundedFastThreadPool(1, 10, "bgp-peer-registration-"
            + Ipv4Util.toStringIP(neighborAddress), BGPPeer.class);
        createDomChain();
    }

    private static Attributes nextHopToAttribute(final Attributes attrs, final MpReachNlri mpReach) {
        if (attrs.getCNextHop() == null) {
            final var reachNextHop = mpReach.getCNextHop();
            if (reachNextHop != null) {
                return new AttributesBuilder(attrs).setCNextHop(reachNextHop).build();
            }
        }
        return attrs;
    }

    /**
     * Creates MPReach for the prefixes to be handled in the same way as linkstate routes.
     *
     * @param message Update message containing prefixes in NLRI
     * @return MpReachNlri with prefixes from the nlri field
     */
    private static MpReachNlri prefixesToMpReach(final Update message) {
        final var builder = new MpReachNlriBuilder();
        final var attrs = message.getAttributes();
        if (attrs != null) {
            builder.setCNextHop(attrs.getCNextHop());
        }
        return builder
            .setAfi(Ipv4AddressFamily.VALUE)
            .setSafi(UnicastSubsequentAddressFamily.VALUE)
            .setAdvertizedRoutes(new AdvertizedRoutesBuilder()
                .setDestinationType(new DestinationIpv4CaseBuilder()
                    .setDestinationIpv4(new DestinationIpv4Builder()
                        .setIpv4Prefixes(message.nonnullNlri().stream()
                            .map(nlri -> new Ipv4PrefixesBuilder()
                                .setPrefix(nlri.getPrefix())
                                .setPathId(nlri.getPathId())
                                .build())
                            .collect(Collectors.toList()))
                        .build())
                    .build())
                .build())
            .build();
    }

    /**
     * Create MPUnreach for the prefixes to be handled in the same way as linkstate routes.
     *
     * @param message            Update message containing withdrawn routes
     * @param isAnyNlriAnnounced isAnyNlriAnnounced
     * @return MpUnreachNlri with prefixes from the withdrawn routes field
     */
    private static MpUnreachNlri prefixesToMpUnreach(final Update message, final boolean isAnyNlriAnnounced) {
        final var prefixes = new ArrayList<Ipv4Prefixes>();
        final var withdrawnRoutes = message.getWithdrawnRoutes();
        if (withdrawnRoutes != null) {
            for (var withdrawnRoute : withdrawnRoutes) {
                final var nlriAnounced = isAnyNlriAnnounced && message.nonnullNlri().stream()
                    .anyMatch(nlri -> Objects.equals(nlri.getPrefix(), withdrawnRoute.getPrefix())
                        && Objects.equals(nlri.getPathId(), withdrawnRoute.getPathId()));
                if (!nlriAnounced) {
                    prefixes.add(new Ipv4PrefixesBuilder()
                        .setPrefix(withdrawnRoute.getPrefix())
                        .setPathId(withdrawnRoute.getPathId())
                        .build());
                }
            }
        }

        return new MpUnreachNlriBuilder()
            .setAfi(Ipv4AddressFamily.VALUE)
            .setSafi(UnicastSubsequentAddressFamily.VALUE)
            .setWithdrawnRoutes(new WithdrawnRoutesBuilder()
                .setDestinationType(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet
                    .rev180329.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type
                    .DestinationIpv4CaseBuilder()
                    .setDestinationIpv4(new DestinationIpv4Builder().setIpv4Prefixes(prefixes).build())
                    .build())
                .build())
            .build();
    }

    private static ImmutableMap<TablesKey, SendReceive> mapTableTypesFamilies(
            final List<AddressFamilies> addPathTablesType) {
        return addPathTablesType.stream().collect(ImmutableMap.toImmutableMap(
            af -> new TablesKey(af.getAfi(), af.getSafi()), BgpAddPathTableType::getSendReceive));
    }

    public synchronized void instantiateServiceInstance() {
        createDomChain();
        ribWriter = AdjRibInWriter.create(rib.getYangRibId(), getRole(), this);
        setActive(true);
    }

    @Override
    public synchronized FluentFuture<? extends CommitInfo> close() {
        final var future = releaseConnection(true);
        closeDomChain();
        setActive(false);
        // shutdown rather than shutdownNow because releaseConnection cleared sessionUp, so any queued registration
        // already bails on its own, no need to interrupt.
        registrationExecutor.shutdown();
        return future;
    }

    @Override
    public void onMessage(final BGPSession session, final Notification<?> msg) throws BGPDocumentedException {
        switch (msg) {
            case RouteRefresh routeRefresh -> onRouteRefreshMessage(routeRefresh);
            case Update update -> onUpdateMessage(update);
            default -> LOG.info("Ignoring unhandled message class {}", msg.getClass());
        }
    }

    private void onRouteRefreshMessage(final RouteRefresh message) {
        final var rrAfi = message.getAfi();
        final var rrSafi = message.getSafi();

        final var key = new TablesKey(rrAfi, rrSafi);
        synchronized (this) {
            final var listener = adjRibOutListenerSet.remove(key);
            if (listener != null) {
                listener.close();
                createAdjRibOutListener(key, listener.isMpSupported());
            } else {
                LOG.info("Ignoring RouteRefresh message. Afi/Safi is not supported: {}, {}.", rrAfi, rrSafi);
            }
        }
    }

    /**
     * Check for presence of well known mandatory attribute LOCAL_PREF in Update message.
     *
     * @param message Update message
     */
    private void checkMandatoryAttributesPresence(final Update message) throws BGPDocumentedException {
        if (MessageUtil.isAnyNlriPresent(message)) {
            final var attrs = message.getAttributes();
            if (getRole() == PeerRole.Ibgp && (attrs == null || attrs.getLocalPref() == null)) {
                throw new BGPDocumentedException(BGPError.MANDATORY_ATTR_MISSING_MSG + "LOCAL_PREF",
                        BGPError.WELL_KNOWN_ATTR_MISSING, new byte[] { LocalPreferenceAttributeParser.TYPE });
            }
        }
    }

    /**
     * Process Update message received.
     * Calls {@link #checkMandatoryAttributesPresence(Update)} to check for presence of mandatory attributes.
     *
     * @param message Update message
     */
    private synchronized void onUpdateMessage(final Update message) throws BGPDocumentedException {
        checkMandatoryAttributesPresence(message);

        // update AdjRibs
        final var attrs = message.getAttributes();
        final boolean isAnyNlriAnnounced = message.getNlri() != null;
        final var mpReach = isAnyNlriAnnounced ? prefixesToMpReach(message) : MessageUtil.getMpReachNlri(attrs);
        if (mpReach != null) {
            ribWriter.updateRoutes(mpReach, nextHopToAttribute(attrs, mpReach));
        }

        final var mpUnreach = message.getWithdrawnRoutes() != null ? prefixesToMpUnreach(message, isAnyNlriAnnounced)
            : MessageUtil.getMpUnreachNlri(attrs);
        final boolean endOfRib = BgpPeerUtil.isEndOfRib(message);
        if (mpUnreach != null) {
            if (endOfRib) {
                final var tablesKey = new TablesKey(mpUnreach.requireAfi(), mpUnreach.requireSafi());
                ribWriter.removeStaleRoutes(tablesKey);
                missingEOT.remove(tablesKey);
                handleGracefulEndOfRib();
            } else {
                ribWriter.removeRoutes(mpUnreach);
            }
        } else if (endOfRib) {
            ribWriter.removeStaleRoutes(IPV4_UCAST_TABLE_KEY);
            missingEOT.remove(IPV4_UCAST_TABLE_KEY);
            handleGracefulEndOfRib();
        }
    }

    @Holding("this")
    private void handleGracefulEndOfRib() {
        if (isLocalRestarting()) {
            if (missingEOT.isEmpty()) {
                createEffRibInWriter();
                effRibInWriter.init();
                registerPrefixesCounters(effRibInWriter, effRibInWriter);
                for (var key : getAfiSafisAdvertized()) {
                    createAdjRibOutListener(key, true);
                }
                setLocalRestartingState(false);
                setGracefulPreferences(false, Collections.emptySet());
            }
        }
    }

    @Override
    public synchronized void onSessionUp(final BGPSession session) {
        currentSession = session;
        sessionUp = true;

        final var chain = rib.createPeerDOMChain();
        ribOutChain = chain;
        chain.addCallback(new FutureCallback<>() {
            @Override
            public void onSuccess(final Empty result) {
                LOG.debug("RibOut transaction chain {} successful.", chain);
            }

            @Override
            public void onFailure(final Throwable cause) {
                onRibOutChainFailed(cause);
            }
        });

        if (currentSession instanceof BGPSessionStateProvider stateProvider) {
            stateProvider.registerMessagesCounter(this);
        }
        final var advertisedGracefulRestartCapability = session.getAdvertisedGracefulRestartCapability();
        final var advertisedTables = advertisedGracefulRestartCapability.getTables();
        final var advertisedLLTables = session.getAdvertisedLlGracefulRestartCapability().getTables();

        final var addPathTablesType = session.getAdvertisedAddPathTableTypes();
        final var advertizedTableTypes = session.getAdvertisedTableTypes();
        LOG.info("Session with peer {} went up with tables {} and Add Path tables {}", getName(), advertizedTableTypes,
            addPathTablesType);

        final var newTables = advertizedTableTypes.stream()
            .map(table -> new TablesKey(table.requireAfi(), table.requireSafi()))
            .collect(ImmutableSet.toImmutableSet());
        tables = newTables;

        addPathTableMaps = mapTableTypesFamilies(addPathTablesType);
        final boolean restartingLocally = isLocalRestarting();
        if (!restartingLocally) {
            addBgp4Support();
        }
        if (!isRestartingGracefully()) {
            peerId = RouterIds.createPeerId(session.getBgpId());

            peerPath = createPeerPath(peerId);
            peerRibOutIId = peerPath.node(ADJRIBOUT_NID);
            createEffRibInWriter();
            registerPrefixesCounters(effRibInWriter, effRibInWriter);

            effRibInWriter.init();
            ribWriter = ribWriter.transform(peerId, peerPath, rib.getRibSupportContext(), tables, addPathTableMaps);

            if (rpcRegistry != null) {
                final var bgpPeerHandler = new BgpPeerRpc(this, session, tables);
                rpcRegistration = rpcRegistry.registerRpcImplementations(List.of(
                    (ResetSession) bgpPeerHandler::resetSession,
                    (RestartGracefully) bgpPeerHandler::restartGracefully,
                    (RouteRefreshRequest) bgpPeerHandler::routeRefreshRequest),
                    ImmutableSet.of(rib.getInstanceIdentifier().toBuilder()
                        .child(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp
                            .rib.rib.Peer.class, new PeerKey(peerId))
                        .build()));
            }
        } else {
            final var forwardingTables = advertisedTables == null ? Collections.<TablesKey>emptySet()
                : advertisedTables.values().stream()
                    .filter(table -> {
                        final var afiFlags = table.getAfiFlags();
                        return afiFlags != null && afiFlags.getForwardingState();
                    })
                    .map(table -> new TablesKey(table.getAfi(), table.getSafi()))
                    .collect(Collectors.toSet());
            ribWriter.clearTables(Sets.difference(tables, forwardingTables));
            if (restartingLocally) {
                effRibInWriter.close();
                peerRestartStopwatch = Stopwatch.createStarted();
                handleSelectionReferralTimer();
                missingEOT.addAll(tables);
            }
        }
        setAdvertizedGracefulRestartTableTypes(advertisedTables == null ? List.of() : advertisedTables.values().stream()
            .map(table -> new TablesKey(table.getAfi(), table.getSafi()))
            .collect(Collectors.toList()));
        setAfiSafiGracefulRestartState(advertisedGracefulRestartCapability.getRestartTime().toJava(), false,
            restartingLocally);

        final Map<TablesKey, Uint24> llTablesReceived;
        if (advertisedLLTables != null) {
            llTablesReceived = new HashMap<>();
            for (var table : advertisedLLTables.values()) {
                llTablesReceived.put(new TablesKey(table.getAfi(), table.getSafi()), table.requireLongLivedStaleTime());
            }
        } else {
            llTablesReceived = Map.of();
        }
        setAdvertizedLlGracefulRestartTableTypes(llTablesReceived);

        if (!llTablesReceived.isEmpty()) {
            llgrSupport = true;
            // FIXME: propagate preserved tables
        } else {
            // FIXME: clear preserved tables
            llgrSupport = false;
        }

        if (!restartingLocally) {
            if (!newTables.contains(IPV4_UCAST_TABLE_KEY)) {
                createAdjRibOutListener(IPV4_UCAST_TABLE_KEY, false);
            }
            for (var key : getAfiSafisAdvertized()) {
                createAdjRibOutListener(key, true);
            }
        }

        // This initialization is separated from the !isRestartingGracefully() check at the top of this method
        // to enforce execution ordering. The listeners (created just above) must be created before the containers
        // are created and the peer is exposed via registration.
        if (!isRestartingGracefully()) {
            initializeAdjRibOutTables(ribWriter.initializationFuture());
        }

        if (treatAsWithdraw) {
            currentSession.addDecoderConstraint(RevisedErrorHandlingSupport.class, switch (getRole()) {
                case Ebgp -> RevisedErrorHandlingSupportImpl.forExternalPeer();
                case Ibgp, Internal, RrClient -> RevisedErrorHandlingSupportImpl.forInternalPeer();
            });
        }
    }

    @Holding("this")
    private void initializeAdjRibOutTables(
            final ListenableFuture<? extends CommitInfo> peerStructureInitialization) {
        // Explicitly initialize the adj-rib-out and tables containers for new session.
        // This prevents a crash if the peer sends an immediate WITHDRAW before any routes are advertised.
        final var tx = ribOutChain.newWriteOnlyTransaction();
        tx.merge(OPERATIONAL, peerRibOutIId, ImmutableNodes.newContainerBuilder()
            .withNodeIdentifier(ADJRIBOUT_NID)
            .withChild(ImmutableNodes.newSystemMapBuilder().withNodeIdentifier(TABLES_NID).build())
            .build());
        // The peer structure and its per-AFI/SAFI tables are initialized on a different transaction chain. Wait for
        // both commits before exposing the peer, so onPeerAdded() can safely populate its Adj-RIB-Out.
        final ListenableFuture<Empty> initialization = Futures
            .whenAllSucceed(peerStructureInitialization, tx.commit())
            .call(() -> {
                registerPeer();
                return Empty.value();
            }, registrationExecutor);
        Futures.addCallback(initialization, new FutureCallback<>() {
            @Override
            public void onSuccess(final Empty result) {
                // No-op
            }

            @Override
            public void onFailure(final Throwable cause) {
                if (!sessionUp) {
                    LOG.trace("Peer {} initialization completed after its session dropped", getPeerId(), cause);
                    return;
                }
                LOG.error("Failed to initialize peer {}", getPeerId(), cause);
                synchronized (BGPPeer.this) {
                    if (sessionUp) {
                        BGPPeer.this.releaseConnection(false);
                    }
                }
            }
        }, MoreExecutors.directExecutor());
    }

    private void registerPeer() {
        if (!sessionUp) {
            LOG.warn("Session for peer {} dropped before datastore initialization completed.", getPeerId());
            return;
        }
        // Register outside the BGPPeer lock. registerPeer synchronously calls LocRibWriter.onPeerAdded, which takes
        // the LocRibWriter lock. LocRibWriter.onDataTreeChanged takes the LocRibWriter lock then the BGPPeer lock, so
        // holding the BGPPeer lock here would reverse that order and deadlock.
        final var registration = rib.getPeerTracker().registerPeer(this);
        synchronized (this) {
            if (sessionUp) {
                trackerRegistration = registration;
            } else {
                LOG.warn("Session for peer {} dropped after registration, closing the registration.", peerId);
                registration.close();
            }
        }
    }

    private boolean isRestartingGracefully() {
        return isLocalRestarting() || isPeerRestarting();
    }

    private synchronized void createEffRibInWriter() {
        final var chain = rib.createPeerDOMChain();
        chain.addCallback(this);

        effRibInWriter = new EffectiveRibInWriter(this, rib, chain, peerPath, tables, tableTypeRegistry, rtMemberships,
            rtCache);
    }

    //try to add a support for old-school BGP-4, if peer did not advertise IPv4-Unicast MP capability
    @Holding("this")
    private void addBgp4Support() {
        if (!tables.contains(IPV4_UCAST_TABLE_KEY)) {
            final var newSet = new HashSet<>(tables);
            newSet.add(IPV4_UCAST_TABLE_KEY);
            tables = ImmutableSet.copyOf(newSet);
        }
    }

    @Holding("this")
    private void createAdjRibOutListener(final TablesKey key, final boolean mpSupport) {
        final var ribSupport = rib.getRibSupportContext().getRIBSupport(key);

        // not particularly nice
        if (ribSupport != null && currentSession instanceof BGPSessionImpl bgpSession) {
            final var adjRibOut = AdjRibOutListener.create(peerId, rib.getYangRibId(), rib.getCodecsRegistry(),
                ribSupport, rib.getService(), bgpSession.getLimiter(), mpSupport);
            adjRibOutListenerSet.put(key, adjRibOut);
            registerPrefixesSentCounter(key, adjRibOut);
        }
    }

    @Override
    public synchronized void onSessionDown(final BGPSession session, final Exception exc) {
        if (exc.getMessage().equals(BGPSessionImpl.END_OF_INPUT)) {
            LOG.info("Session with peer {} went down", getName());
        } else {
            LOG.info("Session with peer {} went down", getName(), exc);
        }
        releaseConnectionGracefully();
    }

    @Override
    public synchronized void onSessionTerminated(final BGPSession session, final BGPTerminationReason cause) {
        LOG.info("Session with peer {} terminated: {}", getName(), cause);
        releaseConnectionGracefully();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("name", getName()).add("tables", tables).toString();
    }

    @Override
    public synchronized FluentFuture<? extends CommitInfo> releaseConnection() {
        return releaseConnection(true);
    }

    /**
     * On transaction chain failure, we don't want to wait for future.
     *
     * @param isWaitForSubmitted if true, wait for submitted future before closing binding chain. if false, don't wait.
     */
    @Holding("this")
    private @NonNull FluentFuture<? extends CommitInfo> releaseConnection(final boolean isWaitForSubmitted) {
        LOG.info("Closing session with peer {}", getName());
        sessionUp = false;
        adjRibOutListenerSet.values().forEach(AdjRibOutListener::close);
        adjRibOutListenerSet.clear();
        final FluentFuture<? extends CommitInfo> future;
        // FIXME: this is a typical example of something which should be handled by a behavior into which we have
        //        transitioned way before this method is called. This really begs to be an abstract base class with
        //        a 'clearTables' or similar callout
        if (isRestartingGracefully()) {
            final var gracefulTables = getGracefulTables();
            ribWriter.storeStaleRoutes(gracefulTables);
            future = ribWriter.clearTables(Sets.difference(tables, gracefulTables));
            if (isPeerRestarting()) {
                peerRestartStopwatch = Stopwatch.createStarted();
                handleRestartTimer();
            }
        } else {
            future = terminateConnection();
        }
        releaseRibOutChain(isWaitForSubmitted);

        closeSession();
        return future;
    }

    @Holding("this")
    @SuppressWarnings("checkstyle:illegalCatch")
    private void closeSession() {
        if (currentSession != null) {
            try {
                if (isRestartingGracefully()) {
                    currentSession.closeWithoutMessage();
                } else {
                    currentSession.close();
                }
            } catch (final Exception e) {
                LOG.warn("Error closing session with peer", e);
            }
            currentSession = null;
        }
    }

    private Set<TablesKey> getGracefulTables() {
        return tables.stream()
                .filter(this::isGracefulRestartReceived)
                .filter(this::isGracefulRestartAdvertized)
                .collect(Collectors.toSet());
    }

    private synchronized FluentFuture<? extends CommitInfo> terminateConnection() {
        if (trackerRegistration != null) {
            trackerRegistration.close();
            trackerRegistration = null;
        }
        if (rpcRegistration != null) {
            rpcRegistration.close();
        }
        ribWriter.releaseChain();

        if (effRibInWriter != null) {
            effRibInWriter.close();
        }
        tables = ImmutableSet.of();
        addPathTableMaps = ImmutableMap.of();
        final var future = removePeer(peerPath);
        resetState();

        return future;
    }

    /**
     * If Graceful Restart Timer expires, remove all routes advertised by peer.
     */
    private synchronized void handleRestartTimer() {
        if (!isPeerRestarting()) {
            return;
        }

        final long peerRestartTimeNanos = TimeUnit.SECONDS.toNanos(getPeerRestartTime());
        final long elapsedNanos = peerRestartStopwatch.elapsed(TimeUnit.NANOSECONDS);
        if (elapsedNanos >= peerRestartTimeNanos) {
            setAfiSafiGracefulRestartState(0, false, false);
            onSessionTerminated(currentSession, new BGPTerminationReason(BGPError.HOLD_TIMER_EXPIRED));
        }

        currentSession.schedule(this::handleRestartTimer, peerRestartTimeNanos - elapsedNanos, TimeUnit.NANOSECONDS);
    }

    private synchronized void handleSelectionReferralTimer() {
        if (!isLocalRestarting()) {
            return;
        }

        final long referalTimerNanos = TimeUnit.SECONDS.toNanos(currentSelectionDeferralTimerSeconds);
        final long elapsedNanos = peerRestartStopwatch.elapsed(TimeUnit.NANOSECONDS);
        if (elapsedNanos >= referalTimerNanos) {
            missingEOT.clear();
            handleGracefulEndOfRib();
        }
        currentSession.schedule(this::handleSelectionReferralTimer, referalTimerNanos - elapsedNanos,
            TimeUnit.NANOSECONDS);
    }

    @Holding("this")
    private void releaseConnectionGracefully() {
        if (getPeerRestartTime() > 0) {
            setRestartingState();
        }
        releaseConnection(true);
    }

    @SuppressFBWarnings("IS2_INCONSISTENT_SYNC")
    @Override
    public SendReceive getSupportedAddPathTables(final TablesKey tableKey) {
        return addPathTableMaps.get(tableKey);
    }

    @Override
    public boolean supportsTable(final TablesKey tableKey) {
        return sessionUp && getAfiSafisAdvertized().contains(tableKey) && tables.contains(tableKey);
    }

    @Override
    public YangInstanceIdentifier getRibOutIId(final NodeIdentifierWithPredicates tablekey) {
        return tablesIId.getUnchecked(tablekey);
    }

    @Override
    public synchronized void onFailure(final Throwable cause) {
        LOG.error("Transaction domChain for peer {} failed.", getName(), cause);
        releaseConnection(true);
    }

    private synchronized void onRibOutChainFailed(final Throwable cause) {
        LOG.error("RibOut transaction chain for peer {} failed.", getName(), cause);
        releaseConnection(false);
    }

    @Override
    public synchronized void markUptodate(final TablesKey tablesKey) {
        ribWriter.markTableUptodate(tablesKey);
    }

    @Override
    public synchronized BGPSessionState getBGPSessionState() {
        return currentSession instanceof BGPSessionStateProvider stateProvider ? stateProvider.getBGPSessionState()
            : null;
    }

    @Override
    public synchronized BGPTimersState getBGPTimersState() {
        return currentSession instanceof BGPSessionStateProvider stateProvider ? stateProvider.getBGPTimersState()
            : null;
    }

    @Override
    public synchronized BGPTransportState getBGPTransportState() {
        return currentSession instanceof BGPSessionStateProvider stateProvider ? stateProvider.getBGPTransportState()
            : null;
    }

    @Override
    public List<RouteTarget> getMemberships() {
        return rtMemberships;
    }

    @Override
    public synchronized ListenableFuture<?> restartGracefully(final long selectionDeferralTimerSeconds) {
        final var tablesToPreserve = getGracefulTables();
        if (tablesToPreserve == null || tablesToPreserve.isEmpty()) {
            LOG.info("Peer {} is not capable of graceful restart or have no matching graceful tables.", peerId);
            return Futures.immediateFailedFuture(
                new UnsupportedOperationException("Peer is not capable of graceful restart"));
        }
        setGracefulPreferences(true, tablesToPreserve);
        currentSelectionDeferralTimerSeconds = selectionDeferralTimerSeconds;
        setLocalRestartingState(true);
        return releaseConnection(true);
    }

    @Override
    boolean supportsLLGR() {
        return llgrSupport;
    }

    private synchronized void setGracefulPreferences(final boolean localRestarting,
                                                     final Set<TablesKey> preservedTables) {
        final var gracefulTables = tables.stream()
                .filter(this::isGracefulRestartAdvertized)
                .collect(Collectors.toSet());
        final var bgpParameters = GracefulRestartUtil.getGracefulBgpParameters(
                bean.getBgpFixedCapabilities(), gracefulTables, preservedTables,
                bean.getGracefulRestartTimer(), localRestarting, Map.of(), unused -> false);
        final var oldPrefs = rib.getDispatcher().getBGPPeerRegistry().getPeerPreferences(getNeighborAddress());
        final var newPrefs = new BGPSessionPreferences(
                oldPrefs.getMyAs(),
                oldPrefs.getHoldTime(),
                oldPrefs.getBgpId(),
                oldPrefs.getExpectedRemoteAs(),
                Collections.singletonList(bgpParameters),
                oldPrefs.getMd5Password());
        rib.getDispatcher().getBGPPeerRegistry().updatePeerPreferences(getNeighborAddress(), newPrefs);
    }
}
