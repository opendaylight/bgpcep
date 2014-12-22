/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi;

import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

public interface AdjRIBsFactory {
    /*
     * Create an instance of route tables for the specified backend data store. Note that the
     * AFI/SAFI is encoded as the key of the instance identifier.
     *
     * @param basePath datastore identifier of local the table.
     */
    AdjRIBsIn<?, ?> createAdjRIBs(@Nonnull KeyedInstanceIdentifier<Tables, TablesKey> basePath);
}
