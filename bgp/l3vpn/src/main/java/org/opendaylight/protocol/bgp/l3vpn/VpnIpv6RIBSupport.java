/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.l3vpn;

import com.google.common.collect.ImmutableCollection;
import java.util.Collection;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.protocol.bgp.rib.spi.AbstractRIBSupport;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.CNextHop;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;

/**
 * @author Kevin Wang
 */
public final class VpnIpv6RIBSupport extends AbstractRIBSupport {
    /**
     * Default constructor. Requires the QName of the container augmented under the routes choice
     * node in instantiations of the rib grouping. It is assumed that this container is defined by
     * the same model which populates it with route grouping instantiation, and by extension with
     * the route attributes container.
     *
     * @param cazeClass      Binding class of the AFI/SAFI-specific case statement, must not be null
     * @param containerClass Binding class of the container in routes choice, must not be null.
     * @param listClass      Binding class of the route list, nust not be null;
     */
    protected VpnIpv6RIBSupport(Class<? extends Routes> cazeClass, Class<? extends DataObject> containerClass, Class<? extends Route> listClass) {
        super(cazeClass, containerClass, listClass);
    }

    @Nonnull
    @Override
    protected YangInstanceIdentifier.NodeIdentifier destinationContainerIdentifier() {
        return null;
    }

    @Override
    protected void deleteDestinationRoutes(DOMDataWriteTransaction tx, YangInstanceIdentifier tablePath, ContainerNode destination, YangInstanceIdentifier.NodeIdentifier routesNodeId) {

    }

    @Override
    protected void putDestinationRoutes(DOMDataWriteTransaction tx, YangInstanceIdentifier tablePath, ContainerNode destination, ContainerNode attributes, YangInstanceIdentifier.NodeIdentifier routesNodeId) {

    }

    @Nonnull
    @Override
    protected MpReachNlri buildReach(Collection<MapEntryNode> routes, CNextHop hop) {
        return null;
    }

    @Nonnull
    @Override
    protected MpUnreachNlri buildUnreach(Collection<MapEntryNode> routes) {
        return null;
    }

    @Nonnull
    @Override
    public ChoiceNode emptyRoutes() {
        return null;
    }

    @Nonnull
    @Override
    public ImmutableCollection<Class<? extends DataObject>> cacheableAttributeObjects() {
        return null;
    }

    @Nonnull
    @Override
    public ImmutableCollection<Class<? extends DataObject>> cacheableNlriObjects() {
        return null;
    }

    @Override
    public boolean isComplexRoute() {
        return false;
    }
}
