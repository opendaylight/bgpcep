/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.mvpn.impl;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.protocol.bgp.rib.spi.AbstractRIBSupport;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev200120.McastVpnSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev200120.MvpnRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev200120.mvpn.MvpnChoice;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev200120.mvpn.routes.MvpnRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.tables.Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.AddressFamily;
import org.opendaylight.yangtools.yang.binding.BindingObject;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.ChoiceIn;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract Mvpn RIBSupport.
 *
 * @author Claudio D. Gasparini
 */
abstract class AbstractMvpnRIBSupport<C extends Routes & DataObject & ChoiceIn<Tables>,
        S extends ChildOf<? super C> & MvpnRoutes> extends AbstractRIBSupport<C, S, MvpnRoute> {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractMvpnRIBSupport.class);
    private final NodeIdentifier nlriRoutesList;
    private final ImmutableCollection<Class<? extends BindingObject>> cacheableNlriObjects;

    /**
     * Default constructor. Requires the QName of the container augmented under the routes choice
     * node in instantiations of the rib grouping. It is assumed that this container is defined by
     * the same model which populates it with route grouping instantiation, and by extension with
     * the route attributes container.
     *
     * @param mappingService     Serialization service
     * @param cazeClass          Binding class of the AFI/SAFI-specific case statement, must not be null
     * @param afiClass           address Family Class
     * @param destContainerQname destination container Qname
     * @param destListQname      destinations list Qname
     */
    AbstractMvpnRIBSupport(
            final BindingNormalizedNodeSerializer mappingService,
            final Class<C> cazeClass,
            final Class<S> containerClass,
            final AddressFamily afiClass,
            final QName destContainerQname,
            final QName destListQname) {
        super(mappingService, cazeClass, containerClass, MvpnRoute.class, afiClass,
                McastVpnSubsequentAddressFamily.VALUE, destContainerQname);
        this.nlriRoutesList = NodeIdentifier.create(destListQname);
        this.cacheableNlriObjects = ImmutableSet.of(cazeClass);

    }

    @Override
    public final ImmutableCollection<Class<? extends BindingObject>> cacheableNlriObjects() {
        return this.cacheableNlriObjects;
    }

    final MvpnChoice extractMvpnChoice(final DataContainerNode route) {
        final DataObject nn = this.mappingService.fromNormalizedNode(this.routeDefaultYii, route).getValue();
        return ((MvpnRoute) nn).getMvpnChoice();
    }

    @Override
    protected final Collection<NodeIdentifierWithPredicates> processDestination(
            final DOMDataTreeWriteTransaction tx,
            final YangInstanceIdentifier routesPath,
            final ContainerNode destination,
            final ContainerNode attributes,
            final ApplyRoute function) {
        if (destination != null) {
            final DataContainerChild routes = destination.childByArg(this.nlriRoutesList);
            if (routes != null) {
                if (routes instanceof UnkeyedListNode) {
                    final YangInstanceIdentifier base = routesYangInstanceIdentifier(routesPath);
                    final Collection<UnkeyedListEntryNode> routesList = ((UnkeyedListNode) routes).body();
                    final List<NodeIdentifierWithPredicates> keys = new ArrayList<>(routesList.size());
                    for (final UnkeyedListEntryNode mvpnDest : routesList) {
                        final NodeIdentifierWithPredicates routeKey = createRouteKey(mvpnDest);
                        function.apply(tx, base, routeKey, mvpnDest, attributes);
                        keys.add(routeKey);
                    }
                    return keys;
                }
                LOG.warn("Routes {} are not a map", routes);
            }
        }
        return Collections.emptyList();
    }

    abstract NodeIdentifierWithPredicates createRouteKey(UnkeyedListEntryNode mvpn);
}
