/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.spi;

import static org.junit.Assert.assertEquals;

import com.google.common.collect.ImmutableMap;
import org.junit.Ignore;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171207.ipv4.routes.ipv4.routes.Ipv4Route;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;

@Ignore
public class AdditionalPathUtilTest {
    private static final String ROUTE_KEY = "prefix";
    private static final String PREFIX = "1.2.3.4/32";
    private static final NodeIdentifierWithPredicates PREFIX_NII =
            new NodeIdentifierWithPredicates(Ipv4Route.QNAME,
                    ImmutableMap.of(QName.create(Ipv4Route.QNAME, ROUTE_KEY).intern(), PREFIX));

    @Test
    public void createRouteKeyPathArgument() {
        //FIXME
        assertEquals(PREFIX_NII, AdditionalPathUtil.createRouteKeyPathArgument(
                PREFIX_NII.getNodeType(),
                PREFIX_NII.getNodeType(),
                PREFIX_NII));
    }
}