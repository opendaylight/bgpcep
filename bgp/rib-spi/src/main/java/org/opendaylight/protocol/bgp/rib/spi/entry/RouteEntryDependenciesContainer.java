/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi.entry;

import javax.annotation.Nonnull;
import org.opendaylight.protocol.bgp.rib.spi.BGPPeerTracker;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRibRoutingPolicy;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.AfiSafiType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.tables.Routes;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.ChoiceIn;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.Identifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

/**
 * Container wrapper for all dependencies related to Route Entry, required for process and storage.
 */
public interface RouteEntryDependenciesContainer {
    /**
     * Returns rib support.
     *
     * @return RIBSupport
     */
    @Nonnull
    <C extends Routes & DataObject & ChoiceIn<Tables>, S extends ChildOf<? super C>,
            R extends Route & ChildOf<? super S> & Identifiable<I>,
            I extends Identifier<R>> RIBSupport<C, S, R, I> getRIBSupport();

    /**
     * Returns the table key(AFI/SAFI) corresponding to the Route Entry.
     *
     * @return TablesKey
     */
    @Nonnull
    TablesKey getLocalTablesKey();

    /**
     * Returns the AfiSafiType(AFI/SAFI) corresponding to the Route Entry.
     *
     * @return TablesKey
     */
    @Nonnull
    Class<? extends AfiSafiType> getAfiSafType();

    /**
     * Returns the loc-rib table to be updated and to which  corresponds this Route Entry.
     *
     * @return InstanceIdentifier containing the path to loc-rib table.
     */
    @Nonnull
    KeyedInstanceIdentifier<Tables, TablesKey> getLocRibTableTarget();

    /**
     * Return routing policies defined per RIB.
     *
     * @return BGPRibRoutingPolicy
     */
    @Nonnull
    BGPRibRoutingPolicy getRoutingPolicies();

    @Nonnull
    BGPPeerTracker getPeerTracker();
}
