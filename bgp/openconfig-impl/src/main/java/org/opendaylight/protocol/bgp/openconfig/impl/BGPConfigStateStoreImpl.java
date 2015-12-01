/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.impl;

import java.util.HashMap;
import java.util.Map;
import org.opendaylight.protocol.bgp.openconfig.impl.comparator.OpenConfigComparatorFactory;
import org.opendaylight.protocol.bgp.openconfig.impl.spi.BGPConfigHolder;
import org.opendaylight.protocol.bgp.openconfig.impl.spi.BGPConfigStateStore;
import org.opendaylight.yangtools.yang.binding.DataObject;

final class BGPConfigStateStoreImpl implements BGPConfigStateStore {

    final Map<Class<? extends DataObject>, BGPConfigHolder<? extends DataObject>> configHolderMap = new HashMap<>();

    @SuppressWarnings("unchecked")
    @Override
    public <T extends DataObject> BGPConfigHolder<T> getBGPConfigHolder(final Class<T> clazz) {
        return (BGPConfigHolder<T>) configHolderMap.get(clazz);
    }

    @Override
    public <T extends DataObject> void registerBGPConfigHolder(final Class<T> clazz) {
        configHolderMap.put(clazz, new BGPConfigHolderImpl<T>(OpenConfigComparatorFactory.getComparator(clazz)));
    }


}
