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
import java.util.Optional;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.protocol.bgp.rib.spi.AbstractRIBSupport;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.PathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.McastVpnSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.mvpn.MvpnChoice;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.mvpn.routes.MvpnRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.mvpn.routes.mvpn.routes.MvpnRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.mvpn.routes.mvpn.routes.MvpnRouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.mvpn.routes.mvpn.routes.MvpnRouteKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.tables.Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.AddressFamily;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
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
public abstract class AbstractMvpnRIBSupport<C extends Routes & DataObject>
        extends AbstractRIBSupport<C, MvpnRoutes, MvpnRoute, MvpnRouteKey> {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractMvpnRIBSupport.class);
    private final NodeIdentifier nlriRoutesList;
    private final ImmutableCollection<Class<? extends DataObject>> cacheableNlriObjects;

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
            final Class<? extends AddressFamily> afiClass,
            final QName destContainerQname,
            final QName destListQname) {
        super(mappingService, cazeClass, MvpnRoutes.class, MvpnRoute.class, afiClass,
                McastVpnSubsequentAddressFamily.class, destContainerQname);
        this.nlriRoutesList = NodeIdentifier.create(destListQname);
        this.cacheableNlriObjects = ImmutableSet.of(cazeClass);

    }

    @Override
    public final ImmutableCollection<Class<? extends DataObject>> cacheableNlriObjects() {
        return this.cacheableNlriObjects;
    }

    @Override
    public final MvpnRoute createRoute(final MvpnRoute route, final String routeKey, final long pathId,
            final Attributes attributes) {
        final MvpnRouteBuilder builder;
        if (route != null) {
            builder = new MvpnRouteBuilder(route);
        } else {
            builder = new MvpnRouteBuilder();
        }
        return builder.withKey(createRouteListKey(pathId, routeKey)).setAttributes(attributes).build();
    }

    @Override
    public final MvpnRouteKey createRouteListKey(final long pathId, final String routeKey) {
        return new MvpnRouteKey(new PathId(pathId), routeKey);
    }

    final MvpnChoice extractMvpnChoice(final DataContainerNode<? extends PathArgument> route) {
        final DataObject nn = this.mappingService.fromNormalizedNode(this.routeDefaultYii, route).getValue();
        return ((MvpnRoute) nn).getMvpnChoice();
    }

    @Override
    protected final void processDestination(
            DOMDataWriteTransaction tx,
            final YangInstanceIdentifier routesPath,
            final ContainerNode destination,
            final ContainerNode attributes,
            final ApplyRoute function) {
        if (destination != null) {
            final Optional<DataContainerChild<? extends PathArgument, ?>> maybeRoutes = destination
                    .getChild(nlriRoutesList);
            if (maybeRoutes.isPresent()) {
                final DataContainerChild<? extends PathArgument, ?> routes = maybeRoutes.get();
                if (routes instanceof UnkeyedListNode) {
                    final YangInstanceIdentifier base = routesPath.node(routesContainerIdentifier()).node(routeNid());
                    for (final UnkeyedListEntryNode mvpnDest : ((UnkeyedListNode) routes).getValue()) {
                        final YangInstanceIdentifier.NodeIdentifierWithPredicates routeKey = createRouteKey(mvpnDest);
                        function.apply(tx, base, routeKey, mvpnDest, attributes);
                    }
                } else {
                    LOG.warn("Routes {} are not a map", routes);
                }
            }
        }
    }

    abstract NodeIdentifierWithPredicates createRouteKey(UnkeyedListEntryNode mvpn);
}
