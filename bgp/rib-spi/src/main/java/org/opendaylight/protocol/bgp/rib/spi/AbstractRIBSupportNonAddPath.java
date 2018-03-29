/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.spi;

import static org.opendaylight.protocol.bgp.parser.spi.PathIdUtil.NON_PATH_ID_VALUE;

import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.Route;
import org.opendaylight.yangtools.yang.binding.Identifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;

public abstract class AbstractRIBSupportNonAddPath<R extends Route, N extends Identifier>
        extends AbstractRIBSupport<R, N> {
    /**
     * Default constructor. Requires the QName of the container augmented under the routes choice
     * node in instantiations of the rib grouping. It is assumed that this container is defined by
     * the same model which populates it with route grouping instantiation, and by extension with
     * the route attributes container.
     *
     * @param cazeClass        Binding class of the AFI/SAFI-specific case statement, must not be null
     * @param containerClass   Binding class of the container in routes choice, must not be null.
     * @param listClass        Binding class of the route list, nust not be null;
     * @param afiClass         address Family Class
     * @param safiClass        SubsequentAddressFamily
     * @param destinationQname destination Qname
     */
    protected AbstractRIBSupportNonAddPath(final Class cazeClass, final Class containerClass, final Class listClass,
            final Class afiClass, final Class safiClass, final QName destinationQname) {
        super(cazeClass, containerClass, listClass, afiClass, safiClass, destinationQname);
    }

    @Override
    public final NodeIdentifierWithPredicates createRouteKeyPathArgument(final NodeIdentifierWithPredicates routeKey) {
        return routeKey;
    }

    @Override
    public final long extractPathId(@Nonnull R route) {
        return NON_PATH_ID_VALUE;
    }

    @Override
    public final N createNewRouteKey(@Nonnull long pathId, @Nonnull N routeKey) {
        return null;
    }
}
