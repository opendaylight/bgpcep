/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi.entry;

import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;

/**
 * Best Path Route Key Identifiers.
 */
public interface RouteKeyIdentifier {

    @NonNull NodeIdentifierWithPredicates getNonAddPathRouteKeyIdentifier();

    @NonNull NodeIdentifierWithPredicates getAddPathRouteKeyIdentifier();
}
