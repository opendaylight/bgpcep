/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.impl;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import java.util.concurrent.ConcurrentMap;
import javax.annotation.concurrent.ThreadSafe;
import org.opendaylight.protocol.bgp.openconfig.impl.spi.BGPConfigHolder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.modules.ModuleKey;

@ThreadSafe
final class BGPConfigHolderImpl<K, V> implements BGPConfigHolder<K, V> {

    private final BiMap<ModuleKey, K> moduleToOpenConfig = Maps.synchronizedBiMap(HashBiMap.<ModuleKey, K>create(1));
    private final ConcurrentMap<K, V> bgpOpenConfigConfig = Maps.newConcurrentMap();

    @Override
    public synchronized boolean remove(final ModuleKey moduleKey) {
        final K key = moduleToOpenConfig.get(moduleKey);
        if (key != null) {
            bgpOpenConfigConfig.remove(key);
            moduleToOpenConfig.remove(moduleKey);
            return true;
        }
        return false;
    }

    @Override
    public synchronized boolean addOrUpdate(final ModuleKey moduleKey, final K key, final V newValue) {
        if (!moduleToOpenConfig.containsKey(moduleKey)) {
            moduleToOpenConfig.put(moduleKey, key);
            bgpOpenConfigConfig.put(key, newValue);
            return true;
        } else if (!newValue.equals(bgpOpenConfigConfig.get(moduleKey))) {
            bgpOpenConfigConfig.put(key, newValue);
            return true;
        }
        return false;
    }

    @Override
    public synchronized ModuleKey getModuleKey(final K key) {
        return this.moduleToOpenConfig.inverse().get(key);
    }
}
