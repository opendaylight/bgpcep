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
import org.opendaylight.protocol.bgp.openconfig.impl.util.GlobalIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.openconfig.rev150718.OpenconfigBgp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.modules.ModuleKey;
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.opendaylight.yangtools.yang.binding.DataObject;

public class BGPConfigHolderImplTest {
    private static final BGPConfigHolderImpl<DataObject> HOLDER = new BGPConfigHolderImpl<DataObject>();

    @Test
    public void test() {
        assertNull(HOLDER.getModuleKey(GlobalIdentifier.GLOBAL_IDENTIFIER));

        final ModuleKey moduleKey = new ModuleKey("key1", OpenconfigBgp.class);
        final ModuleKey moduleKey2 = new ModuleKey("key2", OpenconfigBgp.class);
        assertNull(HOLDER.getKey(moduleKey));

        final MyDataObject obj1 = new MyDataObject();
        final MyDataObject obj2 = new MyDataObject();
        assertTrue(HOLDER.addOrUpdate(moduleKey, GlobalIdentifier.GLOBAL_IDENTIFIER, obj1));
        assertFalse(HOLDER.addOrUpdate(moduleKey, GlobalIdentifier.GLOBAL_IDENTIFIER, obj1));
        assertEquals(moduleKey, HOLDER.getModuleKey(GlobalIdentifier.GLOBAL_IDENTIFIER));
        assertEquals(GlobalIdentifier.GLOBAL_IDENTIFIER, HOLDER.getKey(moduleKey));
        assertTrue(HOLDER.addOrUpdate(moduleKey, GlobalIdentifier.GLOBAL_IDENTIFIER, obj2));
        assertFalse(HOLDER.addOrUpdate(moduleKey, GlobalIdentifier.GLOBAL_IDENTIFIER, obj2));
        assertTrue(HOLDER.remove(moduleKey));
        assertFalse(HOLDER.remove(moduleKey2));
        assertTrue(HOLDER.addOrUpdate(moduleKey, GlobalIdentifier.GLOBAL_IDENTIFIER, obj2));
    }

    private class MyDataObject implements DataObject {
        @Override
        public Class<? extends DataContainer> getImplementedInterface() {
            return null;
        }
    }
}
