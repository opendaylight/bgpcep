/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.Tables;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * An execution context for a single LocRib transaction.
 */
@Deprecated
public interface AdjRIBsTransaction {

    BGPObjectComparator comparator();
    void setUptodate(InstanceIdentifier<Tables> basePath, boolean uptodate);
    <K, T extends Route> void advertise(RouteEncoder ribOut, K key, InstanceIdentifier<T> id, Peer peer, T obj);
    <K, T extends Route> void withdraw(RouteEncoder ribOut, K key, InstanceIdentifier<T> id);
}
