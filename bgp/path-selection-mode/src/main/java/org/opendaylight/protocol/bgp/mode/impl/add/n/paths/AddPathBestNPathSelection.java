/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.mode.impl.add.n.paths;

import static com.google.common.base.Preconditions.checkArgument;

import org.opendaylight.protocol.bgp.mode.api.PathSelectionMode;
import org.opendaylight.protocol.bgp.mode.api.RouteEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.tables.Routes;
import org.opendaylight.yangtools.binding.ChildOf;
import org.opendaylight.yangtools.binding.ChoiceIn;
import org.opendaylight.yangtools.binding.DataObject;

public final class AddPathBestNPathSelection implements PathSelectionMode {
    private final int npaths;

    public AddPathBestNPathSelection(final int npaths) {
        checkArgument(npaths > 1);
        this.npaths = npaths;
    }

    @Override
    public void close() {
        //no-op
    }

    @Override
    public <C extends Routes & DataObject & ChoiceIn<Tables>, S extends ChildOf<? super C>>
            RouteEntry<C, S> createRouteEntry() {
        return new NPathsRouteEntry<>(this.npaths);
    }
}