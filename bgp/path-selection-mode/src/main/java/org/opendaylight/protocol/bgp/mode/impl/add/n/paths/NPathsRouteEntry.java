/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.mode.impl.add.n.paths;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import org.opendaylight.protocol.bgp.mode.impl.add.AddPathAbstractRouteEntry;
import org.opendaylight.protocol.bgp.mode.impl.add.AddPathBestPath;
import org.opendaylight.protocol.bgp.mode.impl.add.AddPathSelector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.tables.Routes;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.ChoiceIn;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class NPathsRouteEntry<C extends Routes & DataObject & ChoiceIn<Tables>, S extends ChildOf<? super C>,
        R extends Route & ChildOf<? super S> & Identifiable<I>, I extends Identifier<R>>
        extends AddPathAbstractRouteEntry<C, S, R, I> {
    private static final Logger LOG = LoggerFactory.getLogger(NPathsRouteEntry.class);

    private final int npaths;

    NPathsRouteEntry(final int npaths) {
        this.npaths = npaths;
    }

    @Override
    protected ImmutableList<AddPathBestPath> selectBest(final long localAs, final int size) {
        final int maxSearch = npaths == 0 ? size : Math.min(npaths, size);
        if (maxSearch == 0) {
            return ImmutableList.of();
        }

        // Scratch pool of offsets, we set them to true as we use them up
        final boolean[] offsets = new boolean[size];
        final Builder<AddPathBestPath> builder = ImmutableList.builderWithExpectedSize(maxSearch);
        for (int search = 0; search < maxSearch; ++search) {
            final AddPathSelector selector = new AddPathSelector(localAs);
            for (int offset = 0; offset < size; ++offset) {
                if (!offsets[offset]) {
                    processOffset(selector, offset);
                }
            }
            final AddPathBestPath result = selector.result();
            LOG.trace("Path {} selected {}", search, result);

            offsets[result.getOffset()] = true;
            builder.add(result);
        }

        return builder.build();
    }
}
