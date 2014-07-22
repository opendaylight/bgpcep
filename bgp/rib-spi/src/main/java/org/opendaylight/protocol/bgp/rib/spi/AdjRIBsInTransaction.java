/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.Tables;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * An execution context for a single LocRib transaction.
 */
public interface AdjRIBsInTransaction {

    void setUptodate(InstanceIdentifier<Tables> basePath, boolean uptodate);
    <T extends DataObject> void advertise(final InstanceIdentifier<T> id, final T obj);
    void withdraw(final InstanceIdentifier<?> id);

}
