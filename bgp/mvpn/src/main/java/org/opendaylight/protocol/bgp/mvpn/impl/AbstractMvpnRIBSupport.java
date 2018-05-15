/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.mvpn.impl;

import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.protocol.bgp.rib.spi.AbstractRIBSupport;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.PathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.Attributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.update.attributes.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.update.attributes.mp.reach.nlri.AdvertizedRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.mvpn.MvpnChoice;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.mvpn.routes.MvpnRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.mvpn.routes.mvpn.routes.MvpnRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.mvpn.routes.mvpn.routes.MvpnRouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.mvpn.routes.mvpn.routes.MvpnRouteKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.tables.Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.SubsequentAddressFamily;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerNode;

/**
 * Abstract Mvpn RIBSupport.
 *
 * @author Claudio D. Gasparini
 */
public abstract class AbstractMvpnRIBSupport<C extends Routes & DataObject>
        extends AbstractRIBSupport<C, MvpnRoutes, MvpnRoute, MvpnRouteKey> {
    private final InstanceIdentifier<AdvertizedRoutes> destIid;

    /**
     * Default constructor. Requires the QName of the container augmented under the routes choice
     * node in instantiations of the rib grouping. It is assumed that this container is defined by
     * the same model which populates it with route grouping instantiation, and by extension with
     * the route attributes container.
     *
     * @param cazeClass        Binding class of the AFI/SAFI-specific case statement, must not be null
     * @param afiClass         address Family Class
     * @param safiClass        SubsequentAddressFamily
     * @param destinationQname destination Qname
     */
    protected AbstractMvpnRIBSupport(
            final BindingNormalizedNodeSerializer mappingService,
            final Class<C> cazeClass,
            final Class<? extends AddressFamily> afiClass,
            final Class<? extends SubsequentAddressFamily> safiClass,
            final QName destinationQname) {
        super(mappingService, cazeClass, MvpnRoutes.class, MvpnRoute.class, afiClass, safiClass, destinationQname);
        this.destIid = InstanceIdentifier.create(Update.class)
                .child(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path
                        .attributes.Attributes.class).augmentation(Attributes1.class)
                .child(MpReachNlri.class).child(AdvertizedRoutes.class);
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
        return builder.setKey(new MvpnRouteKey(new PathId(pathId), routeKey)).setAttributes(attributes).build();
    }

    @Override
    public final MvpnRouteKey createRouteListKey(final long pathId, final String routeKey) {
        return new MvpnRouteKey(new PathId(pathId), routeKey);
    }

    MvpnChoice extractMvpnChoiceFromRoute(final DataContainerNode<? extends PathArgument> route) {
        final YangInstanceIdentifier yii = this.mappingService.toYangInstanceIdentifier(this.routeIid)
                .node(route.getIdentifier());
        final DataObject nn = this.mappingService.fromNormalizedNode(yii, route).getValue();
        return ((MvpnRoute) nn).getMvpnChoice();
    }

    MvpnChoice extractMvpnChoice(final DataContainerNode<? extends PathArgument> mvpnDestination) {
        final YangInstanceIdentifier yii = this.mappingService.toYangInstanceIdentifier(this.destIid)
                .node(mvpnDestination.getIdentifier());
        final DataObject nn = this.mappingService.fromNormalizedNode(yii, mvpnDestination).getValue();
        return ((MvpnRoute) nn).getMvpnChoice();
    }
}
