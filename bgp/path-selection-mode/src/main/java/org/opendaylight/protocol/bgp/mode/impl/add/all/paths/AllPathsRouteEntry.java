/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.mode.impl.add.all.paths;

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

final class AllPathsRouteEntry<C extends Routes & DataObject & ChoiceIn<Tables>, S extends ChildOf<? super C>,
        R extends Route & ChildOf<? super S> & Identifiable<I>, I extends Identifier<R>>
        extends AddPathAbstractRouteEntry<C, S, R, I> {
    private static final Logger LOG = LoggerFactory.getLogger(AllPathsRouteEntry.class);

    @Override
    protected ImmutableList<AddPathBestPath> selectBest(final long localAs, final int size) {
        // Select the best path for the case when AddPath is not supported
        final AddPathSelector selector = new AddPathSelector(localAs);
        for (int offset = 0; offset < size; ++offset) {
            processOffset(selector, offset);
        }

        final AddPathBestPath newBest = selector.result();
        LOG.trace("Best path selected {}", newBest);

        final Builder<AddPathBestPath> builder = ImmutableList.builderWithExpectedSize(size);
        builder.add(newBest);

        // Since we are selecting all paths, add all the other paths, do that in two steps, skipping the selected
        // route.
        for (int offset = 0; offset < newBest.getOffset(); ++offset) {
            builder.add(bestPathAt(offset));
        }
        for (int offset = newBest.getOffset() + 1; offset < size; ++offset) {
            builder.add(bestPathAt(offset));
        }

        return builder.build();
    }
}
