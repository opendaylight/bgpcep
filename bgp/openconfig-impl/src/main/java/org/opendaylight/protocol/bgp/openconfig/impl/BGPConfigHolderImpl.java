/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.impl;

import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.protocol.bgp.openconfig.impl.spi.BGPConfigHolder;
import org.opendaylight.protocol.bgp.openconfig.impl.spi.OpenConfigComparator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.modules.ModuleKey;
import org.opendaylight.yangtools.concepts.Identifier;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class BGPConfigHolderImpl<V extends DataObject> implements BGPConfigHolder<V> {

    private static final Logger LOG = LoggerFactory.getLogger(BGPConfigHolderImpl.class);

    @GuardedBy("this")
    private final BiMap<ModuleKey, Identifier> moduleToOpenConfig = HashBiMap.<ModuleKey, Identifier>create();

    @GuardedBy("this")
    private final Map<Identifier, V> bgpOpenConfigConfig = new HashMap<>();

    private final OpenConfigComparator<V> comparator;

    public BGPConfigHolderImpl(final OpenConfigComparator<V> comparator) {
        this.comparator = Preconditions.checkNotNull(comparator);
    }

    @Override
    public synchronized boolean remove(final ModuleKey moduleKey, final V removeValue) {
        final Identifier key = getKey(moduleKey);
        if (key != null && this.comparator.isSame(bgpOpenConfigConfig.get(key), removeValue)) {
            bgpOpenConfigConfig.remove(key);
            moduleToOpenConfig.remove(moduleKey);
            LOG.trace("Configuration removed: {}", moduleKey);
            return true;
        }
        return false;
    }

    @Override
    public synchronized boolean addOrUpdate(final ModuleKey moduleKey, final Identifier key, final V newValue) {
        if (!moduleToOpenConfig.containsKey(moduleKey)) {
            moduleToOpenConfig.put(moduleKey, key);
            bgpOpenConfigConfig.put(key, newValue);
            LOG.trace("Configuration added: {}", moduleKey);
            return true;
        } else if (!this.comparator.isSame(bgpOpenConfigConfig.get(key), newValue)) {
            bgpOpenConfigConfig.put(key, newValue);
            LOG.trace("Configuration updated: {}", moduleKey);
            return true;
        }
        return false;
    }

    @Override
    public ModuleKey getModuleKey(final Identifier key) {
        return this.moduleToOpenConfig.inverse().get(key);
    }

    @Override
    public Identifier getKey(final ModuleKey moduleKey) {
        return this.moduleToOpenConfig.get(moduleKey);
    }
}
