/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.openconfig.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.opendaylight.yangtools.yang.binding.DataObject;

public class BGPConfigStateStoreImplTest {

    private static final BGPConfigStateStoreImpl STORE = new BGPConfigStateStoreImpl();
    private static final BGPConfigHolderImpl<MyDataObject> HOLDER = new BGPConfigHolderImpl<MyDataObject>();

    @Test
    public void test() {
        assertNull(STORE.getBGPConfigHolder(MyDataObject.class));
        STORE.registerBGPConfigHolder(MyDataObject.class, HOLDER);
        assertEquals(HOLDER, STORE.getBGPConfigHolder(MyDataObject.class));
    }

    private class MyDataObject implements DataObject {
        @Override
        public Class<? extends DataContainer> getImplementedInterface() {
            return null;
        }
    }

}
