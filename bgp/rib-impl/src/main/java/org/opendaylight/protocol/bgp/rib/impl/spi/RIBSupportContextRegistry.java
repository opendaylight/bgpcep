/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl.spi;

import javax.annotation.Nullable;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;

public interface RIBSupportContextRegistry {

    /**
     * Acquire a RIB Support for a AFI/SAFI combination.
     *
     * @param key AFI/SAFI key
     * @return RIBSupport instance, or null if the AFI/SAFI is
     * not implemented.
     */
    @Nullable
    RIBSupport getRIBSupport(TablesKey key);

    /**
     * Acquire a RIB Support Context for a AFI/SAFI combination.
     *
     * @param key AFI/SAFI key
     * @return RIBSupport instance, or null if the AFI/SAFI is
     * not implemented.
     */
    @Nullable
    RIBSupportContext getRIBSupportContext(TablesKey key);
}
