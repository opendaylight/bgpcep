/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.openconfig.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.opendaylight.protocol.bgp.openconfig.impl.comparator.OpenConfigComparatorFactory;
import org.opendaylight.protocol.bgp.openconfig.impl.util.GlobalIdentifier;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.global.base.ConfigBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.Global;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.GlobalBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.openconfig.rev150718.OpenconfigBgp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.modules.ModuleKey;

public class BGPConfigHolderImplTest {
    private static final BGPConfigHolderImpl<Global> HOLDER = new BGPConfigHolderImpl<Global>(OpenConfigComparatorFactory.getComparator(Global.class));

    @Test
    public void test() {
        assertNull(HOLDER.getModuleKey(GlobalIdentifier.GLOBAL_IDENTIFIER));

        final ModuleKey moduleKey = new ModuleKey("key1", OpenconfigBgp.class);
        final ModuleKey moduleKey2 = new ModuleKey("key2", OpenconfigBgp.class);
        assertNull(HOLDER.getKey(moduleKey));

        final Global obj1 = new GlobalBuilder().build();
        final Global obj2 = new GlobalBuilder().setConfig(new ConfigBuilder().build()).build();
        assertTrue(HOLDER.addOrUpdate(moduleKey, GlobalIdentifier.GLOBAL_IDENTIFIER, obj1));
        assertFalse(HOLDER.addOrUpdate(moduleKey, GlobalIdentifier.GLOBAL_IDENTIFIER, obj1));
        assertEquals(moduleKey, HOLDER.getModuleKey(GlobalIdentifier.GLOBAL_IDENTIFIER));
        assertEquals(GlobalIdentifier.GLOBAL_IDENTIFIER, HOLDER.getKey(moduleKey));
        assertTrue(HOLDER.addOrUpdate(moduleKey, GlobalIdentifier.GLOBAL_IDENTIFIER, obj2));
        assertFalse(HOLDER.addOrUpdate(moduleKey, GlobalIdentifier.GLOBAL_IDENTIFIER, obj2));
        assertTrue(HOLDER.remove(moduleKey, obj2));
        assertFalse(HOLDER.remove(moduleKey2, obj1));
        assertTrue(HOLDER.addOrUpdate(moduleKey, GlobalIdentifier.GLOBAL_IDENTIFIER, obj2));
    }
}
