/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.impl.spi;

import javax.annotation.Nonnull;
import org.opendaylight.yangtools.yang.binding.DataObject;

/**
 * BGPConfigStateStore serves as store for BGP configuration holders.
 *
 */
public interface BGPConfigStateStore {

    /**
     * Creates instance of BGP configuration holder and store it for future use.
     * @param clazz the type BGP configuration holder
     * @throws NullPointerException when input parameters are null.
     */
    <T extends DataObject> void registerBGPConfigHolder(@Nonnull Class<T> clazz);

    /**
     * Retrieve BGPConfigHolder by input parameter class type.
     * @param clazz Type of BGPConfigHolder which the consumer wants to load from store.
     * @return BGPConfigHolder or null if holder for such type was not registered.
     * @throws NullPointerException when input parameter is null.
     */
    <T extends DataObject> BGPConfigHolder<T> getBGPConfigHolder(@Nonnull Class<T> clazz);

}
