/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi.entry;

import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.tables.Routes;
import org.opendaylight.yangtools.binding.ChildOf;
import org.opendaylight.yangtools.binding.ChoiceIn;
import org.opendaylight.yangtools.binding.DataObject;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;

/**
 * new Routes to be advertized.
 *
 * @author Claudio D. Gasparini
 */
public final class AdvertizedRoute<C extends Routes & DataObject & ChoiceIn<Tables>,
        S extends ChildOf<? super C>> extends AbstractAdvertizedRoute<C, S> {

    public AdvertizedRoute(final RIBSupport<C, S> ribSupport, final MapEntryNode route,
            final ContainerNode attributes, final PeerId fromPeerId, final boolean depreferenced) {
        this(ribSupport, true, route, attributes, fromPeerId, depreferenced);
    }

    public AdvertizedRoute(final RIBSupport<C, S> ribSupport, final boolean isFirstBestPath,
            final MapEntryNode route, final ContainerNode attributes, final PeerId fromPeerId,
            final boolean depreferenced) {
        super(ribSupport, route, fromPeerId, attributes, depreferenced, isFirstBestPath);
    }

    public boolean isFirstBestPath() {
        return isFirstBestPath;
    }
}
