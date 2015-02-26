/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import javax.annotation.Nonnull;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;

// FIXME: migrate to bgp-rib-spi
public interface RIBSupport {
    /**
     * Return the table-type-specific empty routes container, as augmented into the
     * bgp-rib model under /rib/tables/routes choice node. This needs to include all
     * the skeleton nodes under which the individual routes will be stored.
     *
     * @return Protocol-specific case in the routes choice, may not be null.
     */
    @Nonnull ChoiceNode emptyRoutes();
}
