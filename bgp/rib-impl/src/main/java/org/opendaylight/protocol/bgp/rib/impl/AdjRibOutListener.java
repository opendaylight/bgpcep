/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.base.Preconditions;
import com.google.common.primitives.UnsignedInteger;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.UpdateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerId;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;

/**
 * Instantiated for each peer and table, listens on a particular peer's adj-rib-out,
 * performs transcoding to BA form (message) and sends it down the channel.
 */
@NotThreadSafe
final class AdjRibOutListener implements DOMDataTreeChangeListener {
    private final ChannelOutputLimiter session;
    private final RIBSupport ribSupport;
    private final Map<PathArgument, RouteEntry> routeEntries = new HashMap<>();

    AdjRibOutListener(final RIBSupport ribSupport, final ChannelOutputLimiter session) {
        this.ribSupport = Preconditions.checkNotNull(ribSupport);
        this.session = Preconditions.checkNotNull(session);
    }

    static AdjRibOutListener create(final RIBSupport ribSupport, final ChannelOutputLimiter session) {
        return new AdjRibOutListener(ribSupport, session);
    }

    @Override
    public void onDataTreeChanged(final Collection<DataTreeCandidate> changes) {
        // FIXME: refactor to avoid code duplications
        final Map<RouteUpdateKey, RouteEntry> toUpdate = new HashMap<>();
        for (final DataTreeCandidate tc : changes) {
            final YangInstanceIdentifier path = tc.getRootPath();
            final PathArgument routeId = path.getLastPathArgument();
            final NodeIdentifierWithPredicates peerKey = IdentifierUtils.peerKey(path);
            final PeerId peerId = IdentifierUtils.peerId(peerKey);
            final UnsignedInteger routerId = RouterIds.routerIdForPeerId(peerId);

            RouteEntry entry = this.routeEntries.get(routeId);
            if (tc.getRootNode().getDataAfter().isPresent()) {
                if (entry == null) {
                    entry = new RouteEntry();
                    this.routeEntries.put(routeId, entry);
                }
                entry.addRoute(routerId, (ContainerNode) tc.getRootNode().getDataAfter().get());
            } else if (entry != null && entry.removeRoute(routerId)) {
                this.routeEntries.remove(routeId);
                entry = null;
            }
            toUpdate.put(new RouteUpdateKey(peerId, routeId), entry);
        }
        for (final Entry<RouteUpdateKey, RouteEntry> update : toUpdate.entrySet()) {
            final UpdateBuilder ub = new UpdateBuilder();

            // FIXME: fill the structure (use codecs)

            this.session.write(ub.build());
        }
        this.session.flush();
    }
}
