/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.opendaylight.protocol.bgp.parser.spi.PathIdUtil.NON_PATH_ID;

import org.junit.Test;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;

public class AddPathRibSupportTest {

    private static final NodeIdentifier QNAME = new NodeIdentifier(QName.create("test").intern());

    @Test
    public void defaultAddPathRibSupport() {
        final AddPathRibSupportLocalTest test = new AddPathRibSupportLocalTest();
        assertEquals((Long) NON_PATH_ID, test.extractPathId(null));
        assertNull(test.getRouteIdAddPath(NON_PATH_ID, null));
        assertEquals(QNAME, test.createRouteKeyPathArgument(QNAME));
    }

    private static class AddPathRibSupportLocalTest implements AddPathRibSupport {
    }
}
