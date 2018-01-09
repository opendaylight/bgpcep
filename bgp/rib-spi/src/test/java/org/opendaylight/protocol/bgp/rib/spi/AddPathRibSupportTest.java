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

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171207.ipv4.routes.ipv4.routes.Ipv4Route;

public class AddPathRibSupportTest {

    private static final String PREFIX = "1.2.3.4/32";
    private static final String ROUTE_KEY = "prefix";
    private static final NodeIdentifierWithPredicates QNAME
            = new NodeIdentifierWithPredicates(Ipv4Route.QNAME,
            ImmutableMap.of(QName.create(Ipv4Route.QNAME, ROUTE_KEY).intern(), PREFIX));

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
