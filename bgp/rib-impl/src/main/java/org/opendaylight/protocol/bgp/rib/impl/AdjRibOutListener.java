/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static java.util.Objects.requireNonNull;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.ADJRIBOUT_NID;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.PEER_NID;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.TABLES_NID;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.ClusteredDOMDataTreeChangeListener;
import org.opendaylight.mdsal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.protocol.bgp.rib.impl.spi.Codecs;
import org.opendaylight.protocol.bgp.rib.impl.spi.CodecsRegistry;
import org.opendaylight.protocol.bgp.rib.impl.state.peer.PrefixesSentCounters;
import org.opendaylight.protocol.bgp.rib.spi.IdentifierUtils;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.protocol.bgp.rib.spi.RibSupportUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv4.routes.ipv4.routes.Ipv4Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.PathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.UpdateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.update.message.NlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.update.message.WithdrawnRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeCandidateNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Instantiated for each peer and table, listens on a particular peer's adj-rib-out, performs transcoding to BA form
 * (message) and sends it down the channel. This class is NOT thread-safe.
 */
final class AdjRibOutListener implements ClusteredDOMDataTreeChangeListener, PrefixesSentCounters {
    private static final Logger LOG = LoggerFactory.getLogger(AdjRibOutListener.class);
    private static final QName PREFIX_QNAME = QName.create(Ipv4Route.QNAME, "prefix").intern();
    private static final QName PATHID_QNAME = QName.create(Ipv4Route.QNAME, "path-id").intern();
    private static final NodeIdentifier ROUTE_KEY_PREFIX_LEAF = NodeIdentifier.create(PREFIX_QNAME);
    private static final NodeIdentifier ROUTE_KEY_PATHID_LEAF = NodeIdentifier.create(PATHID_QNAME);

    private final ChannelOutputLimiter session;
    private final Codecs codecs;
    private final RIBSupport<?, ?> support;
    // FIXME: this field needs to be eliminated: either subclass this class or create a filtering ribsupport
    private final boolean mpSupport;
    private final ListenerRegistration<AdjRibOutListener> registerDataTreeChangeListener;
    private final LongAdder prefixesSentCounter = new LongAdder();
    private final TablesKey tablesKey;
    private boolean initalState;

    private AdjRibOutListener(final PeerId peerId, final TablesKey tablesKey, final YangInstanceIdentifier ribId,
            final CodecsRegistry registry, final RIBSupport<?, ?> support, final DOMDataTreeChangeService service,
            final ChannelOutputLimiter session, final boolean mpSupport) {
        this.session = requireNonNull(session);
        this.support = requireNonNull(support);
        codecs = registry.getCodecs(this.support);
        this.mpSupport = mpSupport;
        this.tablesKey = requireNonNull(tablesKey);
        final YangInstanceIdentifier adjRibOutId = ribId.node(PEER_NID).node(IdentifierUtils.domPeerId(peerId))
                .node(ADJRIBOUT_NID).node(TABLES_NID).node(RibSupportUtils.toYangTablesKey(tablesKey));
        /*
         *  After listener registration should always be executed ODTC. Even when empty table is present
         *  in data store. Within this first ODTC execution we should advertise present routes and than
         *  send EOR marker. initialState flag is distinguishing between first ODTC execution and the rest.
         */
        initalState = true;
        registerDataTreeChangeListener = service.registerDataTreeChangeListener(
                new DOMDataTreeIdentifier(LogicalDatastoreType.OPERATIONAL, adjRibOutId), this);
    }

    static AdjRibOutListener create(
            final @NonNull PeerId peerId,
            final @NonNull TablesKey tablesKey,
            final @NonNull YangInstanceIdentifier ribId,
            final @NonNull CodecsRegistry registry,
            final @NonNull RIBSupport<?, ?> support,
            final @NonNull DOMDataTreeChangeService service,
            final @NonNull ChannelOutputLimiter session,
            final boolean mpSupport) {
        return new AdjRibOutListener(peerId, tablesKey, ribId, registry, support, service, session, mpSupport);
    }

    @Override
    public void onInitialData() {
        // FIXME: flush initial state
    }

    @Override
    public void onDataTreeChanged(final List<DataTreeCandidate> changes) {
        LOG.debug("Data change received for AdjRibOut {}", changes);
        for (final DataTreeCandidate tc : changes) {
            LOG.trace("Change {} type {}", tc.getRootNode(), tc.getRootNode().getModificationType());
            for (final DataTreeCandidateNode child : tc.getRootNode().getChildNodes()) {
                for (final DataTreeCandidateNode route : support.changedRoutes(child)) {
                    processRouteChange(route);
                }
            }
        }
        if (initalState) {
            final Update endOfRib = BgpPeerUtil.createEndOfRib(tablesKey);
            session.write(endOfRib);
            initalState = false;
        }
        session.flush();
    }

    private void processRouteChange(final DataTreeCandidateNode route) {
        final Update update;
        switch (route.getModificationType()) {
            case UNMODIFIED:
                LOG.debug("Skipping unmodified route {}", route.getIdentifier());
                return;
            case DELETE:
            case DISAPPEARED:
                // FIXME: we can batch deletions into a single batch
                update = withdraw((MapEntryNode) route.getDataBefore().get());
                LOG.debug("Withdrawing routes {}", update);
                break;
            case APPEARED:
            case SUBTREE_MODIFIED:
            case WRITE:
                update = advertise((MapEntryNode) route.getDataAfter().get());
                LOG.debug("Advertising routes {}", update);
                break;
            default:
                LOG.warn("Ignoring unhandled modification type {}", route.getModificationType());
                return;
        }
        session.write(update);
    }

    private Attributes routeAttributes(final MapEntryNode route) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("AdjRibOut parsing route {}", NormalizedNodes.toStringTree(route));
        }
        final ContainerNode advertisedAttrs = (ContainerNode) NormalizedNodes.findNode(route,
                support.routeAttributesIdentifier()).orElse(null);
        return codecs.deserializeAttributes(advertisedAttrs);
    }

    private Update withdraw(final MapEntryNode route) {
        return mpSupport
            ? support.buildUpdate(Collections.emptyList(), Collections.singleton(route), routeAttributes(route))
                : buildUpdate(Collections.emptyList(), Collections.singleton(route), routeAttributes(route));
    }

    private Update advertise(final MapEntryNode route) {
        prefixesSentCounter.increment();
        return mpSupport
            ? support.buildUpdate(Collections.singleton(route), Collections.emptyList(), routeAttributes(route))
                : buildUpdate(Collections.singleton(route), Collections.emptyList(), routeAttributes(route));
    }

    private static Update buildUpdate(
            final @NonNull Collection<MapEntryNode> advertised,
            final @NonNull Collection<MapEntryNode> withdrawn,
            final @NonNull Attributes attr) {
        return new UpdateBuilder()
            .setWithdrawnRoutes(withdrawn.stream()
                .map(ipv4Route -> new WithdrawnRoutesBuilder()
                    .setPrefix(extractPrefix(ipv4Route))
                    .setPathId(extractPathId(ipv4Route))
                    .build())
                .collect(Collectors.toList()))
            .setNlri(advertised.stream()
                .map(ipv4Route -> new NlriBuilder()
                    .setPrefix(extractPrefix(ipv4Route))
                    .setPathId(extractPathId(ipv4Route)).build())
                .collect(Collectors.toList()))
            .setAttributes(attr).build();
    }

    private static Ipv4Prefix extractPrefix(final MapEntryNode ipv4Route) {
        return new Ipv4Prefix((String) ipv4Route.getChildByArg(ROUTE_KEY_PREFIX_LEAF).body());
    }

    private static PathId extractPathId(final MapEntryNode ipv4Route) {
        final var pathId = ipv4Route.childByArg(ROUTE_KEY_PATHID_LEAF);
        return pathId == null ? null : new PathId((Uint32) pathId.body());
    }

    public void close() {
        registerDataTreeChangeListener.close();
    }

    boolean isMpSupported() {
        return mpSupport;
    }

    @Override
    public long getPrefixesSentCount() {
        return prefixesSentCounter.longValue();
    }
}
