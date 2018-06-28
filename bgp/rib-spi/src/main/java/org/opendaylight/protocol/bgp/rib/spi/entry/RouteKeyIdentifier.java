/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.spi.entry;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.Route;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.Identifier;

/**
 * Best Path Route Key Identifiers.
 *
 * @param <R> Route Key Identifier for non add path route.
 * @param <I> Route Key Identifier for add path route.
 */
public interface RouteKeyIdentifier<R extends Route & Identifiable<I>,
        I extends Identifier<R>> {
    I getNonAddPathRouteKeyIdentifier();

    I getAddPathRouteKeyIdentifier();
}
