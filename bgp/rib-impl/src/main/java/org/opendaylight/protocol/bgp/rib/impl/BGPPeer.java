/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static java.util.Objects.requireNonNull;

import com.google.common.base.MoreObjects;
import com.google.common.base.Stopwatch;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.net.InetAddresses;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.checkerframework.checker.lock.qual.Holding;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.mdsal.binding.api.Transaction;
import org.opendaylight.mdsal.binding.api.TransactionChain;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.dom.api.DOMDataTreeTransaction;
import org.opendaylight.mdsal.dom.api.DOMTransactionChain;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPTableTypeRegistryConsumer;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPError;
import org.opendaylight.protocol.bgp.parser.impl.message.update.LocalPreferenceAttributeParser;
import org.opendaylight.protocol.bgp.parser.spi.MessageUtil;
import org.opendaylight.protocol.bgp.parser.spi.RevisedErrorHandlingSupport;
import org.opendaylight.protocol.bgp.rib.impl.config.BgpPeer;
import org.opendaylight.protocol.bgp.rib.impl.config.GracefulRestartUtil;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionPreferences;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIB;
import org.opendaylight.protocol.bgp.rib.impl.state.BGPSessionStateProvider;
import org.opendaylight.protocol.bgp.rib.spi.BGPSession;
import org.opendaylight.protocol.bgp.rib.spi.BGPSessionListener;
import org.opendaylight.protocol.bgp.rib.spi.BGPTerminationReason;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.protocol.bgp.rib.spi.RouterIds;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPSessionState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPTimersState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPTransportState;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv4.prefixes.DestinationIpv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv4.prefixes.destination.ipv4.Ipv4Prefixes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv4.prefixes.destination.ipv4.Ipv4PrefixesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationIpv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.update.message.Nlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.BgpAddPathTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.RouteRefresh;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.SendReceive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.GracefulRestartCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.add.path.capability.AddressFamilies;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.update.attributes.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.update.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.update.attributes.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.update.attributes.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.update.attributes.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.update.attributes.mp.unreach.nlri.WithdrawnRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.peer.rpc.rev180329.BgpPeerRpcService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.rib.PeerKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.rib.peer.AdjRibOut;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.ClusterIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.RouteTarget;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.SubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class representing a peer. We have a single instance for each peer, which provides translation from BGP events into
 * RIB actions.
 */
public class BGPPeer extends AbstractPeer implements BGPSessionListener {
    private static final Logger LOG = LoggerFactory.getLogger(BGPPeer.class);
    private static final TablesKey IPV4_UCAST_TABLE_KEY = new TablesKey(Ipv4AddressFamily.class,
        UnicastSubsequentAddressFamily.class);

    private ImmutableSet<TablesKey> tables = ImmutableSet.of();
    private final RIB rib;
    private final Map<TablesKey, AdjRibOutListener> adjRibOutListenerSet = new HashMap<>();
    private final List<RouteTarget> rtMemberships = new ArrayList<>();
    private final RpcProviderService rpcRegistry;
    private final BGPTableTypeRegistryConsumer tableTypeRegistry;
    private final BgpPeer bgpPeer;
    private InstanceIdentifier<AdjRibOut> peerRibOutIId;
    private KeyedInstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib
            .rev180329.bgp.rib.rib.Peer, PeerKey> peerIId;
    @GuardedBy("this")
    private Registration trackerRegistration;
    private final LoadingCache<TablesKey, KeyedInstanceIdentifier<Tables, TablesKey>> tablesIId
            = CacheBuilder.newBuilder()
            .build(new CacheLoader<TablesKey, KeyedInstanceIdentifier<Tables, TablesKey>>() {
                @Override
                public KeyedInstanceIdentifier<Tables, TablesKey> load(final TablesKey tablesKey) {
                    return BGPPeer.this.peerRibOutIId.child(Tables.class, tablesKey);
                }
            });

    @GuardedBy("this")
    private BGPSession currentSession;
    @GuardedBy("this")
    private AdjRibInWriter ribWriter;
    @GuardedBy("this")
    private EffectiveRibInWriter effRibInWriter;
    private ObjectRegistration<BgpPeerRpcService> rpcRegistration;
    private Map<TablesKey, SendReceive> addPathTableMaps = Collections.emptyMap();
    private YangInstanceIdentifier peerPath;
    private boolean sessionUp;
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
            final Map<TablesKey, Integer> llGracefulTablesAdvertised,
            final BgpPeer bgpPeer) {
        super(rib, Ipv4Util.toStringIP(neighborAddress), peerGroupName, role, clusterId,
                localAs, neighborAddress, afiSafisAdvertized, afiSafisGracefulAdvertized, llGracefulTablesAdvertised);
        this.tableTypeRegistry = requireNonNull(tableTypeRegistry);
        this.rib = requireNonNull(rib);
        this.rpcRegistry = rpcRegistry;
        this.bgpPeer = bgpPeer;
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
     * Creates MPReach for the prefixes to be handled in the same way as linkstate routes.
     *
     * @param message Update message containing prefixes in NLRI
     * @return MpReachNlri with prefixes from the nlri field
     */
    private static MpReachNlri prefixesToMpReach(final Update message) {
        final List<Ipv4Prefixes> prefixes = message.getNlri().stream()
                .map(n -> new Ipv4PrefixesBuilder().setPrefix(n.getPrefix()).setPathId(n.getPathId()).build())
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
     * Create MPUnreach for the prefixes to be handled in the same way as linkstate routes.
     *
     * @param message            Update message containing withdrawn routes
     * @param isAnyNlriAnnounced isAnyNlriAnnounced
     * @return MpUnreachNlri with prefixes from the withdrawn routes field
     */
    private static MpUnreachNlri prefixesToMpUnreach(final Update message, final boolean isAnyNlriAnnounced) {
        final List<Ipv4Prefixes> prefixes = new ArrayList<>();
        message.getWithdrawnRoutes().forEach(w -> {

            Optional<Nlri> nlriAnounced = Optional.empty();
            if (isAnyNlriAnnounced) {
                nlriAnounced = message.getNlri().stream().filter(n -> Objects.equals(n.getPrefix(), w.getPrefix())
                        && Objects.equals(n.getPathId(), w.getPathId()))
                        .findAny();
            }
            if (!nlriAnounced.isPresent()) {
                prefixes.add(new Ipv4PrefixesBuilder().setPrefix(w.getPrefix()).setPathId(w.getPathId()).build());
            }
        });
        return new MpUnreachNlriBuilder().setAfi(Ipv4AddressFamily.class).setSafi(UnicastSubsequentAddressFamily.class)
                .setWithdrawnRoutes(new WithdrawnRoutesBuilder().setDestinationType(new org.opendaylight.yang.gen.v1
                        .urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.update.attributes.mp.unreach.nlri
                        .withdrawn.routes.destination.type.DestinationIpv4CaseBuilder().setDestinationIpv4(
                        new DestinationIpv4Builder().setIpv4Prefixes(prefixes).build()).build()).build()).build();
    }

    private static Map<TablesKey, SendReceive> mapTableTypesFamilies(final List<AddressFamilies> addPathTablesType) {
        return ImmutableMap.copyOf(addPathTablesType.stream().collect(Collectors.toMap(af -> new TablesKey(af.getAfi(),
                        af.getSafi()), BgpAddPathTableType::getSendReceive)));
    }

    public synchronized void instantiateServiceInstance() {
        createDomChain();
        this.ribWriter = AdjRibInWriter.create(this.rib.getYangRibId(), this.peerRole, this);
        setActive(true);
    }

    @Override
    public synchronized FluentFuture<? extends CommitInfo> close() {
        final FluentFuture<? extends CommitInfo> future = releaseConnection(true);
        closeDomChain();
        setActive(false);
        return future;
    }

    @Override
    public void onMessage(final BGPSession session, final Notification msg) throws BGPDocumentedException {
        if (msg instanceof Update) {
            onUpdateMessage((Update) msg);
        } else if (msg instanceof RouteRefresh) {
            onRouteRefreshMessage((RouteRefresh) msg);
        } else {
            LOG.info("Ignoring unhandled message class {}", msg.getClass());
        }
    }

    private void onRouteRefreshMessage(final RouteRefresh message) {
        final Class<? extends AddressFamily> rrAfi = message.getAfi();
        final Class<? extends SubsequentAddressFamily> rrSafi = message.getSafi();

        final TablesKey key = new TablesKey(rrAfi, rrSafi);
        synchronized (this) {
            final AdjRibOutListener listener = this.adjRibOutListenerSet.remove(key);
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
     */
    private synchronized void onUpdateMessage(final Update message) throws BGPDocumentedException {
        checkMandatoryAttributesPresence(message);

        // update AdjRibs
        final Attributes attrs = message.getAttributes();
        MpReachNlri mpReach;
        final boolean isAnyNlriAnnounced = message.getNlri() != null;
        if (isAnyNlriAnnounced) {
            mpReach = prefixesToMpReach(message);
        } else {
            mpReach = MessageUtil.getMpReachNlri(attrs);
        }
        if (mpReach != null) {
            this.ribWriter.updateRoutes(mpReach, nextHopToAttribute(attrs, mpReach));
        }
        final MpUnreachNlri mpUnreach;
        if (message.getWithdrawnRoutes() != null) {
            mpUnreach = prefixesToMpUnreach(message, isAnyNlriAnnounced);
        } else {
            mpUnreach = MessageUtil.getMpUnreachNlri(attrs);
        }
        final boolean endOfRib = BgpPeerUtil.isEndOfRib(message);
        if (mpUnreach != null) {
            if (endOfRib) {
                final TablesKey tablesKey = new TablesKey(mpUnreach.getAfi(), mpUnreach.getSafi());
                this.ribWriter.removeStaleRoutes(tablesKey);
                this.missingEOT.remove(tablesKey);
                handleGracefulEndOfRib();
            } else {
                this.ribWriter.removeRoutes(mpUnreach);
            }
        } else if (endOfRib) {
            this.ribWriter.removeStaleRoutes(IPV4_UCAST_TABLE_KEY);
            this.missingEOT.remove(IPV4_UCAST_TABLE_KEY);
            handleGracefulEndOfRib();
        }
    }

    @Holding("this")
    private void handleGracefulEndOfRib() {
        if (isLocalRestarting()) {
            if (this.missingEOT.isEmpty()) {
                createEffRibInWriter();
                this.effRibInWriter.init();
                registerPrefixesCounters(this.effRibInWriter, this.effRibInWriter);
                for (final TablesKey key : getAfiSafisAdvertized()) {
                    createAdjRibOutListener(key, true);
                }
                setLocalRestartingState(false);
                setGracefulPreferences(false, Collections.emptySet());
            }
        }
    }

    @Override
    public synchronized void onSessionUp(final BGPSession session) {
        this.currentSession = session;
        this.sessionUp = true;
        this.bindingChain = this.rib.createPeerChain(this);
        if (this.currentSession instanceof BGPSessionStateProvider) {
            ((BGPSessionStateProvider) this.currentSession).registerMessagesCounter(this);
        }
        final GracefulRestartCapability advertisedGracefulRestartCapability =
                session.getAdvertisedGracefulRestartCapability();
        final var advertisedTables = advertisedGracefulRestartCapability.getTables();
        final var advertisedLLTables = session.getAdvertisedLlGracefulRestartCapability().getTables();

        final List<AddressFamilies> addPathTablesType = session.getAdvertisedAddPathTableTypes();
        final Set<BgpTableType> advertizedTableTypes = session.getAdvertisedTableTypes();
        LOG.info("Session with peer {} went up with tables {} and Add Path tables {}", this.name,
                advertizedTableTypes, addPathTablesType);
        final Set<TablesKey> setTables = advertizedTableTypes.stream().map(t -> new TablesKey(t.getAfi(), t.getSafi()))
                .collect(Collectors.toSet());
        this.tables = ImmutableSet.copyOf(setTables);

        this.addPathTableMaps = mapTableTypesFamilies(addPathTablesType);
        final boolean restartingLocally = isLocalRestarting();

        if (!isRestartingGracefully()) {
            this.rawIdentifier = InetAddresses.forString(session.getBgpId().getValue()).getAddress();
            this.peerId = RouterIds.createPeerId(session.getBgpId());
            this.peerIId = getInstanceIdentifier().child(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns
                            .yang.bgp.rib.rev180329.bgp.rib.rib.Peer.class, new PeerKey(this.peerId));
            this.peerPath = createPeerPath();
            this.peerRibOutIId = peerIId.child(AdjRibOut.class);
            this.trackerRegistration = this.rib.getPeerTracker().registerPeer(this);
            createEffRibInWriter();
            registerPrefixesCounters(this.effRibInWriter, this.effRibInWriter);

            this.effRibInWriter.init();
            this.ribWriter = this.ribWriter.transform(this.peerId, this.peerPath, this.rib.getRibSupportContext(),
                    this.tables, this.addPathTableMaps);

            if (this.rpcRegistry != null) {
                this.rpcRegistration = this.rpcRegistry.registerRpcImplementation(BgpPeerRpcService.class,
                    new BgpPeerRpc(this, session, this.tables), ImmutableSet.of(this.rib.getInstanceIdentifier().child(
                        org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.rib
                        .Peer.class, new PeerKey(this.peerId))));
            }
        } else {
            final Set<TablesKey> forwardingTables;
            if (advertisedTables == null) {
                forwardingTables = Collections.emptySet();
            } else {
                forwardingTables = advertisedTables.values().stream()
                        .filter(table -> table.getAfiFlags() != null)
                        .filter(table -> table.getAfiFlags().isForwardingState())
                        .map(table -> new TablesKey(table.getAfi(), table.getSafi()))
                        .collect(Collectors.toSet());
            }
            this.ribWriter.clearTables(Sets.difference(this.tables, forwardingTables));
            if (restartingLocally) {
                this.effRibInWriter.close();
                this.peerRestartStopwatch = Stopwatch.createStarted();
                handleSelectionReferralTimer();
                this.missingEOT.addAll(this.tables);
            }
        }
        if (advertisedTables == null || advertisedTables.isEmpty()) {
            setAdvertizedGracefulRestartTableTypes(Collections.emptyList());
        } else {
            setAdvertizedGracefulRestartTableTypes(advertisedTables.values().stream()
                    .map(t -> new TablesKey(t.getAfi(), t.getSafi())).collect(Collectors.toList()));
        }
        setAfiSafiGracefulRestartState(advertisedGracefulRestartCapability.getRestartTime().toJava(), false,
            restartingLocally);

        final Map<TablesKey, Integer> llTablesReceived;
        if (advertisedLLTables != null) {
            llTablesReceived = new HashMap<>();
            for (var table : advertisedLLTables.values()) {
                llTablesReceived.put(new TablesKey(table.getAfi(), table.getSafi()),
                    table.getLongLivedStaleTime().getValue().intValue());
            }
        } else {
            llTablesReceived = Collections.emptyMap();
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
            addBgp4Support();
            for (final TablesKey key : getAfiSafisAdvertized()) {
                createAdjRibOutListener(key, true);
            }
        }

        // SpotBugs does not grok Optional.ifPresent() and thinks we are using unsynchronized access
        final Optional<RevisedErrorHandlingSupport> errorHandling = this.bgpPeer.getErrorHandling();
        if (errorHandling.isPresent()) {
            this.currentSession.addDecoderConstraint(RevisedErrorHandlingSupport.class, errorHandling.get());
        }
    }

    private boolean isRestartingGracefully() {
        return isLocalRestarting() || isPeerRestarting();
    }

    private synchronized void createEffRibInWriter() {
        this.effRibInWriter = new EffectiveRibInWriter(this, this.rib,
            this.rib.createPeerDOMChain(this),
            this.peerPath, this.tables, this.tableTypeRegistry,
            this.rtMemberships,
            this.rtCache);
    }

    //try to add a support for old-school BGP-4, if peer did not advertise IPv4-Unicast MP capability
    @Holding("this")
    private void addBgp4Support() {
        if (!this.tables.contains(IPV4_UCAST_TABLE_KEY)) {
            final HashSet<TablesKey> newSet = new HashSet<>(this.tables);
            newSet.add(IPV4_UCAST_TABLE_KEY);
            this.tables = ImmutableSet.copyOf(newSet);
            createAdjRibOutListener(IPV4_UCAST_TABLE_KEY, false);
        }
    }

    @Holding("this")
    private void createAdjRibOutListener(final TablesKey key, final boolean mpSupport) {
        final RIBSupport<?, ?, ?, ?> ribSupport = this.rib.getRibSupportContext().getRIBSupport(key);

        // not particularly nice
        if (ribSupport != null && this.currentSession instanceof BGPSessionImpl) {
            final ChannelOutputLimiter limiter = ((BGPSessionImpl) this.currentSession).getLimiter();
            final AdjRibOutListener adjRibOut = AdjRibOutListener.create(this.peerId, key,
                    this.rib.getYangRibId(), this.rib.getCodecsRegistry(), ribSupport,
                    this.rib.getService(), limiter, mpSupport);
            this.adjRibOutListenerSet.put(key, adjRibOut);
            registerPrefixesSentCounter(key, adjRibOut);
        }
    }

    @Override
    public synchronized void onSessionDown(final BGPSession session, final Exception exc) {
        if (exc.getMessage().equals(BGPSessionImpl.END_OF_INPUT)) {
            LOG.info("Session with peer {} went down", this.name);
        } else {
            LOG.info("Session with peer {} went down", this.name, exc);
        }
        releaseConnectionGracefully();
    }

    @Override
    public synchronized void onSessionTerminated(final BGPSession session, final BGPTerminationReason cause) {
        LOG.info("Session with peer {} terminated: {}", this.name, cause);
        releaseConnectionGracefully();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("name", this.name).add("tables", this.tables).toString();
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
        LOG.info("Closing session with peer");
        this.sessionUp = false;
        this.adjRibOutListenerSet.values().forEach(AdjRibOutListener::close);
        this.adjRibOutListenerSet.clear();
        final FluentFuture<? extends CommitInfo> future;
        if (!isRestartingGracefully()) {
            future = terminateConnection();
        } else {
            final Set<TablesKey> gracefulTables = getGracefulTables();
            this.ribWriter.storeStaleRoutes(gracefulTables);
            future = this.ribWriter.clearTables(Sets.difference(this.tables, gracefulTables));
            if (isPeerRestarting()) {
                this.peerRestartStopwatch = Stopwatch.createStarted();
                handleRestartTimer();
            }
        }
        releaseBindingChain(isWaitForSubmitted);

        closeSession();
        return future;
    }

    @Holding("this")
    @SuppressWarnings("checkstyle:illegalCatch")
    private void closeSession() {
        if (this.currentSession != null) {
            try {
                if (isRestartingGracefully()) {
                    this.currentSession.closeWithoutMessage();
                } else {
                    this.currentSession.close();
                }
            } catch (final Exception e) {
                LOG.warn("Error closing session with peer", e);
            }
            this.currentSession = null;
        }
    }

    private Set<TablesKey> getGracefulTables() {
        return this.tables.stream()
                .filter(this::isGracefulRestartReceived)
                .filter(this::isGracefulRestartAdvertized)
                .collect(Collectors.toSet());
    }

    private synchronized FluentFuture<? extends CommitInfo> terminateConnection() {
        final FluentFuture<? extends CommitInfo> future;
        if (this.trackerRegistration != null) {
            this.trackerRegistration.close();
            this.trackerRegistration = null;
        }
        if (this.rpcRegistration != null) {
            this.rpcRegistration.close();
        }
        this.ribWriter.releaseChain();

        if (this.effRibInWriter != null) {
            this.effRibInWriter.close();
        }
        this.tables = ImmutableSet.of();
        this.addPathTableMaps = Collections.emptyMap();
        future = removePeer(this.peerPath);
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
        final long elapsedNanos = this.peerRestartStopwatch.elapsed(TimeUnit.NANOSECONDS);
        if (elapsedNanos >= peerRestartTimeNanos) {
            setAfiSafiGracefulRestartState(0, false, false);
            onSessionTerminated(this.currentSession, new BGPTerminationReason(BGPError.HOLD_TIMER_EXPIRED));
        }

        currentSession.schedule(this::handleRestartTimer, peerRestartTimeNanos - elapsedNanos, TimeUnit.NANOSECONDS);
    }

    private synchronized void handleSelectionReferralTimer() {
        if (!isLocalRestarting()) {
            return;
        }

        final long referalTimerNanos = TimeUnit.SECONDS.toNanos(this.currentSelectionDeferralTimerSeconds);
        final long elapsedNanos = this.peerRestartStopwatch.elapsed(TimeUnit.NANOSECONDS);
        if (elapsedNanos >= referalTimerNanos) {
            this.missingEOT.clear();
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
        return this.addPathTableMaps.get(tableKey);
    }

    @Override
    public boolean supportsTable(final TablesKey tableKey) {
        return this.sessionUp && getAfiSafisAdvertized().contains(tableKey) && this.tables.contains(tableKey);
    }

    @Override
    public KeyedInstanceIdentifier<Tables, TablesKey> getRibOutIId(final TablesKey tablesKey) {
        return this.tablesIId.getUnchecked(tablesKey);
    }

    @Override
    public synchronized void onTransactionChainFailed(final DOMTransactionChain chain,
            final DOMDataTreeTransaction transaction, final Throwable cause) {
        LOG.error("Transaction domChain failed.", cause);
        releaseConnection(true);
    }

    @Override
    public synchronized void onTransactionChainFailed(final TransactionChain chain, final Transaction transaction,
            final Throwable cause) {
        LOG.error("Transaction domChain failed.", cause);
        releaseConnection(false);
    }

    @Override
    public synchronized void markUptodate(final TablesKey tablesKey) {
        this.ribWriter.markTableUptodate(tablesKey);
    }

    @Override
    public synchronized BGPSessionState getBGPSessionState() {
        if (this.currentSession instanceof BGPSessionStateProvider) {
            return ((BGPSessionStateProvider) this.currentSession).getBGPSessionState();
        }
        return null;
    }

    @Override
    public synchronized BGPTimersState getBGPTimersState() {
        if (this.currentSession instanceof BGPSessionStateProvider) {
            return ((BGPSessionStateProvider) this.currentSession).getBGPTimersState();
        }
        return null;
    }

    @Override
    public synchronized BGPTransportState getBGPTransportState() {
        if (this.currentSession instanceof BGPSessionStateProvider) {
            return ((BGPSessionStateProvider) this.currentSession).getBGPTransportState();
        }
        return null;
    }

    @Override
    public List<RouteTarget> getMemberships() {
        return this.rtMemberships;
    }

    @Override
    public synchronized ListenableFuture<?> restartGracefully(final long selectionDeferralTimerSeconds) {
        final Set<TablesKey> tablesToPreserve = getGracefulTables();
        if (tablesToPreserve == null || tablesToPreserve.isEmpty()) {
            LOG.info("Peer {} is not capable of graceful restart or have no matching graceful tables.", this.peerId);
            return Futures.immediateFailedFuture(new UnsupportedOperationException(
                    "Peer is not capable of graceful restart"));
        }
        setGracefulPreferences(true, tablesToPreserve);
        this.currentSelectionDeferralTimerSeconds = selectionDeferralTimerSeconds;
        setLocalRestartingState(true);
        return releaseConnection(true);
    }

    @Override
    boolean supportsLLGR() {
        return this.llgrSupport;
    }

    private synchronized void setGracefulPreferences(final boolean localRestarting,
                                                     final Set<TablesKey> preservedTables) {
        final Set<TablesKey> gracefulTables = this.tables.stream()
                .filter(this::isGracefulRestartAdvertized)
                .collect(Collectors.toSet());
        final BgpParameters bgpParameters = GracefulRestartUtil.getGracefulBgpParameters(
                this.bgpPeer.getBgpFixedCapabilities(), gracefulTables, preservedTables,
                this.bgpPeer.getGracefulRestartTimer(), localRestarting, Collections.emptySet());
        final BGPSessionPreferences oldPrefs = this.rib.getDispatcher().getBGPPeerRegistry()
                .getPeerPreferences(getNeighborAddress());
        final BGPSessionPreferences newPrefs = new BGPSessionPreferences(
                oldPrefs.getMyAs(),
                oldPrefs.getHoldTime(),
                oldPrefs.getBgpId(),
                oldPrefs.getExpectedRemoteAs(),
                Collections.singletonList(bgpParameters),
                oldPrefs.getMd5Password());
        this.rib.getDispatcher().getBGPPeerRegistry()
                .updatePeerPreferences(getNeighborAddress(), newPrefs);
    }
}
