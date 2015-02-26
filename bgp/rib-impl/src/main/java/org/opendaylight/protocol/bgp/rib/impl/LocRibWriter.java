/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.primitives.UnsignedInteger;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerId;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;

@NotThreadSafe
final class LocRibWriter implements DOMDataTreeChangeListener {
    private final Map<PathArgument, RouteEntry> routeEntries = new HashMap<>();

    private static UnsignedInteger routerId(final YangInstanceIdentifier path) {
        final NodeIdentifierWithPredicates peerKey = EffectiveRibInWriter.peerKey(path);
        final PeerId peerId = EffectiveRibInWriter.peerId(peerKey);
        return RouterIds.routerIdForPeerId(peerId);
    }

    @Override
    public void onDataTreeChanged(final Collection<DataTreeCandidate> changes) {
        final Map<PathArgument, RouteEntry> toUpdate = new HashMap<>();

        // Update our cached view of the world
        for (DataTreeCandidate tc : changes) {
            final PathArgument id = tc.getRootPath().getLastPathArgument();

            RouteEntry entry = routeEntries.get(id);
            if (tc.getRootNode().getDataAfter().isPresent()) {
                if (entry == null) {
                    entry = new RouteEntry();
                    routeEntries.put(id, entry);
                }

                entry.addRoute(routerId(tc.getRootPath()), (ContainerNode) tc.getRootNode().getDataAfter().get());
            } else if (entry != null) {
                if (entry.removeRoute(routerId(tc.getRootPath()))) {
                    routeEntries.remove(id);
                    entry = null;
                }
            }

            toUpdate.put(id, entry);
        }
    }
}
