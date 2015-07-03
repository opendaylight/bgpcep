/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIBSupportContextRegistry;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.protocol.bgp.rib.spi.RibSupportUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv4.routes.ipv4.routes.Ipv4Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.UpdateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.message.NlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.message.WithdrawnRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.rib.Peer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.rib.peer.AdjRibOut;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Instantiated for each peer and table, listens on a particular peer's adj-rib-out,
 * performs transcoding to BA form (message) and sends it down the channel.
 */
@NotThreadSafe
final class AdjRibOutListener implements DOMDataTreeChangeListener {

    private static final Logger LOG = LoggerFactory.getLogger(AdjRibOutListener.class);

    static final QName PREFIX_QNAME = QName.cachedReference(QName.create(Ipv4Route.QNAME, "prefix"));
    private final YangInstanceIdentifier.NodeIdentifier routeKeyLeaf = new YangInstanceIdentifier.NodeIdentifier(PREFIX_QNAME);

    private final ChannelOutputLimiter session;
    private final RIBSupportContextImpl context;
    private final RIBSupport support;
    private final boolean mpSupport;

    private AdjRibOutListener(final PeerId peerId, final TablesKey tablesKey, final YangInstanceIdentifier ribId, final DOMDataTreeChangeService service, final RIBSupportContextRegistry registry, final ChannelOutputLimiter session, final boolean mpSupport) {
        this.session = Preconditions.checkNotNull(session);
        this.context = (RIBSupportContextImpl) registry.getRIBSupportContext(tablesKey);
        this.support = this.context.getRibSupport();
        this.mpSupport = mpSupport;
        final YangInstanceIdentifier adjRibOutId = ribId.node(Peer.QNAME).node(IdentifierUtils.domPeerId(peerId)).node(AdjRibOut.QNAME).node(Tables.QNAME).node(RibSupportUtils.toYangTablesKey(tablesKey));
        service.registerDataTreeChangeListener(new DOMDataTreeIdentifier(LogicalDatastoreType.OPERATIONAL, adjRibOutId), this);
    }

    static AdjRibOutListener create(@Nonnull final PeerId peerId, @Nonnull final TablesKey tablesKey, @Nonnull final YangInstanceIdentifier ribId,
                                    @Nonnull final DOMDataTreeChangeService service, @Nonnull final RIBSupportContextRegistry registry,
                                    @Nonnull final ChannelOutputLimiter session, @Nonnull final boolean mpSupport) {
        return new AdjRibOutListener(peerId, tablesKey, ribId, service, registry, session, mpSupport);
    }

    @Override
    public void onDataTreeChanged(final Collection<DataTreeCandidate> changes) {
        LOG.debug("Data change received for AdjRibOut {}", changes);
        for (final DataTreeCandidate tc : changes) {
            LOG.trace("Change {} type {}", tc.getRootNode(), tc.getRootNode().getModificationType());
            for (final DataTreeCandidateNode child : tc.getRootNode().getChildNodes()) {
                for (final DataTreeCandidateNode route : this.context.getRibSupport().changedRoutes(child)) {
                    final Update update;

                    switch (route.getModificationType()) {
                    case UNMODIFIED:
                        LOG.debug("Skipping unmodified route {}", route.getIdentifier());
                        continue;
                    case DELETE:
                        // FIXME: we can batch deletions into a single batch
                        update = withdraw((MapEntryNode) route.getDataBefore().get());
                        break;
                    case SUBTREE_MODIFIED:
                    case WRITE:
                        update = advertise((MapEntryNode) route.getDataAfter().get());
                        break;
                    default:
                        LOG.warn("Ignoring unhandled modification type {}", route.getModificationType());
                        continue;
                    }

                    LOG.debug("Writing update {}", update);
                    this.session.write(update);
                }
            }
        }
        this.session.flush();
    }

    private Attributes routeAttributes(final MapEntryNode route) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("AdjRibOut parsing route {}", NormalizedNodes.toStringTree(route));
        }

        final ContainerNode advertisedAttrs = (ContainerNode) NormalizedNodes.findNode(route, this.support.routeAttributesIdentifier()).orNull();
        return this.context.deserializeAttributes(advertisedAttrs);
    }

    private Update withdraw(final MapEntryNode route) {
        return this.support.buildUpdate(Collections.<MapEntryNode>emptyList(), Collections.singleton(route), routeAttributes(route));
    }

    private Update advertise(final MapEntryNode route) {
        if (!mpSupport) {
            return buildUpdate(Collections.singleton(route), Collections.<MapEntryNode>emptyList(), routeAttributes(route));
        }
        return this.support.buildUpdate(Collections.singleton(route), Collections.<MapEntryNode>emptyList(), routeAttributes(route));
    }

    private Update buildUpdate(@Nonnull final Collection<MapEntryNode> advertised, @Nonnull final Collection<MapEntryNode> withdrawn, @Nonnull Attributes attr) {
        final UpdateBuilder ub = new UpdateBuilder()
            .setWithdrawnRoutes(new WithdrawnRoutesBuilder().setWithdrawnRoutes(extractPrefixes(withdrawn)).build())
            .setNlri(new NlriBuilder().setNlri(extractPrefixes(advertised)).build());
        ub.setAttributes(attr);
        return ub.build();
    }

    private List<Ipv4Prefix> extractPrefixes(final Collection<MapEntryNode> routes) {
        final List<Ipv4Prefix> prefs = new ArrayList<>(routes.size());
        for (final MapEntryNode ipv4Route : routes) {
            final String prefix = (String) ipv4Route.getChild(this.routeKeyLeaf).get().getValue();
            prefs.add(new Ipv4Prefix(prefix));
        }
        return prefs;
    }
}
