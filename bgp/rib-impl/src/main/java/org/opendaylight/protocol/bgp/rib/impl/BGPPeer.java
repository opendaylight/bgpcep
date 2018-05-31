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
import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.base.Objects;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.net.InetAddresses;
import com.google.common.util.concurrent.FluentFuture;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChain;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RoutedRpcRegistration;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPTableTypeRegistryConsumer;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPError;
import org.opendaylight.protocol.bgp.parser.impl.message.update.LocalPreferenceAttributeParser;
import org.opendaylight.protocol.bgp.parser.spi.MessageUtil;
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
import org.opendaylight.protocol.concepts.AbstractRegistration;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv4.prefixes.DestinationIpv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv4.prefixes.destination.ipv4.Ipv4Prefixes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv4.prefixes.destination.ipv4.Ipv4PrefixesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationIpv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.update.message.Nlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.BgpAddPathTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.RouteRefresh;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.SendReceive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.add.path.capability.AddressFamilies;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.update.attributes.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.update.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.update.attributes.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.update.attributes.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.update.attributes.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.update.attributes.mp.unreach.nlri.WithdrawnRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.peer.rpc.rev180329.BgpPeerRpcService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.peer.rpc.rev180329.PeerContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.rib.PeerKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.rib.peer.AdjRibOut;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.ClusterIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.SubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.UnicastSubsequentAddressFamily;
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

    private Set<TablesKey> tables = Collections.emptySet();
    private final RIB rib;
    private final Map<TablesKey, AdjRibOutListener> adjRibOutListenerSet = new HashMap<>();
    private final RpcProviderRegistry rpcRegistry;
    private final BGPTableTypeRegistryConsumer tableTypeRegistry;
    private InstanceIdentifier<AdjRibOut> peerRibOutIId;
    @GuardedBy("this")
    private AbstractRegistration trackerRegistration;
    private final LoadingCache<TablesKey, KeyedInstanceIdentifier<Tables, TablesKey>> tablesIId
            = CacheBuilder.newBuilder()
            .build(new CacheLoader<TablesKey, KeyedInstanceIdentifier<Tables, TablesKey>>() {
                @Override
                public KeyedInstanceIdentifier<Tables, TablesKey> load(final TablesKey tablesKey) {
                    return BGPPeer.this.peerRibOutIId.child(Tables.class, tablesKey);
                }
            });

    @GuardedBy("this")
    private BGPSession session;
    @GuardedBy("this")
    private final DOMTransactionChain chain;
    @GuardedBy("this")
    private AdjRibInWriter ribWriter;
    @GuardedBy("this")
    private EffectiveRibInWriter effRibInWriter;
    private RoutedRpcRegistration<BgpPeerRpcService> rpcRegistration;
    private Map<TablesKey, SendReceive> addPathTableMaps = Collections.emptyMap();
    private YangInstanceIdentifier peerPath;
    private boolean sessionUp;

    public BGPPeer(
            final BGPTableTypeRegistryConsumer tableTypeRegistry,
            final IpAddress neighborAddress,
            final String peerGroupName,
            final RIB rib,
            final PeerRole role,
            final ClusterIdentifier clusterId,
            final AsNumber localAs,
            final RpcProviderRegistry rpcRegistry,
            final Set<TablesKey> afiSafisAdvertized,
            final Set<TablesKey> afiSafisGracefulAdvertized) {
        super(rib, Ipv4Util.toStringIP(neighborAddress), peerGroupName, role, clusterId,
                localAs, neighborAddress, afiSafisAdvertized, afiSafisGracefulAdvertized);
        this.tableTypeRegistry = requireNonNull(tableTypeRegistry);
        this.rib = requireNonNull(rib);
        this.rpcRegistry = rpcRegistry;
        this.chain = rib.createPeerDOMChain(this);
    }

    BGPPeer(
            final BGPTableTypeRegistryConsumer tableTypeRegistry,
            final IpAddress neighborAddress,
            final RIB rib,
            final PeerRole role,
            final RpcProviderRegistry rpcRegistry,
            final Set<TablesKey> afiSafisAdvertized,
            final Set<TablesKey> afiSafisGracefulAdvertized) {
        this(tableTypeRegistry, neighborAddress, null, rib, role, null, null, rpcRegistry,
                afiSafisAdvertized, afiSafisGracefulAdvertized);
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
                nlriAnounced = message.getNlri().stream().filter(n -> Objects.equal(n.getPrefix(), w.getPrefix())
                        && Objects.equal(n.getPathId(), w.getPathId()))
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
        this.ribWriter = AdjRibInWriter.create(this.rib.getYangRibId(), this.peerRole, this.chain);
        setActive(true);
    }

    @Override
    public synchronized FluentFuture<? extends CommitInfo> close() {
        final FluentFuture<? extends CommitInfo> future = releaseConnection();
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
            onRouteRefreshMessage((RouteRefresh) msg);
        }
    }

    private void onRouteRefreshMessage(final RouteRefresh message) {
        final Class<? extends AddressFamily> rrAfi = message.getAfi();
        final Class<? extends SubsequentAddressFamily> rrSafi = message.getSafi();

        final TablesKey key = new TablesKey(rrAfi, rrSafi);
        final AdjRibOutListener listener = this.adjRibOutListenerSet.get(key);
        if (listener != null) {
            listener.close();
            this.adjRibOutListenerSet.remove(key);
            createAdjRibOutListener(key, listener.isMpSupported());
        } else {
            LOG.info("Ignoring RouteRefresh message. Afi/Safi is not supported: {}, {}.", rrAfi, rrSafi);
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
        MpUnreachNlri mpUnreach;
        if (message.getWithdrawnRoutes() != null) {
            mpUnreach = prefixesToMpUnreach(message, isAnyNlriAnnounced);
        } else {
            mpUnreach = MessageUtil.getMpUnreachNlri(attrs);
        }
        if (mpUnreach != null) {
            this.ribWriter.removeRoutes(mpUnreach);
        }
    }

    @Override
    public synchronized void onSessionUp(final BGPSession session) {
        this.session = session;
        this.sessionUp = true;
        if (this.session instanceof BGPSessionStateProvider) {
            ((BGPSessionStateProvider) this.session).registerMessagesCounter(this);
        }

        final List<AddressFamilies> addPathTablesType = session.getAdvertisedAddPathTableTypes();
        final Set<BgpTableType> advertizedTableTypes = session.getAdvertisedTableTypes();
        final List<BgpTableType> advertizedGracefulRestartTableTypes = session.getAdvertisedGracefulRestartTableTypes();
        LOG.info("Session with peer {} went up with tables {} and Add Path tables {}", this.name,
                advertizedTableTypes, addPathTablesType);
        this.rawIdentifier = InetAddresses.forString(session.getBgpId().getValue()).getAddress();
        this.peerId = RouterIds.createPeerId(session.getBgpId());
        final KeyedInstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib
                .rev180329.bgp.rib.rib.Peer, PeerKey> peerIId =
                getInstanceIdentifier().child(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns
                .yang.bgp.rib.rev180329.bgp.rib.rib.Peer.class, new PeerKey(this.peerId));
        final Set<TablesKey> setTables = advertizedTableTypes.stream().map(t -> new TablesKey(t.getAfi(), t.getSafi()))
                .collect(Collectors.toSet());
        this.tables = ImmutableSet.copyOf(setTables);
        this.effRibInWriter = new EffectiveRibInWriter(this, this.rib,
                this.rib.createPeerChain(this),
                peerIId, this.tables, this.tableTypeRegistry);
        registerPrefixesCounters(this.effRibInWriter, this.effRibInWriter);
        this.peerRibOutIId = peerIId.child(AdjRibOut.class);
        this.effRibInWriter.init();
        setAdvertizedGracefulRestartTableTypes(advertizedGracefulRestartTableTypes.stream()
                .map(t -> new TablesKey(t.getAfi(), t.getSafi())).collect(Collectors.toList()));
        this.addPathTableMaps = ImmutableMap.copyOf(mapTableTypesFamilies(addPathTablesType));
        this.trackerRegistration = this.rib.getPeerTracker().registerPeer(this);

        for (final TablesKey key : this.tables) {
            createAdjRibOutListener(key, true);
        }

        addBgp4Support();

        this.peerPath = createPeerPath();
        this.ribWriter = this.ribWriter.transform(this.peerId, this.peerPath, this.rib.getRibSupportContext(),
                this.tables, this.addPathTableMaps);

        if (this.rpcRegistry != null) {
            this.rpcRegistration = this.rpcRegistry.addRoutedRpcImplementation(BgpPeerRpcService.class,
                    new BgpPeerRpc(this, session, this.tables));
            final KeyedInstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib
                    .rev180329.bgp.rib.rib.Peer, PeerKey> path = this.rib.getInstanceIdentifier()
                    .child(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib
                             .rib.Peer.class, new PeerKey(this.peerId));
            this.rpcRegistration.registerPath(PeerContext.class, path);
        }
    }

    //try to add a support for old-school BGP-4, if peer did not advertise IPv4-Unicast MP capability
    private synchronized void addBgp4Support() {
        final TablesKey key = new TablesKey(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class);
        if (!this.tables.contains(key)) {
            final HashSet<TablesKey> newSet = new HashSet<>(this.tables);
            newSet.add(key);
            this.tables = ImmutableSet.copyOf(newSet);
            createAdjRibOutListener(key, false);
        }
    }

    private synchronized void createAdjRibOutListener(final TablesKey key,
            final boolean mpSupport) {
        final RIBSupport ribSupport = this.rib.getRibSupportContext().getRIBSupport(key);

        // not particularly nice
        if (ribSupport != null && this.session instanceof BGPSessionImpl) {
            final ChannelOutputLimiter limiter = ((BGPSessionImpl) this.session).getLimiter();
            final AdjRibOutListener adjRibOut = AdjRibOutListener.create(this.peerId, key,
                    this.rib.getYangRibId(), this.rib.getCodecsRegistry(), ribSupport,
                    this.rib.getService(), limiter, mpSupport);
            this.adjRibOutListenerSet.put(key, adjRibOut);
            registerPrefixesSentCounter(key, adjRibOut);
        }
    }

    private synchronized FluentFuture<? extends CommitInfo> cleanup() {
        // FIXME: BUG-196: support graceful
        this.adjRibOutListenerSet.values().forEach(AdjRibOutListener::close);
        this.adjRibOutListenerSet.clear();
        if (this.effRibInWriter != null) {
            this.effRibInWriter.close();
        }
        this.tables = Collections.emptySet();
        this.addPathTableMaps = Collections.emptyMap();
        return removePeer(this.chain, this.peerPath);
    }

    @Override
    public synchronized void onSessionDown(final BGPSession session, final Exception e) {
        if (e.getMessage().equals(BGPSessionImpl.END_OF_INPUT)) {
            LOG.info("Session with peer {} went down", this.name);
        } else {
            LOG.info("Session with peer {} went down", this.name, e);
        }
        releaseConnection();
    }

    @Override
    public synchronized void onSessionTerminated(final BGPSession session, final BGPTerminationReason cause) {
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
    public synchronized FluentFuture<? extends CommitInfo> releaseConnection() {
        LOG.info("Closing session with peer");
        this.sessionUp = false;
        closeRegistration();
        if (this.rpcRegistration != null) {
            this.rpcRegistration.close();
        }
        final FluentFuture<? extends CommitInfo> future = cleanup();

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

    private synchronized void closeRegistration() {
        if (this.trackerRegistration != null) {
            this.trackerRegistration.close();
            this.trackerRegistration = null;
        }
    }

    @SuppressFBWarnings("IS2_INCONSISTENT_SYNC")
    @Override
    public SendReceive getSupportedAddPathTables(final TablesKey tableKey) {
        return this.addPathTableMaps.get(tableKey);
    }

    @Override
    public boolean supportsTable(final TablesKey tableKey) {
        return this.tables.contains(tableKey) && this.sessionUp;
    }

    @Override
    public KeyedInstanceIdentifier<Tables, TablesKey> getRibOutIId(final TablesKey tablesKey) {
        return this.tablesIId.getUnchecked(tablesKey);
    }

    @Override
    public synchronized void onTransactionChainFailed(final TransactionChain<?, ?> chain,
            final AsyncTransaction<?, ?> transaction, final Throwable cause) {
        LOG.error("Transaction chain failed.", cause);
        this.chain.close();
        //FIXME BGPCEP-731
        /*
        this.chain = this.rib.createPeerDOMChain(this);
        this.ribWriter = AdjRibInWriter.create(this.rib.getYangRibId(), this.peerRole, this.chain);
        releaseConnection();*/
    }

    @Override
    public synchronized void markUptodate(final TablesKey tablesKey) {
        this.ribWriter.markTableUptodate(tablesKey);
    }

    @Override
    public synchronized BGPSessionState getBGPSessionState() {
        if (this.session instanceof BGPSessionStateProvider) {
            return ((BGPSessionStateProvider) this.session).getBGPSessionState();
        }
        return null;
    }

    @Override
    public synchronized BGPTimersState getBGPTimersState() {
        if (this.session instanceof BGPSessionStateProvider) {
            return ((BGPSessionStateProvider) this.session).getBGPTimersState();
        }
        return null;
    }

    @Override
    public synchronized BGPTransportState getBGPTransportState() {
        if (this.session instanceof BGPSessionStateProvider) {
            return ((BGPSessionStateProvider) this.session).getBGPTransportState();
        }
        return null;
    }
}
